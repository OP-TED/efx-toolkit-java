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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * A construct that translates its calls itself: an operator or a built-in function, a construct
 * the grammar writes inside an expression, such as the conditional or the index, or a function
 * declared in EFX. It has {@link Overload overloads}, each a signature with the composer that
 * translates a call the signature takes.
 *
 * <p>A call {@link #resolve resolves} to the most specific overload that takes its arguments, and
 * is translated by it. A construct that only checks its calls, such as a template or the condition
 * of a rule, is a {@link Construct} alone.</p>
 */
public interface Composable extends Construct {

  /**
   * @return the overloads, none of which takes the same arguments as another
   */
  List<Overload> getOverloads();

  /**
   * @return the parameters of the signature of each overload
   */
  @Override
  default List<Parameters<?>> getParameterLists() {
    return this.getOverloads().stream()
        .<Parameters<?>>map(overload -> overload.getSignature().getParameters())
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Chooses the overload that takes the arguments of a call: the most specific of those that take
   * them.
   *
   * @param arguments the translated arguments of the call
   * @return the resolution, which names the overload, or nothing if no overload takes the
   *         arguments
   * @throws TranslatorConfigurationException if more than one overload takes the arguments and
   *         none of them is more specific than all the others, a defect of the construct
   */
  default Optional<Resolution> resolve(final List<? extends TypedExpression> arguments) {
    return Applicable.mostSpecific(this, this.getOverloads(),
        overload -> overload.getSignature().getParameters(), arguments)
        .map(applicable -> new Resolution(applicable.getCandidate(), applicable.getArguments()));
  }
}
