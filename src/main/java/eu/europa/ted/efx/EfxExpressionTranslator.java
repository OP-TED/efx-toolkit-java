package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.efx.EfxParser.BooleanComparisonContext;
import eu.europa.ted.efx.EfxParser.CodeListContext;
import eu.europa.ted.efx.EfxParser.CodelistReferenceContext;
import eu.europa.ted.efx.EfxParser.ConcatFunctionContext;
import eu.europa.ted.efx.EfxParser.ContainsFunctionContext;
import eu.europa.ted.efx.EfxParser.CountFunctionContext;
import eu.europa.ted.efx.EfxParser.DateComparisonContext;
import eu.europa.ted.efx.EfxParser.DateFromStringFunctionContext;
import eu.europa.ted.efx.EfxParser.DurationComparisonContext;
import eu.europa.ted.efx.EfxParser.DurationFromDatesFunctionContext;
import eu.europa.ted.efx.EfxParser.EndsWithFunctionContext;
import eu.europa.ted.efx.EfxParser.ExplicitListContext;
import eu.europa.ted.efx.EfxParser.FalseBooleanLiteralContext;
import eu.europa.ted.efx.EfxParser.FieldValueComparisonContext;
import eu.europa.ted.efx.EfxParser.FormatNumberFunctionContext;
import eu.europa.ted.efx.EfxParser.NodeReferenceWithPredicateContext;
import eu.europa.ted.efx.EfxParser.NotFunctionContext;
import eu.europa.ted.efx.EfxParser.NumberFunctionContext;
import eu.europa.ted.efx.EfxParser.NumericComparisonContext;
import eu.europa.ted.efx.EfxParser.NumericLiteralContext;
import eu.europa.ted.efx.EfxParser.ParenthesizedNumericExpressionContext;
import eu.europa.ted.efx.EfxParser.SimpleFieldReferenceContext;
import eu.europa.ted.efx.EfxParser.SimpleNodeReferenceContext;
import eu.europa.ted.efx.EfxParser.SingleExpressionContext;
import eu.europa.ted.efx.EfxParser.StartsWithFunctionContext;
import eu.europa.ted.efx.EfxParser.StringComparisonContext;
import eu.europa.ted.efx.EfxParser.StringLengthFunctionContext;
import eu.europa.ted.efx.EfxParser.StringLiteralContext;
import eu.europa.ted.efx.EfxParser.SubstringFunctionContext;
import eu.europa.ted.efx.EfxParser.SumFunctionContext;
import eu.europa.ted.efx.EfxParser.TimeComparisonContext;
import eu.europa.ted.efx.EfxParser.TimeFromStringFunctionContext;
import eu.europa.ted.efx.EfxParser.ToStringFunctionContext;
import eu.europa.ted.efx.EfxParser.TrueBooleanLiteralContext;
import eu.europa.ted.efx.EfxParser.UntypedAttributeValueReferenceContext;
import eu.europa.ted.efx.EfxParser.UntypedFieldValueReferenceContext;
import eu.europa.ted.efx.interfaces.SymbolMap;
import eu.europa.ted.efx.interfaces.SyntaxMap;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;

/**
 * The the goal of the EfxExpressionTranslator is to take an EFX expression and translate it to a
 * target scripting language.
 * 
 * The target language syntax is not hadcoded into the translator so that this class can be reused
 * to translate to several different languages.
 * 
 * Appart from writing expressions that can be translated and evaluated in a target scripting
 * language (e.g. XPath/XQuery, JavaScript etc.), EFX also allows the definition of templates that
 * can be traslated to a target template markup language (e.g. XSLT, Thymeleaf etc.). The
 * {@link EfxExpressionTranslator} only focuses on EFX expressions. To translate EFX templates you
 * need to use the {@link EfxTemplateTranslator} which derrives from this class.
 */
public class EfxExpressionTranslator extends EfxBaseListener {

    static final boolean debug = false;

    /**
     * The stack is used by the methods of this listener to pass data to each other as the parse
     * tree is being walked.
     */
    protected CallStack stack = new CallStack();

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
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);
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
            sb.insert(0, '\n').insert(0, this.stack.pop(Expression.class).script);
        }
        return sb.toString().trim();
    }

    /**
     * Helper method that starts from a given {@link ParserRuleContext} and recursivelly searches
     * for a {@link SimpleFieldReferenceContext} to locate a field identifier.
     */
    protected static String getFieldIdFromChildSimpleFieldReferenceContext(ParserRuleContext ctx) {

        SimpleFieldReferenceContext fieldReferenceContext =
                ctx.getChild(SimpleFieldReferenceContext.class, 0);
        if (fieldReferenceContext != null) {
            return fieldReferenceContext.FieldId().getText();
        }

        for (ParseTree child : ctx.children) {
            if (child instanceof ParserRuleContext) {
                String fieldId =
                        getFieldIdFromChildSimpleFieldReferenceContext((ParserRuleContext) child);
                if (fieldId != null) {
                    return fieldId;
                }
            }
        }

        return null;
    }

    /**
     * Helper method that starts from a given {@link ParserRuleContext} and recursivelly searches
     * for a {@link SimpleNodeReferenceContext} to locate a field identifier.
     */
    protected static String getNodeIdFromChildSimpleNodeReferenceContext(ParserRuleContext ctx) {

        SimpleNodeReferenceContext nodeReferenceContext =
                ctx.getChild(SimpleNodeReferenceContext.class, 0);
        if (nodeReferenceContext != null) {
            return nodeReferenceContext.NodeId().getText();
        }

        for (ParseTree child : ctx.children) {
            if (child instanceof ParserRuleContext) {
                String nodeId =
                        getNodeIdFromChildSimpleNodeReferenceContext((ParserRuleContext) child);
                if (nodeId != null) {
                    return nodeId;
                }
            }
        }

        return null;
    }

    @Override
    public void enterSingleExpression(SingleExpressionContext ctx) {
        final TerminalNode fieldContext = ctx.FieldContext();
        if (fieldContext != null) {
            // In this case we want the context to be that of the specified field's parent node.
            // This is a temporary workaround for the fact that the current version of the MDC
            // passes the field identifier as the context when evaluating conditions for schematron
            // generation.
            // TODO: we should fix that in the MDC and revert the next line to push the field's
            // context when a field is specified as a context of the expression.
            // see TEDEFO-852
            this.efxContext.pushNodeContext(symbols.parentNodeOfField(fieldContext.getText()));
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
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.syntax.mapLogicalAnd(left, right));
    }

    @Override
    public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.syntax.mapLogicalOr(left, right));
    }

    @Override
    public void exitFieldValueComparison(FieldValueComparisonContext ctx) {
        Expression right = this.stack.pop(Expression.class);
        Expression left = this.stack.pop(Expression.class);
        if (!left.getClass().equals(right.getClass())) {
            throw new InputMismatchException(
                    "Type mismatch. Cannot compare values of different types: " + left.getClass()
                            + " and " + right.getClass());
        }
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitStringComparison(StringComparisonContext ctx) {
        StringExpression right = this.stack.pop(StringExpression.class);
        StringExpression left = this.stack.pop(StringExpression.class);
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitNumericComparison(NumericComparisonContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitBooleanComparison(BooleanComparisonContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDateComparison(DateComparisonContext ctx) {
        DateExpression right = this.stack.pop(DateExpression.class);
        DateExpression left = this.stack.pop(DateExpression.class);
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitTimeComparison(TimeComparisonContext ctx) {
        TimeExpression right = this.stack.pop(TimeExpression.class);
        TimeExpression left = this.stack.pop(TimeExpression.class);
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDurationComparison(DurationComparisonContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DurationExpression left = this.stack.pop(DurationExpression.class);
        this.stack.push(this.syntax.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
        StringExpression expression = this.stack.pop(StringExpression.class);
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals("not") ? "!=" : "==";
        this.stack.push(
                this.syntax.mapComparisonOperator(expression, operator, this.syntax.mapString("")));
    }

    @Override
    public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
        PathExpression reference = this.stack.pop(PathExpression.class);
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            this.stack.push(this.syntax.mapLogicalNot(this.syntax.mapExistsExpression(reference)));
        } else {
            this.stack.push(this.syntax.mapExistsExpression(reference));
        }
    }

    @Override
    public void exitParenthesizedBooleanExpression(
            EfxParser.ParenthesizedBooleanExpressionContext ctx) {
        this.stack.push(this.syntax.mapParenthesizedExpression(
                this.stack.pop(BooleanExpression.class), BooleanExpression.class));
    }

    @Override
    public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
        StringExpression expression = this.stack.pop(StringExpression.class);

        BooleanExpression condition =
                this.syntax.mapMatchesPatternCondition(expression, ctx.pattern.getText());
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            condition = this.syntax.mapLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    @Override
    public void exitInListCondition(EfxParser.InListConditionContext ctx) {
        StringListExpression list = this.stack.pop(StringListExpression.class);
        StringExpression expression = this.stack.pop(StringExpression.class);
        BooleanExpression condition = this.syntax.mapInListCondition(expression, list);
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            condition = this.syntax.mapLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    /*** Numeric expreessions ***/

    @Override
    public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.syntax.mapNumericOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.syntax.mapNumericOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitParenthesizedNumericExpression(ParenthesizedNumericExpressionContext ctx) {
        this.stack.push(this.syntax.mapParenthesizedExpression(
                this.stack.pop(NumericExpression.class), NumericExpression.class));
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

        List<StringExpression> list = new ArrayList<>();
        for (int i = 0; i < ctx.expression().size(); i++) {
            list.add(0, this.stack.pop(StringExpression.class));
        }
        this.stack.push(this.syntax.mapList(list));
    }

    @Override
    public void exitNumericLiteral(NumericLiteralContext ctx) {
        this.stack.push(this.syntax.mapNumericLiteral(ctx.getText()));
    }

    @Override
    public void exitStringLiteral(StringLiteralContext ctx) {
        this.stack.push(this.syntax.mapStringLiteral(ctx.getText()));
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
        this.stack.push(this.syntax.mapExternalReference(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitNodeReferenceWithPredicate(NodeReferenceWithPredicateContext ctx) {
        BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
        PathExpression nodeReference = this.stack.pop(PathExpression.class);
        this.stack.push(this.syntax.mapNodeReferenceWithPredicate(nodeReference, predicate,
                PathExpression.class));
    }

    @Override
    public void exitSimpleNodeReference(SimpleNodeReferenceContext ctx) {
        this.stack.push(this.symbols.relativeXpathOfNode(ctx.NodeId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void exitFieldInNoticeReference(EfxParser.FieldInNoticeReferenceContext ctx) {
        PathExpression field = this.stack.pop(PathExpression.class);
        Expression notice = this.stack.pop(Expression.class);
        this.stack.push(this.syntax.mapFieldInExternalReference(notice, field));
    }

    @Override
    public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
        this.stack.push(symbols.relativeXpathOfField(ctx.FieldId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
        BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
        PathExpression fieldReference = this.stack.pop(PathExpression.class);
        this.stack.push(this.syntax.mapFieldReferenceWithPredicate(fieldReference, predicate,
                PathExpression.class));
    }

    @Override
    public void exitUntypedFieldValueReference(UntypedFieldValueReferenceContext ctx) {
        String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
        if (fieldId != null) {
            this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop(PathExpression.class),
                    Expression.types.get(this.symbols.typeOfField(fieldId))));
        } else {
            this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop(PathExpression.class),
                    PathExpression.class));
        }
    }

    @Override
    public void exitUntypedAttributeValueReference(UntypedAttributeValueReferenceContext ctx) {
        this.stack.push(this.syntax.mapFieldValueReference(this.stack.pop(PathExpression.class),
                StringExpression.class));
    }

    /**
     * Any field references in the predicate must be resolved relative to the field on which the
     * predicate is applied. Therefore we need to switch to the field's context while the predicate
     * is being parsed.
     */
    @Override
    public void enterPredicate(EfxParser.PredicateContext ctx) {
        final String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx.getParent());
        this.efxContext.pushFieldContext(fieldId);
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
        this.stack.push(this.syntax.mapList(this.symbols.expandCodelist(ctx.codeListId.getText())
                .stream().map(s -> this.syntax.mapString(s)).collect(Collectors.toList())));
    }



    /*** Boolean functions ***/

    @Override
    public void exitNotFunction(NotFunctionContext ctx) {
        this.stack.push(this.syntax.mapLogicalNot(this.stack.pop(BooleanExpression.class)));
    }

    @Override
    public void exitContainsFunction(ContainsFunctionContext ctx) {
        final StringExpression needle = this.stack.pop(StringExpression.class);
        final StringExpression haystack = this.stack.pop(StringExpression.class);
        this.stack.push(this.syntax.mapStringContainsFunction(haystack, needle));
    }

    @Override
    public void exitStartsWithFunction(StartsWithFunctionContext ctx) {
        final StringExpression startsWith = this.stack.pop(StringExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        this.stack.push(this.syntax.mapStringStartsWithFunction(text, startsWith));
    }

    @Override
    public void exitEndsWithFunction(EndsWithFunctionContext ctx) {
        final StringExpression endsWith = this.stack.pop(StringExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        this.stack.push(this.syntax.mapStringEndsWithFunction(text, endsWith));
    }


    /*** Numeric functions ***/

    @Override
    public void exitCountFunction(CountFunctionContext ctx) {
        this.stack.push(this.syntax.mapCountFunction(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitNumberFunction(NumberFunctionContext ctx) {
        this.stack.push(this.syntax.mapToNumberFunction(this.stack.pop(StringExpression.class)));
    }

    @Override
    public void exitSumFunction(SumFunctionContext ctx) {
        this.stack.push(this.syntax.mapSumFunction(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
        this.stack
                .push(this.syntax.mapStringLengthFunction(this.stack.pop(StringExpression.class)));
    }


    /*** String functions ***/

    @Override
    public void exitSubstringFunction(SubstringFunctionContext ctx) {
        final NumericExpression length =
                ctx.length != null ? this.stack.pop(NumericExpression.class) : null;
        final NumericExpression start = this.stack.pop(NumericExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        if (length != null) {
            this.stack.push(this.syntax.mapSubstringFunction(text, start, length));
        } else {
            this.stack.push(this.syntax.mapSubstringFunction(text, start));
        }
    }

    @Override
    public void exitToStringFunction(ToStringFunctionContext ctx) {
        this.stack.push(
                this.syntax.mapNumberToStringFunction(this.stack.pop(NumericExpression.class)));
    }

    @Override
    public void exitConcatFunction(ConcatFunctionContext ctx) {
        if (this.stack.empty() || ctx.stringExpression().size() == 0) {
            this.stack.push(this.syntax.mapStringConcatenationFunction(Collections.emptyList()));
            return;
        }

        List<StringExpression> list = new ArrayList<>();
        for (int i = 0; i < ctx.stringExpression().size(); i++) {
            list.add(0, this.stack.pop(StringExpression.class));
        }
        this.stack.push(this.syntax.mapStringConcatenationFunction(list));
    }

    @Override
    public void exitFormatNumberFunction(FormatNumberFunctionContext ctx) {
        final StringExpression format = this.stack.pop(StringExpression.class);
        final NumericExpression number = this.stack.pop(NumericExpression.class);
        this.stack.push(this.syntax.mapFormatNumberFunction(number, format));
    }

    /*** Date functions ***/

    @Override
    public void exitDateFromStringFunction(DateFromStringFunctionContext ctx) {
        this.stack.push(
                this.syntax.mapDateFromStringFunction(this.stack.pop(StringExpression.class)));
    }

    /*** Time functions ***/

    @Override
    public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
        this.stack.push(
                this.syntax.mapTimeFromStringFunction(this.stack.pop(StringExpression.class)));
    }

    /*** Duration functions ***/

    @Override
    public void exitDurationFromDatesFunction(DurationFromDatesFunctionContext ctx) {
        final DateExpression endDate = this.stack.pop(DateExpression.class);
        final DateExpression startDate = this.stack.pop(DateExpression.class);
        this.stack.push(this.syntax.mapDurationFromDatesFunction(startDate, endDate));
    }
}
