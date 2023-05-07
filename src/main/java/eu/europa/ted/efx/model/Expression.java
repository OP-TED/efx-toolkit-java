package eu.europa.ted.efx.model;

import static java.util.Map.entry;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * This class represents an expression in the target scripting language.
 * 
 * The class is used to restrict the parameter types and return types of the methods that the EFX
 * translator calls. This makes it easier for users to understand the nature and usage of each
 * parameter when implementing {@link eu.europa.ted.efx.interfaces.ScriptGenerator},
 * {@link eu.europa.ted.efx.interfaces.MarkupGenerator} and
 * {@link eu.europa.ted.efx.interfaces.SymbolResolver} interfaces for translating to a new target
 * language. It also enables to EFX translator to perform type checking of EFX expressions.
 *
 */
public class Expression extends CallStackObject {

  /**
   * eForms types are mapped to Expression types.
   */
  public static final Map<String, Class<? extends Expression>> types =
      Map.ofEntries(entry("id", StringExpression.class), //
          entry("id-ref", StringExpression.class), //
          entry("text", StringExpression.class), //
          entry("text-multilingual", MultilingualStringExpression.class), //
          entry("indicator", BooleanExpression.class), //
          entry("amount", NumericExpression.class), //
          entry("number", NumericExpression.class), //
          entry("measure", DurationExpression.class), //
          entry("code", StringExpression.class), entry("internal-code", StringExpression.class), //
          entry("integer", NumericExpression.class), //
          entry("date", DateExpression.class), //
          entry("zoned-date", DateExpression.class), //
          entry("time", TimeExpression.class), //
          entry("zoned-time", TimeExpression.class), //
          entry("url", StringExpression.class), //
          entry("phone", StringExpression.class), //
          entry("email", StringExpression.class));

  /**
   * ListExpression types equivalent to eForms types.
   */
  public static final Map<String, Class<? extends Expression>> listTypes = Map.ofEntries(
      entry("id", StringListExpression.class), //
      entry("id-ref", StringListExpression.class), //
      entry("text", StringListExpression.class), //
      entry("text-multilingual", MultilingualStringListExpression.class), //
      entry("indicator", BooleanListExpression.class), //
      entry("amount", NumericListExpression.class), //
      entry("number", NumericListExpression.class), //
      entry("measure", DurationListExpression.class), //
      entry("code", StringListExpression.class), entry("internal-code", StringListExpression.class), //
      entry("integer", NumericListExpression.class), //
      entry("date", DateListExpression.class), //
      entry("zoned-date", DateListExpression.class), //
      entry("time", TimeListExpression.class), //
      entry("zoned-time", TimeListExpression.class), //
      entry("url", StringListExpression.class), //
      entry("phone", StringListExpression.class), //
      entry("email", StringListExpression.class));

  /**
   * Stores the expression represented in the target language.
   */
  public final String script;

  public final Boolean isLiteral;

  public Expression(final String script) {
    this.script = script;
    this.isLiteral = false;
  }

  public Expression(final String script, final Boolean isLiteral) {
    this.script = script;
    this.isLiteral = isLiteral;
  }

  public static <T extends Expression> T instantiate(String script, Class<T> type) {
    try {
      Constructor<T> constructor = type.getConstructor(String.class);
      return constructor.newInstance(script);
    } catch (Exception e) {
      throw new ParseCancellationException(e);
    }
  }

  public static <T extends Expression> T empty(Class<T> type) { 
    return instantiate("", type);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (Expression.class.isAssignableFrom(obj.getClass())) {
      return this.script.equals(((Expression) obj).script);
    }

    return false;
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

  public static class ContextExpression extends Expression {

    public ContextExpression(final String script) {
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

    public BooleanExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
    }
  }

  /**
   * Represents a numeric expression, value or literal in the target language.
   */
  public static class NumericExpression extends Expression {

    public NumericExpression(final String script) {
      super(script);
    }

    public NumericExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
    }
  }

  /**
   * Represents a string expression, value or literal in the target language.
   */
  public static class StringExpression extends Expression {

    public StringExpression(final String script) {
      super(script);
    }

    public StringExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
    }
  }

  public static class MultilingualStringExpression extends StringExpression {

    public MultilingualStringExpression(final String script) {
      super(script);
    }

    public MultilingualStringExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
    }
  }

  /**
   * Represents a date expression or value in the target language.
   */
  public static class DateExpression extends Expression {

    public DateExpression(final String script) {
      super(script);
    }

    public DateExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
    }
  }

  /**
   * Represents a time expression, value or literal in the target language.
   */
  public static class TimeExpression extends Expression {

    public TimeExpression(final String script) {
      super(script);
    }

    public TimeExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
    }
  }

  /**
   * Represents a duration expression, value or literal in the target language.
   */
  public static class DurationExpression extends Expression {

    public DurationExpression(final String script) {
      super(script);
    }
    
    public DurationExpression(final String script, final Boolean isLiteral) {
      super(script, isLiteral);
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

  public static class MultilingualStringListExpression extends ListExpression<MultilingualStringExpression> {

    public MultilingualStringListExpression(final String script) {
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

  /**
   * Used to represent iterators (for traversing a list using a variable) 
   */
  public static class IteratorExpression extends Expression {

    public IteratorExpression(final String script) {
      super(script);
    }
  }

  /**
   * Used to represent a collection of {@link IteratorExpression}.
   */
  public static class IteratorListExpression extends ListExpression<IteratorExpression> {

    public IteratorListExpression(final String script) {
      super(script);
    }
  }
}
