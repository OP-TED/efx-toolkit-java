package eu.europa.ted.efx.autocomplete;

import java.util.Arrays;
import java.util.Optional;

import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType.BooleanScalar;
import eu.europa.ted.efx.model.types.EfxDataType.BooleanSequence;
import eu.europa.ted.efx.model.types.EfxDataType.DateScalar;
import eu.europa.ted.efx.model.types.EfxDataType.DateSequence;
import eu.europa.ted.efx.model.types.EfxDataType.DurationScalar;
import eu.europa.ted.efx.model.types.EfxDataType.DurationSequence;
import eu.europa.ted.efx.model.types.EfxDataType.NumberScalar;
import eu.europa.ted.efx.model.types.EfxDataType.NumberSequence;
import eu.europa.ted.efx.model.types.EfxDataType.StringScalar;
import eu.europa.ted.efx.model.types.EfxDataType.StringSequence;
import eu.europa.ted.efx.model.types.EfxDataType.TimeScalar;
import eu.europa.ted.efx.model.types.EfxDataType.TimeSequence;

/**
 * The EFX data types, as defined by the EFX grammar.
 *
 * <p>Each names a type of the toolkit's lattice, a value type in one cardinality, and stands for
 * that type and every type below it, such as a multilingual text for {@link #TEXT}. A node has no
 * data type here.</p>
 */
public enum EfxDataType {
  TEXT(StringScalar.class),
  NUMBER(NumberScalar.class),
  INDICATOR(BooleanScalar.class),
  DATE(DateScalar.class),
  TIME(TimeScalar.class),
  DURATION(DurationScalar.class),
  TEXT_SEQUENCE(StringSequence.class),
  NUMBER_SEQUENCE(NumberSequence.class),
  INDICATOR_SEQUENCE(BooleanSequence.class),
  DATE_SEQUENCE(DateSequence.class),
  TIME_SEQUENCE(TimeSequence.class),
  DURATION_SEQUENCE(DurationSequence.class);

  private final Class<? extends eu.europa.ted.efx.model.types.EfxDataType> type;

  EfxDataType(final Class<? extends eu.europa.ted.efx.model.types.EfxDataType> type) {
    this.type = type;
  }

  /**
   * Finds the data type of a parameter type of the signature catalogue.
   *
   * @param parameterType a parameter or result type, with {@code T} bound
   * @return the data type that stands for it, or nothing for a node
   */
  static Optional<EfxDataType> of(final ParameterType<?> parameterType) {
    final var type = TypedExpression.getEfxDataType(parameterType.getExpressionType());
    return Arrays.stream(values()).filter(value -> value.type.isAssignableFrom(type)).findFirst();
  }
}
