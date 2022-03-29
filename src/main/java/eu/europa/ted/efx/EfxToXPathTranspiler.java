package eu.europa.ted.efx;

import java.util.Stack;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.EfxParser.CodeListContext;
import eu.europa.ted.efx.EfxParser.CodelistReferenceContext;
import eu.europa.ted.efx.EfxParser.ExplicitListContext;
import eu.europa.ted.efx.EfxParser.ParenthesizedExpressionContext;
import eu.europa.ted.efx.EfxParser.SingleExpressionContext;
import eu.europa.ted.efx.interfaces.SymbolMap;

public class EfxToXPathTranspiler extends EfxBaseListener {

    static final boolean debug = true;

    /**
     * The stack is used by the methods of this listener to pass data to each other as the parse
     * tree is being walked.
     */
    protected Stack<String> stack = new Stack<>();

    /**
     * Symbols are field identifiers and node identifiers. The symbols map is used to resolve these
     * identifiers to their xPath.
     */
    protected final SymbolMap symbols;

    /**
     * The context stack is used to keep track of xPath contexts for nested conditions.
     */
    protected Stack<String> efxContext = new Stack<>();

    public EfxToXPathTranspiler(final SymbolMap symbols) {
        this.symbols = symbols;
    }

    public static String transpileExpression(final String context, final String expression,
            final SymbolMap symbols, final BaseErrorListener errorListener) {
        final EfxLexer lexer = new EfxLexer(
                CharStreams.fromString(String.format("%s::${%s}", context, expression)));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final EfxParser parser = new EfxParser(tokens);

        if (errorListener != null) {
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);
        }

        final ParseTree tree = parser.singleExpression();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final EfxToXPathTranspiler translator = new EfxToXPathTranspiler(symbols);

        walker.walk(translator, tree);

        return translator.getTranspiledXPath();
    }

    public static String transpileExpression(final String context, final String expression, final SymbolMap symbols) {
        return transpileExpression(context, expression, symbols, null);
    }
    
    /**
     * Call this method to get the translated code after the walker finished its walk.
     *
     * @return The translated code, trimmed
     */
    private String getTranspiledXPath() {
        final StringBuilder sb = new StringBuilder(this.stack.size() * 10);
        while (!this.stack.empty()) {
            sb.insert(0, '\n').insert(0, this.stack.pop());
        }
        return sb.toString().trim();
    }

    @Override
    public void enterSingleExpression(SingleExpressionContext ctx) {
        this.efxContext.push(this.symbols.contextPathOfField(ctx.Context().getText()));
    }

    @Override
    public void exitSingleExpression(SingleExpressionContext ctx) {
        this.efxContext.pop();
        if (debug) {
            System.out.println("[In]: " + ctx.getText());
            System.out.println("[Out]: " + this.stack.peek());
            System.out.println();
        }
    }

    @Override
    public void exitLogicalAndCondition(EfxParser.LogicalAndConditionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(left + " " + this.symbols.mapOperator("and") + " " + right);
    }

    @Override
    public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(left + " " + this.symbols.mapOperator("or") + " " + right);
    }

    @Override
    public void exitLogicalNotCondition(EfxParser.LogicalNotConditionContext ctx) {
        String condition = this.stack.pop();
        this.stack.push(this.symbols.mapOperator("not") + condition);
    }

    @Override
    public void exitAlwaysCondition(EfxParser.AlwaysConditionContext ctx) {
        this.stack.push("true");
    }

    @Override
    public void exitNeverCondition(EfxParser.NeverConditionContext ctx) {
        this.stack.push("false");
    }

    @Override
    public void exitComparisonCondition(EfxParser.ComparisonConditionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(left + this.symbols.mapOperator(ctx.operator.getText()) + right);
    }

    @Override
    public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
        String expression = this.stack.pop();
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals("not") ? "!=" : "==";
        this.stack.push(expression + " " + this.symbols.mapOperator(operator) + " ''");
    }

    @Override
    public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
        String reference = this.stack.pop();
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals("not") ? "==" : "!=";
        this.stack.push(reference + " " + this.symbols.mapOperator(operator) + " ''");
    }

    @Override
    public void exitParenthesizedCondition(EfxParser.ParenthesizedConditionContext ctx) {
        String condition = this.stack.pop();
        this.stack.push('(' + condition + ')');
    }

    @Override
    public void exitExpressionCondition(EfxParser.ExpressionConditionContext ctx) {
        String expression = this.stack.pop();
        this.stack.push(expression);
    }

    @Override
    public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
        String expression = this.stack.pop();
        boolean hasNot = ctx.modifier != null && ctx.modifier.getText().equals("not");
        String not = hasNot ? "not(" : "";
        String endNot = hasNot ? ")" : "";
        this.stack.push(
                not + "fn:matches(" + expression + ", " + ctx.pattern.getText() + ")" + endNot);
    }

    @Override
    public void exitInListCondition(EfxParser.InListConditionContext ctx) {
        String list = this.stack.pop();
        String expression = this.stack.pop();
        boolean hasNot = ctx.modifier != null && ctx.modifier.getText().equals("not");
        String not = hasNot ? "not(" : "";
        String endNot = hasNot ? ")" : "";
        this.stack.push(not + expression + "=" + list + endNot);
    }

    @Override
    public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(left + this.symbols.mapOperator(ctx.operator.getText()) + right);
    }

    @Override
    public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(left + this.symbols.mapOperator(ctx.operator.getText()) + right);
    }

    @Override
    public void exitParenthesizedExpression(ParenthesizedExpressionContext ctx) {
        String expression = this.stack.pop();
        this.stack.push('(' + expression + ')');
    }

    @Override
    public void exitCodeList(CodeListContext ctx) {
        if (this.stack.empty()) {
            this.stack.push("()");
            return;
        }
    }

    @Override
    public void exitExplicitList(ExplicitListContext ctx) {
        if (this.stack.empty() || ctx.value().size() == 0) {
            this.stack.push("()");
            return;
        }

        String list = this.stack.pop() + ")";
        for (int i = 1; i < ctx.value().size(); i++) {
            list = this.stack.pop() + ", " + list;
        }
        list = "(" + list;
        this.stack.push(list);
    }

    @Override
    public void exitLiteral(EfxParser.LiteralContext ctx) {
        this.stack.push(ctx.getText());
    }

    @Override
    public void exitFunctionCall(EfxParser.FunctionCallContext ctx) {
        this.stack.push(ctx.getText());
    }

    @Override
    public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
        this.stack.push("fn:doc('http://notice.service/" + this.stack.pop() + "')");
    }

    @Override
    public void exitNodeReference(EfxParser.NodeReferenceContext ctx) {
        this.stack.push(
                symbols.relativeXpathOfNode(ctx.node.getText(), this.efxContext.peek()));
    }

    @Override
    public void exitFieldInNoticeReference(EfxParser.FieldInNoticeReferenceContext ctx) {
        String field = this.stack.pop();
        String notice = this.stack.pop();
        this.stack.push(notice + "/" + field);
    }

    @Override
    public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
        this.stack.push(
                symbols.relativeXpathOfField(ctx.field.getText(), this.efxContext.peek()));
    }

    @Override
    public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
        String condition = this.stack.pop();
        String fieldRef = this.stack.pop();
        this.stack.push(fieldRef + '[' + condition + ']');
    }

    /**
     * Any field references in the predicate must be resolved relative to the field on which the
     * predicate is applied. Therefore we need to switch to the field's context while the predicate
     * is being parsed.
     */
    @Override
    public void enterPredicate(EfxParser.PredicateContext ctx) {
        EfxParser.SimpleFieldReferenceContext refCtx =
                ctx.getParent().getChild(EfxParser.SimpleFieldReferenceContext.class, 0);
        this.efxContext.push(
                symbols.contextPathOfField(refCtx.field.getText(), this.efxContext.peek()));
    }

    /**
     * After the predicate is parsed we need to switch back to the previous context.
     */
    @Override
    public void exitPredicate(EfxParser.PredicateContext ctx) {
        this.efxContext.pop();
    }

    @Override
    public void enterReferenceWithContextOverride(
            EfxParser.ReferenceWithContextOverrideContext ctx) {
        this.efxContext
                .push(symbols.contextPathOfField(ctx.ctx.getText(), this.efxContext.peek()));
    }

    @Override
    public void exitReferenceWithContextOverride(
            EfxParser.ReferenceWithContextOverrideContext ctx) {
        this.efxContext.pop();
    }

    @Override
    public void exitSimpleReference(EfxParser.SimpleReferenceContext ctx) {
        String field = this.stack.pop();
        String attribute =
                ctx.attribute != null ? "/@" + ctx.attribute.getText() : "/normalize-space(text())";
        this.stack.push(field + attribute);
    }

    @Override
    public void exitCodelistReference(CodelistReferenceContext ctx) {
        this.stack.push(this.symbols.expandCodelist(ctx.codeListId.getText()));
    }
}
