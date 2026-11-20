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

import static eu.europa.ted.efx.model.constructs.Overload.overload;
import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.Signature.signature;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.builtins.ArithmeticOperators;
import eu.europa.ted.efx.model.builtins.BuiltIn;
import eu.europa.ted.efx.model.builtins.ComparisonOperators;
import eu.europa.ted.efx.model.builtins.StringFunctions;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateLiteral;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericLiteral;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringLiteral;
import eu.europa.ted.efx.model.expressions.sequence.DateSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.StringSequencePath;

/**
 * Tests that the rejection a mismatch reports does not depend on the order of the overloads, nor on
 * which argument of a call wrong in two ways comes first: an overload that rejects only a
 * cardinality wins, then the one at the furthest position, and at the same position a primitive
 * failure comes before a path one. Also tests what the diagnosis of a mismatch says.
 */
class MismatchDiagnosisTest {

  private static final StringLiteral TEXT =
      Expression.instantiate("$text", StringLiteral.class);
  private static final StringSequencePath REPEATABLE_TEXT_FIELD =
      Expression.instantiate("$repeatable", StringSequencePath.class);
  private static final NodePath NODE =
      Expression.instantiate("$node", NodePath.class);
  private static final NumericLiteral NUMBER_LITERAL =
      Expression.instantiate("1", NumericLiteral.class);
  private static final DateLiteral DATE =
      Expression.instantiate("$date", DateLiteral.class);
  private static final DateSequencePath REPEATABLE_DATE_FIELD =
      Expression.instantiate("$repeatableDate", DateSequencePath.class);

  private static final Overload NUMBER_THEN_TEXT = overload(
      signature(parameter("first", ParameterType.NUMBER), parameter("second", ParameterType.TEXT))
          .returning(ParameterType.TEXT),
      (script, first, second) -> second);
  private static final Overload TEXT_THEN_TEXT = overload(
      signature(parameter("first", ParameterType.TEXT), parameter("second", ParameterType.TEXT))
          .returning(ParameterType.TEXT),
      (script, first, second) -> second);
  private static final Overload TEXT_THEN_NUMBER = overload(
      signature(parameter("first", ParameterType.TEXT), parameter("second", ParameterType.NUMBER))
          .returning(ParameterType.TEXT),
      (script, first, second) -> first);
  private static final Overload NUMBER = overload(
      signature(parameter("value", ParameterType.NUMBER)).returning(ParameterType.NUMBER),
      (script, value) -> value);
  private static final Overload NODES = overload(
      signature(parameter("nodes", ParameterType.NODES)).returning(ParameterType.NUMBER),
      (script, nodes) -> script.composeCountOperation(nodes));

  @Test
  void testFurthestPositionAndCardinalityWin_WhateverTheOrderOfTheOverloads() {
    for (List<Overload> overloads : orders(NUMBER_THEN_TEXT, TEXT_THEN_TEXT, TEXT_THEN_NUMBER)) {
      Rejection rejection = rejection(overloads, TEXT, REPEATABLE_TEXT_FIELD);
      assertEquals(1, rejection.getPosition(), overloads::toString);
      assertEquals(Rejection.Kind.CARDINALITY, rejection.getKind(), overloads::toString);
      assertEquals(StringExpression.class, rejection.getRequired().getExpressionType(),
          overloads::toString);
    }
  }

  @Test
  void testPrimitiveWinsOverPath_WhateverTheOrderOfTheOverloads() {
    for (List<Overload> overloads : orders(NUMBER, NODES)) {
      Rejection rejection = rejection(overloads, TEXT);
      assertEquals(Rejection.Kind.PRIMITIVE, rejection.getKind(), overloads::toString);
      assertEquals(NumericExpression.class, rejection.getRequired().getExpressionType(),
          overloads::toString);
    }
  }

  @Test
  void testNodeWhereAValueIsRequired_IsAPathFailure() {
    assertEquals(Rejection.Kind.PATH, rejection(List.of(NUMBER), NODE).getKind());
  }

  @Test
  void testSequenceWhereASingleValueOfItsPrimitiveIsRequired_IsACardinalityFailure() {
    Rejection rejection = ComparisonOperators.EQUAL
        .mismatchOf(List.of(REPEATABLE_TEXT_FIELD, TEXT)).getRejection();
    assertEquals(0, rejection.getPosition());
    assertEquals(Rejection.Kind.CARDINALITY, rejection.getKind());
  }

  @Test
  void testPairWrongInPrimitiveAndCardinality_IsAPrimitiveFailure_WhicheverSideTheSequenceIsOn() {
    for (List<TypedExpression> arguments : List.<List<TypedExpression>>of(
        List.of(REPEATABLE_TEXT_FIELD, NUMBER_LITERAL),
        List.of(NUMBER_LITERAL, REPEATABLE_TEXT_FIELD))) {
      Rejection rejection = ComparisonOperators.EQUAL.mismatchOf(arguments).getRejection();
      assertEquals(1, rejection.getPosition(), arguments::toString);
      assertEquals(Rejection.Kind.PRIMITIVE, rejection.getKind(), arguments::toString);
    }
  }

  @Test
  void testSequenceOfDatesMinusADate_IsACardinalityFailure() {
    // The date - date overload fits but for the cardinality of the left operand, so that is
    // reported, not that the right operand must be a duration.
    Rejection rejection = ArithmeticOperators.MINUS
        .mismatchOf(List.of(REPEATABLE_DATE_FIELD, DATE)).getRejection();
    assertEquals(0, rejection.getPosition());
    assertEquals(Rejection.Kind.CARDINALITY, rejection.getKind());
    assertEquals(DateExpression.class, rejection.getRequired().getExpressionType());
  }

  @Test
  void testDiagnosis_GivesEveryTypeTheRejectedPositionTakes_InCatalogueOrder() {
    assertEquals(
        "Operator '+' cannot take 'text' and 'number'. "
            + "Its left operand must be 'number', 'duration' or 'date'.",
        ArithmeticOperators.PLUS.mismatchOf(List.of(TEXT, NUMBER_LITERAL)).diagnose()
            .describe());
  }

  @Test
  void testArityMismatch_HasNoMismatch() {
    assertThrows(IllegalArgumentException.class,
        () -> StringFunctions.SUBSTRING.mismatchOf(List.of(TEXT)));
  }

  private static Rejection rejection(final List<Overload> overloads,
      final TypedExpression... arguments) {
    BuiltIn builtIn = new TestBuiltIn(overloads);
    assertTrue(builtIn.resolve(List.of(arguments)).isEmpty());
    return builtIn.mismatchOf(List.of(arguments)).getRejection();
  }

  /**
   * Gives every order of the overloads.
   */
  private static List<List<Overload>> orders(final Overload... overloads) {
    List<List<Overload>> orders = new ArrayList<>();
    permute(new ArrayList<>(List.of(overloads)), 0, orders);
    return orders;
  }

  private static void permute(final List<Overload> overloads, final int from,
      final List<List<Overload>> orders) {
    if (from == overloads.size()) {
      orders.add(List.copyOf(overloads));
      return;
    }
    for (int position = from; position < overloads.size(); position++) {
      Collections.swap(overloads, from, position);
      permute(overloads, from + 1, orders);
      Collections.swap(overloads, from, position);
    }
  }

  /**
   * A built-in whose overloads the test gives, in the order it gives them.
   */
  private static final class TestBuiltIn implements BuiltIn {

    private final List<Overload> overloads;

    TestBuiltIn(final List<Overload> overloads) {
      this.overloads = overloads;
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public List<Overload> getOverloads() {
      return this.overloads;
    }
  }
}
