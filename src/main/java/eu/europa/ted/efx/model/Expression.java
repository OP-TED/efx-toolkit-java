package eu.europa.ted.efx.model;

import static java.util.Map.entry;
import java.util.Map;

/**
 * This class represents an expression in the target scripting language.
 * 
 * The class is used to restrict the parameter types and return types of the methods that the EFX
 * translator calls. This makes it easier for users to understand the nature and usage of each
 * parameter when implementing {@link eu.europa.ted.efx.interfaces.ScriptGenerator}, {@link eu.europa.ted.efx.interfaces.MarkupGenerator}
 * and {@link eu.europa.ted.efx.interfaces.SymbolResolver} interfaces
 * for translating to a new target language. It also enables to EFX translator to perform type
 * checking of EFX expressions.
 *
 */
public class Expression extends CallStackObjectBase {

    /**
     * eForms types are mapped to Expression types.
     */
    public static final Map<String, Class<? extends Expression>> types = Map.ofEntries(
            entry("id", StringExpression.class), //
            entry("id-ref", StringExpression.class), //
            entry("text", StringExpression.class), //
            entry("text-multilingual", StringExpression.class), //
            entry("indicator", BooleanExpression.class), //
            entry("amount", NumericExpression.class), // 
            entry("number", NumericExpression.class), //
            entry("measure", DurationExpression.class), //
            entry("code", StringExpression.class),
            entry("internal-code", StringExpression.class), //
            entry("integer", NumericExpression.class), //
            entry("date", DateExpression.class), //
            entry("zoned-date", DateExpression.class), //
            entry("time", TimeExpression.class), //
            entry("zoned-time", TimeExpression.class), //
            entry("url", StringExpression.class), //
            entry("phone", StringExpression.class), //
            entry("email", StringExpression.class));

    /**
     * Stores the expression represented in the target language.
     */
    public final String script;

    public Expression(final String script) {
        this.script = script;
    }

    /**
     * An PathExpression points to a node in your data set.
     * 
     * Typically the data set is a XML document and therefore, in this case, the path expression is a
     * XPath expression.
     */
    public static class PathExpression extends Expression {

        public PathExpression(final String script) {
            super(script);
        }
    }

    /**
     * Represents a boolean expression, value or literal in the target language.
     */
    public static class BooleanExpression extends Expression {

        public BooleanExpression(final String script) {
            super(script);
        }
    }

    /**
     * Represents a numeric expression, value or literal in the target language.
     */
    public static class NumericExpression extends Expression {

        public NumericExpression(final String script) {
            super(script);
        }
    }

    /**
     * Represents a string expression, value or literal in the target language.
     */
    public static class StringExpression extends Expression {

        public StringExpression(final String script) {
            super(script);
        }
    }

    /**
     * Represents a date expression or value in the target language.
     */
    public static class DateExpression extends Expression {

        public DateExpression(final String script) {
            super(script);
        }
    }

    /**
     * Represents a time expression, value or literal in the target language.
     */
    public static class TimeExpression extends Expression {

        public TimeExpression(final String script) {
            super(script);
        }
    }

    /** 
     * Represents a duration expression, value or literal in the target language.
     */
    public static class DurationExpression extends Expression {

        public DurationExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of strings in the target language.
     */
    public static class ListExpression<T extends Expression> extends Expression {

        public ListExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of strings in the target language.
     */
    public static class StringListExpression extends ListExpression<StringExpression> {

        public StringListExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of numbers in the target language.
     */
    public static class NumericListExpression extends ListExpression<NumericExpression> {

        public NumericListExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of dates in the target language.
     */
    public static class DateListExpression extends ListExpression<DateExpression> {

        public DateListExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of times in the target language.
     */
    public static class TimeListExpression extends ListExpression<TimeExpression> {

        public TimeListExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of durations in the target language.
     */
    public static class DurationListExpression extends ListExpression<DurationExpression> {

        public DurationListExpression(final String script) {
            super(script);
        }
    }


    /**
     * Used to represent a list of booleans in the target language.
     */
    public static class BooleanListExpression extends ListExpression<BooleanExpression> {

        public BooleanListExpression(final String script) {
            super(script);
        }
    }
    
}
