package eu.europa.ted.efx.model;

import static java.util.Map.entry;
import java.util.Map;
import eu.europa.ted.efx.interfaces.MarkupGenerator;

/**
 * This class represents an expression in the target scripting language.
 * 
 * The class is used to restric the parameter types and return types of the methods that the EFX
 * translator calls. This makes it easier for users to understand the nature and usage of each
 * parameter when implementing {@link ScriptGenerator}, {@link MarkupGenerator} and {@link SymbolResolver} interfaces
 * for transalting to a new target language. It also enables to EFX translator to parform type
 * checking of EFX expressions.
 *
 */
public class Expression extends StackObjectBase {

    /**
     * eForms types a mapped to Expression types. This allows the tran
     */
    public static final Map<String, Class<? extends Expression>> types = Map.ofEntries(
            entry("internal-code", StringExpression.class), entry("id", StringExpression.class),
            entry("text-multilingual", StringExpression.class), entry("date", DateExpression.class),
            entry("time", TimeExpression.class), entry("code", StringExpression.class),
            entry("integer", NumericExpression.class), entry("indicator", BooleanExpression.class),
            entry("amount", NumericExpression.class), entry("url", StringExpression.class),
            entry("text", StringExpression.class), entry("zoned-date", DateExpression.class),
            entry("zoned-time", TimeExpression.class), entry("id-ref", StringExpression.class),
            entry("number", NumericExpression.class), entry("measure", NumericExpression.class),
            entry("phone", StringExpression.class), entry("email", StringExpression.class));


    /**
     * Stores the expression represented in the target language.
     */
    public final String script;

    public Expression(final String script) {
        this.script = script;
    }

    /**
     * A PathExpression points to a node in your data set.
     * 
     * Typicaly the data set is a XML document and therefore, in this case, the path expression is a
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

    public static class ListExpression extends Expression {

        public ListExpression(final String script) {
            super(script);
        }
    }

    /**
     * Used to represent a list of strings in the target language.
     */
    public static class StringListExpression extends ListExpression {

        public StringListExpression(final String script) {
            super(script);
        }
    }
}
