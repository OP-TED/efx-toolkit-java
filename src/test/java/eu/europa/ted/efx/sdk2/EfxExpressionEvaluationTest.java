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
 * what the expression means: how it is grouped, by the order of the operators in the grammar, and
 * in the XPath, which keeps that grouping where XPath would group the same operations differently,
 * or would not accept them without parentheses; what an index selects; and what value a context
 * variable gives.
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

  /**
   * A notice with two {@code ND-RepeatableNode}, each with one
   * {@code BT-00-Text-In-Repeatable-Node}: {@code a} in the first and {@code b} in the second.
   */
  private static final String TWO_REPEATABLE_NODES = "<Notice>"
      + "<RepeatableNode><TextField>a</TextField></RepeatableNode>"
      + "<RepeatableNode><TextField>b</TextField></RepeatableNode>"
      + "</Notice>";

  /**
   * A notice whose {@code BT-00-Text} holds {@code abc} with a space before and after it.
   */
  private static final String TEXT_WITH_SPACES = "<Notice><PathNode>"
      + "<TextField> abc </TextField>"
      + "</PathNode></Notice>";

  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  private void assertResult(final String expected, final String expression) {
    assertResult(expected, expression, NOTICE);
  }

  private void assertResult(final String expected, final String expression, final String notice) {
    assertEquals(expected,
        evaluate(translateComputeExpressionWithContext("ND-Root", expression), notice),
        expression);
  }

  /**
   * Asserts the result of an expression that reads an indicator parameter {@code $p}.
   */
  private void assertResultWithParameter(final String expected, final String expression,
      final String parameterValue) {
    final String withParameter = "WITH ND-Root, indicator:$p COMPUTE " + expression;
    assertEquals(expected, evaluate(translateExpression(withParameter, parameterValue), NOTICE),
        withParameter);
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

  @Test
  void testConditionBeforeComparison_Present() {
    assertResult("true", "TRUE == BT-00-Repeatable-Number is present");
    assertResult("true", "BT-00-Repeatable-Number is present == TRUE");
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
   * The expression after {@code return} extends as far to the right as it can, so a condition
   * after it is tested for each value: {@code for number:$x in [1, 2] return 1 has duplicates}
   * gives {@code false} twice. Applied to the whole sequence, the condition gives one value.
   */
  @Test
  void testReturnExtendsToTheRight() {
    assertResult("false false", "for number:$x in [1, 2] return 1 has duplicates");
    assertResult("true", "(for number:$x in [1, 2] return 1) has duplicates");
    assertResult("", "for text:$x in BT-00-Repeatable-Text return $x is empty");
    assertResult("true", "(for text:$x in BT-00-Repeatable-Text return $x) is empty");
  }

  @Test
  void testElseExtendsToTheRight() {
    assertResult("true", "if TRUE then TRUE else FALSE == FALSE");
    assertResult("false", "(if TRUE then TRUE else FALSE) == FALSE");
    assertResult("1", "if TRUE then 1 else 2 + 3");
    assertResult("4", "(if TRUE then 1 else 2) + 3");
  }

  /**
   * The expression after {@code else} extends as far to the right as it can, also when the
   * operand after the operator is a variable.
   */
  @Test
  void testElseExtendsToTheRight_VariableOperand() {
    assertResultWithParameter("true", "if TRUE then TRUE else FALSE == $p", "FALSE");
    assertResultWithParameter("false", "(if TRUE then TRUE else FALSE) == $p", "FALSE");
    assertResultWithParameter("true", "if TRUE then TRUE else FALSE and $p", "FALSE");
    assertResultWithParameter("false", "(if TRUE then TRUE else FALSE) and $p", "FALSE");
  }

  /**
   * The expression after {@code satisfies} extends as far to the right as it can. The notice has
   * no {@code BT-00-Repeatable-Text}, so {@code every} gives {@code true} and {@code some} gives
   * {@code false}, whatever follows {@code satisfies}: a {@code not in} applied to the whole
   * quantified expression gives the opposite.
   */
  @Test
  void testSatisfiesExtendsToTheRight() {
    assertResult("true", "every text:$x in BT-00-Repeatable-Text satisfies FALSE not in [TRUE]");
    assertResult("false", "(every text:$x in BT-00-Repeatable-Text satisfies FALSE) not in [TRUE]");
    assertResult("false", "some text:$x in BT-00-Repeatable-Text satisfies FALSE not in [TRUE]");
    assertResult("true", "(some text:$x in BT-00-Repeatable-Text satisfies FALSE) not in [TRUE]");
  }

  /**
   * The expression after {@code satisfies} extends as far to the right as it can, also when the
   * list after {@code not in} holds a variable.
   */
  @Test
  void testSatisfiesExtendsToTheRight_VariableInTheList() {
    assertResultWithParameter("true",
        "every text:$x in BT-00-Repeatable-Text satisfies FALSE not in [$p]", "TRUE");
    assertResultWithParameter("false",
        "(every text:$x in BT-00-Repeatable-Text satisfies FALSE) not in [$p]", "TRUE");
    assertResultWithParameter("false",
        "some text:$x in BT-00-Repeatable-Text satisfies FALSE not in [$p]", "TRUE");
    assertResultWithParameter("true",
        "(some text:$x in BT-00-Repeatable-Text satisfies FALSE) not in [$p]", "TRUE");
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

  /**
   * Additions and subtractions of durations are read from the left, as those of numbers are.
   */
  @Test
  void testDurationArithmetic() {
    assertResult("P2D", "P1D - P2D + P3D");
    assertResult("-P4D", "P1D - (P2D + P3D)");
  }

  /**
   * An index selects the nth element of the whole sequence, also before {@code :rawValue}, and
   * not the nth element under each parent: {@code [at 2]} selects the field of the second
   * {@code ND-RepeatableNode}.
   */
  @Test
  void testIndexBeforeRawValue_CountsAcrossParents() {
    assertResult("a", "BT-00-Text-In-Repeatable-Node[at 1]:rawValue", TWO_REPEATABLE_NODES);
    assertResult("b", "BT-00-Text-In-Repeatable-Node[at 2]:rawValue", TWO_REPEATABLE_NODES);
  }

  /**
   * A field context variable used as a value gives the value of its field, with its spaces
   * normalised, as the field itself does.
   */
  @Test
  void testContextVariableComparedWithText() {
    assertResult("true", "for context:$c in BT-00-Text return $c == 'abc'", TEXT_WITH_SPACES);
    assertResult("true", "BT-00-Text == 'abc'", TEXT_WITH_SPACES);
  }
}
