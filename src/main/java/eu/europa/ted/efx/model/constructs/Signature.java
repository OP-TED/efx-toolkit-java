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
import java.util.stream.Collectors;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * The signature of a construct, or of one of its overloads: its {@link Parameters}, which may end
 * in a variadic tail, and its result type.
 *
 * <p>A variadic tail takes one or more arguments after the fixed parameters. An optional argument
 * is not part of a signature: the built-in declares a second overload instead. A signature whose
 * parameters mention {@code T} is generic, and is bound to the value primitive of the argument at
 * the first position of {@code T} before its arguments are checked.</p>
 *
 * <p>The signature of a built-in or of an expression construct is declared in one of five
 * shapes, nested here: {@link Nullary}, {@link Unary}, {@link Binary} and {@link Ternary}, of no
 * parameter up to three, and {@link Variadic}, of a variadic tail alone. A shape is typed by the
 * expression classes of its parameter types and of its result type, so that what translates a call
 * of it can be checked against it when the toolkit is built. The signature of a function declared
 * in EFX has no shape: its parameters are known only when its declaration is read. Nor has a
 * signature with {@code T} bound: it is listed, never translated.</p>
 */
public class Signature {

  private final Parameters<?> parameters;
  private final ParameterType<?> result;

  private Signature(final Parameters<?> parameters, final ParameterType<?> result) {
    this.parameters = parameters;
    this.result = result;
  }

  /**
   * Gives the signature of a function declared in EFX.
   *
   * @param parameters the parameters it declares
   * @param result the type it declares for its value
   * @return the signature
   */
  public static Signature of(final Parameters<?> parameters, final ParameterType<?> result) {
    return new Signature(parameters, result);
  }

  /**
   * Starts a signature without fixed parameters: of no parameter, or of a variadic tail alone.
   * Complete it with {@link Nullary.Builder#returning}, or add the tail with
   * {@link Nullary.Builder#variadic}.
   *
   * @return a builder for the signature
   */
  public static Nullary.Builder signature() {
    return new Nullary.Builder();
  }

  /**
   * Starts a signature of one parameter. Complete it with {@link Unary.Builder#returning}.
   *
   * @param <A> the expression class of the parameter
   * @param parameter the parameter
   * @return a builder for the signature
   */
  public static <A extends TypedExpression> Unary.Builder<A> signature(
      final Parameter.Impl<A> parameter) {
    return new Unary.Builder<>(parameter);
  }

  /**
   * Starts a signature of two parameters. Complete it with {@link Binary.Builder#returning}.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param first the first parameter
   * @param second the second parameter
   * @return a builder for the signature
   */
  public static <A extends TypedExpression, B extends TypedExpression> Binary.Builder<A, B>
      signature(final Parameter.Impl<A> first, final Parameter.Impl<B> second) {
    return new Binary.Builder<>(first, second);
  }

  /**
   * Starts a signature of three parameters. Complete it with {@link Ternary.Builder#returning}.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <C> the expression class of the third parameter
   * @param first the first parameter
   * @param second the second parameter
   * @param third the third parameter
   * @return a builder for the signature
   */
  public static <A extends TypedExpression, B extends TypedExpression, C extends TypedExpression>
      Ternary.Builder<A, B, C> signature(final Parameter.Impl<A> first,
          final Parameter.Impl<B> second, final Parameter.Impl<C> third) {
    return new Ternary.Builder<>(first, second, third);
  }

  /**
   * @return the parameters, which arguments are matched against
   */
  public Parameters<?> getParameters() {
    return this.parameters;
  }

  /**
   * @return the result type
   */
  public ParameterType<?> getResult() {
    return this.result;
  }

  /**
   * Gives the result type of a call whose arguments this signature takes: with {@code T} bound by
   * the argument at its first position, as the parameters are bound before they are matched.
   *
   * @param arguments the arguments of the call, which the parameters of this signature take
   * @return the result type, concrete where it is {@code T} or {@code T*}
   * @throws IllegalArgumentException if the parameters mention {@code T} and the arguments cannot
   *         bind it
   */
  public ParameterType<?> resultFor(final List<? extends TypedExpression> arguments) {
    if (!this.parameters.isGeneric()) {
      return this.result;
    }
    return this.parameters.bindingOf(arguments).<ParameterType<?>>map(this.result::bind)
        .orElseThrow(() -> new IllegalArgumentException(this + " does not take " + arguments));
  }

  /**
   * Gives this signature with {@code T} bound.
   *
   * @param primitive the value primitive {@code T} is bound to
   * @return the signature with every parameter type and the result type bound
   */
  Signature bind(final Class<? extends EfxDataType.Primitive> primitive) {
    return new Signature(this.parameters.bind(primitive), this.result.bind(primitive));
  }

  /**
   * Gives the signatures this one stands for: one for each value primitive {@code T} can be bound
   * to, when it mentions {@code T}, and itself otherwise.
   *
   * @return the signatures with {@code T} bound, or this signature alone
   */
  public List<Signature> concreteForms() {
    if (!this.parameters.isGeneric()) {
      return List.of(this);
    }
    return ParameterType.Generic.valuePrimitives().stream().map(this::bind)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public String toString() {
    return this.parameters + " -> " + this.result;
  }

  /**
   * A signature of no parameter, such as that of {@code current-date}.
   *
   * @param <R> the expression class of the result
   */
  public static final class Nullary<R extends TypedExpression> extends Signature {

    private Nullary(final ParameterType<R> result) {
      super(new Parameters<>(List.of(), null), result);
    }

    /**
     * Builds a signature without fixed parameters: a {@link Nullary}, or a {@link Variadic} once a
     * tail is added.
     */
    public static final class Builder {

      private Builder() {}

      /**
       * Adds a variadic tail, which takes one or more arguments.
       *
       * @param <V> the expression class of the tail
       * @param tail the parameter every argument of the tail matches
       * @return a builder for the signature with that tail
       */
      public <V extends TypedExpression> Variadic.Builder<V> variadic(
          final Parameter.Impl<V> tail) {
        return new Variadic.Builder<>(tail);
      }

      /**
       * Completes the signature.
       *
       * @param <R> the expression class of the result
       * @param result the result type
       * @return the signature
       */
      public <R extends TypedExpression> Nullary<R> returning(final ParameterType<R> result) {
        return new Nullary<>(result);
      }
    }
  }

  /**
   * A signature of one parameter, such as that of {@code upper-case}.
   *
   * @param <A> the expression class of the parameter
   * @param <R> the expression class of the result
   */
  public static final class Unary<A extends TypedExpression, R extends TypedExpression>
      extends Signature {

    private final Parameter.Impl<A> parameter;

    private Unary(final Parameter.Impl<A> parameter, final ParameterType<R> result) {
      super(new Parameters<>(List.of(parameter), null), result);
      this.parameter = parameter;
    }

    /**
     * @return the parameter
     */
    public Parameter.Impl<A> getParameter() {
      return this.parameter;
    }

    /**
     * Builds a signature of one parameter.
     *
     * @param <A> the expression class of the parameter
     */
    public static final class Builder<A extends TypedExpression> {

      private final Parameter.Impl<A> parameter;

      private Builder(final Parameter.Impl<A> parameter) {
        this.parameter = parameter;
      }

      /**
       * Completes the signature.
       *
       * @param <R> the expression class of the result
       * @param result the result type
       * @return the signature
       */
      public <R extends TypedExpression> Unary<A, R> returning(final ParameterType<R> result) {
        return new Unary<>(this.parameter, result);
      }
    }
  }

  /**
   * A signature of two parameters, such as that of {@code contains}.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <R> the expression class of the result
   */
  public static final class Binary<A extends TypedExpression, B extends TypedExpression,
      R extends TypedExpression> extends Signature {

    private final Parameter.Impl<A> first;
    private final Parameter.Impl<B> second;

    private Binary(final Parameter.Impl<A> first, final Parameter.Impl<B> second,
        final ParameterType<R> result) {
      super(new Parameters<>(List.of(first, second), null), result);
      this.first = first;
      this.second = second;
    }

    /**
     * @return the first parameter
     */
    public Parameter.Impl<A> getFirst() {
      return this.first;
    }

    /**
     * @return the second parameter
     */
    public Parameter.Impl<B> getSecond() {
      return this.second;
    }

    /**
     * Builds a signature of two parameters.
     *
     * @param <A> the expression class of the first parameter
     * @param <B> the expression class of the second parameter
     */
    public static final class Builder<A extends TypedExpression, B extends TypedExpression> {

      private final Parameter.Impl<A> first;
      private final Parameter.Impl<B> second;

      private Builder(final Parameter.Impl<A> first, final Parameter.Impl<B> second) {
        this.first = first;
        this.second = second;
      }

      /**
       * Completes the signature.
       *
       * @param <R> the expression class of the result
       * @param result the result type
       * @return the signature
       */
      public <R extends TypedExpression> Binary<A, B, R> returning(final ParameterType<R> result) {
        return new Binary<>(this.first, this.second, result);
      }
    }
  }

  /**
   * A signature of three parameters, such as that of {@code replace}.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <C> the expression class of the third parameter
   * @param <R> the expression class of the result
   */
  public static final class Ternary<A extends TypedExpression, B extends TypedExpression,
      C extends TypedExpression, R extends TypedExpression> extends Signature {

    private final Parameter.Impl<A> first;
    private final Parameter.Impl<B> second;
    private final Parameter.Impl<C> third;

    private Ternary(final Parameter.Impl<A> first, final Parameter.Impl<B> second,
        final Parameter.Impl<C> third, final ParameterType<R> result) {
      super(new Parameters<>(List.of(first, second, third), null), result);
      this.first = first;
      this.second = second;
      this.third = third;
    }

    /**
     * @return the first parameter
     */
    public Parameter.Impl<A> getFirst() {
      return this.first;
    }

    /**
     * @return the second parameter
     */
    public Parameter.Impl<B> getSecond() {
      return this.second;
    }

    /**
     * @return the third parameter
     */
    public Parameter.Impl<C> getThird() {
      return this.third;
    }

    /**
     * Builds a signature of three parameters.
     *
     * @param <A> the expression class of the first parameter
     * @param <B> the expression class of the second parameter
     * @param <C> the expression class of the third parameter
     */
    public static final class Builder<A extends TypedExpression, B extends TypedExpression,
        C extends TypedExpression> {

      private final Parameter.Impl<A> first;
      private final Parameter.Impl<B> second;
      private final Parameter.Impl<C> third;

      private Builder(final Parameter.Impl<A> first, final Parameter.Impl<B> second,
          final Parameter.Impl<C> third) {
        this.first = first;
        this.second = second;
        this.third = third;
      }

      /**
       * Completes the signature.
       *
       * @param <R> the expression class of the result
       * @param result the result type
       * @return the signature
       */
      public <R extends TypedExpression> Ternary<A, B, C, R> returning(
          final ParameterType<R> result) {
        return new Ternary<>(this.first, this.second, this.third, result);
      }
    }
  }

  /**
   * A signature of a variadic tail alone, which takes one or more arguments, such as that of
   * {@code concat}.
   *
   * @param <V> the expression class of the tail
   * @param <R> the expression class of the result
   */
  public static final class Variadic<V extends TypedExpression, R extends TypedExpression>
      extends Signature {

    private final Parameter.Impl<V> tail;

    private Variadic(final Parameter.Impl<V> tail, final ParameterType<R> result) {
      super(new Parameters<>(List.of(), tail), result);
      this.tail = tail;
    }

    /**
     * @return the parameter every argument of the tail matches
     */
    public Parameter.Impl<V> getTail() {
      return this.tail;
    }

    /**
     * Builds a signature of a variadic tail alone.
     *
     * @param <V> the expression class of the tail
     */
    public static final class Builder<V extends TypedExpression> {

      private final Parameter.Impl<V> tail;

      private Builder(final Parameter.Impl<V> tail) {
        this.tail = tail;
      }

      /**
       * Completes the signature.
       *
       * @param <R> the expression class of the result
       * @param result the result type
       * @return the signature
       */
      public <R extends TypedExpression> Variadic<V, R> returning(final ParameterType<R> result) {
        return new Variadic<>(this.tail, result);
      }
    }
  }
}
