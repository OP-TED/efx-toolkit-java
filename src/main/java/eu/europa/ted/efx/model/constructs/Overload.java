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
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

/**
 * One overload of a {@link Composable}: a {@link Signature} and the {@link Composer} that
 * translates a call the signature takes.
 *
 * <p>An overload is declared with a signature and a composer of the same shape, so that the
 * composer takes the arguments as the signature types them: {@code overload(signature(
 * parameter("value", TEXT), parameter("start", NUMBER)).returning(TEXT), (script, value, start) ->
 * script.composeSubstringExtraction(value, start))}. The overload of a function declared in EFX
 * has a signature without a shape, known only when its declaration is read: {@link #of} pairs it
 * with a composer of the arguments as a list.</p>
 */
public final class Overload {

  private final Signature signature;
  private final Composer composer;

  private Overload(final Signature signature, final Composer composer) {
    this.signature = signature;
    this.composer = composer;
  }

  /**
   * Declares the overload of a function declared in EFX, whose signature has no shape.
   *
   * @param signature the signature the declaration gives the function
   * @param composer the composer that translates a call of it, from the arguments as a list
   * @return the overload
   */
  public static Overload of(final Signature signature, final Composer composer) {
    return new Overload(signature, composer);
  }

  /**
   * Declares an overload of no parameter.
   *
   * @param <R> the expression class of the result
   * @param signature the signature of the overload
   * @param composer the composer that translates a call of it
   * @return the overload
   */
  public static <R extends TypedExpression> Overload overload(
      final Signature.Nullary<R> signature, final Composer.Nullary<R> composer) {
    return new Overload(signature, (script, arguments) -> composer.compose(script));
  }

  /**
   * Declares an overload of one parameter.
   *
   * @param <A> the expression class of the parameter
   * @param <R> the expression class of the result
   * @param signature the signature of the overload
   * @param composer the composer that translates a call of it
   * @return the overload
   */
  public static <A extends TypedExpression, R extends TypedExpression> Overload overload(
      final Signature.Unary<A, R> signature, final Composer.Unary<A, R> composer) {
    final Class<A> type = signature.getParameter().getType().getExpressionType();
    return new Overload(signature,
        (script, arguments) -> composer.compose(script, arguments.at(0, type)));
  }

  /**
   * Declares an overload of two parameters.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <R> the expression class of the result
   * @param signature the signature of the overload
   * @param composer the composer that translates a call of it
   * @return the overload
   */
  public static <A extends TypedExpression, B extends TypedExpression, R extends TypedExpression>
      Overload overload(final Signature.Binary<A, B, R> signature,
          final Composer.Binary<A, B, R> composer) {
    final Class<A> first = signature.getFirst().getType().getExpressionType();
    final Class<B> second = signature.getSecond().getType().getExpressionType();
    return new Overload(signature, (script, arguments) -> composer.compose(script,
        arguments.at(0, first), arguments.at(1, second)));
  }

  /**
   * Declares an overload of three parameters.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <C> the expression class of the third parameter
   * @param <R> the expression class of the result
   * @param signature the signature of the overload
   * @param composer the composer that translates a call of it
   * @return the overload
   */
  public static <A extends TypedExpression, B extends TypedExpression, C extends TypedExpression,
      R extends TypedExpression> Overload overload(final Signature.Ternary<A, B, C, R> signature,
          final Composer.Ternary<A, B, C, R> composer) {
    final Class<A> first = signature.getFirst().getType().getExpressionType();
    final Class<B> second = signature.getSecond().getType().getExpressionType();
    final Class<C> third = signature.getThird().getType().getExpressionType();
    return new Overload(signature, (script, arguments) -> composer.compose(script,
        arguments.at(0, first), arguments.at(1, second), arguments.at(2, third)));
  }

  /**
   * Declares an overload of a variadic tail alone.
   *
   * @param <V> the expression class of the tail
   * @param <R> the expression class of the result
   * @param signature the signature of the overload
   * @param composer the composer that translates a call of it
   * @return the overload
   */
  public static <V extends TypedExpression, R extends TypedExpression> Overload overload(
      final Signature.Variadic<V, R> signature, final Composer.Variadic<V, R> composer) {
    final Class<V> type = signature.getTail().getType().getExpressionType();
    return new Overload(signature,
        (script, arguments) -> composer.compose(script, arguments.from(0, type)));
  }

  /**
   * @return the signature of the overload
   */
  public Signature getSignature() {
    return this.signature;
  }

  /**
   * Translates a call of this overload.
   *
   * @param script the script generator of the target language
   * @param arguments the arguments, converted to this overload's parameters
   * @return the translated call
   */
  public TypedExpression compose(final ScriptGenerator script, final Arguments<?> arguments) {
    return this.composer.compose(script, arguments);
  }

  /**
   * Gives the negation of this overload, for an overload whose result is an indicator: the same
   * signature, and the logical negation of its translation.
   *
   * @return the negated overload
   */
  public Overload negated() {
    return new Overload(this.signature, (script, arguments) -> script
        .composeLogicalNot(BooleanExpression.class.cast(this.compose(script, arguments))));
  }

  @Override
  public String toString() {
    return this.signature.toString();
  }
}
