package eu.europa.ted.efx.sdk0.v7;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.BooleanListExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DateListExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.DurationListExpression;
import eu.europa.ted.efx.model.Expression.ListExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.NumericListExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;
import eu.europa.ted.efx.model.Expression.TimeListExpression;
import eu.europa.ted.efx.sdk0.v7.EfxParser.*;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

/**
 * The the goal of the EfxExpressionTranslator is to take an EFX expression and translate it to a
 * target scripting language.
 * 
 * The target language syntax is not hardcoded into the translator so that this class can be reused
 * to translate to several different languages. Instead a {@link ScriptGenerator} interface is used
 * to provide specifics on the syntax of the target scripting language.
 * 
 * Apart from writing expressions that can be translated and evaluated in a target scripting
 * language (e.g. XPath/XQuery, JavaScript etc.), EFX also allows the definition of templates that
 * can be translated to a target template markup language (e.g. XSLT, Thymeleaf etc.). The
 * {@link EfxExpressionTranslator} only focuses on EFX expressions. To translate EFX templates you
 * need to use the {@link EfxTemplateTranslator} which derives from this class.
 */
public class EfxExpressionTranslator extends EfxBaseListener {

    private static final String NOT_MODIFIER =
            EfxLexer.VOCABULARY.getLiteralName(EfxLexer.Not).replaceAll("^'|'$", "");

    /**
     *
     */
    private static final String TYPE_MISMATCH_CANNOT_COMPARE_VALUES_OF_DIFFERENT_TYPES =
            "Type mismatch. Cannot compare values of different types: ";

    /**
     * The stack is used by the methods of this listener to pass data to each other as the parse
     * tree is being walked.
     */
    protected CallStack stack = new CallStack();

    /**
     * The context stack is used to keep track of context switching in nested expressions.
     */
    protected final ContextStack efxContext;

    /**
     * Symbols are the field identifiers and node identifiers. The symbols map is used to resolve
     * them to their location in the data source (typically their XPath).
     */
    protected final SymbolResolver symbols;

    /**
     * The ScriptGenerator is called to determine the target language syntax whenever needed.
     */
    protected final ScriptGenerator script;

    public EfxExpressionTranslator(final SymbolResolver symbols,
            final ScriptGenerator scriptGenerator) {
        this.symbols = symbols;
        this.script = scriptGenerator;
        this.efxContext = new ContextStack(symbols);
    }

    public static String translateExpression(final String context, final String expression,
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
        final EfxExpressionTranslator translator =
                new EfxExpressionTranslator(symbols, scriptGenerator);

        walker.walk(translator, tree);

        return translator.getTranslatedScript();
    }

    /**
     * Used to get the translated target language script, after the walker finished its walk.
     *
     * @return The translated code, trimmed
     */
    private String getTranslatedScript() {
        final StringBuilder sb = new StringBuilder(this.stack.size() * 100);
        while (!this.stack.empty()) {
            sb.insert(0, '\n').insert(0, this.stack.pop(Expression.class).script);
        }
        return sb.toString().trim();
    }

    /**
     * Helper method that starts from a given {@link ParserRuleContext} and recursively searches for
     * a {@link SimpleFieldReferenceContext} to locate a field identifier.
     */
    protected static String getFieldIdFromChildSimpleFieldReferenceContext(ParserRuleContext ctx) {

        if (ctx instanceof SimpleFieldReferenceContext) {
            return ((SimpleFieldReferenceContext) ctx).FieldId().getText();
        }

        if (ctx instanceof AbsoluteFieldReferenceContext) {
            return ((AbsoluteFieldReferenceContext) ctx).reference.simpleFieldReference().FieldId()
                    .getText();
        }

        if (ctx instanceof FieldReferenceWithFieldContextOverrideContext) {
            return ((FieldReferenceWithFieldContextOverrideContext) ctx).reference
                    .simpleFieldReference().FieldId().getText();
        }

        if (ctx instanceof FieldReferenceWithNodeContextOverrideContext) {
            return ((FieldReferenceWithNodeContextOverrideContext) ctx).reference
                    .fieldReferenceWithPredicate().simpleFieldReference().FieldId().getText();
        }

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
     * Helper method that starts from a given {@link ParserRuleContext} and recursively searches for
     * a {@link SimpleNodeReferenceContext} to locate a node identifier.
     */
    protected static String getNodeIdFromChildSimpleNodeReferenceContext(ParserRuleContext ctx) {

        if (ctx instanceof SimpleNodeReferenceContext) {
            return ((SimpleNodeReferenceContext) ctx).NodeId().getText();
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
    }

    /*** Boolean expressions ***/

    @Override
    public void exitParenthesizedBooleanExpression(
            EfxParser.ParenthesizedBooleanExpressionContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(
                this.stack.pop(BooleanExpression.class), BooleanExpression.class));
    }

    @Override
    public void exitLogicalAndCondition(EfxParser.LogicalAndConditionContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeLogicalAnd(left, right));
    }

    @Override
    public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeLogicalOr(left, right));
    }

    /*** Boolean expressions - Comparisons ***/

    @Override
    public void exitFieldValueComparison(FieldValueComparisonContext ctx) {
        Expression right = this.stack.pop(Expression.class);
        Expression left = this.stack.pop(Expression.class);
        if (!left.getClass().equals(right.getClass())) {
            throw new ParseCancellationException(
                    TYPE_MISMATCH_CANNOT_COMPARE_VALUES_OF_DIFFERENT_TYPES + left.getClass()
                            + " and " + right.getClass());
        }
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitStringComparison(StringComparisonContext ctx) {
        StringExpression right = this.stack.pop(StringExpression.class);
        StringExpression left = this.stack.pop(StringExpression.class);
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitNumericComparison(NumericComparisonContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitBooleanComparison(BooleanComparisonContext ctx) {
        BooleanExpression right = this.stack.pop(BooleanExpression.class);
        BooleanExpression left = this.stack.pop(BooleanExpression.class);
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDateComparison(DateComparisonContext ctx) {
        DateExpression right = this.stack.pop(DateExpression.class);
        DateExpression left = this.stack.pop(DateExpression.class);
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitTimeComparison(TimeComparisonContext ctx) {
        TimeExpression right = this.stack.pop(TimeExpression.class);
        TimeExpression left = this.stack.pop(TimeExpression.class);
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitDurationComparison(DurationComparisonContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DurationExpression left = this.stack.pop(DurationExpression.class);
        this.stack
                .push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
    }

    /*** Boolean expressions - Conditions ***/

    @Override
    public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
        StringExpression expression = this.stack.pop(StringExpression.class);
        String operator =
                ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER) ? "!=" : "==";
        this.stack.push(this.script.composeComparisonOperation(expression, operator,
                this.script.getStringLiteralFromUnquotedString("")));
    }

    @Override
    public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
        PathExpression reference = this.stack.pop(PathExpression.class);
        if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
            this.stack.push(
                    this.script.composeLogicalNot(this.script.composeExistsCondition(reference)));
        } else {
            this.stack.push(this.script.composeExistsCondition(reference));
        }
    }

    @Override
    public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
        StringExpression expression = this.stack.pop(StringExpression.class);

        BooleanExpression condition =
                this.script.composePatternMatchCondition(expression, ctx.pattern.getText());
        if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
            condition = this.script.composeLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    /*** Boolean expressions - List membership conditions ***/

    @Override
    public void exitStringInListCondition(EfxParser.StringInListConditionContext ctx) {
        this.exitInListCondition(ctx.modifier, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitBooleanInListCondition(BooleanInListConditionContext ctx) {
        this.exitInListCondition(ctx.modifier, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitNumberInListCondition(NumberInListConditionContext ctx) {
        this.exitInListCondition(ctx.modifier, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitDateInListCondition(DateInListConditionContext ctx) {
        this.exitInListCondition(ctx.modifier, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitTimeInListCondition(TimeInListConditionContext ctx) {
        this.exitInListCondition(ctx.modifier, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitDurationInListCondition(DurationInListConditionContext ctx) {
        this.exitInListCondition(ctx.modifier, DurationExpression.class, DurationListExpression.class);
    }

    private <T extends Expression, L extends ListExpression<T>> void exitInListCondition(Token modifier, Class<T> expressionType, Class<L> listType) {
        ListExpression<T> list = this.stack.pop(listType);
        T expression = this.stack.pop(expressionType);
        BooleanExpression condition = this.script.composeContainsCondition(expression, list);
        if (modifier != null && modifier.getText().equals(NOT_MODIFIER)) {
            condition = this.script.composeLogicalNot(condition);
        }
        this.stack.push(condition);
    }

    /*** Quantified expressions ***/

    @Override
    public void exitStringQuantifiedExpression(StringQuantifiedExpressionContext ctx) {
        this.exitQuantifiedExpression(ctx.Every() != null, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitBooleanQuantifiedExpression(BooleanQuantifiedExpressionContext ctx) {
        this.exitQuantifiedExpression(ctx.Every() != null, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitNumericQuantifiedExpression(NumericQuantifiedExpressionContext ctx) {
        this.exitQuantifiedExpression(ctx.Every() != null, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitDateQuantifiedExpression(DateQuantifiedExpressionContext ctx) {
        this.exitQuantifiedExpression(ctx.Every() != null, DateExpression.class, DateListExpression.class);
    }
    
    @Override
    public void exitTimeQuantifiedExpression(TimeQuantifiedExpressionContext ctx) {
        this.exitQuantifiedExpression(ctx.Every() != null, TimeExpression.class, TimeListExpression.class);
    }
    
    @Override
    public void exitDurationQuantifiedExpression(DurationQuantifiedExpressionContext ctx) {
        this.exitQuantifiedExpression(ctx.Every() != null, DurationExpression.class, DurationListExpression.class);
    }

    private <T extends Expression, L extends ListExpression<T>> void exitQuantifiedExpression(boolean every, Class<T> expressionType, Class<L> listType) {
        BooleanExpression booleanExpression = this.stack.pop(BooleanExpression.class);
        L list = this.stack.pop(listType);
        T variable = this.stack.pop(expressionType);
        if (every) {
            this.stack.push(this.script.composeAllSatisfy(list, variable.script, booleanExpression));
        } else {
            this.stack.push(this.script.composeAnySatisfies(list, variable.script, booleanExpression));
        }
    }

    /*** Numeric expressions ***/

    @Override
    public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.composeNumericOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
        NumericExpression right = this.stack.pop(NumericExpression.class);
        NumericExpression left = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.composeNumericOperation(left, ctx.operator.getText(), right));
    }

    @Override
    public void exitParenthesizedNumericExpression(ParenthesizedNumericExpressionContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(
                this.stack.pop(NumericExpression.class), NumericExpression.class));
    }

    /*** Duration Expressions ***/

    @Override
    public void exitDurationAdditionExpression(DurationAdditionExpressionContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DurationExpression left = this.stack.pop(DurationExpression.class);
        this.stack.push(this.script.composeAddition(left, right));
    }

    @Override
    public void exitDurationSubtractionExpression(DurationSubtractionExpressionContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DurationExpression left = this.stack.pop(DurationExpression.class);
        this.stack.push(this.script.composeSubtraction(left, right));
    }

    @Override
    public void exitDurationLeftMultiplicationExpression(
            DurationLeftMultiplicationExpressionContext ctx) {
        DurationExpression duration = this.stack.pop(DurationExpression.class);
        NumericExpression number = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.composeMultiplication(number, duration));
    }

    @Override
    public void exitDurationRightMultiplicationExpression(
            DurationRightMultiplicationExpressionContext ctx) {
        NumericExpression number = this.stack.pop(NumericExpression.class);
        DurationExpression duration = this.stack.pop(DurationExpression.class);
        this.stack.push(this.script.composeMultiplication(number, duration));
    }

    @Override
    public void exitDateSubtractionExpression(DateSubtractionExpressionContext ctx) {
        final DateExpression startDate = this.stack.pop(DateExpression.class);
        final DateExpression endDate = this.stack.pop(DateExpression.class);
        this.stack.push(this.script.composeSubtraction(startDate, endDate));
    }

    @Override
    public void exitCodeList(CodeListContext ctx) {
        if (this.stack.empty()) {
            this.stack.push(this.script.composeList(Collections.emptyList(), StringListExpression.class));
            return;
        }
    }

    @Override
    public void exitStringList(StringListContext ctx) {
        this.exitList(ctx.stringExpression().size(), StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitBooleanList(BooleanListContext ctx) {
        this.exitList(ctx.booleanExpression().size(), BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitNumericList(NumericListContext ctx) {
        this.exitList(ctx.numericExpression().size(), NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitDateList(DateListContext ctx) {
        this.exitList(ctx.dateExpression().size(), DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitTimeList(TimeListContext ctx) {
        this.exitList(ctx.timeExpression().size(), TimeExpression.class, TimeListExpression.class);
    }


    @Override
    public void exitDurationList(DurationListContext ctx) {
        this.exitList(ctx.durationExpression().size(), DurationExpression.class, DurationListExpression.class);
    }

    private <T extends Expression, L extends ListExpression<T>> void exitList(int listSize, Class<T> expressionType, Class<L> listType) {
        if (this.stack.empty() || listSize == 0) {
            this.stack.push(this.script.composeList(Collections.emptyList(), listType));
            return;
        }

        List<T> list = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            list.add(0, this.stack.pop(expressionType));
        }
        this.stack.push(this.script.composeList(list, listType));
    }

    /*** Conditional Expressions ***/

    @Override
    public void exitConditionalBooleanExpression(ConditionalBooleanExpressionContext ctx) {
        BooleanExpression whenFalse = this.stack.pop(BooleanExpression.class);
        BooleanExpression whenTrue = this.stack.pop(BooleanExpression.class);
        BooleanExpression condition = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, BooleanExpression.class));
    }

    @Override
    public void exitConditionalNumericExpression(ConditionalNumericExpressionContext ctx) {
        NumericExpression whenFalse = this.stack.pop(NumericExpression.class);
        NumericExpression whenTrue = this.stack.pop(NumericExpression.class);
        BooleanExpression condition = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, NumericExpression.class));
    }

    @Override
    public void exitConditionalStringExpression(ConditionalStringExpressionContext ctx) {
        StringExpression whenFalse = this.stack.pop(StringExpression.class);
        StringExpression whenTrue = this.stack.pop(StringExpression.class);
        BooleanExpression condition = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, StringExpression.class));
    }

    @Override
    public void exitConditionalDateExpression(ConditionalDateExpressionContext ctx) {
        DateExpression whenFalse = this.stack.pop(DateExpression.class);
        DateExpression whenTrue = this.stack.pop(DateExpression.class);
        BooleanExpression condition = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, DateExpression.class));
    }

    @Override
    public void exitConditionalTimeExpression(ConditionalTimeExpressionContext ctx) {
        TimeExpression whenFalse = this.stack.pop(TimeExpression.class);
        TimeExpression whenTrue = this.stack.pop(TimeExpression.class);
        BooleanExpression condition = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, TimeExpression.class));
    }

    @Override
    public void exitConditionalDurationExpression(ConditionalDurationExpressionContext ctx) {
        DurationExpression whenFalse = this.stack.pop(DurationExpression.class);
        DurationExpression whenTrue = this.stack.pop(DurationExpression.class);
        BooleanExpression condition = this.stack.pop(BooleanExpression.class);
        this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, DurationExpression.class));
    }

    /*** Iteration expressions ***/

    @Override
    public void exitParenthesizedStringsFromIteration(
            ParenthesizedStringsFromIterationContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(this.stack.pop(StringListExpression.class), StringListExpression.class));
    }
    
    @Override
    public void exitParenthesizedNumbersFromIteration(
            ParenthesizedNumbersFromIterationContext ctx) {
                this.stack.push(this.script.composeParenthesizedExpression(this.stack.pop(NumericListExpression.class), NumericListExpression.class));
    }

    @Override
    public void exitParenthesizedBooleansFromIteration(
            ParenthesizedBooleansFromIterationContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(
                this.stack.pop(BooleanListExpression.class), BooleanListExpression.class));
    }

    @Override
    public void exitParenthesizedDatesFromIteration(ParenthesizedDatesFromIterationContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(
                this.stack.pop(DateListExpression.class), DateListExpression.class));
    }

    @Override
    public void exitParenthesizedTimesFromIteration(ParenthesizedTimesFromIterationContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(
                this.stack.pop(TimeListExpression.class), TimeListExpression.class));
    }

    @Override
    public void exitParenthesizedDurationsFromITeration(
            ParenthesizedDurationsFromITerationContext ctx) {
        this.stack.push(this.script.composeParenthesizedExpression(
                this.stack.pop(DurationListExpression.class), DurationListExpression.class));
    }
    @Override
    public void exitStringsFromStringIteration(StringsFromStringIterationContext ctx) {
        this.exitIterationExpression(StringExpression.class, StringListExpression.class, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitStringsFromBooleanIteration(StringsFromBooleanIterationContext ctx) {
        this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitStringsFromNumericIteration(StringsFromNumericIterationContext ctx) {
        this.exitIterationExpression(NumericExpression.class, NumericListExpression.class, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitStringsFromDateIteration(StringsFromDateIterationContext ctx) {
            this.exitIterationExpression(DateExpression.class, DateListExpression.class, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitStringsFromTimeIteration(StringsFromTimeIterationContext ctx) {
        this.exitIterationExpression(TimeExpression.class, TimeListExpression.class, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitStringsFromDurationIteration(StringsFromDurationIterationContext ctx) {
        this.exitIterationExpression(DurationExpression.class, DurationListExpression.class, StringExpression.class, StringListExpression.class);
    }

    @Override
    public void exitBooleansFromStringIteration(BooleansFromStringIterationContext ctx) {
        this.exitIterationExpression(StringExpression.class, StringListExpression.class, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitBooleansFromBooleanIteration(BooleansFromBooleanIterationContext ctx) {
        this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitBooleansFromNumericIteration(BooleansFromNumericIterationContext ctx) {
        this.exitIterationExpression(NumericExpression.class, NumericListExpression.class, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitBooleansFromDateIteration(BooleansFromDateIterationContext ctx) {
            this.exitIterationExpression(DateExpression.class, DateListExpression.class, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitBooleansFromTimeIteration(BooleansFromTimeIterationContext ctx) {
        this.exitIterationExpression(TimeExpression.class, TimeListExpression.class, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitBooleansFromDurationIteration(BooleansFromDurationIterationContext ctx) {
        this.exitIterationExpression(DurationExpression.class, DurationListExpression.class, BooleanExpression.class, BooleanListExpression.class);
    }

    @Override
    public void exitNumbersFromStringIteration(NumbersFromStringIterationContext ctx) {
        this.exitIterationExpression(StringExpression.class, StringListExpression.class, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitNumbersFromBooleanIteration(NumbersFromBooleanIterationContext ctx) {
        this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitNumbersFromNumericIteration(NumbersFromNumericIterationContext ctx) {
        this.exitIterationExpression(NumericExpression.class, NumericListExpression.class, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitNumbersFromDateIteration(NumbersFromDateIterationContext ctx) {
            this.exitIterationExpression(DateExpression.class, DateListExpression.class, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitNumbersFromTimeIteration(NumbersFromTimeIterationContext ctx) {
        this.exitIterationExpression(TimeExpression.class, TimeListExpression.class, NumericExpression.class, NumericListExpression.class);
    }

    @Override
    public void exitNumbersFromDurationIteration(NumbersFromDurationIterationContext ctx) {
        this.exitIterationExpression(DurationExpression.class, DurationListExpression.class, NumericExpression.class, NumericListExpression.class);
    }


    @Override
    public void exitDatesFromStringIteration(DatesFromStringIterationContext ctx) {
        this.exitIterationExpression(StringExpression.class, StringListExpression.class, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitDatesFromBooleanIteration(DatesFromBooleanIterationContext ctx) {
        this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitDatesFromNumericIteration(DatesFromNumericIterationContext ctx) {
        this.exitIterationExpression(NumericExpression.class, NumericListExpression.class, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitDatesFromDateIteration(DatesFromDateIterationContext ctx) {
            this.exitIterationExpression(DateExpression.class, DateListExpression.class, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitDatesFromTimeIteration(DatesFromTimeIterationContext ctx) {
        this.exitIterationExpression(TimeExpression.class, TimeListExpression.class, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitDatesFromDurationIteration(DatesFromDurationIterationContext ctx) {
        this.exitIterationExpression(DurationExpression.class, DurationListExpression.class, DateExpression.class, DateListExpression.class);
    }

    @Override
    public void exitTimesFromStringIteration(TimesFromStringIterationContext ctx) {
        this.exitIterationExpression(StringExpression.class, StringListExpression.class, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitTimesFromBooleanIteration(TimesFromBooleanIterationContext ctx) {
        this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitTimesFromNumericIteration(TimesFromNumericIterationContext ctx) {
        this.exitIterationExpression(NumericExpression.class, NumericListExpression.class, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitTimesFromDateIteration(TimesFromDateIterationContext ctx) {
            this.exitIterationExpression(DateExpression.class, DateListExpression.class, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitTimesFromTimeIteration(TimesFromTimeIterationContext ctx) {
        this.exitIterationExpression(TimeExpression.class, TimeListExpression.class, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitTimesFromDurationIteration(TimesFromDurationIterationContext ctx) {
        this.exitIterationExpression(DurationExpression.class, DurationListExpression.class, TimeExpression.class, TimeListExpression.class);
    }

    @Override
    public void exitDurationsFromStringIteration(DurationsFromStringIterationContext ctx) {
        this.exitIterationExpression(StringExpression.class, StringListExpression.class, DurationExpression.class, DurationListExpression.class);
    }

    @Override
    public void exitDurationsFromBooleanIteration(DurationsFromBooleanIterationContext ctx) {
        this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class, DurationExpression.class, DurationListExpression.class);
    }

    @Override
    public void exitDurationsFromNumericIteration(DurationsFromNumericIterationContext ctx) {
        this.exitIterationExpression(NumericExpression.class, NumericListExpression.class, DurationExpression.class, DurationListExpression.class);
    }

    @Override
    public void exitDurationsFromDateIteration(DurationsFromDateIterationContext ctx) {
        this.exitIterationExpression(DateExpression.class, DateListExpression.class, DurationExpression.class, DurationListExpression.class);
    }

    @Override
    public void exitDurationsFromTimeIteration(DurationsFromTimeIterationContext ctx) {
        this.exitIterationExpression(TimeExpression.class, TimeListExpression.class, DurationExpression.class, DurationListExpression.class);
    }

    @Override
    public void exitDurationsFromDurationIteration(DurationsFromDurationIterationContext ctx) {
        this.exitIterationExpression(DurationExpression.class, DurationListExpression.class, DurationExpression.class, DurationListExpression.class);
    }

    public <T1 extends Expression, L1 extends ListExpression<T1>, T2 extends Expression, L2 extends ListExpression<T2>> void exitIterationExpression(Class<T1> variableType, Class<L1> sourceListType, Class<T2> expressionType, Class<L2> targetListType) {
        T2 expression = this.stack.pop(expressionType);
        L1 list = this.stack.pop(sourceListType);
        T1 variable = this.stack.pop(variableType);
        this.stack.push(this.script.composeForExpression(variable.script, list, expression, targetListType));
    }

    /*** Literals ***/

    @Override
    public void exitNumericLiteral(NumericLiteralContext ctx) {
        this.stack.push(this.script.getNumericLiteralEquivalent(ctx.getText()));
    }

    @Override
    public void exitStringLiteral(StringLiteralContext ctx) {
        this.stack.push(this.script.getStringLiteralEquivalent(ctx.getText()));
    }

    @Override
    public void exitTrueBooleanLiteral(TrueBooleanLiteralContext ctx) {
        this.stack.push(this.script.getBooleanEquivalent(true));
    }

    @Override
    public void exitFalseBooleanLiteral(FalseBooleanLiteralContext ctx) {
        this.stack.push(this.script.getBooleanEquivalent(false));
    }

    @Override
    public void exitDateLiteral(DateLiteralContext ctx) {
        this.stack.push(this.script.getDateLiteralEquivalent(ctx.DATE().getText()));
    }

    @Override
    public void exitTimeLiteral(TimeLiteralContext ctx) {
        this.stack.push(this.script.getTimeLiteralEquivalent(ctx.TIME().getText()));
    }

    @Override
    public void exitDurationLiteral(DurationLiteralContext ctx) {
        this.stack.push(this.script.getDurationLiteralEquivalent(ctx.getText()));
    }

    /*** References ***/

    @Override
    public void exitSimpleNodeReference(SimpleNodeReferenceContext ctx) {
        this.stack.push(this.symbols.getRelativePathOfNode(ctx.NodeId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
        this.stack.push(symbols.getRelativePathOfField(ctx.FieldId().getText(),
                this.efxContext.absolutePath()));
    }

    @Override
    public void enterAbsoluteFieldReference(AbsoluteFieldReferenceContext ctx) {
        if (ctx.Slash() != null) {
            this.efxContext.push(null);
        }
    }

    @Override
    public void exitAbsoluteFieldReference(EfxParser.AbsoluteFieldReferenceContext ctx) {
        if (ctx.Slash() != null) {
            this.efxContext.pop();
        }
    }

    @Override
    public void enterAbsoluteNodeReference(EfxParser.AbsoluteNodeReferenceContext ctx) {
        if (ctx.Slash() != null) {
            this.efxContext.push(null);
        }
    }

    @Override
    public void exitAbsoluteNodeReference(AbsoluteNodeReferenceContext ctx) {
        if (ctx.Slash() != null) {
            this.efxContext.pop();
        }
    }


    /*** References with Predicates ***/

    @Override
    public void exitNodeReferenceWithPredicate(NodeReferenceWithPredicateContext ctx) {
        if (ctx.predicate() != null) {
            BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
            PathExpression nodeReference = this.stack.pop(PathExpression.class);
            this.stack.push(this.script.composeNodeReferenceWithPredicate(nodeReference, predicate,
                    PathExpression.class));
        }
    }

    @Override
    public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
        if (ctx.predicate() != null) {
            BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
            PathExpression fieldReference = this.stack.pop(PathExpression.class);
            this.stack.push(this.script.composeFieldReferenceWithPredicate(fieldReference,
                    predicate, PathExpression.class));
        }
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

    /*** External References ***/

    @Override
    public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
        this.stack
                .push(this.script.composeExternalReference(this.stack.pop(StringExpression.class)));
    }

    @Override
    public void exitFieldReferenceInOtherNotice(EfxParser.FieldReferenceInOtherNoticeContext ctx) {
        if (ctx.noticeReference() != null) {
            PathExpression field = this.stack.pop(PathExpression.class);
            PathExpression notice = this.stack.pop(PathExpression.class);
            this.stack.push(this.script.composeFieldInExternalReference(notice, field));
        }
    }

    /*** Value References ***/

    @Override
    public void exitUntypedFieldValueReference(UntypedFieldValueReferenceContext ctx) {

        PathExpression path = this.stack.pop(PathExpression.class);
        String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
        // TODO: Use an interface for locating attributes. A PathExpression is not necessarily an
        // XPath in every implementation.
        XPathAttributeLocator parsedPath = XPathAttributeLocator.findAttribute(path);

        if (parsedPath.hasAttribute()) {
            this.stack.push(this.script.composeFieldAttributeReference(parsedPath.getPath(),
                    parsedPath.getAttribute(), StringExpression.class));
        } else if (fieldId != null) {
            this.stack.push(this.script.composeFieldValueReference(path,
                    Expression.types.get(this.symbols.getTypeOfField(fieldId))));
        } else {
            this.stack.push(this.script.composeFieldValueReference(path, PathExpression.class));
        }
    }

    @Override
    public void exitUntypedAttributeValueReference(UntypedAttributeValueReferenceContext ctx) {
        this.stack.push(
                this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
                        ctx.Identifier().getText(), StringExpression.class));
    }

    /*** References with context override ***/

    /**
     * Handles expressions of the form ContextField::ReferencedField. Changes the context before the
     * reference is resolved.
     */
    @Override
    public void exitFieldContext(FieldContextContext ctx) {
        this.stack.pop(PathExpression.class); // Discard the PathExpression placed in the stack for
                                              // the context field.
        final String contextFieldId =
                ctx.context.reference.simpleFieldReference().FieldId().getText();
        this.efxContext.push(new FieldContext(contextFieldId,
                this.symbols.getAbsolutePathOfField(contextFieldId), this.symbols
                        .getRelativePathOfField(contextFieldId, this.efxContext.absolutePath())));
    }


    /**
     * Handles expressions of the form ContextField::ReferencedField. Changes the context before the
     * reference is resolved.
     */
    @Override
    public void exitFieldReferenceWithFieldContextOverride(
            FieldReferenceWithFieldContextOverrideContext ctx) {
        if (ctx.context != null) {
            final PathExpression field = this.stack.pop(PathExpression.class);
            this.stack.push(this.script.joinPaths(this.efxContext.relativePath(), field));
            this.efxContext.pop(); // Restores the previous context
        }
    }

    /**
     * Handles expressions of the form ContextNode::ReferencedField. Changes the context before the
     * reference is resolved.
     */
    @Override
    public void exitNodeContext(NodeContextContext ctx) {
        this.stack.pop(PathExpression.class); // Discard the PathExpression placed in the stack for
                                              // the context node.
        final String contextNodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx.context);
        this.efxContext.push(new NodeContext(contextNodeId,
                this.symbols.getAbsolutePathOfNode(contextNodeId),
                this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.absolutePath())));
    }

    /**
     * Handles expressions of the form ContextNode::ReferencedField. Restores the context after the
     * reference is resolved.
     */
    @Override
    public void exitFieldReferenceWithNodeContextOverride(
            FieldReferenceWithNodeContextOverrideContext ctx) {
        if (ctx.context != null) {
            final PathExpression field = this.stack.pop(PathExpression.class);
            this.stack.push(this.script.joinPaths(this.efxContext.relativePath(), field));
            this.efxContext.pop(); // Restores the previous context
        }
    }

    /*** Other References ***/

    @Override
    public void exitCodelistReference(CodelistReferenceContext ctx) {
        this.stack.push(this.script
                .composeList(this.symbols.expandCodelist(ctx.codeListId.getText()).stream()
                        .map(s -> this.script.getStringLiteralFromUnquotedString(s))
                        .collect(Collectors.toList()), StringListExpression.class));
    }

    @Override
    public void exitStringVariable(StringVariableContext ctx) {
        this.stack.push(this.script.composeVariableReference(ctx.Variable().getText(), StringExpression.class));
    }

    @Override
    public void exitBooleanVariable(BooleanVariableContext ctx) {
        this.stack.push(this.script.composeVariableReference(ctx.Variable().getText(), BooleanExpression.class));
    }
    
    @Override
    public void exitNumericVariable(NumericVariableContext ctx) {
        this.stack.push(this.script.composeVariableReference(ctx.Variable().getText(), NumericExpression.class));
    }

    @Override
    public void exitDateVariable(DateVariableContext ctx) {
        this.stack.push(this.script.composeVariableReference(ctx.Variable().getText(), DateExpression.class));
    }

    @Override
    public void exitTimeVariable(TimeVariableContext ctx) {
        this.stack.push(this.script.composeVariableReference(ctx.Variable().getText(), TimeExpression.class));
    }

    @Override
    public void exitDurationVariable(DurationVariableContext ctx) {
        this.stack.push(this.script.composeVariableReference(ctx.Variable().getText(), DurationExpression.class));
    }

    /*** Boolean functions ***/

    @Override
    public void exitNotFunction(NotFunctionContext ctx) {
        this.stack.push(this.script.composeLogicalNot(this.stack.pop(BooleanExpression.class)));
    }

    @Override
    public void exitContainsFunction(ContainsFunctionContext ctx) {
        final StringExpression needle = this.stack.pop(StringExpression.class);
        final StringExpression haystack = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.composeContainsCondition(haystack, needle));
    }

    @Override
    public void exitStartsWithFunction(StartsWithFunctionContext ctx) {
        final StringExpression startsWith = this.stack.pop(StringExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.composeStartsWithCondition(text, startsWith));
    }

    @Override
    public void exitEndsWithFunction(EndsWithFunctionContext ctx) {
        final StringExpression endsWith = this.stack.pop(StringExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        this.stack.push(this.script.composeEndsWithCondition(text, endsWith));
    }

    /*** Numeric functions ***/

    @Override
    public void exitCountFunction(CountFunctionContext ctx) {
        this.stack.push(this.script.composeCountOperation(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitNumberFunction(NumberFunctionContext ctx) {
        this.stack.push(
                this.script.composeToNumberConversion(this.stack.pop(StringExpression.class)));
    }

    @Override
    public void exitSumFunction(SumFunctionContext ctx) {
        this.stack.push(this.script.composeSumOperation(this.stack.pop(PathExpression.class)));
    }

    @Override
    public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
        this.stack.push(
                this.script.composeStringLengthCalculation(this.stack.pop(StringExpression.class)));
    }

    /*** String functions ***/

    @Override
    public void exitSubstringFunction(SubstringFunctionContext ctx) {
        final NumericExpression length =
                ctx.length != null ? this.stack.pop(NumericExpression.class) : null;
        final NumericExpression start = this.stack.pop(NumericExpression.class);
        final StringExpression text = this.stack.pop(StringExpression.class);
        if (length != null) {
            this.stack.push(this.script.composeSubstringExtraction(text, start, length));
        } else {
            this.stack.push(this.script.composeSubstringExtraction(text, start));
        }
    }

    @Override
    public void exitToStringFunction(ToStringFunctionContext ctx) {
        this.stack.push(
                this.script.composeToStringConversion(this.stack.pop(NumericExpression.class)));
    }

    @Override
    public void exitConcatFunction(ConcatFunctionContext ctx) {
        if (this.stack.empty() || ctx.stringExpression().size() == 0) {
            this.stack.push(this.script.composeStringConcatenation(Collections.emptyList()));
            return;
        }

        List<StringExpression> list = new ArrayList<>();
        for (int i = 0; i < ctx.stringExpression().size(); i++) {
            list.add(0, this.stack.pop(StringExpression.class));
        }
        this.stack.push(this.script.composeStringConcatenation(list));
    }

    @Override
    public void exitFormatNumberFunction(FormatNumberFunctionContext ctx) {
        final StringExpression format = this.stack.pop(StringExpression.class);
        final NumericExpression number = this.stack.pop(NumericExpression.class);
        this.stack.push(this.script.composeNumberFormatting(number, format));
    }

    /*** Date functions ***/

    @Override
    public void exitDateFromStringFunction(DateFromStringFunctionContext ctx) {
        this.stack
                .push(this.script.composeToDateConversion(this.stack.pop(StringExpression.class)));
    }

    @Override
    public void exitDatePlusMeasureFunction(DatePlusMeasureFunctionContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DateExpression left = this.stack.pop(DateExpression.class);
        this.stack.push(this.script.composeAddition(left, right));
    }

    @Override
    public void exitDateMinusMeasureFunction(DateMinusMeasureFunctionContext ctx) {
        DurationExpression right = this.stack.pop(DurationExpression.class);
        DateExpression left = this.stack.pop(DateExpression.class);
        this.stack.push(this.script.composeSubtraction(left, right));
    }

    /*** Time functions ***/

    @Override
    public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
        this.stack
                .push(this.script.composeToTimeConversion(this.stack.pop(StringExpression.class)));
    }
}
