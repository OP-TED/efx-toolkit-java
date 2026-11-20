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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.europa.ted.efx.exceptions.Diagnosis;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * Why no parameter list of a {@link Construct} takes the arguments of a call of a number it
 * {@link Construct#takes takes}: each parameter list that takes that many arguments rejects one of
 * them.
 *
 * <p>A mismatch keeps the arguments and, for each parameter list that takes their number, its
 * {@link Rejection}: the parameter list, the argument it rejects and why. Its
 * {@link #getRejection rejection} tells the translator which argument to report, and its
 * {@link #diagnose diagnosis} what the error says about it.</p>
 */
public final class Mismatch {

  /**
   * The order in which the rejections of the parameter lists explain a mismatch: see
   * {@link #getRejection}.
   */
  private static final Comparator<Rejection> LIST_ORDER = Comparator
      .comparing((Rejection rejection) -> rejection.getKind() != Rejection.Kind.CARDINALITY)
      .thenComparing(Comparator.comparingInt(Rejection::getPosition).reversed())
      .thenComparing(Rejection::getKind);

  private final Construct construct;
  private final List<TypedExpression> arguments;
  private final List<Rejection> rejections;
  private final Rejection rejection;

  /**
   * @throws IllegalArgumentException if there is no rejection
   */
  private Mismatch(final Construct construct, final List<? extends TypedExpression> arguments,
      final List<Rejection> rejections) {
    this.construct = construct;
    this.arguments = List.copyOf(arguments);
    this.rejections = List.copyOf(rejections);
    this.rejection = rejections.stream().min(LIST_ORDER).orElseThrow(
        () -> new IllegalArgumentException("A mismatch of " + construct.describe()
            + " needs a rejection"));
  }

  /**
   * Gives why no parameter list of a construct takes the arguments of a call: the rejection of
   * each one that takes their number.
   *
   * @param construct the construct called
   * @param arguments the translated arguments of the call, of a number the construct takes,
   *        which no parameter list takes
   * @return the mismatch
   * @throws IllegalArgumentException if no parameter list takes that many arguments, or one takes
   *         the arguments
   */
  static Mismatch of(final Construct construct,
      final List<? extends TypedExpression> arguments) {
    final List<Parameters<?>> taking = construct.getParameterLists().stream()
        .filter(parameters -> parameters.takes(arguments.size()))
        .collect(Collectors.toList());
    if (taking.isEmpty()) {
      throw new IllegalArgumentException("No parameter list of " + construct.describe()
          + " takes " + arguments.size() + " arguments");
    }
    final List<Rejection> rejections = taking.stream()
        .map(parameters -> parameters.rejectionOf(arguments))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
    if (rejections.size() < taking.size()) {
      throw new IllegalArgumentException(construct.describe() + " takes ("
          + describe(arguments) + ")");
    }
    return new Mismatch(construct, arguments, rejections);
  }

  /**
   * Gives the mismatch of an argument that a construct cannot read for what it is written as,
   * although its type fits the parameter at its position: a text path where a context is
   * required, for example, or a predicate after a value. The construct has one parameter list.
   *
   * @param construct the construct that cannot read the argument
   * @param arguments the translated arguments of the call
   * @param position the position of the argument it cannot read
   * @return the mismatch, rejected at that position by the parameter there
   */
  public static Mismatch unreadable(final Construct construct,
      final List<? extends TypedExpression> arguments, final int position) {
    final Parameters<?> parameters = construct.getParameterLists().get(0);
    return new Mismatch(construct, arguments, List.of(new Rejection(parameters, position,
        parameters.parameterAt(position).getType(), arguments.get(position))));
  }

  /**
   * @return for each parameter list that takes that many arguments, its rejection
   */
  public List<Rejection> getRejections() {
    return this.rejections;
  }

  /**
   * Gives the rejection that explains the mismatch, whatever the order of the parameter lists.
   *
   * <p>A parameter list that rejects only a cardinality, since every primitive of the call fits
   * it, explains the mismatch before any that rejects a primitive or a path. Among the rest,
   * the rejection at the furthest position explains it, and at the same position a primitive
   * failure before a path one.</p>
   *
   * @return the rejection to report
   */
  public Rejection getRejection() {
    return this.rejection;
  }

  /**
   * Says what the error of the mismatch says: the construct, with the types of all the arguments,
   * the role of the argument at the position of the {@link #getRejection rejection}, and every
   * type that position takes. Those are the types required by all the parameter lists that reject
   * the same position in the same way, in their order, each once. What the construct suggests for
   * the arguments follows them.
   *
   * @return the diagnosis of the mismatch
   */
  public Diagnosis diagnose() {
    return this.diagnosis(this.construct.describe(),
        this.construct.roleOf(this.getRejection().getPosition(), this.arguments.size()));
  }

  /**
   * Says what the error of the mismatch says, as {@link #diagnose()} does, but naming the call and
   * the argument at fault as the call spells the built-in, such as "Function 'string'" for a call
   * of {@code text}.
   *
   * @param spelling the built-in as the call spells it
   * @return the diagnosis of the mismatch
   */
  public Diagnosis diagnose(final Spelling spelling) {
    return this.diagnosis(spelling.describe(),
        spelling.roleOf(this.getRejection().getPosition(), this.arguments.size()));
  }

  private Diagnosis diagnosis(final String called, final String role) {
    final Rejection reported = this.getRejection();
    final List<String> requirements = this.rejections.stream()
        .filter(rejection -> rejection.getPosition() == reported.getPosition()
            && rejection.getKind() == reported.getKind())
        .map(rejection -> rejection.getRequired().describe())
        .distinct()
        .collect(Collectors.toList());
    return new Diagnosis(called, role, this.arguments, requirements,
        this.construct.suggestionsFor(this.arguments));
  }

  @Override
  public String toString() {
    return "no parameter list of " + this.construct.describe() + " takes ("
        + describe(this.arguments) + ")";
  }

  /**
   * Names the classes of the arguments of a call, as the messages about the call show them.
   *
   * @param arguments the translated arguments of the call
   * @return the simple class names of the arguments, separated by commas
   */
  static String describe(final List<? extends TypedExpression> arguments) {
    return arguments.stream().map(argument -> argument.getClass().getSimpleName())
        .collect(Collectors.joining(", "));
  }
}
