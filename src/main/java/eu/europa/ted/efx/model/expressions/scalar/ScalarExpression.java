package eu.europa.ted.efx.model.expressions.scalar;

import static java.util.Map.entry;

import java.util.Map;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxExpressionType;
import eu.europa.ted.efx.model.types.EfxExpressionTypeAssociation;
import eu.europa.ted.efx.model.types.FieldTypes;

public interface ScalarExpression extends TypedExpression, EfxExpressionType.Scalar {

  /**
   * Maps {@link FieldTypes} to their corresponding {@link ScalarExpression}
   * class.
   */
  Map<FieldTypes, Class<? extends ScalarExpression>> fromFieldType = Map.ofEntries(
      entry(FieldTypes.ID, StringExpression.class), //
      entry(FieldTypes.ID_REF, StringExpression.class), //
      entry(FieldTypes.TEXT, StringExpression.class), //
      entry(FieldTypes.TEXT_MULTILINGUAL, MultilingualStringExpression.class), //
      entry(FieldTypes.INDICATOR, BooleanExpression.class), //
      entry(FieldTypes.AMOUNT, NumericExpression.class), //
      entry(FieldTypes.NUMBER, NumericExpression.class), //
      entry(FieldTypes.MEASURE, DurationExpression.class), //
      entry(FieldTypes.CODE, StringExpression.class), entry(FieldTypes.INTERNAL_CODE, StringExpression.class), //
      entry(FieldTypes.INTEGER, NumericExpression.class), //
      entry(FieldTypes.DATE, DateExpression.class), //
      entry(FieldTypes.ZONED_DATE, DateExpression.class), //
      entry(FieldTypes.TIME, TimeExpression.class), //
      entry(FieldTypes.ZONED_TIME, TimeExpression.class), //
      entry(FieldTypes.URL, StringExpression.class), //
      entry(FieldTypes.PHONE, StringExpression.class), //
      entry(FieldTypes.EMAIL, StringExpression.class));

  /**
   * Maps {@link EfxDataType} to their corresponding {@link ScalarExpression}
   * class.
   */
  Map<Class<? extends EfxDataType>, Class<? extends ScalarExpression>> fromEfxDataType = Map
      .ofEntries(
          entry(EfxDataType.String.class, StringExpression.class), //
          entry(EfxDataType.MultilingualString.class, MultilingualStringExpression.class), //
          entry(EfxDataType.Boolean.class, BooleanExpression.class), //
          entry(EfxDataType.Number.class, NumericExpression.class), //
          entry(EfxDataType.Date.class, DateExpression.class), //
          entry(EfxDataType.Time.class, TimeExpression.class), //
          entry(EfxDataType.Duration.class, DurationExpression.class) //
      );

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
  static <T extends ScalarExpression> T from(TypedExpression source, Class<T> returnType) {
    return Expression.from(source, returnType);
  }

  /**
   * Creates an object that implements {@link ScalarExpression} and conforms to
   * the given {@link EfxDataType}.
   * 
   * @param script      The target language script that resolves to a value.
   * @param efxDataType The {@link EfxDataType} of the field (or node) that the
   *                    returned {@link ScalarExpression} points to.
   * 
   * @return An object that implements {@link ScalarExpression} and conforms to
   *         the
   *         given {@link EfxDataType}.
   */
  static ScalarExpression instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    return Expression.instantiate(script, fromEfxDataType.get(efxDataType));
  }

  /**
   * A base class for {@link ScalarExpression} implementations.
   */
  @EfxExpressionTypeAssociation(expressionType = EfxExpressionType.Scalar.class)
  public abstract class Impl<T extends EfxDataType> extends TypedExpression.Impl<T>
      implements ScalarExpression {

    protected Impl(String script, Class<T> dataType) {
      super(script, EfxExpressionType.Scalar.class, dataType);
    }

    protected Impl(String script, Boolean isLiteral, Class<? extends T> dataType) {
      super(script, isLiteral, EfxExpressionType.Scalar.class, dataType);
    }
  }
}