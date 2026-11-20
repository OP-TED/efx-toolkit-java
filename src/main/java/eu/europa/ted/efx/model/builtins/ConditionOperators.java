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

import static eu.europa.ted.efx.model.constructs.Overload.overload;
import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.ParameterType.INDICATOR;
import static eu.europa.ted.efx.model.constructs.ParameterType.PATH;
import static eu.europa.ted.efx.model.constructs.ParameterType.T;
import static eu.europa.ted.efx.model.constructs.ParameterType.T_SEQUENCE;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.constructs.Composer;
import eu.europa.ted.efx.model.constructs.Overload;
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
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;

/**
 * The condition operators: membership, emptiness, presence, uniqueness and duplicates, each with
 * its negated form. ({@code like} is a {@link PatternMatch}.)
 *
 * <p>A negated form is the logical negation of its positive form. {@code has duplicates} is the
 * negation of {@code has no duplicates}, which is the condition the script generator composes.</p>
 */
public enum ConditionOperators implements BuiltIn {

  IN("in",
      overload(signature(parameter("needle", T), parameter("haystack", T_SEQUENCE)).returning(INDICATOR),
          (script, needle, haystack) -> script.composeContainsCondition(needle, haystack))),

  NOT_IN("not in", IN),

  IS_EMPTY("is empty",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(INDICATOR),
          (script, sequence) -> script.composeEmptySequenceCondition(sequence))),

  IS_NOT_EMPTY("is not empty", IS_EMPTY),

  IS_PRESENT("is present",
      overload(signature(parameter("reference", PATH)).returning(INDICATOR),
          (script, reference) -> script.composeExistsCondition(reference))),

  IS_NOT_PRESENT("is not present", IS_PRESENT),

  IS_UNIQUE_IN("is unique in",
      overload(signature(parameter("needle", T), parameter("haystack", T_SEQUENCE))
          .returning(INDICATOR), UniqueValueCondition::compose)),

  IS_NOT_UNIQUE_IN("is not unique in", IS_UNIQUE_IN),

  HAS_NO_DUPLICATES("has no duplicates",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(INDICATOR),
          (script, sequence) -> script.composeIsDistinctCondition(sequence))),

  HAS_DUPLICATES("has duplicates", HAS_NO_DUPLICATES);

  private final String name;
  private final List<Overload> overloads;

  ConditionOperators(final String name, final Overload... overloads) {
    this.name = name;
    this.overloads = List.of(overloads);
  }

  /**
   * Declares the negated form of a condition.
   *
   * @param name the negated operator as written
   * @param positive the condition it negates
   */
  ConditionOperators(final String name, final ConditionOperators positive) {
    this.name = name;
    this.overloads = positive.overloads.stream().map(Overload::negated)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public List<Overload> getOverloads() {
    return this.overloads;
  }

  /**
   * The condition {@code is unique in} for each value type. The script generator composes it with
   * one method for each type, so the class of the needle picks the method.
   */
  private enum UniqueValueCondition {

    STRING(StringExpression.class, StringSequenceExpression.class,
        (script, needle, haystack) -> script.composeUniqueValueCondition(needle, haystack)),

    NUMERIC(NumericExpression.class, NumericSequenceExpression.class,
        (script, needle, haystack) -> script.composeUniqueValueCondition(needle, haystack)),

    BOOLEAN(BooleanExpression.class, BooleanSequenceExpression.class,
        (script, needle, haystack) -> script.composeUniqueValueCondition(needle, haystack)),

    DATE(DateExpression.class, DateSequenceExpression.class,
        (script, needle, haystack) -> script.composeUniqueValueCondition(needle, haystack)),

    TIME(TimeExpression.class, TimeSequenceExpression.class,
        (script, needle, haystack) -> script.composeUniqueValueCondition(needle, haystack)),

    DURATION(DurationExpression.class, DurationSequenceExpression.class,
        (script, needle, haystack) -> script.composeUniqueValueCondition(needle, haystack));

    private final Class<? extends ScalarExpression> needleType;
    private final Composer.Binary<ScalarExpression, SequenceExpression, BooleanExpression> composer;

    /**
     * @param needleType the class of the needle, which picks this condition
     * @param haystackType the class of the haystack, a sequence of the same primitive
     * @param composer the composer of the condition for those classes
     */
    <N extends ScalarExpression, H extends SequenceExpression> UniqueValueCondition(
        final Class<N> needleType, final Class<H> haystackType,
        final Composer.Binary<N, H, BooleanExpression> composer) {
      this.needleType = needleType;
      this.composer = (script, needle, haystack) -> composer.compose(script,
          needleType.cast(needle), haystackType.cast(haystack));
    }

    /**
     * Composes the condition with the method for the class of the needle.
     *
     * @param script the script generator of the target language
     * @param needle the needle, converted to the class {@code T} is bound to
     * @param haystack the haystack, converted to the sequence class {@code T*} is bound to
     * @return the condition
     */
    static BooleanExpression compose(final ScriptGenerator script, final ScalarExpression needle,
        final SequenceExpression haystack) {
      return Arrays.stream(values()).filter(condition -> condition.needleType.isInstance(needle))
          .findFirst().orElseThrow().composer.compose(script, needle, haystack);
    }
  }
}
