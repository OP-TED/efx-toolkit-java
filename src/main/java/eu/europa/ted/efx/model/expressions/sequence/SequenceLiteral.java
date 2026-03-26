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
import eu.europa.ted.efx.model.expressions.LiteralExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

/**
 * A {@link SequenceExpression} that is also a {@link LiteralExpression}.
 *
 * Represents a sequence of constant values known at translation time, embedded directly in the
 * generated script.
 *
 * @see eu.europa.ted.efx.model.expressions.scalar.ScalarLiteral for single-value literals
 */
public interface SequenceLiteral extends SequenceExpression, LiteralExpression {

  /**
   * Maps primitive {@link EfxDataType} to concrete sequence literal expression classes.
   */
  Map<Class<? extends EfxDataType.Primitive>, Class<? extends SequenceLiteral>> fromEfxDataType = Map.ofEntries(
      entry(EfxDataType.String.class, StringSequenceLiteral.class),
      entry(EfxDataType.MultilingualString.class, StringSequenceLiteral.class),
      entry(EfxDataType.Boolean.class, BooleanSequenceLiteral.class),
      entry(EfxDataType.Number.class, NumericSequenceLiteral.class),
      entry(EfxDataType.Date.class, DateSequenceLiteral.class),
      entry(EfxDataType.Time.class, TimeSequenceLiteral.class),
      entry(EfxDataType.Duration.class, DurationSequenceLiteral.class)
  );

  /**
   * Creates a {@link SequenceLiteral} for the given {@link EfxDataType}.
   *
   * @param <T>         The type of the returned object.
   * @param script      The target language script representing the literal sequence.
   * @param efxDataType The {@link EfxDataType} of the literal sequence values.
   *
   * @return A {@link SequenceLiteral} for the given data type.
   */
  @SuppressWarnings("unchecked")
  static <T extends SequenceLiteral> T instantiate(String script, Class<? extends EfxDataType> efxDataType) {
    Class<? extends EfxDataType> primitiveType = EfxTypeLattice.toPrimitive(efxDataType);
    Class<? extends SequenceLiteral> type = fromEfxDataType.get(primitiveType);
    return (T) Expression.instantiate(script, type);
  }
}
