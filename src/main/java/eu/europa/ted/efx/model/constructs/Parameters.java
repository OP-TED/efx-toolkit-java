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

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * A parameter list: fixed parameters, in order, and an optional variadic tail. Arguments are
 * matched against it: those of a built-in, an expression construct or a clause against the list
 * of a {@link Signature}, those of a function or template declared in EFX against the list of its
 * declaration.
 *
 * <p>A variadic tail takes one or more arguments after the fixed parameters. Parameters that
 * mention {@code T} are generic: they are bound to the value primitive of the argument at the
 * first position of {@code T} before the arguments are checked against them.</p>
 *
 * <p>Two parameter lists are equal as lists are: by their fixed parameters, in order.</p>
 *
 * @param <P> the kind of parameter: declared by a signature, or parsed with a function or a
 *        template declared in EFX
 */
public class Parameters<P extends Parameter> extends LinkedList<P> {

  /**
   * The order in which the rejected arguments of a call are reported: any argument of the wrong
   * primitive, or not the path required, before a sequence where a single value is required, and
   * then by position.
   */
  private static final Comparator<Rejection> ARGUMENT_ORDER =
      Comparator
          .comparing((Rejection rejection) -> rejection.getKind() == Rejection.Kind.CARDINALITY)
          .thenComparingInt(Rejection::getPosition);

  private final P variadic;

  /**
   * Creates an empty list with no variadic tail, to which a declaration adds its parameters as
   * they are translated.
   */
  protected Parameters() {
    this.variadic = null;
  }

  Parameters(final List<? extends P> fixed, final P variadic) {
    super(fixed);
    this.variadic = variadic;
  }

  /**
   * Gives a list of fixed parameters, without a variadic tail, such as the parameter of a clause.
   *
   * @param <P> the kind of parameter
   * @param fixed the parameters, in order
   * @return the parameter list
   */
  public static <P extends Parameter> Parameters<P> of(final List<? extends P> fixed) {
    return new Parameters<>(fixed, null);
  }

  /**
   * @return the parameter of the variadic tail, if there is one
   */
  public Optional<P> getVariadic() {
    return Optional.ofNullable(this.variadic);
  }

  /**
   * Tells whether the parameter list takes a number of arguments.
   *
   * @param count the number of arguments
   * @return {@code true} if the count is that of the fixed parameters, or, with a variadic tail,
   *         greater than it
   */
  public boolean takes(final int count) {
    return this.variadic == null ? count == this.size() : count > this.size();
  }

  /**
   * Gives the parameter at a position, which is the variadic tail past the fixed parameters.
   *
   * @param position the position of an argument the parameter list {@link #takes}
   * @return the parameter at that position
   */
  public P parameterAt(final int position) {
    return position < this.size() ? this.get(position) : this.variadic;
  }

  /**
   * Says how a type error names the argument at a position: the name of its parameter, numbered
   * by position among the arguments of a variadic tail that takes more than one.
   *
   * @param position the position of an argument the parameter list {@link #takes}
   * @param count the number of arguments of the call
   * @return the role, such as "condition", or "item" and "2nd item" for the tail of a list
   */
  public String roleOf(final int position, final int count) {
    final String name = this.parameterAt(position).getName();
    return position < this.size() ? name
        : numbered(name, position - this.size(), count - this.size());
  }

  /**
   * Names one of several things of a kind by its position, or by the noun alone if it is the only
   * one.
   *
   * @param noun the kind of thing, such as "argument"
   * @param position its position among them, from 0
   * @param count the number of them
   * @return "argument" if there is one, otherwise "1st argument", "2nd argument" and so on
   */
  static String numbered(final String noun, final int position, final int count) {
    return count == 1 ? noun : ordinal(position + 1) + " " + noun;
  }

  private static String ordinal(final int number) {
    final int lastTwoDigits = number % 100;
    if (lastTwoDigits >= 11 && lastTwoDigits <= 13) {
      return number + "th";
    }
    switch (number % 10) {
      case 1:
        return number + "st";
      case 2:
        return number + "nd";
      case 3:
        return number + "rd";
      default:
        return number + "th";
    }
  }

  /**
   * Matches the arguments of a call that the parameter list {@link #takes}: binds {@code T} by the
   * argument at its first position, checks each argument against its parameter, and converts the
   * arguments to the expression classes of their parameters.
   *
   * @param arguments the translated arguments of the call, as many as the list takes
   * @return the converted arguments, or nothing if the list {@link #rejectionOf rejects} one
   * @throws IllegalArgumentException if the parameter list does not take that many arguments
   */
  public Optional<Arguments<?>> match(final List<? extends TypedExpression> arguments) {
    this.requireTaken(arguments);
    return this.boundTo(arguments)
        .filter(bound -> bound.rejectionsOf(this, arguments).findAny().isEmpty())
        .map(bound -> bound.converted(arguments));
  }

  /**
   * Gives the rejection that the parameter list reports when it does not {@link #match} the
   * arguments of a call that it {@link #takes}.
   *
   * <p>Every argument is checked before one is reported, and a wrong primitive or path is reported
   * before a wrong cardinality: the first argument of another primitive than its parameter's, or
   * that is not the path its parameter requires, and only if there is none, the first sequence
   * where a single value is required. A call wrong in both ways is thus reported by its primitive,
   * whichever of its arguments is the sequence.</p>
   *
   * @param arguments the translated arguments of the call, as many as the list takes
   * @return the rejection to report: of the argument at the first position of a {@code T} it
   *         cannot bind, or of the one the argument order puts first; nothing if the list matches
   *         the arguments
   * @throws IllegalArgumentException if the parameter list does not take that many arguments
   */
  public Optional<Rejection> rejectionOf(final List<? extends TypedExpression> arguments) {
    this.requireTaken(arguments);
    final Optional<Parameters<?>> bound = this.boundTo(arguments);
    if (bound.isEmpty()) {
      final int position = this.typeVariablePosition();
      return Optional.of(
          new Rejection(this, position, this.typeAt(position), arguments.get(position)));
    }
    return bound.get().rejectionsOf(this, arguments).min(ARGUMENT_ORDER);
  }

  private void requireTaken(final List<? extends TypedExpression> arguments) {
    if (!this.takes(arguments.size())) {
      throw new IllegalArgumentException(this + " does not take " + arguments.size()
          + " arguments");
    }
  }

  /**
   * Gives these parameters with {@code T} bound. The bound list holds parameters declared by a
   * signature; it is only reached for a list that mentions {@code T}, which the list of a
   * declaration never does.
   *
   * @param primitive the value primitive {@code T} is bound to
   * @return the parameter list with every parameter type bound
   */
  Parameters<Parameter.Impl<?>> bind(final Class<? extends EfxDataType.Primitive> primitive) {
    return new Parameters<>(
        this.stream().map(parameter -> bound(parameter, primitive)).collect(Collectors.toList()),
        this.variadic == null ? null : bound(this.variadic, primitive));
  }

  /**
   * Tells whether a parameter of this list is {@code T}.
   *
   * @return {@code true} if a fixed parameter or the variadic tail is a type variable
   */
  boolean isGeneric() {
    return this.all().anyMatch(parameter -> parameter.getType().isTypeVariable());
  }

  /**
   * Checks the arguments against these parameters, with {@code T} bound.
   *
   * @param declared the parameter list as the construct declares it, which a rejection reports
   * @param arguments the translated arguments of the call
   * @return a rejection for each argument that does not convert to its parameter, in order
   */
  private Stream<Rejection> rejectionsOf(final Parameters<?> declared,
      final List<? extends TypedExpression> arguments) {
    return IntStream.range(0, arguments.size())
        .filter(position -> !this.typeAt(position).accepts(arguments.get(position)))
        .mapToObj(position -> new Rejection(declared, position, this.typeAt(position),
            arguments.get(position)));
  }

  private Arguments<?> converted(final List<? extends TypedExpression> arguments) {
    List<P> parameters = new ArrayList<>();
    List<TypedExpression> values = new ArrayList<>();
    for (int position = 0; position < arguments.size(); position++) {
      parameters.add(this.parameterAt(position));
      values.add(this.typeAt(position).convert(arguments.get(position)));
    }
    return new Arguments<>(parameters, values);
  }

  private ParameterType<?> typeAt(final int position) {
    return this.parameterAt(position).getType();
  }

  /**
   * Gives the value primitive the arguments of a call bind {@code T} to, for a list that mentions
   * it: the primitive of the argument at the first position of {@code T}.
   *
   * @param arguments the translated arguments of the call, as many as the list takes
   * @return the value primitive, or nothing if that argument cannot bind {@code T} (a node)
   */
  Optional<Class<? extends EfxDataType.Primitive>> bindingOf(
      final List<? extends TypedExpression> arguments) {
    return ParameterType.Generic.bindingOf(arguments.get(this.typeVariablePosition()));
  }

  private Optional<Parameters<?>> boundTo(final List<? extends TypedExpression> arguments) {
    if (!this.isGeneric()) {
      return Optional.of(this);
    }
    return this.bindingOf(arguments).map(this::bind);
  }

  private int typeVariablePosition() {
    return IntStream.range(0, this.size())
        .filter(position -> this.get(position).getType().isTypeVariable())
        .findFirst()
        .orElse(this.size());
  }

  private Stream<P> all() {
    return Stream.concat(this.stream(), this.getVariadic().stream());
  }

  private static Parameter.Impl<?> bound(final Parameter parameter,
      final Class<? extends EfxDataType.Primitive> primitive) {
    return parameter(parameter.getName(), parameter.getType().bind(primitive));
  }

  @Override
  public String toString() {
    return "(" + this.all().map(Parameter::toString).collect(Collectors.joining(", "))
        + (this.variadic == null ? "" : "...") + ")";
  }
}
