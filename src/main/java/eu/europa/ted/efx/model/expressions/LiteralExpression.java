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
package eu.europa.ted.efx.model.expressions;

import eu.europa.ted.efx.model.expressions.scalar.ScalarLiteral;
import eu.europa.ted.efx.model.expressions.sequence.SequenceLiteral;

/**
 * A {@link TypedExpression} representing a constant value known at translation time.
 *
 * Unlike a {@link PathExpression} which generates a script that resolves to a value at runtime,
 * a literal expression generates a script that embeds the constant value itself.
 *
 * {@link ScalarLiteral} represents single-value literals, while {@link SequenceLiteral}
 * represents sequences of literals.
 *
 * @see PathExpression for expressions that reference document data
 */
public interface LiteralExpression extends TypedExpression {

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
  static <T extends LiteralExpression> T from(TypedExpression source, Class<T> returnType) {
    return Expression.from(source, returnType);
  }
}
