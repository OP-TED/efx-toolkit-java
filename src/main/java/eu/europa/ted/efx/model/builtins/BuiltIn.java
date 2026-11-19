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
package eu.europa.ted.efx.model.builtins;

import eu.europa.ted.efx.model.constructs.Composable;
import eu.europa.ted.efx.model.constructs.Overload;
import eu.europa.ted.efx.model.constructs.Spelling;

/**
 * An EFX operator or built-in function: a construct of the catalogue, which translates its calls
 * itself. It has a name and {@link Overload overloads}, each a signature with the composer that
 * translates a call the signature takes.
 *
 * <p>The name is the operator as written, such as {@code +} or {@code is not empty}, or the
 * function token, such as {@code substring}. The standard built-ins are the constants of the enums
 * of this package, gathered by {@link BuiltIns}; a {@link PatternMatch} is made for each use of
 * {@code like}.</p>
 */
public interface BuiltIn extends Composable {

  /**
   * @return the operator as written, or the function token
   */
  String getName();

  /**
   * Tells whether the built-in may only be used in a template.
   *
   * @return {@code true} for the formatting and preferred-language functions
   */
  default boolean isTemplateOnly() {
    return false;
  }

  /**
   * Tells whether the built-in is a function, called with its arguments in parentheses after its
   * name, rather than an operator.
   *
   * @return {@code true} for the functions, {@code not} among them
   */
  default boolean isFunction() {
    return false;
  }

  /**
   * Gives the built-in as a call spells it by its own name. A call may spell it otherwise, as
   * {@code string} spells {@code text}; the error of such a call names it as the call spells it.
   *
   * @return the name of the built-in, spelled as a function or as an operator
   */
  default Spelling getSpelling() {
    return this.isFunction() ? Spelling.function(this.getName())
        : Spelling.operator(this.getName());
  }

  /**
   * @return "Function 'substring'" for a function, "Operator '+'" for an operator
   */
  @Override
  default String describe() {
    return this.getSpelling().describe();
  }

  /**
   * @return the arguments of a function by their position, such as "2nd argument"; the operands of
   *         an operator, "operand", or "left operand" and "right operand"
   */
  @Override
  default String roleOf(final int position, final int count) {
    return this.getSpelling().roleOf(position, count);
  }
}
