package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.efx.EfxParser.BooleanAttributeReferenceContext;
import eu.europa.ted.efx.EfxParser.BooleanComparisonContext;
import eu.europa.ted.efx.EfxParser.BooleanFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.CodeListContext;
import eu.europa.ted.efx.EfxParser.CodelistReferenceContext;
import eu.europa.ted.efx.EfxParser.ConcatFunctionContext;
import eu.europa.ted.efx.EfxParser.ContainsFunctionContext;
import eu.europa.ted.efx.EfxParser.CountFunctionContext;
import eu.europa.ted.efx.EfxParser.DateAttributeReferenceContext;
import eu.europa.ted.efx.EfxParser.DateComparisonContext;
import eu.europa.ted.efx.EfxParser.DateFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.DateFromStringFunctionContext;
import eu.europa.ted.efx.EfxParser.DurationAttributeReferenceContext;
import eu.europa.ted.efx.EfxParser.DurationComparisonContext;
import eu.europa.ted.efx.EfxParser.DurationFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.DurationFromDatesFunctionContext;
import eu.europa.ted.efx.EfxParser.EndsWithFunctionContext;
import eu.europa.ted.efx.EfxParser.ExplicitListContext;
import eu.europa.ted.efx.EfxParser.FalseBooleanLiteralContext;
import eu.europa.ted.efx.EfxParser.FormatNumberFunctionContext;
import eu.europa.ted.efx.EfxParser.NodeReferenceWithPredicateContext;
import eu.europa.ted.efx.EfxParser.NotFunctionContext;
import eu.europa.ted.efx.EfxParser.NumberFunctionContext;
import eu.europa.ted.efx.EfxParser.NumericAttributeReferenceContext;
import eu.europa.ted.efx.EfxParser.NumericComparisonContext;
import eu.europa.ted.efx.EfxParser.NumericFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.NumericLiteralContext;
import eu.europa.ted.efx.EfxParser.ParenthesizedNumericExpressionContext;
import eu.europa.ted.efx.EfxParser.SimpleNodeReferenceContext;
import eu.europa.ted.efx.EfxParser.SingleExpressionContext;
import eu.europa.ted.efx.EfxParser.StartsWithFunctionContext;
import eu.europa.ted.efx.EfxParser.StringComparisonContext;
import eu.europa.ted.efx.EfxParser.StringLengthFunctionContext;
import eu.europa.ted.efx.EfxParser.StringLiteralContext;
import eu.europa.ted.efx.EfxParser.SubstringFunctionContext;
import eu.europa.ted.efx.EfxParser.SumFunctionContext;
import eu.europa.ted.efx.EfxParser.TextAttributeReferenceContext;
import eu.europa.ted.efx.EfxParser.TextFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.TimeAttributeReferenceContext;
import eu.europa.ted.efx.EfxParser.TimeComparisonContext;
import eu.europa.ted.efx.EfxParser.TimeFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.TimeFromStringFunctionContext;
import eu.europa.ted.efx.EfxParser.ToStringFunctionContext;
import eu.europa.ted.efx.EfxParser.TrueBooleanLiteralContext;
import eu.europa.ted.efx.interfaces.SymbolMap;
import eu.europa.ted.efx.interfaces.SyntaxMap;
import eu.europa.ted.efx.model.ContextStack;

public class EfxExpressionTranslator extends EfxBaseListener {

    static final boolean debug = false;

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

    /*** Boolean expressions ***/

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
    public void exitStringComparison(StringComparisonContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitNumericComparison(NumericComparisonContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitBooleanComparison(BooleanComparisonContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDateComparison(DateComparisonContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitTimeComparison(TimeComparisonContext ctx) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(this.syntax.mapOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDurationComparison(DurationComparisonContext ctx) {
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
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            this.stack.push(this.syntax.mapLogicalNot(this.syntax.mapExistsExpression(reference)));
        } else {
            this.stack.push(this.syntax.mapExistsExpression(reference));
        }
    }

    @Override
    public void exitParenthesizedBooleanExpression(
            EfxParser.ParenthesizedBooleanExpressionContext ctx) {
        this.stack.push(this.syntax.mapParenthesizedExpression(this.stack.pop()));
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

    /*** Numeric expreessions ***/

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
    public void exitParenthesizedNumericExpression(ParenthesizedNumericExpressionContext ctx) {
        this.stack.push(this.syntax.mapParenthesizedExpression(this.stack.pop()));
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
        if (this.stack.empty() || ctx.expression().size() == 0) {
            this.stack.push(this.syntax.mapList(Collections.emptyList()));
            return;
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < ctx.expression().size(); i++) {
            list.add(0, this.stack.pop());
        }
        this.stack.push(this.syntax.mapList(list));
    }

    @Override
    public void exitNumericLiteral(NumericLiteralContext ctx) {
        this.stack.push(this.syntax.mapLiteral(ctx.getText()));
    }

    @Override
    public void exitStringLiteral(StringLiteralContext ctx) {
        this.stack.push(this.syntax.mapLiteral(ctx.getText()));
    }

    @Override
    public void exitTrueBooleanLiteral(TrueBooleanLiteralContext ctx) {
        this.stack.push(this.syntax.mapBoolean(true));
    }

    @Override
    public void exitFalseBooleanLiteral(FalseBooleanLiteralContext ctx) {
        this.stack.push(this.syntax.mapBoolean(false));
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

    @Override
    public void exitTextFieldReference(TextFieldReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop()));
    }

    @Override
    public void exitTextAttributeReference(TextAttributeReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldAttributeReference(this.stack.pop(),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitNumericFieldReference(NumericFieldReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop()));
    }

    @Override
    public void exitNumericAttributeReference(NumericAttributeReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldAttributeReference(this.stack.pop(),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitBooleanFieldReference(BooleanFieldReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop()));
    }

    @Override
    public void exitBooleanAttributeReference(BooleanAttributeReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldAttributeReference(this.stack.pop(),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitDateFieldReference(DateFieldReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop()));
    }

    @Override
    public void exitDateAttributeReference(DateAttributeReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldAttributeReference(this.stack.pop(),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitTimeFieldReference(TimeFieldReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop()));
    }

    @Override
    public void exitTimeAttributeReference(TimeAttributeReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldAttributeReference(this.stack.pop(),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitDurationFieldReference(DurationFieldReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop()));
    }

    @Override
    public void exitDurationAttributeReference(DurationAttributeReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldAttributeReference(this.stack.pop(),
                ctx.Identifier().getText()));
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
        this.efxContext.pushFieldContextForPredicate(refCtx.FieldId().getText());
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
    public void exitCodelistReference(CodelistReferenceContext ctx) {
        this.stack.push(this.syntax.mapList(this.symbols.expandCodelist(ctx.codeListId.getText())));
    }



    /*** Boolean functions ***/

    @Override
    public void exitNotFunction(NotFunctionContext ctx) {
        this.stack.push(this.syntax.mapLogicalNot(this.stack.pop()));
    }

    @Override
    public void exitContainsFunction(ContainsFunctionContext ctx) {
        final String needle = this.stack.pop();
        final String haystack = this.stack.pop();
        this.stack.push(this.syntax.mapStringContainsFunction(haystack, needle));
    }

    @Override
    public void exitStartsWithFunction(StartsWithFunctionContext ctx) {
        final String startsWith = this.stack.pop();
        final String text = this.stack.pop();
        this.stack.push(this.syntax.mapStringStartsWithFunction(text, startsWith));
    }

    @Override
    public void exitEndsWithFunction(EndsWithFunctionContext ctx) {
        final String endsWith = this.stack.pop();
        final String text = this.stack.pop();
        this.stack.push(this.syntax.mapStringEndsWithFunction(text, endsWith));
    }


    /*** Numeric functions ***/

    @Override
    public void exitCountFunction(CountFunctionContext ctx) {
        this.stack.push(this.syntax.mapCountFunction(this.stack.pop()));
    }

    @Override
    public void exitNumberFunction(NumberFunctionContext ctx) {
        this.stack.push(this.syntax.mapToNumberFunction(this.stack.pop()));
    }

    @Override
    public void exitSumFunction(SumFunctionContext ctx) {
        this.stack.push(this.syntax.mapSumFunction(this.stack.pop()));
    }

    @Override
    public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
        this.stack.push(this.syntax.mapStringLengthFunction(this.stack.pop()));
    }


    /*** String functions ***/

    @Override
    public void exitSubstringFunction(SubstringFunctionContext ctx) {
        final String length = ctx.length != null ? this.stack.pop() : null;
        final String start = this.stack.pop();
        final String text = this.stack.pop();
        if (length != null) {
            this.stack.push(this.syntax.mapSubstringFunction(text, start, length));
        } else {
            this.stack.push(this.syntax.mapSubstringFunction(text, start));
        }
    }

    @Override
    public void exitToStringFunction(ToStringFunctionContext ctx) {
        this.stack.push(this.syntax.mapNumberToStringFunction(this.stack.pop()));
    }

    @Override
    public void exitConcatFunction(ConcatFunctionContext ctx) {
        if (this.stack.empty() || ctx.stringExpression().size() == 0) {
            this.stack.push(this.syntax.mapStringConcatenationFunction(Collections.emptyList()));
            return;
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < ctx.stringExpression().size(); i++) {
            list.add(0, this.stack.pop());
        }
        this.stack.push(this.syntax.mapStringConcatenationFunction(list));
    }

    @Override
    public void exitFormatNumberFunction(FormatNumberFunctionContext ctx) {
        final String format = this.stack.pop();
        final String number = this.stack.pop();
        this.stack.push(this.syntax.mapFormatNumberFunction(number, format));
    }

    /*** Date functions ***/

    @Override
    public void exitDateFromStringFunction(DateFromStringFunctionContext ctx) {
        this.stack.push(this.syntax.mapDateFromStringFunction(this.stack.pop()));
    }

    /*** Time functions ***/

    @Override
    public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
        this.stack.push(this.syntax.mapTimeFromStringFunction(this.stack.pop()));
    }

    /*** Duration functions ***/

    @Override
    public void exitDurationFromDatesFunction(DurationFromDatesFunctionContext ctx) {
        final String endDate = this.stack.pop();
        final String startDate = this.stack.pop();
        this.stack.push(this.syntax.mapDurationFromDatesFunction(startDate, endDate));
    }
}
