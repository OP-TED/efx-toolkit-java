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

import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;

/**
 * Translates a call of an {@link Overload}: it gives the converted {@link Arguments} to the
 * {@link ScriptGenerator} method of the overload.
 *
 * <p>An overload of a built-in or of an expression construct writes its composer in the shape of
 * its {@link Signature}, one of the five nested here: {@link Nullary}, {@link Unary},
 * {@link Binary}, {@link Ternary} or {@link Variadic}. A composer of a shape takes the converted
 * arguments one by one, of the expression classes the signature gives them, and returns the
 * expression class of its result, so that the compiler checks it against the signature.
 * {@link Overload} pairs each shape with a signature of the same shape, and makes it a composer of
 * the arguments as a list, which is what the overload of a function declared in EFX writes.</p>
 *
 * <p>A generic method of {@link ScriptGenerator}, such as
 * {@link ScriptGenerator#composeUnionFunction}, also takes the sequence class {@code T*} is bound
 * to, which a lambda cannot name. So each of its four shapes has a generic functional interface
 * below, implemented by a method reference, and a factory that applies it with the sequence class
 * of its first sequence. That argument is the one that binds {@code T}, so its sequence class is
 * the one {@code T*} is bound to.</p>
 */
@FunctionalInterface
public interface Composer {

  /**
   * Translates a call.
   *
   * @param script the script generator of the target language
   * @param arguments the converted arguments of the call
   * @return the translated call
   */
  TypedExpression compose(ScriptGenerator script, Arguments<?> arguments);

  /**
   * Composes a generic function of one sequence, such as {@code sort}.
   *
   * @param function the script generator method, as a method reference
   * @return a composer that applies it to the sequence
   */
  static Unary<SequenceExpression, SequenceExpression> ofSequence(
      final SequenceFunction function) {
    return (script, list) -> applySequence(function, script, list, sequenceClassOf(list));
  }

  /**
   * Composes a generic function of two sequences of the same class, such as {@code value-union}.
   *
   * @param function the script generator method, as a method reference
   * @return a composer that applies it to the two sequences
   */
  static Binary<SequenceExpression, SequenceExpression, SequenceExpression> ofSequencePair(
      final SequencePairFunction function) {
    return (script, one, two) -> applySequencePair(function, script, one, two,
        sequenceClassOf(one));
  }

  /**
   * Composes {@code subsequence} with a start.
   *
   * @param function the script generator method, as a method reference
   * @return a composer that applies it to the sequence and the start
   */
  static Binary<SequenceExpression, NumericExpression, SequenceExpression> ofSubsequence(
      final SubsequenceFunction function) {
    return (script, list, start) -> applySubsequence(function, script, list, start,
        sequenceClassOf(list));
  }

  /**
   * Composes {@code subsequence} with a start and a length.
   *
   * @param function the script generator method, as a method reference
   * @return a composer that applies it to the sequence, the start and the length
   */
  static Ternary<SequenceExpression, NumericExpression, NumericExpression, SequenceExpression>
      ofSubsequenceWithLength(final SubsequenceWithLengthFunction function) {
    return (script, list, start, length) -> applySubsequenceWithLength(function, script, list,
        start, length, sequenceClassOf(list));
  }

  /**
   * Gives the sequence class of a converted sequence: the class {@code T*} is bound to when that
   * sequence binds {@code T}.
   */
  private static Class<? extends SequenceExpression> sequenceClassOf(
      final SequenceExpression list) {
    return ParameterType.T_SEQUENCE.boundBy(list).orElseThrow().getExpressionType();
  }

  private static <T extends SequenceExpression> T applySequence(final SequenceFunction function,
      final ScriptGenerator script, final SequenceExpression list, final Class<T> type) {
    return function.apply(script, type.cast(list), type);
  }

  private static <T extends SequenceExpression> T applySequencePair(
      final SequencePairFunction function, final ScriptGenerator script,
      final SequenceExpression one, final SequenceExpression two, final Class<T> type) {
    return function.apply(script, type.cast(one), type.cast(two), type);
  }

  private static <T extends SequenceExpression> T applySubsequence(
      final SubsequenceFunction function, final ScriptGenerator script,
      final SequenceExpression list, final NumericExpression start, final Class<T> type) {
    return function.apply(script, type.cast(list), start, type);
  }

  private static <T extends SequenceExpression> T applySubsequenceWithLength(
      final SubsequenceWithLengthFunction function, final ScriptGenerator script,
      final SequenceExpression list, final NumericExpression start,
      final NumericExpression length, final Class<T> type) {
    return function.apply(script, type.cast(list), start, length, type);
  }

  /**
   * The composer of a {@link Signature.Nullary} signature.
   *
   * @param <R> the expression class of the result
   */
  @FunctionalInterface
  interface Nullary<R extends TypedExpression> {

    /**
     * @param script the script generator of the target language
     * @return the translated call
     */
    R compose(ScriptGenerator script);
  }

  /**
   * The composer of a {@link Signature.Unary} signature.
   *
   * @param <A> the expression class of the parameter
   * @param <R> the expression class of the result
   */
  @FunctionalInterface
  interface Unary<A extends TypedExpression, R extends TypedExpression> {

    /**
     * @param script the script generator of the target language
     * @param argument the converted argument
     * @return the translated call
     */
    R compose(ScriptGenerator script, A argument);
  }

  /**
   * The composer of a {@link Signature.Binary} signature.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <R> the expression class of the result
   */
  @FunctionalInterface
  interface Binary<A extends TypedExpression, B extends TypedExpression,
      R extends TypedExpression> {

    /**
     * @param script the script generator of the target language
     * @param first the converted first argument
     * @param second the converted second argument
     * @return the translated call
     */
    R compose(ScriptGenerator script, A first, B second);
  }

  /**
   * The composer of a {@link Signature.Ternary} signature.
   *
   * @param <A> the expression class of the first parameter
   * @param <B> the expression class of the second parameter
   * @param <C> the expression class of the third parameter
   * @param <R> the expression class of the result
   */
  @FunctionalInterface
  interface Ternary<A extends TypedExpression, B extends TypedExpression,
      C extends TypedExpression, R extends TypedExpression> {

    /**
     * @param script the script generator of the target language
     * @param first the converted first argument
     * @param second the converted second argument
     * @param third the converted third argument
     * @return the translated call
     */
    R compose(ScriptGenerator script, A first, B second, C third);
  }

  /**
   * The composer of a {@link Signature.Variadic} signature.
   *
   * @param <V> the expression class of the tail
   * @param <R> the expression class of the result
   */
  @FunctionalInterface
  interface Variadic<V extends TypedExpression, R extends TypedExpression> {

    /**
     * @param script the script generator of the target language
     * @param arguments the converted arguments of the tail, in order
     * @return the translated call
     */
    R compose(ScriptGenerator script, List<V> arguments);
  }

  /**
   * The shape of a generic {@link ScriptGenerator} method of one sequence, such as
   * {@link ScriptGenerator#composeSortFunction}.
   */
  @FunctionalInterface
  interface SequenceFunction {

    /**
     * @param <T> the sequence class
     * @param script the script generator
     * @param list the sequence
     * @param type the sequence class
     * @return the translated call
     */
    <T extends SequenceExpression> T apply(ScriptGenerator script, T list, Class<T> type);
  }

  /**
   * The shape of a generic {@link ScriptGenerator} method of two sequences, such as
   * {@link ScriptGenerator#composeUnionFunction}.
   */
  @FunctionalInterface
  interface SequencePairFunction {

    /**
     * @param <T> the sequence class
     * @param script the script generator
     * @param one the first sequence
     * @param two the second sequence
     * @param type the sequence class
     * @return the translated call
     */
    <T extends SequenceExpression> T apply(ScriptGenerator script, T one, T two, Class<T> type);
  }

  /**
   * The shape of {@link ScriptGenerator#composeSubsequenceFunction} with a start.
   */
  @FunctionalInterface
  interface SubsequenceFunction {

    /**
     * @param <T> the sequence class
     * @param script the script generator
     * @param list the sequence
     * @param start the position of the first item kept
     * @param type the sequence class
     * @return the translated call
     */
    <T extends SequenceExpression> T apply(ScriptGenerator script, T list, NumericExpression start,
        Class<T> type);
  }

  /**
   * The shape of {@link ScriptGenerator#composeSubsequenceFunction} with a start and a length.
   */
  @FunctionalInterface
  interface SubsequenceWithLengthFunction {

    /**
     * @param <T> the sequence class
     * @param script the script generator
     * @param list the sequence
     * @param start the position of the first item kept
     * @param length the number of items kept
     * @param type the sequence class
     * @return the translated call
     */
    <T extends SequenceExpression> T apply(ScriptGenerator script, T list, NumericExpression start,
        NumericExpression length, Class<T> type);
  }
}
