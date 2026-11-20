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
package eu.europa.ted.efx.model.constructs;

import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * The overload of a {@link Composable} that takes the arguments of a call, with the arguments
 * converted to its parameters: the most specific of those that take them, as
 * {@link Composable#resolve} chooses it.
 *
 * <p>A call that no overload takes has no resolution; its {@link Mismatch} says why. A resolved
 * call is translated by the overload that took it.</p>
 */
public final class Resolution {

  private final Overload overload;
  private final Arguments<?> arguments;

  Resolution(final Overload overload, final Arguments<?> arguments) {
    this.overload = overload;
    this.arguments = arguments;
  }

  /**
   * @return the overload that takes the arguments, as the construct declares it
   */
  public Overload getOverload() {
    return this.overload;
  }

  /**
   * @return the arguments, converted to the parameters of the overload that takes them
   */
  public Arguments<?> getArguments() {
    return this.arguments;
  }

  /**
   * Translates the call with the overload that takes it.
   *
   * @param script the script generator of the target language
   * @return the translated call
   */
  public TypedExpression compose(final ScriptGenerator script) {
    return this.overload.compose(script, this.arguments);
  }
}
