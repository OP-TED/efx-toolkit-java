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
package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTestsBase;

/**
 * Evaluates with Saxon the XPath that EFX expressions are translated to, so that each result shows
 * how the expression is grouped: by the order of the operators in the grammar, and in the XPath,
 * which keeps that grouping where XPath would group the same operations differently, or would not
 * accept them without parentheses.
 */
class EfxExpressionEvaluationTest extends EfxTestsBase {

  /**
   * A notice with five values of {@code BT-00-Repeatable-Number}: 1 and 2 are each there twice.
   */
  private static final String NOTICE = "<Notice><PathNode>"
      + "<RepeatableNumberField>1</RepeatableNumberField>"
      + "<RepeatableNumberField>1</RepeatableNumberField>"
      + "<RepeatableNumberField>2</RepeatableNumberField>"
      + "<RepeatableNumberField>2</RepeatableNumberField>"
      + "<RepeatableNumberField>3</RepeatableNumberField>"
      + "</PathNode></Notice>";

  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  private void assertResult(final String expected, final String expression) {
    assertEquals(expected,
        evaluate(translateComputeExpressionWithContext("ND-Root", expression), NOTICE),
        expression);
  }

  /**
   * A membership test is translated to an XPath comparison, which cannot be an operand of another
   * comparison without parentheses. The two groupings give different results.
   */
  @Test
  void testMembershipAndComparison_BothGroupings() {
    assertResult("false", "(FALSE == TRUE) not in [TRUE, FALSE]");
    assertResult("true", "FALSE == (TRUE not in [TRUE, FALSE])");
    assertResult("true", "TRUE in [TRUE] in [TRUE]");
  }

  /**
   * A condition binds tighter than a comparison, on either side of it:
   * {@code TRUE == 3 in BT-00-Repeatable-Number} compares {@code TRUE} with the result of
   * {@code 3 in BT-00-Repeatable-Number}.
   */
  @Test
  void testConditionBeforeComparison_In() {
    assertResult("true", "TRUE == 3 in BT-00-Repeatable-Number");
    assertResult("true", "3 in BT-00-Repeatable-Number == TRUE");
    assertResult("true", "TRUE != 3 not in BT-00-Repeatable-Number");
    assertResult("true", "3 not in BT-00-Repeatable-Number != TRUE");
  }

  @Test
  void testConditionBeforeComparison_UniqueIn() {
    assertResult("true", "TRUE == 3 is unique in BT-00-Repeatable-Number");
    assertResult("true", "3 is unique in BT-00-Repeatable-Number == TRUE");
    assertResult("true", "TRUE != 3 is not unique in BT-00-Repeatable-Number");
    assertResult("true", "3 is not unique in BT-00-Repeatable-Number != TRUE");
  }

  @Test
  void testConditionBeforeComparison_Empty() {
    assertResult("true", "TRUE == BT-00-Repeatable-Text is empty");
    assertResult("true", "BT-00-Repeatable-Text is empty == TRUE");
    assertResult("true", "TRUE != BT-00-Repeatable-Text is not empty");
    assertResult("true", "BT-00-Repeatable-Text is not empty != TRUE");
  }

  @Test
  void testConditionBeforeComparison_Duplicates() {
    assertResult("true", "TRUE == BT-00-Repeatable-Number has duplicates");
    assertResult("true", "BT-00-Repeatable-Number has duplicates == TRUE");
    assertResult("true", "TRUE != BT-00-Repeatable-Number has no duplicates");
    assertResult("true", "BT-00-Repeatable-Number has no duplicates != TRUE");
  }

  @Test
  void testConditionBeforeComparison_Like() {
    assertResult("true", "TRUE == 'a' like 'a'");
    assertResult("true", "'a' like 'a' == TRUE");
    assertResult("true", "TRUE != 'a' not like 'a'");
    assertResult("true", "'a' not like 'a' != TRUE");
  }

  /**
   * Only the negated form: {@code is present} gives the path itself, which XPath cannot compare
   * with an indicator (TEDEFO-5329).
   */
  @Test
  void testConditionBeforeComparison_NotPresent() {
    assertResult("true", "FALSE == BT-00-Repeatable-Number is not present");
    assertResult("true", "BT-00-Repeatable-Number is not present == FALSE");
    assertResult("true", "TRUE != BT-00-Repeatable-Number is not present");
    assertResult("true", "BT-00-Repeatable-Number is not present != TRUE");
  }

  /**
   * Comparisons are read from the left; XPath does not chain them.
   */
  @Test
  void testComparisonsReadFromTheLeft() {
    assertResult("true", "1 == 1 == TRUE");
    assertResult("true", "1 == 2 == FALSE");
    assertResult("true", "1 < 2 == TRUE");
  }

  /**
   * {@code every}, {@code some} and {@code for} extend as far to the right as they can, so they
   * can be the right operand of an operation. XPath accepts them there in parentheses only.
   */
  @Test
  void testPrefixExpressionAsAnOperand() {
    assertResult("false", "TRUE and every number:$x in [1, 2] satisfies $x > 1");
    assertResult("true", "FALSE or some number:$x in [1, 2] satisfies $x > 1");
    assertResult("true", "FALSE == every number:$x in [1, 2] satisfies $x > 1");
    assertResult("true", "3 in for number:$x in [1, 2] return $x + 1");
  }

  /**
   * A function or a property that is translated to an operation keeps the grouping of a call.
   */
  @Test
  void testCallTranslatedToAnOperation() {
    assertResult("4", "2 * count-duplicates(BT-00-Repeatable-Number)");
    assertResult("4", "count-duplicates(BT-00-Repeatable-Number) * 2");
    assertResult("1", "get-duplicates(BT-00-Repeatable-Number)[at 1]");
    assertResult("true", "empty('a') == FALSE");
    assertResult("true", "BT-00-Repeatable-Text:isWithheld == FALSE");
  }

  /**
   * {@code is unique in} is translated to a predicate after the sequence it searches.
   */
  @Test
  void testUniqueValueCondition_SequenceBeforeAPredicate() {
    assertResult("false", "1 is unique in 1 + 1");
    assertResult("true", "-1 is unique in -1");
    assertResult("true", "2 is unique in for number:$x in [1, 2] return $x");
  }

  /**
   * A value passed for a parameter keeps its grouping: the negative number before the predicate
   * that {@code is unique in} is translated to is put in parentheses.
   */
  @Test
  void testUniqueValueCondition_NegativeParameterValue() {
    assertEquals("true", evaluate(
        translateExpression("WITH ND-Root, number:$p COMPUTE -1 is unique in $p", "-1"), NOTICE));
  }

  /**
   * Additions and multiplications keep their grouping, with or without parentheses.
   */
  @Test
  void testArithmetic() {
    assertResult("-4", "1 - 2 - 3");
    assertResult("2", "1 - (2 - 3)");
    assertResult("2", "1 - -1");
    assertResult("9", "(1 + 2) * 3");
  }
}
