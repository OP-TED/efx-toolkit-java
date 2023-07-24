package eu.europa.ted.efx.model.expressions.path;

import static java.util.Map.entry;

import java.util.Map;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxExpressionType;
import eu.europa.ted.efx.model.types.EfxExpressionTypeAssociation;
import eu.europa.ted.efx.model.types.FieldTypes;

/**
 * A {@link PathExpression} is a {@link TypedExpression} that represents a
 * pointer (a path) to an element in a structured document. Typically the
 * document will be an XML document but it could also be a JSON
 * object or any other structured data source.
 * 
 * {@link PathExpression} objects are used to resolve eForms fields and Nodes.
 * To develop an intuition on the behaviour of a {@link PathExpression}, you can
 * think of it as being similar to an XPath expression.
 *
 * The {@link #getScript()} method should return an expression in the target
 * language that resolves to a value.
 */
public interface PathExpression extends ScalarExpression, SequenceExpression, EfxExpressionType.Path {

  /**
   * Maps {@link FieldTypes}, to concrete java classes that implement
   * {@link PathExpression}.
   */
  Map<FieldTypes, Class<? extends PathExpression>> fromFieldType = Map.ofEntries(
      entry(FieldTypes.ID, StringPathExpression.class), //
      entry(FieldTypes.ID_REF, StringPathExpression.class), //
      entry(FieldTypes.TEXT, StringPathExpression.class), //
      entry(FieldTypes.TEXT_MULTILINGUAL, MultilingualStringPathExpression.class), //
      entry(FieldTypes.INDICATOR, BooleanPathExpression.class), //
      entry(FieldTypes.AMOUNT, NumericPathExpression.class), //
      entry(FieldTypes.NUMBER, NumericPathExpression.class), //
      entry(FieldTypes.MEASURE, DurationPathExpression.class), //
      entry(FieldTypes.CODE, StringPathExpression.class),
      entry(FieldTypes.INTERNAL_CODE, StringPathExpression.class), //
      entry(FieldTypes.INTEGER, NumericPathExpression.class), //
      entry(FieldTypes.DATE, DatePathExpression.class), //
      entry(FieldTypes.ZONED_DATE, DatePathExpression.class), //
      entry(FieldTypes.TIME, TimePathExpression.class), //
      entry(FieldTypes.ZONED_TIME, TimePathExpression.class), //
      entry(FieldTypes.URL, StringPathExpression.class), //
      entry(FieldTypes.PHONE, StringPathExpression.class), //
      entry(FieldTypes.EMAIL, StringPathExpression.class));

  /**
   * Maps subclasses of {@link EfxDataType}, to concrete java classes that
   * implement {@link PathExpression}.
   */
  Map<Class<? extends EfxDataType>, Class<? extends PathExpression>> fromEfxDataType = Map.ofEntries(
      entry(EfxDataType.String.class, StringPathExpression.class), //
      entry(EfxDataType.MultilingualString.class, MultilingualStringPathExpression.class), //
      entry(EfxDataType.Boolean.class, BooleanPathExpression.class), //
      entry(EfxDataType.Number.class, NumericPathExpression.class), //
      entry(EfxDataType.Date.class, DatePathExpression.class), //
      entry(EfxDataType.Time.class, TimePathExpression.class), //
      entry(EfxDataType.Duration.class, DurationPathExpression.class), //
      entry(EfxDataType.Node.class, NodePathExpression.class) //
  );

  /**
   * Creates an object that implements {@link PathExpression} and conforms to the
   * given field type.
   * 
   * @param script    The target language script that resolves to a value.
   * @param fieldType The type of field (or node) that the returned
   *                  {@link PathExpression} points to.
   * 
   * @return An object that implements {@link PathExpression} and conforms to the
   *         given field type.
   */
  static PathExpression instantiate(String script, FieldTypes fieldType) {
    return Expression.instantiate(script, fromFieldType.get(fieldType));
  }

  /**
   * Creates an object that implements {@link PathExpression} and conforms to the
   * given {@link EfxDataType}.
   * 
   * @param script      The target language script that resolves to a value.
   * @param efxDataType The {@link EfxDataType} of the field (or node) that the
   *                    returned {@link PathExpression} points to.
   * 
   * @return An object that implements {@link PathExpression} and conforms to the
   *         given {@link EfxDataType}.
   */
  static PathExpression instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    return Expression.instantiate(script, fromEfxDataType.get(efxDataType));
  }

  /**
   * Creates an object of the given type, by using the given
   * {@link TypedExpression} as a source.
   * 
   * @param <T>        The type of the returned object.
   * @param source     The source {@link TypedExpression} to copy.
   * @param returnType The type of object to be returned.
   * 
   * @return An object of the given type, having the same property values as the
   *         source.
   */
  static <T extends PathExpression> T from(TypedExpression source, Class<T> returnType) {
    return Expression.from(source, returnType);
  }

  /**
   * A base class for {@link PathExpression} implementations.
   */
  @EfxExpressionTypeAssociation(expressionType = EfxExpressionType.Path.class)
  public abstract class Impl<T extends EfxDataType> extends TypedExpression.Impl<T>
      implements PathExpression {

    protected Impl(final String script, Class<? extends T> dataType) {
      super(script, EfxExpressionType.Path.class, dataType);
    }
  }
}