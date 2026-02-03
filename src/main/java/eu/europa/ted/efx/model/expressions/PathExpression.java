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

import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;

/**
 * A {@link TypedExpression} representing a reference to an eForms Field or Node in a document.
 *
 * The generated script resolves to a value at runtime (similar to an XPath expression).
 *
 * {@link ScalarPath} represents references that resolve to a single value, while
 * {@link SequencePath} represents references that may resolve to multiple values.
 * Path expressions can convert between scalar and sequence representations via
 * {@link #asScalar()} and {@link #asSequence()}.
 */
public interface PathExpression extends TypedExpression {

  /**
   * Returns a scalar version of this path expression.
   * If already scalar, returns this. If a sequence, creates the corresponding
   * scalar path expression with the same script.
   *
   * @return A {@link ScalarPath} with the same script and primitive type.
   */
  ScalarPath asScalar();

  /**
   * Returns a sequence version of this path expression.
   * If already a sequence, returns this. If scalar, creates the corresponding
   * sequence path expression with the same script.
   *
   * @return A {@link SequencePath} with the same script and primitive type.
   */
  SequencePath asSequence();

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
}