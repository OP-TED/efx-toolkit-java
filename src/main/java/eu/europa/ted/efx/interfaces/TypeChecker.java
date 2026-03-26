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
package eu.europa.ted.efx.interfaces;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.sdk1.TypeCheckerV1;
import eu.europa.ted.efx.sdk2.TypeCheckerV2;

/**
 * Determines whether one EFX expression type can be converted to another.
 *
 * Used by the {@link eu.europa.ted.efx.model.CallStack} to validate type compatibility during
 * EFX expression evaluation. Different SDK versions have different type checking rules:
 * {@link eu.europa.ted.efx.sdk1.TypeCheckerV1} (lenient) and
 * {@link eu.europa.ted.efx.sdk2.TypeCheckerV2} (strict, requiring concrete types).
 */
public interface TypeChecker {

  TypeChecker V1 = TypeCheckerV1.INSTANCE;
  TypeChecker V2 = TypeCheckerV2.INSTANCE;

  /**
   * Checks if an expression of type {@code from} can be converted to type {@code to}.
   *
   * @param from the source expression type
   * @param to the target expression type
   * @return {@code true} if the conversion is allowed, {@code false} otherwise
   */
  boolean canConvert(Class<? extends TypedExpression> from,
                     Class<? extends TypedExpression> to);
}
