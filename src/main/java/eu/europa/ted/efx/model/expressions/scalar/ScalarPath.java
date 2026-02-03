/*
 * Copyright 2026 European Union
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
package eu.europa.ted.efx.model.expressions.scalar;

import static java.util.Map.entry;

import java.util.Map;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;
import eu.europa.ted.efx.model.types.FieldTypes;

/**
 * A {@link ScalarExpression} that is also a {@link PathExpression}.
 *
 * References a single value in a document (non-repeatable from the current context).
 * Can convert to {@link SequencePath} via {@link #asSequence()}.
 *
 * @see SequencePath for paths that may resolve to multiple values
 */
public interface ScalarPath extends ScalarExpression, PathExpression {

  /**
   * Maps {@link FieldTypes} to concrete scalar path expression classes.
   */
  Map<FieldTypes, Class<? extends ScalarPath>> fromFieldType = Map.ofEntries(
      entry(FieldTypes.ID, StringPath.class), //
      entry(FieldTypes.ID_REF, StringPath.class), //
      entry(FieldTypes.TEXT, StringPath.class), //
      entry(FieldTypes.TEXT_MULTILINGUAL, MultilingualStringPath.class), //
      entry(FieldTypes.INDICATOR, BooleanPath.class), //
      entry(FieldTypes.AMOUNT, NumericPath.class), //
      entry(FieldTypes.NUMBER, NumericPath.class), //
      entry(FieldTypes.MEASURE, DurationPath.class), //
      entry(FieldTypes.CODE, StringPath.class),
      entry(FieldTypes.INTERNAL_CODE, StringPath.class), //
      entry(FieldTypes.INTEGER, NumericPath.class), //
      entry(FieldTypes.DATE, DatePath.class), //
      entry(FieldTypes.ZONED_DATE, DatePath.class), //
      entry(FieldTypes.TIME, TimePath.class), //
      entry(FieldTypes.ZONED_TIME, TimePath.class), //
      entry(FieldTypes.URL, StringPath.class), //
      entry(FieldTypes.PHONE, StringPath.class), //
      entry(FieldTypes.EMAIL, StringPath.class));

  /**
   * Maps primitive {@link EfxDataType} to concrete scalar path expression classes.
   */
  Map<Class<? extends EfxDataType.Primitive>, Class<? extends ScalarPath>> fromEfxDataType = Map.ofEntries(
      entry(EfxDataType.String.class, StringPath.class), //
      entry(EfxDataType.MultilingualString.class, MultilingualStringPath.class), //
      entry(EfxDataType.Boolean.class, BooleanPath.class), //
      entry(EfxDataType.Number.class, NumericPath.class), //
      entry(EfxDataType.Date.class, DatePath.class), //
      entry(EfxDataType.Time.class, TimePath.class), //
      entry(EfxDataType.Duration.class, DurationPath.class), //
      entry(EfxDataType.Node.class, NodePath.class) //
  );

  /**
   * Creates a {@link ScalarPath} for the given field type.
   *
   * @param <T>       The type of the returned object.
   * @param script    The target language script that resolves to a value.
   * @param fieldType The type of field that the returned expression points to.
   *
   * @return A {@link ScalarPath} for the given field type.
   */
  @SuppressWarnings("unchecked")
  static <T extends ScalarPath> T instantiate(String script, FieldTypes fieldType) {
    Class<? extends ScalarPath> type = fromFieldType.get(fieldType);
    return (T) Expression.instantiate(script, type);
  }

  /**
   * Creates an empty {@link ScalarPath} for the given field type.
   *
   * @param <T>       The type of the returned object.
   * @param fieldType The type of field that the returned expression points to.
   * @return An empty {@link ScalarPath} for the given field type.
   */
  @SuppressWarnings("unchecked")
  static <T extends ScalarPath> T empty(FieldTypes fieldType) {
    Class<? extends ScalarPath> type = fromFieldType.get(fieldType);
    return (T) Expression.instantiate("", type);
  }

  /**
   * Creates a {@link ScalarPath} for the given {@link EfxDataType}.
   *
   * @param <T>         The type of the returned object.
   * @param script      The target language script that resolves to a value.
   * @param efxDataType The {@link EfxDataType} of the field that the returned expression points to.
   *
   * @return A {@link ScalarPath} for the given data type.
   */
  @SuppressWarnings("unchecked")
  static <T extends ScalarPath> T instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    Class<? extends ScalarPath> type = fromEfxDataType.get(EfxTypeLattice.toPrimitive(efxDataType));
    return (T) Expression.instantiate(script, type);
  }

  /**
   * A base class for {@link ScalarPath} implementations.
   *
   * @param <T> the EFX data type for the expression.
   */
  public abstract class Impl<T extends EfxDataType> extends ScalarExpression.Impl<T>
      implements ScalarPath {

    protected Impl(final String script, Class<? extends T> dataType) {
      super(script, dataType);
    }

    @Override
    public ScalarPath asScalar() {
      return this;
    }

    @Override
    public SequencePath asSequence() {
      Class<? extends EfxDataType> sequenceType = EfxTypeLattice.toSequence(getDataType());
      return SequencePath.instantiate(getScript(), sequenceType);
    }
  }
}
