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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import eu.europa.ted.efx.model.builtins.BuiltIn;
import eu.europa.ted.efx.model.builtins.BuiltIns;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DynamicExpression;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;

/**
 * Proves that {@code T} binds the same way whichever argument comes first, for every built-in that
 * takes two arguments of type {@code T} or {@code T*}. {@code T} binds to the value primitive of
 * the first argument, so a pair of a subtype and its supertype must resolve in both orders, and a
 * pair of unrelated primitives must be rejected in both orders at the second argument.
 *
 * <p>A dynamic value has no sequence class, so where a sequence is expected it is passed as a single
 * value and promoted.</p>
 */
class TypeVariableBindingTest {

  private static final Map<ParameterType<?>, Map<String, TypedExpression>> VALUES = Map.of(
      ParameterType.T, Map.of(
          "multilingual", value(MultilingualStringExpression.class),
          "text", value(StringExpression.class),
          "dynamic", value(DynamicExpression.class),
          "indicator", value(BooleanExpression.class),
          "number", value(NumericExpression.class)),
      ParameterType.T_SEQUENCE, Map.of(
          "multilingual", value(MultilingualStringSequenceExpression.class),
          "text", value(StringSequenceExpression.class),
          "dynamic", value(DynamicExpression.class),
          "indicator", value(BooleanSequenceExpression.class),
          "number", value(NumericSequenceExpression.class)));

  static Stream<org.junit.jupiter.params.provider.Arguments> overloadsOfTwoTypeVariables() {
    return BuiltIns.INSTANCE.getAll().stream()
        .flatMap(builtIn -> builtIn.getOverloads().stream()
            .filter(TypeVariableBindingTest::takesTwoTypeVariables)
            .map(overload -> org.junit.jupiter.params.provider.Arguments.of(builtIn.getName(), builtIn,
                overload)));
  }

  @Test
  void testCatalogue_HasBuiltInsOfTwoTypeVariables() {
    assertEquals(
        List.of("==", "!=", "<", "<=", ">", ">=", "in", "not in", "is unique in",
            "is not unique in", "value-union", "value-intersect", "value-except", "sequence-equal",
            "index-of"),
        overloadsOfTwoTypeVariables().map(arguments -> arguments.get()[0])
            .collect(Collectors.toList()));
  }

  @ParameterizedTest(name = "{0} {2}")
  @MethodSource("overloadsOfTwoTypeVariables")
  void testSubtypeAndSupertype_ResolveInEitherOrder(final String name, final BuiltIn builtIn,
      final Overload overload) {
    assertResolvesInEitherOrder(builtIn, overload, "multilingual", "text");
    assertResolvesInEitherOrder(builtIn, overload, "dynamic", "indicator");
  }

  @ParameterizedTest(name = "{0} {2}")
  @MethodSource("overloadsOfTwoTypeVariables")
  void testUnrelatedPrimitives_AreRejectedInEitherOrder(final String name, final BuiltIn builtIn,
      final Overload overload) {
    assertRejectedAtTheSecond(builtIn, overload, "text", "number");
    assertRejectedAtTheSecond(builtIn, overload, "number", "text");
  }

  private static void assertResolvesInEitherOrder(final BuiltIn builtIn, final Overload overload,
      final String first, final String second) {
    assertSame(overload,
        builtIn.resolve(arguments(overload, first, second)).orElseThrow().getOverload());
    assertSame(overload,
        builtIn.resolve(arguments(overload, second, first)).orElseThrow().getOverload());
  }

  private static void assertRejectedAtTheSecond(final BuiltIn builtIn, final Overload overload,
      final String first, final String second) {
    List<TypedExpression> arguments = arguments(overload, first, second);
    assertTrue(builtIn.resolve(arguments).isEmpty());
    assertEquals(List.of(1), builtIn.mismatchOf(arguments).getRejections().stream()
        .filter(rejection -> rejection.getParameters() == overload.getSignature().getParameters())
        .map(Rejection::getPosition)
        .collect(Collectors.toList()));
  }

  /**
   * Gives two arguments of the given kinds, each in the cardinality the overload's parameter at its
   * position expects.
   */
  private static List<TypedExpression> arguments(final Overload overload, final String first,
      final String second) {
    Parameters<?> parameters = overload.getSignature().getParameters();
    return List.of(
        VALUES.get(parameters.get(0).getType()).get(first),
        VALUES.get(parameters.get(1).getType()).get(second));
  }

  private static boolean takesTwoTypeVariables(final Overload overload) {
    Parameters<?> parameters = overload.getSignature().getParameters();
    return parameters.size() == 2
        && parameters.stream().allMatch(parameter -> parameter.getType().isTypeVariable());
  }

  private static TypedExpression value(final Class<? extends TypedExpression> type) {
    return Expression.instantiate(type.getSimpleName(), type);
  }
}
