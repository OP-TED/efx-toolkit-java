/*
 * Copyright 2023 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model.expressions.sequence;

import static java.util.Map.entry;

import java.util.Map;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;
import eu.europa.ted.efx.model.types.FieldTypes;

/**
 * A {@link TypedExpression} representing multiple values (as opposed to a single value).
 *
 * Concrete implementations are typed by primitive: {@link BooleanSequenceExpression},
 * {@link StringSequenceExpression}, {@link NumericSequenceExpression}, {@link DateSequenceExpression},
 * {@link TimeSequenceExpression}, {@link DurationSequenceExpression}.
 *
 * @see eu.europa.ted.efx.model.expressions.scalar.ScalarExpression for single-value expressions
 */
public interface SequenceExpression extends TypedExpression {

  /**
   * Maps {@link FieldTypes} to the corresponding {@link SequenceExpression}.
   */
  Map<FieldTypes, Class<? extends SequenceExpression>> fromFieldType = Map.ofEntries(
      entry(FieldTypes.ID, StringSequenceExpression.class), //
      entry(FieldTypes.ID_REF, StringSequenceExpression.class), //
      entry(FieldTypes.TEXT, StringSequenceExpression.class), //
      entry(FieldTypes.TEXT_MULTILINGUAL, MultilingualStringSequenceExpression.class), //
      entry(FieldTypes.INDICATOR, BooleanSequenceExpression.class), //
      entry(FieldTypes.AMOUNT, NumericSequenceExpression.class), //
      entry(FieldTypes.NUMBER, NumericSequenceExpression.class), //
      entry(FieldTypes.MEASURE, NumericSequenceExpression.class), //
      entry(FieldTypes.DURATION, DurationSequenceExpression.class), //
      entry(FieldTypes.CODE, StringSequenceExpression.class), //
      entry(FieldTypes.INTERNAL_CODE, StringSequenceExpression.class), //
      entry(FieldTypes.INTEGER, NumericSequenceExpression.class), //
      entry(FieldTypes.DATE, DateSequenceExpression.class), //
      entry(FieldTypes.ZONED_DATE, DateSequenceExpression.class), //
      entry(FieldTypes.TIME, TimeSequenceExpression.class), //
      entry(FieldTypes.ZONED_TIME, TimeSequenceExpression.class), //
      entry(FieldTypes.URL, StringSequenceExpression.class), //
      entry(FieldTypes.PHONE, StringSequenceExpression.class), //
      entry(FieldTypes.EMAIL, StringSequenceExpression.class));

  /**
   * Maps primitive {@link EfxDataType} to the corresponding {@link SequenceExpression}.
   */
  Map<Class<? extends EfxDataType.Primitive>, Class<? extends SequenceExpression>> fromEfxDataType = Map
      .ofEntries(
          entry(EfxDataType.String.class, StringSequenceExpression.class), //
          entry(EfxDataType.MultilingualString.class, MultilingualStringSequenceExpression.class), //
          entry(EfxDataType.Boolean.class, BooleanSequenceExpression.class), //
          entry(EfxDataType.Number.class, NumericSequenceExpression.class), //
          entry(EfxDataType.Date.class, DateSequenceExpression.class), //
          entry(EfxDataType.Time.class, TimeSequenceExpression.class), //
          entry(EfxDataType.Duration.class, DurationSequenceExpression.class) //
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
  static <T extends SequenceExpression> T from(TypedExpression source, Class<? extends T> returnType) {
    return Expression.from(source, returnType);
  }

  /**
   * Creates an object that implements {@link SequenceExpression} and conforms to the
   * given {@link EfxDataType}.
   * 
   * @param script      The target language script that resolves to a value.
   * @param efxDataType The {@link EfxDataType} of the field (or node) that the
   *                    returned {@link SequenceExpression} points to.
   * 
   * @return An object that implements {@link SequenceExpression} and conforms to the
   *         given {@link EfxDataType}.
   */
  static SequenceExpression instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    return Expression.instantiate(script, fromEfxDataType.get(EfxTypeLattice.toPrimitive(efxDataType)));
  }

  /**
   * A base class for {@link SequenceExpression} implementations.
   *
   * @param <T> the data type of the sequence expression result
   */
  public abstract class Impl<T extends EfxDataType> extends TypedExpression.Impl<T>
      implements SequenceExpression {

    protected Impl(final String script, Class<? extends T> dataType) {
      super(script, dataType);
    }
  }
}