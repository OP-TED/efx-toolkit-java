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

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * A construct as a type check sees it: the parameter lists its arguments are checked against, and
 * the words a type error uses for it and for each of its arguments.
 *
 * <p>A <em>call</em> is one place in EFX where a construct is written with its arguments, such as
 * {@code substring($s, 1, 3)}, {@code $a == $b}, {@code if $c then 1 else 2} or
 * {@code INVOKE greeting('World')}. The construct checks the type of each argument against what
 * it takes at that position.</p>
 *
 * <p>Every construct is checked this way. A {@link Composable} also translates its calls, with
 * the overload that takes them: a built-in, a construct that the grammar writes inside an
 * expression, or a function declared in EFX. Any other construct, such as a template or the
 * condition of a rule, is only checked: its arguments are {@link #match matched} against its
 * parameter lists, and what it is part of translates them.</p>
 */
public interface Construct {

  /**
   * @return how a type error names the construct, such as "Operator '+'",
   *         "Function 'substring'", "Template 'greeting'" or "Predicate"
   */
  String describe();

  /**
   * @param position the position of an argument
   * @param count the number of arguments of the call
   * @return how a type error names the argument at that position, such as "right operand",
   *         "2nd argument" or "condition"
   */
  String roleOf(int position, int count);

  /**
   * @return the parameter lists that the arguments of a call are checked against: one, or one for
   *         each overload, none of which takes the same arguments as another
   */
  List<Parameters<?>> getParameterLists();

  /**
   * Says what to write instead of the arguments of a call that no parameter list takes, where the
   * mistake suggests it.
   *
   * @param arguments the translated arguments of the call
   * @return each suggestion, a sentence; none by default
   */
  default List<String> suggestionsFor(final List<? extends TypedExpression> arguments) {
    return List.of();
  }

  /**
   * Checks the arguments of a call against the parameter lists of this construct, and converts
   * them to the most specific of those that take them.
   *
   * @param arguments the translated arguments of the call
   * @return the arguments, converted to the parameter list that takes them, or nothing if none
   *         does
   * @throws TranslatorConfigurationException if more than one parameter list takes the arguments
   *         and none of them is more specific than all the others, a defect of the construct
   */
  default Optional<Arguments<?>> match(final List<? extends TypedExpression> arguments) {
    return Applicable.mostSpecific(this, this.getParameterLists(), parameters -> parameters,
        arguments).map(Applicable::getArguments);
  }

  /**
   * Tells whether a parameter list of this construct takes a number of arguments.
   *
   * @param count the number of arguments of a call
   * @return {@code true} if one does; otherwise a call of that many arguments is wrong by its
   *         number alone
   */
  default boolean takes(final int count) {
    return this.getParameterLists().stream().anyMatch(parameters -> parameters.takes(count));
  }

  /**
   * Says why no parameter list of this construct takes the arguments of a call that it does not
   * {@link #match}, although one {@link #takes} their number.
   *
   * @param arguments the translated arguments of the call
   * @return the mismatch
   * @throws IllegalArgumentException if no parameter list takes that many arguments, or one takes
   *         the arguments
   */
  default Mismatch mismatchOf(final List<? extends TypedExpression> arguments) {
    return Mismatch.of(this, arguments);
  }
}
