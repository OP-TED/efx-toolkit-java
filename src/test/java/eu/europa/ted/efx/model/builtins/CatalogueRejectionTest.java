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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanLiteral;
import eu.europa.ted.efx.model.expressions.scalar.DurationLiteral;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericLiteral;
import eu.europa.ted.efx.model.expressions.scalar.StringLiteral;
import eu.europa.ted.efx.model.expressions.scalar.TimeLiteral;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

/**
 * Proves that the catalogue rejects one expression of each rule of the invalid-operation table
 * ({@code EfxInvalidOperationV2Test}) that a built-in decides. The table checks the same rules
 * through the translator; this class checks the catalogue alone.
 *
 * <p>The table's other rules are not the catalogue's: the condition and the branches of a
 * conditional, the body of a quantified expression, the values of a list, an iterator variable, a
 * predicate or an index, and a raw value are checked where those expressions are translated.</p>
 */
class CatalogueRejectionTest {

  private static final StringLiteral TEXT = value("'a'", StringLiteral.class);
  private static final StringLiteral OTHER_TEXT = value("'b'", StringLiteral.class);
  private static final NumericLiteral NUMBER = value("1", NumericLiteral.class);
  private static final BooleanLiteral INDICATOR = value("true()", BooleanLiteral.class);
  private static final TimeLiteral TIME = value("xs:time('10:00:00Z')", TimeLiteral.class);
  private static final DurationLiteral DURATION =
      value("xs:dayTimeDuration('P1D')", DurationLiteral.class);
  private static final StringSequenceExpression TEXTS =
      value("('a','b')", StringSequenceExpression.class);
  private static final NumericSequenceExpression NUMBERS =
      value("(1,2)", NumericSequenceExpression.class);
  private static final NodePath NODE = value("SubNode", NodePath.class);

  static Stream<org.junit.jupiter.params.provider.Arguments> rejectedCalls() {
    return Stream.of(
        // A comparison needs two operands of the same type
        rejected("'a' == 1", ComparisonOperators.EQUAL, TEXT, NUMBER),
        // + adds two numbers, two durations, or a duration to a date
        rejected("'a' + 'b'", ArithmeticOperators.PLUS, TEXT, OTHER_TEXT),
        // - subtracts two numbers, two durations, two dates, or a duration from a date
        rejected("11:00:00Z - 10:00:00Z", ArithmeticOperators.MINUS, TIME, TIME),
        // * multiplies two numbers, or a number and a duration
        rejected("P1D * P2D", ArithmeticOperators.MULTIPLY, DURATION, DURATION),
        // / and % divide numbers only
        rejected("P2D / 2", ArithmeticOperators.DIVIDE, DURATION, NUMBER),
        // and, or and not() take booleans
        rejected("1 and TRUE", LogicalOperators.AND, NUMBER, INDICATOR),
        // like matches text
        rejected("1 like '[0-9]'", PatternMatch.like("'[0-9]'"), NUMBER),
        // in looks for a value in a sequence of the same type
        rejected("1 in ['a', 'b']", ConditionOperators.IN, NUMBER, TEXTS),
        // is unique in looks for a value in a sequence of the same type
        rejected("'a' is unique in [1, 2]", ConditionOperators.IS_UNIQUE_IN, TEXT, NUMBERS),
        // A sequence cannot stand where a single value is required
        rejected("upper-case(['a'])", StringFunctions.UPPER_CASE, TEXTS),
        // A node has no value
        rejected("ND-SubNode == 'a'", ComparisonOperators.EQUAL, NODE, TEXT),
        // is present applies to a field or node reference
        rejected("1 is present", ConditionOperators.IS_PRESENT, NUMBER),
        // Each argument of a built-in function has the type its parameter declares
        rejected("string-length(1)", StringFunctions.STRING_LENGTH, NUMBER),
        // A built-in function takes the number of arguments it declares
        rejectedArity("upper-case()", StringFunctions.UPPER_CASE),
        // The result of a built-in function has the type the function declares
        rejected("count(['a']) like 'a'", PatternMatch.like("'a'"),
            NumericFunctions.COUNT.resolve(List.of(TEXTS)).orElseThrow()
                .compose(new XPathScriptGenerator(EfxTranslatorOptions.DEFAULT))),
        // Each argument of a template-only function has the type its parameter declares
        rejected("format-number('x', '0.00')", TemplateFunctions.FORMAT_NUMBER, TEXT, OTHER_TEXT),
        // A template-only function takes the number of arguments it declares
        rejectedArity("format-number(1)", TemplateFunctions.FORMAT_NUMBER, NUMBER));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedCalls")
  void testCall_IsRejected(final String expression, final BuiltIn builtIn,
      final List<TypedExpression> arguments, final boolean arityMismatch) {
    assertTrue(builtIn.resolve(arguments).isEmpty());
    assertEquals(arityMismatch, !builtIn.takes(arguments.size()));
  }

  private static org.junit.jupiter.params.provider.Arguments rejected(final String expression,
      final BuiltIn builtIn, final TypedExpression... arguments) {
    return org.junit.jupiter.params.provider.Arguments.of(expression, builtIn, List.of(arguments),
        false);
  }

  private static org.junit.jupiter.params.provider.Arguments rejectedArity(final String expression,
      final BuiltIn builtIn, final TypedExpression... arguments) {
    return org.junit.jupiter.params.provider.Arguments.of(expression, builtIn, List.of(arguments),
        true);
  }

  private static <T extends TypedExpression> T value(final String script, final Class<T> type) {
    return Expression.instantiate(script, type);
  }
}
