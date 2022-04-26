package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;
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
import eu.europa.ted.efx.EfxParser.DateLiteralContext;
import eu.europa.ted.efx.EfxParser.DurationComparisonContext;
import eu.europa.ted.efx.EfxParser.DurationFromDatesFunctionContext;
import eu.europa.ted.efx.EfxParser.DurationLiteralContext;
import eu.europa.ted.efx.EfxParser.EndsWithFunctionContext;
import eu.europa.ted.efx.EfxParser.ExplicitListContext;
import eu.europa.ted.efx.EfxParser.FalseBooleanLiteralContext;
import eu.europa.ted.efx.EfxParser.FieldReferenceWithFieldContextOverrideContext;
import eu.europa.ted.efx.EfxParser.FieldReferenceWithNodeContextOverrideContext;
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
import eu.europa.ted.efx.EfxParser.TimeLiteralContext;
import eu.europa.ted.efx.EfxParser.ToStringFunctionContext;
import eu.europa.ted.efx.EfxParser.TrueBooleanLiteralContext;
import eu.europa.ted.efx.EfxParser.UntypedAttributeValueReferenceContext;
import eu.europa.ted.efx.EfxParser.UntypedFieldValueReferenceContext;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

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
    protected final SymbolResolver symbols;

    protected final ScriptGenerator script;

    /**
     * The context stack is used to keep track of xPath contexts for nested conditions.
     */
    protected final ContextStack efxContext;

    public EfxExpressionTranslator(final SymbolResolver symbols, final ScriptGenerator scriptGenerator) {
        this.symbols = symbols;
        this.script = scriptGenerator;
        this.efxContext = new ContextStack(symbols);
    }

    public static String transpileExpression(final String context, final String expression,
            final SymbolResolver symbols, final ScriptGenerator scriptGenerator,
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
        final EfxExpressionTranslator translator = new EfxExpressionTranslator(symbols, scriptGenerator);

        walker.walk(translator, tree);

        return translator.getTranspiledXPath();
    }

    public static String transpileExpression(final String context, final String expression,
            final SymbolResolver symbols, final ScriptGenerator scriptGenerator) {
        return transpileExpression(context, expression, symbols, scriptGenerator, null);
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
        final TerminalNode fieldContext = ctx.FieldId();
        if (fieldContext != null) {
            this.efxContext.pushFieldContext(fieldContext.getText());
        } else {
            final TerminalNode nodeContext = ctx.NodeId();
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
        this.stack.push(this.script.mapLogicalAnd(left, right));
    }

    @Override
    public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.mapLogicalOr(left, right));
    }

    @Override
    public void exitFieldValueComparison(FieldValueComparisonContext ctx) {
        Expression right = this.stack.pop(Expression.class);
        Expression left = this.stack.pop(Expression.class);
        if (!left.getClass().equals(right.getClass())) {
            throw new ParseCancellationException(
                    "Type mismatch. Cannot compare values of different types: " + left.getClass()
                            + " and " + right.getClass());
        }
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitStringComparison(StringComparisonContext ctx) {
        StringExpression right = this.stack.pop(StringExpression.class);
        StringExpression left = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitNumericComparison(NumericComparisonContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitBooleanComparison(BooleanComparisonContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDateComparison(DateComparisonContext ctx) {
        DateExpression right = this.stack.pop(DateExpression.class);
        DateExpression left = this.stack.pop(DateExpression.class);
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitTimeComparison(TimeComparisonContext ctx) {
        TimeExpression right = this.stack.pop(TimeExpression.class);
        TimeExpression left = this.stack.pop(TimeExpression.class);
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDurationComparison(DurationComparisonContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DurationExpression left = this.stack.pop(DurationExpression.class);
        this.stack.push(this.script.mapComparisonOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
        StringExpression expression = this.stack.pop(StringExpression.class);
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals("not") ? "!=" : "==";
        this.stack.push(
                this.script.mapComparisonOperator(expression, operator, this.script.mapString("")));
    }

    @Override
    public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
        PathExpression reference = this.stack.pop(PathExpression.class);
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            this.stack.push(this.script.mapLogicalNot(this.script.mapExistsExpression(reference)));
        } else {
            this.stack.push(this.script.mapExistsExpression(reference));
        }
    }

    @Override
    public void exitParenthesizedBooleanExpression(
            EfxParser.ParenthesizedBooleanExpressionContext ctx) {
        this.stack.push(this.script.mapParenthesizedExpression(
                this.stack.pop(BooleanExpression.class), BooleanExpression.class));
    }

    @Override
    public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
        StringExpression expression = this.stack.pop(StringExpression.class);

        BooleanExpression condition =
                this.script.mapMatchesPatternCondition(expression, ctx.pattern.getText());
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            condition = this.script.mapLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    @Override
    public void exitInListCondition(EfxParser.InListConditionContext ctx) {
        StringListExpression list = this.stack.pop(StringListExpression.class);
        StringExpression expression = this.stack.pop(StringExpression.class);
        BooleanExpression condition = this.script.mapInListCondition(expression, list);
        if (ctx.modifier != null && ctx.modifier.getText().equals("not")) {
            condition = this.script.mapLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    /*** Numeric expreessions ***/

    @Override
    public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.mapNumericOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.mapNumericOperator(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitParenthesizedNumericExpression(ParenthesizedNumericExpressionContext ctx) {
        this.stack.push(this.script.mapParenthesizedExpression(
                this.stack.pop(NumericExpression.class), NumericExpression.class));
    }

    @Override
    public void exitCodeList(CodeListContext ctx) {
        if (this.stack.empty()) {
            this.stack.push(this.script.mapList(Collections.emptyList()));
            return;
        }
    }

    @Override
    public void exitExplicitList(ExplicitListContext ctx) {
        if (this.stack.empty() || ctx.expression().size() == 0) {
            this.stack.push(this.script.mapList(Collections.emptyList()));
            return;
        }

        List<StringExpression> list = new ArrayList<>();
        for (int i = 0; i < ctx.expression().size(); i++) {
            list.add(0, this.stack.pop(StringExpression.class));
        }
        this.stack.push(this.script.mapList(list));
    }

    @Override
    public void exitNumericLiteral(NumericLiteralContext ctx) {
        this.stack.push(this.script.mapNumericLiteral(ctx.getText()));
    }

    @Override
    public void exitStringLiteral(StringLiteralContext ctx) {
        this.stack.push(this.script.mapStringLiteral(ctx.getText()));
    }

    @Override
    public void exitTrueBooleanLiteral(TrueBooleanLiteralContext ctx) {
        this.stack.push(this.script.mapBoolean(true));
    }

    @Override
    public void exitFalseBooleanLiteral(FalseBooleanLiteralContext ctx) {
        this.stack.push(this.script.mapBoolean(false));
    }

    @Override
    public void exitDateLiteral(DateLiteralContext ctx) {
        this.stack.push(this.script.mapDateLiteral(ctx.DATE().getText()));
    }

    @Override
    public void exitTimeLiteral(TimeLiteralContext ctx) {
        this.stack.push(this.script.mapTimeLiteral(ctx.TIME().getText()));
    }

    @Override
    public void exitDurationLiteral(DurationLiteralContext ctx) {
        this.stack.push(this.script.mapDurationLiteral(ctx.DURATION().getText()));
    }

    @Override
    public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
        this.stack.push(this.script.mapExternalReference(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitNodeReferenceWithPredicate(NodeReferenceWithPredicateContext ctx) {
        BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
        PathExpression nodeReference = this.stack.pop(PathExpression.class);
        this.stack.push(this.script.mapNodeReferenceWithPredicate(nodeReference, predicate,
                PathExpression.class));
    }

    @Override
    public void exitSimpleNodeReference(SimpleNodeReferenceContext ctx) {
        this.stack.push(this.symbols.relativeXpathOfNode(ctx.NodeId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void exitFieldReferenceInOtherNotice(EfxParser.FieldReferenceInOtherNoticeContext ctx) {
        PathExpression field = this.stack.pop(PathExpression.class);
        Expression notice = this.stack.pop(Expression.class);
        this.stack.push(this.script.mapFieldInExternalReference(notice, field));
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
        this.stack.push(this.script.mapFieldReferenceWithPredicate(fieldReference, predicate,
                PathExpression.class));
    }

    @Override
    public void exitUntypedFieldValueReference(UntypedFieldValueReferenceContext ctx) {

        PathExpression path = this.stack.pop(PathExpression.class);
        String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
        XPathAttributeLocator parsedPath = XPathAttributeLocator.findAttribute(path);

        if (parsedPath.hasAttribute()) {
            this.stack.push(this.script.mapFieldAttributeReference(parsedPath.getPath(),
                    parsedPath.getAttribute(), StringExpression.class));
        } else if (fieldId != null) {
            this.stack.push(this.script.mapFieldValueReference(path,
                    Expression.types.get(this.symbols.typeOfField(fieldId))));
        } else {
            this.stack.push(this.script.mapFieldValueReference(path, PathExpression.class));
        }
    }

    @Override
    public void exitUntypedAttributeValueReference(UntypedAttributeValueReferenceContext ctx) {
        this.stack.push(this.script.mapFieldAttributeReference(this.stack.pop(PathExpression.class),
                ctx.Identifier().getText(), StringExpression.class));
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
    public void enterFieldReferenceWithFieldContextOverride(FieldReferenceWithFieldContextOverrideContext ctx) {
        final String contextFieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx.context);
        this.efxContext.push(new FieldContext(contextFieldId, new PathExpression( ctx.context.getText())));
    }

    @Override
    public void exitFieldReferenceWithFieldContextOverride(FieldReferenceWithFieldContextOverrideContext ctx) {
        this.efxContext.pop();
    }

    @Override
    public void enterFieldReferenceWithNodeContextOverride(FieldReferenceWithNodeContextOverrideContext ctx) {
        final String contextNodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx.context);
        this.efxContext.push( new NodeContext(contextNodeId, new PathExpression(ctx.context.getText())));
    }

    @Override
    public void exitFieldReferenceWithNodeContextOverride(FieldReferenceWithNodeContextOverrideContext ctx) {
        this.efxContext.pop();
    }

    @Override
    public void exitCodelistReference(CodelistReferenceContext ctx) {
        this.stack.push(this.script.mapList(this.symbols.expandCodelist(ctx.codeListId.getText())
                .stream().map(s -> this.script.mapString(s)).collect(Collectors.toList())));
    }



    /*** Boolean functions ***/

    @Override
    public void exitNotFunction(NotFunctionContext ctx) {
        this.stack.push(this.script.mapLogicalNot(this.stack.pop(BooleanExpression.class)));
    }

    @Override
    public void exitContainsFunction(ContainsFunctionContext ctx) {
        final StringExpression needle = this.stack.pop(StringExpression.class);
        final StringExpression haystack = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.mapStringContainsFunction(haystack, needle));
    }

    @Override
    public void exitStartsWithFunction(StartsWithFunctionContext ctx) {
        final StringExpression startsWith = this.stack.pop(StringExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.mapStringStartsWithFunction(text, startsWith));
    }

    @Override
    public void exitEndsWithFunction(EndsWithFunctionContext ctx) {
        final StringExpression endsWith = this.stack.pop(StringExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.mapStringEndsWithFunction(text, endsWith));
    }


    /*** Numeric functions ***/

    @Override
    public void exitCountFunction(CountFunctionContext ctx) {
        this.stack.push(this.script.mapCountFunction(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitNumberFunction(NumberFunctionContext ctx) {
        this.stack.push(this.script.mapToNumberFunction(this.stack.pop(StringExpression.class)));
    }

    @Override
    public void exitSumFunction(SumFunctionContext ctx) {
        this.stack.push(this.script.mapSumFunction(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
        this.stack
                .push(this.script.mapStringLengthFunction(this.stack.pop(StringExpression.class)));
    }


    /*** String functions ***/

    @Override
    public void exitSubstringFunction(SubstringFunctionContext ctx) {
        final NumericExpression length =
                ctx.length != null ? this.stack.pop(NumericExpression.class) : null;
        final NumericExpression start = this.stack.pop(NumericExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        if (length != null) {
            this.stack.push(this.script.mapSubstringFunction(text, start, length));
        } else {
            this.stack.push(this.script.mapSubstringFunction(text, start));
        }
    }

    @Override
    public void exitToStringFunction(ToStringFunctionContext ctx) {
        this.stack.push(
                this.script.mapNumberToStringFunction(this.stack.pop(NumericExpression.class)));
    }

    @Override
    public void exitConcatFunction(ConcatFunctionContext ctx) {
        if (this.stack.empty() || ctx.stringExpression().size() == 0) {
            this.stack.push(this.script.mapStringConcatenationFunction(Collections.emptyList()));
            return;
        }

        List<StringExpression> list = new ArrayList<>();
        for (int i = 0; i < ctx.stringExpression().size(); i++) {
            list.add(0, this.stack.pop(StringExpression.class));
        }
        this.stack.push(this.script.mapStringConcatenationFunction(list));
    }

    @Override
    public void exitFormatNumberFunction(FormatNumberFunctionContext ctx) {
        final StringExpression format = this.stack.pop(StringExpression.class);
        final NumericExpression number = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.mapFormatNumberFunction(number, format));
    }

    /*** Date functions ***/

    @Override
    public void exitDateFromStringFunction(DateFromStringFunctionContext ctx) {
        this.stack.push(
                this.script.mapDateFromStringFunction(this.stack.pop(StringExpression.class)));
    }

    /*** Time functions ***/

    @Override
    public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
        this.stack.push(
                this.script.mapTimeFromStringFunction(this.stack.pop(StringExpression.class)));
    }

    /*** Duration functions ***/

    @Override
    public void exitDurationFromDatesFunction(DurationFromDatesFunctionContext ctx) {
        final DateExpression endDate = this.stack.pop(DateExpression.class);
        final DateExpression startDate = this.stack.pop(DateExpression.class);
        this.stack.push(this.script.mapDurationFromDatesFunction(startDate, endDate));
    }
}
