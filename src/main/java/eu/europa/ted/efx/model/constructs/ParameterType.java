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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import eu.europa.ted.efx.interfaces.TypeChecker;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;
import eu.europa.ted.efx.model.types.EfxTypeTokenLookup;

/**
 * The type of a {@link Parameter}: the pattern an argument must match, and the expression class
 * the argument is converted to.
 *
 * <p>A parameter type is one of three kinds, each a class nested here: {@link Concrete}, a value
 * of one exact type, such as {@link #NUMBER}; {@link Path}, a reference, such as {@link #PATH};
 * and {@link Generic}, the type variable {@link #T} as a single value or as a sequence. A
 * signature that mentions {@code T} is generic: it is bound to a value primitive before its
 * arguments are checked, and only the parameter types of a bound signature accept and convert
 * arguments. Only this package makes parameter types: the constants, {@link #of} and a
 * binding.</p>
 *
 * <p>Outside a signature, {@link #T} and {@link #T_SEQUENCE} also give the type of a value that a
 * construct takes from one of its arguments: {@link #boundBy} binds them as a signature is
 * bound.</p>
 *
 * <p>Each parameter type names the expression class it gives a composer, as in
 * {@code ParameterType<StringExpression>}, so that a signature types the arguments of its
 * composer.</p>
 *
 * <p>The constants are named after the type vocabulary of the autocomplete catalogue.</p>
 *
 * @param <E> the expression class an argument is converted to
 */
public interface ParameterType<E extends TypedExpression> {

  /** A text, or a multilingual text read as a text. */
  ParameterType<StringExpression> TEXT = new Concrete<>(StringExpression.class);

  /** A number. */
  ParameterType<NumericExpression> NUMBER = new Concrete<>(NumericExpression.class);

  /** An indicator, or a dynamic value read as an indicator. */
  ParameterType<BooleanExpression> INDICATOR = new Concrete<>(BooleanExpression.class);

  /** A date. */
  ParameterType<DateExpression> DATE = new Concrete<>(DateExpression.class);

  /** A time. */
  ParameterType<TimeExpression> TIME = new Concrete<>(TimeExpression.class);

  /** A duration. */
  ParameterType<DurationExpression> DURATION = new Concrete<>(DurationExpression.class);

  /** A sequence of texts; a single text is promoted to a sequence of one. */
  ParameterType<StringSequenceExpression> TEXT_SEQUENCE =
      new Concrete<>(StringSequenceExpression.class);

  /** A sequence of numbers; a single number is promoted to a sequence of one. */
  ParameterType<NumericSequenceExpression> NUMBER_SEQUENCE =
      new Concrete<>(NumericSequenceExpression.class);

  /** A sequence of indicators; a single indicator is promoted to a sequence of one. */
  ParameterType<BooleanSequenceExpression> INDICATOR_SEQUENCE =
      new Concrete<>(BooleanSequenceExpression.class);

  /** A sequence of dates; a single date is promoted to a sequence of one. */
  ParameterType<DateSequenceExpression> DATE_SEQUENCE =
      new Concrete<>(DateSequenceExpression.class);

  /** A sequence of times; a single time is promoted to a sequence of one. */
  ParameterType<TimeSequenceExpression> TIME_SEQUENCE =
      new Concrete<>(TimeSequenceExpression.class);

  /** A sequence of durations; a single duration is promoted to a sequence of one. */
  ParameterType<DurationSequenceExpression> DURATION_SEQUENCE =
      new Concrete<>(DurationSequenceExpression.class);

  /** A single value of the value primitive {@code T} stands for. */
  ParameterType<ScalarExpression> T = new Generic<>("T", "a value", ScalarExpression.class,
      ScalarExpression.fromEfxDataType);

  /** A sequence of the value primitive {@code T} stands for, or a single value promoted to one. */
  ParameterType<SequenceExpression> T_SEQUENCE = new Generic<>("T*", "a sequence of values",
      SequenceExpression.class, SequenceExpression.fromEfxDataType);

  /** A reference to a field or a node, taken as a path rather than as its value. */
  ParameterType<PathExpression> PATH =
      new Path<>(PathExpression.class, "a field or node reference");

  /** A reference to a node, repeatable or not, taken as the sequence of nodes it selects. */
  ParameterType<NodeSequencePath> NODES = new Path<>(NodeSequencePath.class, "a node reference");

  /** A reference to a multilingual text field, taken as a path. */
  ParameterType<MultilingualStringSequencePath> MULTILINGUAL_FIELD =
      new Path<>(MultilingualStringSequencePath.class, "a multilingual field");

  /**
   * Gives the parameter type of a value expression class, such as the class of a parameter
   * declared in an EFX file.
   *
   * @param <E> the expression class
   * @param expressionType a scalar or sequence class of one value primitive
   * @return the parameter type that accepts what converts to that class
   */
  static <E extends TypedExpression> ParameterType<E> of(final Class<E> expressionType) {
    return new Concrete<>(expressionType);
  }

  /**
   * Tells whether this parameter type is the type variable {@code T}, as a single value or as a
   * sequence.
   *
   * @return {@code true} for {@link #T} and {@link #T_SEQUENCE}
   */
  default boolean isTypeVariable() {
    return false;
  }

  /**
   * Gives the parameter type this one stands for once {@code T} is bound.
   *
   * @param primitive the value primitive {@code T} is bound to
   * @return the concrete type for a type variable, and this parameter type for any other
   */
  default ParameterType<? extends E> bind(Class<? extends EfxDataType.Primitive> primitive) {
    return this;
  }

  /**
   * Gives the parameter type this one stands for once an argument binds {@code T}, as a generic
   * signature is bound by the argument at the first position of {@code T}.
   *
   * @param argument the argument that binds {@code T}
   * @return the concrete type for a type variable, or nothing if the argument cannot bind
   *         {@code T} (a node); this parameter type for any other
   */
  default Optional<ParameterType<? extends E>> boundBy(TypedExpression argument) {
    return Optional.of(this);
  }

  /**
   * Gives the expression class an argument is converted to. For a type variable, whose arguments
   * are only converted once it is bound, it is the class of every value or every sequence it
   * stands for.
   *
   * @return the expression class of this parameter type
   */
  Class<E> getExpressionType();

  /**
   * Tells whether an argument converts to this parameter type: unchanged, widened from a subtype,
   * or promoted from a single value to a sequence of one.
   *
   * @param argument the translated argument
   * @return {@code true} if the argument converts to {@link #getExpressionType()}
   */
  boolean accepts(TypedExpression argument);

  /**
   * Tells what is wrong with an argument this parameter type does not accept: its cardinality
   * alone, when a single value of its primitive would convert, so that it is a sequence where a
   * single value is required; its primitive; or a path where a value is required, or a node, which
   * has no value, where one is.
   *
   * @param argument a translated argument this parameter type does not {@link #accepts accept}
   * @return the kind of the rejection
   */
  Rejection.Kind rejectionKindOf(TypedExpression argument);

  /**
   * Says what this parameter type takes, as a type error says it: a type in quotes, as a
   * declaration writes it, such as {@code 'text'} or {@code 'number*'}, or a phrase, such as
   * "a value" or "a field or node reference".
   *
   * @return the words for what this parameter type takes
   */
  String describe();

  /**
   * Converts an accepted argument to {@link #getExpressionType() its class}, as
   * {@link eu.europa.ted.efx.model.CallStack#pop(Class)} does on the call stack.
   *
   * @param argument an argument this parameter type accepts
   * @return the argument itself if it already has that class, otherwise its conversion
   */
  default E convert(TypedExpression argument) {
    final Class<E> type = this.getExpressionType();
    return type.isInstance(argument) ? type.cast(argument) : TypedExpression.from(argument, type);
  }

  /**
   * A parameter type that names one expression class, such as a number or a sequence of texts. An
   * argument matches it when the EFX-2 type checker allows the conversion.
   *
   * @param <E> the expression class
   */
  public final class Concrete<E extends TypedExpression> implements ParameterType<E> {

    private final Class<E> expressionType;

    Concrete(final Class<E> expressionType) {
      this.expressionType = expressionType;
    }

    @Override
    public Class<E> getExpressionType() {
      return this.expressionType;
    }

    @Override
    public boolean accepts(final TypedExpression argument) {
      return TypeChecker.V2.canConvert(argument.getClass(), this.expressionType);
    }

    /**
     * Compares the class of the argument's single values, the one the argument binds {@code T} to,
     * with this one: if they convert, only the argument's cardinality is wrong.
     */
    @Override
    public Rejection.Kind rejectionKindOf(final TypedExpression argument) {
      return T.boundBy(argument)
          .map(value -> TypeChecker.V2.canConvert(value.getExpressionType(), this.expressionType)
              ? Rejection.Kind.CARDINALITY
              : Rejection.Kind.PRIMITIVE)
          .orElse(Rejection.Kind.PATH);
    }

    /**
     * Names the type in quotes, as a declaration writes it.
     */
    @Override
    public String describe() {
      return "'" + EfxTypeTokenLookup.nameOf(TypedExpression.getEfxDataType(this.expressionType))
          + "'";
    }

    /**
     * Two concrete types are equal when they take the same expression class, since
     * {@link ParameterType#of} makes a new one each time, as when {@code T} is bound.
     */
    @Override
    public boolean equals(final Object other) {
      return other instanceof Concrete
          && Objects.equals(this.expressionType, ((Concrete<?>) other).expressionType);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.expressionType);
    }

    @Override
    public String toString() {
      return this.expressionType.getSimpleName();
    }
  }

  /**
   * A parameter type that takes a reference as a path rather than as its value: any path, or a path
   * of one type. An argument that is not a path never matches it, and since a path parameter takes
   * no values, no argument is rejected by it for its cardinality alone.
   *
   * @param <E> the class of the paths it takes
   */
  public final class Path<E extends PathExpression> implements ParameterType<E> {

    private final Class<E> pathType;
    private final String description;

    /**
     * @param pathType the class of the paths this type takes
     * @param description what it takes, as a type error says it, such as "a field reference"
     */
    Path(final Class<E> pathType, final String description) {
      this.pathType = pathType;
      this.description = description;
    }

    @Override
    public Class<E> getExpressionType() {
      return this.pathType;
    }

    @Override
    public boolean accepts(final TypedExpression argument) {
      return this.pathType.isInstance(argument) || argument instanceof PathExpression
          && TypeChecker.V2.canConvert(argument.getClass(), this.pathType);
    }

    @Override
    public Rejection.Kind rejectionKindOf(final TypedExpression argument) {
      return Rejection.Kind.PATH;
    }

    @Override
    public String describe() {
      return this.description;
    }

    @Override
    public String toString() {
      return this.pathType.getSimpleName();
    }
  }

  /**
   * The type variable {@code T} of a generic signature: as a single value, {@link #T}, or as a
   * sequence, {@link #T_SEQUENCE}.
   *
   * <p>{@code T} ranges over the six value primitives. A multilingual text binds it to text and a
   * dynamic value to indicator; a node never binds it. Until it is bound, a type variable accepts
   * no argument, and its expression class is that of every value, or every sequence, it stands
   * for.</p>
   *
   * @param <E> the expression class of every value, or every sequence, it stands for
   */
  public final class Generic<E extends TypedExpression> implements ParameterType<E> {

    private static final List<Class<? extends EfxDataType.Primitive>> VALUE_PRIMITIVES = List.of(
        EfxDataType.String.class, EfxDataType.Boolean.class, EfxDataType.Number.class,
        EfxDataType.Date.class, EfxDataType.Time.class, EfxDataType.Duration.class);

    private final String symbol;
    private final String description;
    private final Class<E> expressionType;
    private final Map<Class<? extends EfxDataType.Primitive>, Class<? extends E>> concreteTypes;

    /**
     * @param symbol the type variable as a signature writes it, such as "T*"
     * @param description what it takes, as a type error says it, such as "a value"
     * @param expressionType the class of every value, or every sequence, it stands for
     * @param concreteTypes the class it stands for once {@code T} is bound, for each primitive
     */
    Generic(final String symbol, final String description, final Class<E> expressionType,
        final Map<Class<? extends EfxDataType.Primitive>, Class<? extends E>> concreteTypes) {
      this.symbol = symbol;
      this.description = description;
      this.expressionType = expressionType;
      this.concreteTypes = concreteTypes;
    }

    /**
     * Gives the value primitive an argument binds {@code T} to.
     *
     * @param argument the argument at the first position of {@code T} in a signature
     * @return the value primitive of the argument, or nothing for a node
     */
    static Optional<Class<? extends EfxDataType.Primitive>> bindingOf(
        final TypedExpression argument) {
      Class<? extends EfxDataType.Primitive> primitive =
          EfxTypeLattice.toPrimitive(argument.getDataType());
      return VALUE_PRIMITIVES.stream().filter(value -> value.isAssignableFrom(primitive))
          .findFirst();
    }

    /**
     * Lists the value primitives {@code T} ranges over.
     *
     * @return the six value primitives
     */
    static List<Class<? extends EfxDataType.Primitive>> valuePrimitives() {
      return VALUE_PRIMITIVES;
    }

    @Override
    public boolean isTypeVariable() {
      return true;
    }

    /**
     * Gives the concrete type this type variable stands for once {@code T} is bound.
     */
    @Override
    public ParameterType<? extends E> bind(
        final Class<? extends EfxDataType.Primitive> primitive) {
      return ParameterType.of(this.concreteTypes.get(primitive));
    }

    @Override
    public Optional<ParameterType<? extends E>> boundBy(final TypedExpression argument) {
      return bindingOf(argument).map(this::bind);
    }

    @Override
    public Class<E> getExpressionType() {
      return this.expressionType;
    }

    @Override
    public boolean accepts(final TypedExpression argument) {
      throw new IllegalStateException(this.symbol + " accepts no argument until it is bound");
    }

    /**
     * An unbound type variable rejects only the argument that cannot bind it, a node.
     */
    @Override
    public Rejection.Kind rejectionKindOf(final TypedExpression argument) {
      return Rejection.Kind.PATH;
    }

    /**
     * An unbound type variable takes a value of any primitive, which a node is not.
     */
    @Override
    public String describe() {
      return this.description;
    }

    @Override
    public String toString() {
      return this.symbol;
    }
  }
}
