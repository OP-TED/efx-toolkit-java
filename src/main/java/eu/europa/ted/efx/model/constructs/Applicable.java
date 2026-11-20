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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.TypeChecker;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * A candidate of a call that takes its arguments: a parameter list of a construct, or an overload
 * of one, with the arguments converted to its parameters.
 *
 * <p>A candidate takes the arguments when its parameters {@link Parameters#match match} them: they
 * take their number and, once {@code T} is bound, every argument converts to its parameter. When
 * more than one takes them, the most specific one is chosen: the one whose converted arguments
 * would also convert to the parameters of every other, as {@code (T)} is more specific than
 * {@code (T*)} for a single value, a multilingual field than a sequence of texts, and a path
 * parameter of a class than one of its superclass, as {@code NODES} is than {@code PATH}. A call
 * with no single most specific candidate raises a {@link TranslatorConfigurationException}: the
 * construct is ambiguous, which the catalogue's unit tests prove no built-in is.</p>
 *
 * @param <C> the kind of candidate: a parameter list or an overload
 */
final class Applicable<C> {

  private final C candidate;
  private final Parameters<?> parameters;
  private final Arguments<?> arguments;

  private Applicable(final C candidate, final Parameters<?> parameters,
      final Arguments<?> arguments) {
    this.candidate = candidate;
    this.parameters = parameters;
    this.arguments = arguments;
  }

  /**
   * Checks the arguments of a call against each candidate whose parameters take their number,
   * and chooses the most specific of those that take them.
   *
   * @param <C> the kind of candidate
   * @param construct the construct called, which an ambiguity names
   * @param candidates its candidates, in order
   * @param parametersOf the parameters of a candidate
   * @param arguments the translated arguments of the call
   * @return the most specific candidate that takes the arguments, or nothing if none does
   * @throws TranslatorConfigurationException if more than one candidate takes the arguments and
   *         none of them is more specific than all the others, a defect of the construct
   */
  static <C> Optional<Applicable<C>> mostSpecific(final Construct construct,
      final List<C> candidates, final Function<C, Parameters<?>> parametersOf,
      final List<? extends TypedExpression> arguments) {
    final List<Applicable<C>> applicable = candidates.stream()
        .filter(candidate -> parametersOf.apply(candidate).takes(arguments.size()))
        .flatMap(candidate -> parametersOf.apply(candidate).match(arguments)
            .map(converted -> new Applicable<>(candidate, parametersOf.apply(candidate),
                converted))
            .stream())
        .collect(Collectors.toList());
    if (applicable.size() < 2) {
      return applicable.stream().findFirst();
    }
    final List<Applicable<C>> mostSpecific = applicable.stream()
        .filter(candidate -> applicable.stream().allMatch(candidate::isAsSpecificAs))
        .collect(Collectors.toList());
    if (mostSpecific.size() != 1) {
      throw TranslatorConfigurationException.ambiguousOverloads(construct.describe(),
          applicable.stream().map(candidate -> candidate.parameters.toString())
              .collect(Collectors.joining(", ", "[", "]")),
          Mismatch.describe(arguments));
    }
    return Optional.of(mostSpecific.get(0));
  }

  /**
   * @return the candidate, as the construct declares it
   */
  C getCandidate() {
    return this.candidate;
  }

  /**
   * @return the arguments, converted to the parameters of the candidate
   */
  Arguments<?> getArguments() {
    return this.arguments;
  }

  /**
   * Tells whether this candidate is at least as specific as another that takes the same
   * arguments: each argument was converted to a class that converts to the class the other
   * converted it to, as a single value converts to a sequence of one.
   *
   * @param other another candidate that takes the same arguments
   * @return {@code true} if every argument converted for this candidate would also convert to the
   *         parameter of the other
   */
  private boolean isAsSpecificAs(final Applicable<?> other) {
    final Iterator<? extends Argument<?>> theirs = other.arguments.iterator();
    for (final Argument<?> mine : this.arguments) {
      if (!convertsTo(mine.parameter.getType(), theirs.next().parameter.getType())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tells whether an argument converted to one parameter type would also convert to another. A
   * path parameter takes a path of its class, not a value, so only a path of its class or of a
   * subclass converts to it. {@link ParameterType#PATH} takes any path and names no type of EFX,
   * so what it takes converts only to it. Anything else converts as the type checker converts
   * it, as a multilingual field converts to a sequence of texts.
   *
   * @param from the parameter type the argument was converted to, with {@code T} bound
   * @param to the other parameter type, with {@code T} bound
   * @return {@code true} if it would also convert to the other
   */
  private static boolean convertsTo(final ParameterType<?> from, final ParameterType<?> to) {
    if (to instanceof ParameterType.Path || from == ParameterType.PATH) {
      return to.getExpressionType().isAssignableFrom(from.getExpressionType());
    }
    return TypeChecker.V2.canConvert(from.getExpressionType(), to.getExpressionType());
  }
}
