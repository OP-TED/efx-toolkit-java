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
package eu.europa.ted.efx.model.expressions.sequence;

import static java.util.Map.entry;

import java.util.Map;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;
import eu.europa.ted.efx.model.types.FieldTypes;

/**
 * A {@link SequenceExpression} that is also a {@link PathExpression}.
 *
 * References multiple values in a document (repeatable from the current context).
 * Can convert to {@link ScalarPath} via {@link #asScalar()}.
 *
 * @see ScalarPath for paths that resolve to a single value
 */
public interface SequencePath extends SequenceExpression, PathExpression {

  /**
   * Maps {@link FieldTypes} to concrete sequence path expression classes.
   */
  Map<FieldTypes, Class<? extends SequencePath>> fromFieldType = Map.ofEntries(
      entry(FieldTypes.ID, StringSequencePath.class), //
      entry(FieldTypes.ID_REF, StringSequencePath.class), //
      entry(FieldTypes.TEXT, StringSequencePath.class), //
      entry(FieldTypes.TEXT_MULTILINGUAL, MultilingualStringSequencePath.class), //
      entry(FieldTypes.INDICATOR, BooleanSequencePath.class), //
      entry(FieldTypes.AMOUNT, NumericSequencePath.class), //
      entry(FieldTypes.NUMBER, NumericSequencePath.class), //
      entry(FieldTypes.MEASURE, DurationSequencePath.class), //
      entry(FieldTypes.CODE, StringSequencePath.class),
      entry(FieldTypes.INTERNAL_CODE, StringSequencePath.class), //
      entry(FieldTypes.INTEGER, NumericSequencePath.class), //
      entry(FieldTypes.DATE, DateSequencePath.class), //
      entry(FieldTypes.ZONED_DATE, DateSequencePath.class), //
      entry(FieldTypes.TIME, TimeSequencePath.class), //
      entry(FieldTypes.ZONED_TIME, TimeSequencePath.class), //
      entry(FieldTypes.URL, StringSequencePath.class), //
      entry(FieldTypes.PHONE, StringSequencePath.class), //
      entry(FieldTypes.EMAIL, StringSequencePath.class));

  /**
   * Maps primitive {@link EfxDataType} to concrete sequence path expression classes.
   */
  Map<Class<? extends EfxDataType.Primitive>, Class<? extends SequencePath>> fromEfxDataType = Map.ofEntries(
      entry(EfxDataType.String.class, StringSequencePath.class), //
      entry(EfxDataType.MultilingualString.class, MultilingualStringSequencePath.class), //
      entry(EfxDataType.Boolean.class, BooleanSequencePath.class), //
      entry(EfxDataType.Number.class, NumericSequencePath.class), //
      entry(EfxDataType.Date.class, DateSequencePath.class), //
      entry(EfxDataType.Time.class, TimeSequencePath.class), //
      entry(EfxDataType.Duration.class, DurationSequencePath.class), //
      entry(EfxDataType.Node.class, NodeSequencePath.class) //
  );

  /**
   * Creates a {@link SequencePath} for the given field type.
   * Use this when the field is repeatable from the current context.
   *
   * @param <T>       The type of the returned object.
   * @param script    The target language script that resolves to a sequence.
   * @param fieldType The type of field that the returned expression points to.
   *
   * @return A {@link SequencePath} for the given field type.
   */
  @SuppressWarnings("unchecked")
  static <T extends SequencePath> T instantiate(String script, FieldTypes fieldType) {
    Class<? extends SequencePath> type = fromFieldType.get(fieldType);
    return (T) Expression.instantiate(script, type);
  }

  /**
   * Creates a {@link SequencePath} for the given {@link EfxDataType}.
   * Use this when the field is repeatable from the current context.
   *
   * @param <T>         The type of the returned object.
   * @param script      The target language script that resolves to a sequence.
   * @param efxDataType The {@link EfxDataType} of the field that the returned expression points to.
   *
   * @return A {@link SequencePath} for the given data type.
   */
  @SuppressWarnings("unchecked")
  static <T extends SequencePath> T instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    Class<? extends EfxDataType> primitiveType = EfxTypeLattice.toPrimitive(efxDataType);
    Class<? extends SequencePath> type = fromEfxDataType.get(primitiveType);
    return (T) Expression.instantiate(script, type);
  }

  /**
   * A base class for {@link SequencePath} implementations.
   *
   * @param <T> the EFX data type for the expression.
   */
  public abstract class Impl<T extends EfxDataType> extends SequenceExpression.Impl<T>
      implements SequencePath {

    protected Impl(final String script, Class<? extends T> dataType) {
      super(script, dataType);
    }

    @Override
    public ScalarPath asScalar() {
      Class<? extends EfxDataType> scalarType = EfxTypeLattice.toScalar(getDataType());
      return ScalarPath.instantiate(getScript(), scalarType);
    }

    @Override
    public SequencePath asSequence() {
      return this;
    }
  }
}
