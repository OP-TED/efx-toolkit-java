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
import eu.europa.ted.efx.model.expressions.LiteralExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

/**
 * A {@link ScalarExpression} that is also a {@link LiteralExpression}.
 *
 * Represents a single constant value known at translation time, embedded directly in the
 * generated script.
 *
 * @see eu.europa.ted.efx.model.expressions.sequence.SequenceLiteral for sequences of literals
 */
public interface ScalarLiteral extends ScalarExpression, LiteralExpression {

  /**
   * Maps primitive {@link EfxDataType} to concrete scalar literal expression classes.
   */
  Map<Class<? extends EfxDataType.Primitive>, Class<? extends ScalarLiteral>> fromEfxDataType = Map.ofEntries(
      entry(EfxDataType.String.class, StringLiteral.class),
      entry(EfxDataType.MultilingualString.class, StringLiteral.class),
      entry(EfxDataType.Boolean.class, BooleanLiteral.class),
      entry(EfxDataType.Number.class, NumericLiteral.class),
      entry(EfxDataType.Date.class, DateLiteral.class),
      entry(EfxDataType.Time.class, TimeLiteral.class),
      entry(EfxDataType.Duration.class, DurationLiteral.class)
  );

  /**
   * Creates a {@link ScalarLiteral} for the given {@link EfxDataType}.
   *
   * @param <T>         The type of the returned object.
   * @param script      The target language script representing the literal value.
   * @param efxDataType The {@link EfxDataType} of the literal value.
   *
   * @return A {@link ScalarLiteral} for the given data type.
   */
  @SuppressWarnings("unchecked")
  static <T extends ScalarLiteral> T instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    Class<? extends ScalarLiteral> type = fromEfxDataType.get(EfxTypeLattice.toPrimitive(efxDataType));
    return (T) Expression.instantiate(script, type);
  }
}
