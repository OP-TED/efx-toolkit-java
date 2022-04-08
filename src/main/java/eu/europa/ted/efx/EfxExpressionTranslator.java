package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.efx.EfxParser.CodeListContext;
import eu.europa.ted.efx.EfxParser.CodelistReferenceContext;
import eu.europa.ted.efx.EfxParser.ExplicitListContext;
import eu.europa.ted.efx.EfxParser.NodeReferenceWithPredicateContext;
import eu.europa.ted.efx.EfxParser.ParenthesizedExpressionContext;
import eu.europa.ted.efx.EfxParser.SimpleNodeReferenceContext;
import eu.europa.ted.efx.EfxParser.SingleExpressionContext;
import eu.europa.ted.efx.interfaces.SymbolMap;
import eu.europa.ted.efx.interfaces.SyntaxMap;

public class EfxExpressionTranslator extends EfxBaseListener {

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

    protected final SyntaxMap syntax;

    /**
     * The context stack is used to keep track of xPath contexts for nested conditions.
     */
    protected final ContextStack efxContext;

    public EfxExpressionTranslator(final SymbolMap symbols, final SyntaxMap syntax) {
        this.symbols = symbols;
        this.syntax = syntax;
        this.efxContext = new ContextStack(symbols);
    }

    public static String transpileExpression(final String context, final String expression,
            final SymbolMap symbols, final SyntaxMap syntax,
            final BaseErrorListener errorListener) {
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
        final EfxExpressionTranslator translator = new EfxExpressionTranslator(symbols, syntax);

        walker.walk(translator, tree);

        return translator.getTranspiledXPath();
    }

    public static String transpileExpression(final String context, final String expression,
            final SymbolMap symbols, final SyntaxMap syntax) {
        return transpileExpression(context, expression, symbols, syntax, null);
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
        final TerminalNode fieldContext = ctx.FieldContext();
        if (fieldContext != null) {
            this.efxContext.pushFieldContext(fieldContext.getText());
        } else {
            final TerminalNode nodeContext = ctx.NodeContext();
            if (nodeContext != null) {
                this.efxContext.pushNodeContext(nodeContext.getText());
            }
        }
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
        this.stack.push(this.syntax.mapLogicalAnd(left, right));
    }

    @Override
    public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapLogicalOr(left, right));
    }

    @Override
    public void exitLogicalNotCondition(EfxParser.LogicalNotConditionContext ctx) {
        String condition = this.stack.pop();
        this.stack.push(this.syntax.mapLogicalNot(condition));
    }

    @Override
    public void exitAlwaysCondition(EfxParser.AlwaysConditionContext ctx) {
        this.stack.push(this.syntax.mapBoolean(true));
    }

    @Override
    public void exitNeverCondition(EfxParser.NeverConditionContext ctx) {
        this.stack.push(this.syntax.mapBoolean(false));
    }

    @Override
    public void exitComparisonCondition(EfxParser.ComparisonConditionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
        String expression = this.stack.pop();
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals("not") ? "!=" : "==";
        this.stack.push(this.syntax.mapOperator(expression, operator, "''"));
    }

    @Override
    public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
        String reference = this.stack.pop();
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals("not") ? "==" : "!=";
        this.stack.push(this.syntax.mapOperator(reference, operator, "''"));
    }

    @Override
    public void exitParenthesizedCondition(EfxParser.ParenthesizedConditionContext ctx) {
        String condition = this.stack.pop();
        this.stack.push(this.syntax.mapParenthesizedExpression(condition));
    }

    @Override
    public void exitExpressionCondition(EfxParser.ExpressionConditionContext ctx) {
        String expression = this.stack.pop();
        this.stack.push(expression);
    }

    @Override
    public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
        String expression = this.stack.pop();

        String condition =
                this.syntax.mapMatchesPatternCondition(expression, ctx.pattern.getText());
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            condition = this.syntax.mapLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    @Override
    public void exitInListCondition(EfxParser.InListConditionContext ctx) {
        String list = this.stack.pop();
        String expression = this.stack.pop();
        String condition = this.syntax.mapInListCondition(expression, list);
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            condition = this.syntax.mapLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    @Override
    public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitParenthesizedExpression(ParenthesizedExpressionContext ctx) {
        String expression = this.stack.pop();
        this.stack.push(this.syntax.mapParenthesizedExpression(expression));
    }

    @Override
    public void exitCodeList(CodeListContext ctx) {
        if (this.stack.empty()) {
            this.stack.push(this.syntax.mapList(Collections.emptyList()));
            return;
        }
    }

    @Override
    public void exitExplicitList(ExplicitListContext ctx) {
        if (this.stack.empty() || ctx.value().size() == 0) {
            this.stack.push(this.syntax.mapList(Collections.emptyList()));
            return;
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < ctx.value().size(); i++) {
            list.add(0, this.stack.pop());
        }
        this.stack.push(this.syntax.mapList(list));
    }

    @Override
    public void exitLiteral(EfxParser.LiteralContext ctx) {
        this.stack.push(this.syntax.mapLiteral(ctx.getText()));
    }

    @Override
    public void exitFunctionCall(EfxParser.FunctionCallContext ctx) {
        final List<String> arguments = ctx.arguments() == null ? Collections.emptyList()
                : ctx.arguments().argument().stream().map(c -> c.getText())
                        .collect(Collectors.toList());
        this.stack.push(this.syntax.mapFunctionCall(ctx.FunctionName().getText(), arguments));
    }

    @Override
    public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
        this.stack.push(this.syntax.mapExternalReference(this.stack.pop()));
    }

    @Override
    public void exitNodeReferenceWithPredicate(NodeReferenceWithPredicateContext ctx) {
        String predicate = this.stack.pop();
        String nodeReference = this.stack.pop();
        this.stack.push(this.syntax.mapNodeReferenceWithPredicate(nodeReference, predicate));
    }

    @Override
    public void exitSimpleNodeReference(SimpleNodeReferenceContext ctx) {
        this.stack.push(this.symbols.relativeXpathOfNode(ctx.NodeId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void exitFieldInNoticeReference(EfxParser.FieldInNoticeReferenceContext ctx) {
        String field = this.stack.pop();
        String notice = this.stack.pop();
        this.stack.push(this.syntax.mapFieldInExternalReference(notice, field));
    }

    @Override
    public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
        this.stack.push(symbols.relativeXpathOfField(ctx.FieldId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
        String predicate = this.stack.pop();
        String fieldReference = this.stack.pop();
        this.stack.push(this.syntax.mapFieldReferenceWithPredicate(fieldReference, predicate));
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
        this.efxContext.pushFieldContext(refCtx.FieldId().getText());
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
        this.efxContext.pushFieldContext(ctx.ctx.getText());
    }

    @Override
    public void exitReferenceWithContextOverride(
            EfxParser.ReferenceWithContextOverrideContext ctx) {
        this.efxContext.pop();
    }

    @Override
    public void exitSimpleReference(EfxParser.SimpleReferenceContext ctx) {
        String field = this.stack.pop();
        if (ctx.attribute != null) {
            this.stack.push(this.syntax.mapAttributeReference(field, ctx.attribute.getText()));
        } else {
            this.stack.push(this.syntax.mapFieldTextReference(field));
        }
    }

    @Override
    public void exitCodelistReference(CodelistReferenceContext ctx) {
        this.stack.push(this.syntax.mapList(this.symbols.expandCodelist(ctx.codeListId.getText())));
    }
}
