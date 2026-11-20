/*
 * Copyright 2023 European Union
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.EfxCompilationException;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

class EfxExpressionTranslatorV2Test extends EfxTestsBase {
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  // #region: Boolean expressions ---------------------------------------------

  @Test
  void testParenthesizedBooleanExpression() {
    testExpressionTranslationWithContext("(true() or true()) and false()", "BT-00-Text",
        "(ALWAYS or TRUE) and NEVER");
  }

  @Test
  void testLogicalOrCondition() {
    testExpressionTranslationWithContext("true() or false()", "BT-00-Text", "ALWAYS or NEVER");
  }

  @Test
  void testLogicalAndCondition() {
    testExpressionTranslationWithContext("true() and 1 + 1 = 2", "BT-00-Text",
        "ALWAYS and 1 + 1 == 2");
  }

  @Test
  void testInListCondition() {
    testExpressionTranslationWithContext("not('x' = ('a','b','c'))", "BT-00-Text",
        "'x' not in ['a', 'b', 'c']");
  }

  @Test
  void testBooleanInListCondition_WithFieldReference() {
    testExpressionTranslationWithContext("PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) = (true(),false())", "ND-Root",
        "BT-00-Indicator in [TRUE, FALSE]");
  }

  @Test
  void testNumberInListCondition_WithFieldReference() {
    testExpressionTranslationWithContext("PathNode/NumberField/number() = (1,2)", "ND-Root",
        "BT-00-Number in [1, 2]");
  }

  @Test
  void testNumberInListCondition_WithRepeatableField() {
    testExpressionTranslationWithContext("1 = PathNode/RepeatableNumberField/number()", "ND-Root",
        "1 in BT-00-Repeatable-Number");
  }

  @Test
  void testDateInListCondition_WithFieldReference() {
    testExpressionTranslationWithContext(
        "PathNode/StartDateField/xs:date(text()) = (xs:date('2020-01-01Z'),xs:date('2020-01-02Z'))",
        "ND-Root", "BT-00-StartDate in [2020-01-01Z, 2020-01-02Z]");
  }

  @Test
  void testTimeInListCondition_WithFieldReference() {
    testExpressionTranslationWithContext(
        "PathNode/StartTimeField/xs:time(text()) = (xs:time('10:00:00Z'),xs:time('11:00:00Z'))",
        "ND-Root", "BT-00-StartTime in [10:00:00Z, 11:00:00Z]");
  }

  @Test
  void testTimeInListCondition_WithRepeatableField() {
    testExpressionTranslationWithContext(
        "xs:time('10:00:00Z') = PathNode/RepeatableTimeField/xs:time(text())", "ND-Root",
        "10:00:00Z in BT-00-Repeatable-Time");
  }

  @Test
  void testPresenceCondition() {
    testExpressionTranslationWithContext("exists(PathNode/TextField)", "ND-Root", "BT-00-Text is present");
  }

  @Test
  void testPresenceCondition_WithNot() {
    testExpressionTranslationWithContext("not(exists(PathNode/TextField))", "ND-Root",
        "BT-00-Text is not present");
  }

  @Test
  void testPresenceCondition_WithNodeReference() {
    testExpressionTranslationWithContext("exists(SubNode)", "ND-Root", "ND-SubNode is present");
  }

  @Test
  void testPresenceCondition_WithNodeReference_WithNot() {
    testExpressionTranslationWithContext("not(exists(SubNode))", "ND-Root",
        "ND-SubNode is not present");
  }

  @Test
  void testPresenceCondition_WithAbsoluteNodeReference() {
    testExpressionTranslationWithContext("exists(/*/SubNode)", "BT-00-Text",
        "/ND-SubNode is present");
  }

  @Test
  void testPresenceCondition_WithNodeReferenceWithPredicate() {
    testExpressionTranslationWithContext("exists(SubNode['a' = 'a'])", "ND-Root",
        "ND-SubNode['a' == 'a'] is present");
  }

  @Test
  void testPresenceCondition_WithIndexedNodeReference() {
    testExpressionTranslationWithContext("exists((SubNode)[1])", "ND-Root", "ND-SubNode[at 1] is present");
  }

  @Test
  void testIndex_AtInUpperCase_IsRejected() {
    assertSyntaxError(
        () -> translateComputeExpressionWithContext("ND-Root", "ND-SubNode[AT 1] is present"));
  }

  @Test
  void testPresenceCondition_WithRawValue() {
    testExpressionTranslationWithContext("exists(PathNode/TextField/text())", "ND-Root",
        "BT-00-Text:rawValue is present");
  }

  @Test
  void testPresenceCondition_WithNodeContextVariable() {
    testExpressionTranslationWithContext("some $c in RepeatableNode satisfies exists($c)", "ND-Root",
        "some context:$c in ND-RepeatableNode satisfies $c is present");
  }

  @Test
  void testUniqueValueCondition() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/TextField/normalize-space(text()), $__x in /*/PathNode/TextField/normalize-space(text())[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Text");
  }

  @Test
  void testUniqueValueCondition_WithNot() {
    testExpressionTranslationWithContext(
        "not(count(for $__n in PathNode/TextField/normalize-space(text()), $__x in /*/PathNode/TextField/normalize-space(text())[. = $__n] return $__x) = 1)",
        "ND-Root", "BT-00-Text is not unique in /BT-00-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in 'b', $__x in ('a','b','c','b')[. = $__n] return $__x) = 1",
        "BT-00-Text", "'b' is unique in ['a', 'b', 'c', 'b']");
  }

  @Test
  void testNumericUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in 2, $__x in (1,2,3,2)[. = $__n] return $__x) = 1",
        "BT-00-Number", "2 is unique in [1, 2, 3, 2]");
  }

  @Test
  void testStringUniqueValueCondition_WithRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/TextField/normalize-space(text()), $__x in /*/PathNode/RepeatableTextField/normalize-space(text())[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Repeatable-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithNot() {
    testExpressionTranslationWithContext(
        "not(count(for $__n in 'x', $__x in ('a','b','c')[. = $__n] return $__x) = 1)",
        "BT-00-Text", "'x' is not unique in ['a', 'b', 'c']");
  }

  @Test
  void testStringUniqueValueCondition_WithRelativeFieldReference() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/TextField/normalize-space(text()), $__x in PathNode/RepeatableTextField/normalize-space(text())[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Text is unique in BT-00-Repeatable-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithFieldReferencePredicate() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/TextField/normalize-space(text()), $__x in /*/PathNode/RepeatableTextField[./normalize-space(text()) != '']/normalize-space(text())[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Repeatable-Text[BT-00-Repeatable-Text != '']");
  }

  @Test
  void testStringUniqueValueCondition_WithFieldInRepeatableNodePredicate() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/TextField/normalize-space(text()), $__x in /*/RepeatableNode/TextField[./normalize-space(text()) != '']/normalize-space(text())[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Text-In-Repeatable-Node[BT-00-Text-In-Repeatable-Node != '']");
  }

  @Test
  void testStringUniqueValueCondition_WithLiteralInRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in 'a', $__x in PathNode/RepeatableTextField/normalize-space(text())[. = $__n] return $__x) = 1",
        "ND-Root", "'a' is unique in BT-00-Repeatable-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithFieldInLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/TextField/normalize-space(text()), $__x in ('a','b')[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Text is unique in ['a', 'b']");
  }

  @Test
  void testNumericUniqueValueCondition_WithLiteralInRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in 1, $__x in PathNode/RepeatableNumberField/number()[. = $__n] return $__x) = 1",
        "ND-Root", "1 is unique in BT-00-Repeatable-Number");
  }

  @Test
  void testNumericUniqueValueCondition_WithFieldInLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/NumberField/number(), $__x in (1,2)[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Number is unique in [1, 2]");
  }

  @Test
  void testBooleanUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in true(), $__x in (true(),false(),true())[. = $__n] return $__x) = 1", "ND-Root",
        "TRUE is unique in [TRUE, FALSE, TRUE]");
  }

  @Test
  void testBooleanUniqueValueCondition_WithLiteralInRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in true(), $__x in PathNode/RepeatableIndicatorField/(normalize-space(text()) = ('true', '1'))[. = $__n] return $__x) = 1",
        "ND-Root", "TRUE is unique in BT-00-Repeatable-Indicator");
  }

  @Test
  void testBooleanUniqueValueCondition_WithFieldInLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')), $__x in (true(),false())[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Indicator is unique in [TRUE, FALSE]");
  }

  @Test
  void testDateUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in xs:date('2020-01-01Z'), $__x in (xs:date('2020-01-01Z'),xs:date('2020-01-02Z'))[. = $__n] return $__x) = 1",
        "ND-Root", "2020-01-01Z is unique in [2020-01-01Z, 2020-01-02Z]");
  }

  @Test
  void testDateUniqueValueCondition_WithLiteralInRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in xs:date('2020-01-01Z'), $__x in PathNode/RepeatableDateField/xs:date(text())[. = $__n] return $__x) = 1",
        "ND-Root", "2020-01-01Z is unique in BT-00-Repeatable-Date");
  }

  @Test
  void testDateUniqueValueCondition_WithFieldInLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/StartDateField/xs:date(text()), $__x in (xs:date('2020-01-01Z'),xs:date('2020-01-02Z'))[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-StartDate is unique in [2020-01-01Z, 2020-01-02Z]");
  }

  @Test
  void testTimeUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in xs:time('10:00:00Z'), $__x in (xs:time('10:00:00Z'),xs:time('11:00:00Z'))[. = $__n] return $__x) = 1",
        "ND-Root", "10:00:00Z is unique in [10:00:00Z, 11:00:00Z]");
  }

  @Test
  void testTimeUniqueValueCondition_WithLiteralInRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in xs:time('10:00:00Z'), $__x in PathNode/RepeatableTimeField/xs:time(text())[. = $__n] return $__x) = 1",
        "ND-Root", "10:00:00Z is unique in BT-00-Repeatable-Time");
  }

  @Test
  void testTimeUniqueValueCondition_WithFieldInLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in PathNode/StartTimeField/xs:time(text()), $__x in (xs:time('10:00:00Z'),xs:time('11:00:00Z'))[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-StartTime is unique in [10:00:00Z, 11:00:00Z]");
  }

  @Test
  void testDurationUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in xs:dayTimeDuration('P1D'), $__x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'))[. = $__n] return $__x) = 1",
        "ND-Root", "P1D is unique in [P1D, P2D]");
  }

  @Test
  void testDurationUniqueValueCondition_WithLiteralInRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $__n in xs:dayTimeDuration('P1D'), $__x in (for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))[. = $__n] return $__x) = 1",
        "ND-Root", "P1D is unique in BT-00-Repeatable-Duration");
  }

  @Test
  void testDurationUniqueValueCondition_WithFieldInLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $__n in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())), $__x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'))[. = $__n] return $__x) = 1",
        "ND-Root", "BT-00-Duration is unique in [P1D, P2D]");
  }


  @Test
  void testLikePatternCondition() {
    testExpressionTranslationWithContext("fn:matches(normalize-space('123'), '[0-9]*')",
        "BT-00-Text", "'123' like '[0-9]*'");
  }

  @Test
  void testLikePatternCondition_WithEscapedDot() {
    testExpressionTranslationWithContext(
        "fn:matches(normalize-space('12.3'), '[0-9]+\\.[0-9]+')",
        "BT-00-Text", "'12.3' like '[0-9]+\\.[0-9]+'");
  }

  @Test
  void testLikePatternCondition_WithEscapedSingleQuote() {
    testExpressionTranslationWithContext("fn:matches(normalize-space('test'), 'a''b')",
        "BT-00-Text", "'test' like 'a\\'b'");
  }

  @Test
  void testLikePatternCondition_WithEscapedDoubleQuote() {
    testExpressionTranslationWithContext("fn:matches(normalize-space('test'), 'a\"b')",
        "BT-00-Text", "'test' like 'a\\\"b'");
  }

  @Test
  void testLikePatternCondition_WithNot() {
    testExpressionTranslationWithContext("not(fn:matches(normalize-space('123'), '[0-9]*'))",
        "BT-00-Text", "'123' not like '[0-9]*'");
  }

  @Test
  void testLikePatternCondition_WithTextField() {
    testExpressionTranslation("fn:matches(normalize-space(PathNode/TextField/normalize-space(text())), '[0-9]*')",
        "{ND-Root} ${BT-00-Text like '[0-9]*'}");
  }


  @Test
  void testPreferredLanguage_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "preferred-language(BT-00-Text-Multilingual)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
    assertTrue(exception.getMessage().startsWith("line "), "Error message should include source position");
  }

  @Test
  void testPreferredLanguageText_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "preferred-language-text(BT-00-Text-Multilingual)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testPreferredLanguageProperty_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text-Multilingual:preferredLanguage"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
    assertEquals("line 1:12 Operator ':preferredLanguage' can only be used in templates,"
        + " not in expressions or validation rules.", exception.getMessage());
  }

  @Test
  void testPreferredLanguageTextProperty_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text-Multilingual:preferredLanguageText"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
    assertEquals("line 1:12 Operator ':preferredLanguageText' can only be used in templates,"
        + " not in expressions or validation rules.", exception.getMessage());
  }

  @Test
  void testFieldValueComparison_UsingTextFields_ThrowsBecauseMultilingual() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text == BT-00-Text-Multilingual"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL, exception.getErrorCode());
  }

  @Test
  void testFieldValueComparison_UsingNumericFields() {
    testExpressionTranslationWithContext(
        "PathNode/NumberField/number() <= PathNode/IntegerField/number()", "ND-Root",
        "BT-00-Number <= BT-00-Integer");
  }

  @Test
  void testFieldValueComparison_UsingIndicatorFields() {
    testExpressionTranslationWithContext("PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) != PathNode/IndicatorField/(normalize-space(text()) = ('true', '1'))",
        "ND-Root", "BT-00-Indicator != BT-00-Indicator");
  }

  @Test
  void testFieldValueComparison_UsingDateFields() {
    testExpressionTranslationWithContext(
        "PathNode/StartDateField/xs:date(text()) <= PathNode/EndDateField/xs:date(text())",
        "ND-Root", "BT-00-StartDate <= BT-00-EndDate");
  }

  @Test
  void testFieldValueComparison_UsingTimeFields() {
    testExpressionTranslationWithContext(
        "PathNode/StartTimeField/xs:time(text()) <= PathNode/EndTimeField/xs:time(text())",
        "ND-Root", "BT-00-StartTime <= BT-00-EndTime");
  }

  @Test
  void testFieldValueComparison_UsingDurationFields() {
    assertEquals(
        "boolean(for $__T in (current-date()) return ($__T + (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) <= $__T + (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))))",
        translateExpressionWithContext("ND-Root", "BT-00-Duration <= BT-00-Duration"));
  }

  @Test
  void testFieldValueComparison_WithStringLiteral() {
    testExpressionTranslationWithContext("PathNode/TextField/normalize-space(text()) = 'abc'",
        "ND-Root", "BT-00-Text == 'abc'");
  }

  @Test
  void testFieldValueComparison_WithNumericLiteral() {
    testExpressionTranslationWithContext(
        "PathNode/IntegerField/number() - PathNode/NumberField/number() > 0", "ND-Root",
        "BT-00-Integer - BT-00-Number > 0");
  }

  @Test
  void testFieldValueComparison_WithDateLiteral() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') > PathNode/StartDateField/xs:date(text())", "ND-Root",
        "2022-01-01Z > BT-00-StartDate");
  }

  @Test
  void testFieldValueComparison_WithTimeLiteral() {
    testExpressionTranslationWithContext(
        "xs:time('00:01:00Z') > PathNode/EndTimeField/xs:time(text())", "ND-Root",
        "00:01:00Z > BT-00-EndTime");
  }

  @Test
  void testFieldValueComparison_TypeMismatch() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "00:01:00Z > BT-00-StartDate"));
    assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, exception.getErrorCode());
  }


  @Test
  void testBooleanComparison_UsingLiterals() {
    testExpressionTranslationWithContext("false() != true()", "BT-00-Text", "NEVER != ALWAYS");
  }

  /**
   * An indicator field used on its own reads its value, not its presence: a field holding
   * "false" must not count as true (TEDEFO-5328).
   */
  @Test
  void testIndicatorField_OnItsOwn_ReadsItsValue() {
    testExpressionTranslationWithContext("../IndicatorField/(normalize-space(text()) = ('true', '1'))",
        "BT-00-Text", "BT-00-Indicator");
    testExpressionTranslationWithContext(
        "not(../IndicatorField/(normalize-space(text()) = ('true', '1')))", "BT-00-Text",
        "not(BT-00-Indicator)");
  }

  @Test
  void testBooleanComparison_UsingFieldReference() {
    testExpressionTranslationWithContext("../IndicatorField/(normalize-space(text()) = ('true', '1')) != true()", "BT-00-Text",
        "BT-00-Indicator != ALWAYS");
  }

  @Test
  void testBooleanComparison_UsingFieldReferenceOnTheRight() {
    testExpressionTranslationWithContext("true() = PathNode/IndicatorField/(normalize-space(text()) = ('true', '1'))", "ND-Root",
        "TRUE == BT-00-Indicator");
  }

  @Test
  void testNumericComparison() {
    testExpressionTranslationWithContext(
        "2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5 and ../NumberField/number() > ../IntegerField/number()",
        "BT-00-Text", "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5 and BT-00-Number > BT-00-Integer");
  }

  @Test
  void testNumericComparison_UsingFieldReferenceOnTheRight() {
    testExpressionTranslationWithContext("2 > PathNode/NumberField/number()", "ND-Root",
        "2 > BT-00-Number");
  }

  @Test
  void testStringComparison() {
    testExpressionTranslationWithContext("'aaa' < 'bbb'", "BT-00-Text", "'aaa' < 'bbb'");
  }

  @Test
  void testStringComparison_WithEscapedSingleQuote() {
    testExpressionTranslationWithContext("'a''b' = 'c''d'", "BT-00-Text", "'a\\'b' == 'c\\'d'");
  }

  @Test
  void testDateComparison_OfTwoDateLiterals() {
    testExpressionTranslationWithContext("xs:date('2018-01-01Z') > xs:date('2018-01-01Z')",
        "BT-00-Text", "2018-01-01Z > 2018-01-01Z");
  }

  @Test
  void testDateComparison_OfTwoDateReferences() {
    testExpressionTranslationWithContext(
        "PathNode/StartDateField/xs:date(text()) = PathNode/EndDateField/xs:date(text())",
        "ND-Root", "BT-00-StartDate == BT-00-EndDate");
  }

  @Test
  void testDateComparison_OfDateReferenceAndDateFunction() {
    testExpressionTranslationWithContext(
        "PathNode/StartDateField/xs:date(text()) = xs:date(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "BT-00-StartDate == date(BT-00-Text)");
  }

  @Test
  void testTimeComparison_OfTwoTimeLiterals() {
    testExpressionTranslationWithContext("xs:time('13:00:10Z') > xs:time('21:20:30Z')",
        "BT-00-Text", "13:00:10Z > 21:20:30Z");
  }

  @Test
  void testZonedTimeComparison_OfTwoTimeLiterals() {
    testExpressionTranslationWithContext("xs:time('13:00:10+01:00') > xs:time('21:20:30+02:00')",
        "BT-00-Text", "13:00:10+01:00 > 21:20:30+02:00");
  }

  @Test
  void testTimeComparison_OfTwoTimeReferences() {
    testExpressionTranslationWithContext(
        "PathNode/StartTimeField/xs:time(text()) = PathNode/EndTimeField/xs:time(text())",
        "ND-Root", "BT-00-StartTime == BT-00-EndTime");
  }

  @Test
  void testTimeComparison_OfTimeReferenceAndTimeFunction() {
    testExpressionTranslationWithContext(
        "PathNode/StartTimeField/xs:time(text()) = xs:time(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "BT-00-StartTime == time(BT-00-Text)");
  }

  @Test
  void testDurationComparison_UsingYearMonthDurationLiterals() {
    testExpressionTranslationWithContext(
        "boolean(for $__T in (current-date()) return ($__T + xs:yearMonthDuration('P1Y') = $__T + xs:yearMonthDuration('P12M')))",
        "BT-00-Text", "P1Y == P12M");
  }

  @Test
  void testDurationComparison_UsingDayTimeDurationLiterals() {
    testExpressionTranslationWithContext(
        "boolean(for $__T in (current-date()) return ($__T + xs:dayTimeDuration('P21D') > $__T + xs:dayTimeDuration('P7D')))",
        "BT-00-Text", "P3W > P7D");
  }

  @Test
  @ResourceLock(Resources.LOCALE)
  void testDurationComparison_UsingDayTimeDurationLiterals_AsciiDigitsWhateverTheDefaultLocale() {
    Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
    try {
      Locale.setDefault(Locale.Category.FORMAT, new Locale("ar", "EG"));
      testExpressionTranslationWithContext(
          "boolean(for $__T in (current-date()) return ($__T + xs:dayTimeDuration('P21D') > $__T + xs:dayTimeDuration('P7D')))",
          "BT-00-Text", "P3W > P7D");
    } finally {
      Locale.setDefault(Locale.Category.FORMAT, formatLocale);
    }
  }

  @Test
  void testCalculatedDurationComparison() {
    testExpressionTranslationWithContext(
        "boolean(for $__T in (current-date()) return ($__T + xs:yearMonthDuration('P3M') > $__T + (xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text())))))",
        "ND-Root", "P3M > (BT-00-EndDate - BT-00-StartDate)");
  }


  @Test
  void testNegativeDuration_Literal() {
    testExpressionTranslationWithContext("xs:yearMonthDuration('-P3M')", "ND-Root", "-P3M");
  }

  @Test
  void testNegativeDuration_ViaMultiplication() {
    testExpressionTranslationWithContext("(-3 * (2 * xs:yearMonthDuration('-P3M')))", "ND-Root",
        "2 * -P3M * -3");
  }

  @Test
  void testNegativeDuration_ViaMultiplicationWithField() {
    assertEquals(
        "(-3 * (2 * (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))))",
        translateExpressionWithContext("ND-Root", "2 * BT-00-Duration * -3"));
  }

  @Test
  void testDurationMultiplication_DurationTimesLateBoundNumeric() {
    testExpressionTranslationWithContext(
        "(PathNode/NumberField/number() * xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D * BT-00-Number");
  }

  @Test
  void testDurationMultiplication_LateBoundNumericTimesDuration() {
    testExpressionTranslationWithContext(
        "(PathNode/NumberField/number() * xs:dayTimeDuration('P1D'))", "ND-Root",
        "BT-00-Number * P1D");
  }

  @Test
  void testDurationMultiplication_NumberTimesLateBoundDuration() {
    testExpressionTranslationWithContext(
        "boolean(for $__T in (current-date()) return ($__T + (2 * (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))) > $__T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "2 * BT-00-Duration > P1D");
  }

  @Test
  void testDurationAddition() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P3D') + (xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text()))))",
        "ND-Root", "P3D + (BT-00-StartDate - BT-00-EndDate)");
  }

  @Test
  void testDurationSubtraction() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P3D') - (xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text()))))",
        "ND-Root", "P3D - (BT-00-StartDate - BT-00-EndDate)");
  }

  @Test
  void testDurationAddition_UsingDurationLiterals() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P1D') + xs:dayTimeDuration('P2D'))", "ND-Root",
        "P1D + P2D");
  }

  @Test
  void testDurationAddition_UsingFieldReferenceOnTheLeft() {
    testExpressionTranslationWithContext(
        "((for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) + xs:dayTimeDuration('P1D'))",
        "ND-Root", "BT-00-Duration + P1D");
  }

  @Test
  void testDurationSubtraction_UsingDurationLiterals() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P2D') - xs:dayTimeDuration('P1D'))", "ND-Root",
        "P2D - P1D");
  }

  @Test
  void testDurationSubtraction_UsingFieldReferenceOnTheLeft() {
    testExpressionTranslationWithContext(
        "((for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) - xs:dayTimeDuration('P1D'))",
        "ND-Root", "BT-00-Duration - P1D");
  }

  @Test
  void testDateSubtraction_UsingDateLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration(xs:date('2020-01-02Z') - xs:date('2020-01-01Z'))", "ND-Root",
        "2020-01-02Z - 2020-01-01Z");
  }

  @Test
  void testDateSubtraction_UsingFieldReferenceOnTheLeft() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - xs:date('2020-01-01Z'))",
        "ND-Root", "BT-00-EndDate - 2020-01-01Z");
  }

  @Test
  void testDateSubtraction_UsingFieldReferenceOnTheRight() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration(xs:date('2020-01-02Z') - PathNode/StartDateField/xs:date(text()))",
        "ND-Root", "2020-01-02Z - BT-00-StartDate");
  }

  // The operators take the overload their operands match, whichever grammar alternative read them,
  // so a date and a duration add and subtract as add-duration and subtract-duration do, and a
  // duration multiplies by a number on either side.

  @Test
  void testDateAddition_UsingFieldReferenceOnTheLeft() {
    testExpressionTranslationWithContext(
        "(PathNode/StartDateField/xs:date(text()) + xs:dayTimeDuration('P3D'))",
        "ND-Root", "BT-00-StartDate + P3D");
  }

  @Test
  void testDateAddition_UsingFieldReferenceOnTheRight() {
    testExpressionTranslationWithContext(
        "(PathNode/StartDateField/xs:date(text()) + xs:dayTimeDuration('P3D'))",
        "ND-Root", "P3D + BT-00-StartDate");
  }

  @Test
  void testDateAddition_UsingDurationFieldOnTheLeft() {
    testExpressionTranslationWithContext(
        "(PathNode/StartDateField/xs:date(text()) + (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "BT-00-Duration + BT-00-StartDate");
  }

  @Test
  void testDateMinusDuration_UsingFieldReferenceOnTheLeft() {
    testExpressionTranslationWithContext(
        "(PathNode/StartDateField/xs:date(text()) - xs:dayTimeDuration('P3D'))",
        "ND-Root", "BT-00-StartDate - P3D");
  }

  @Test
  void testDateMinusDuration_UsingFieldReferenceOnTheRight() {
    testExpressionTranslationWithContext(
        "(xs:date('2020-01-01Z') - (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "2020-01-01Z - BT-00-Duration");
  }

  @Test
  void testDurationMultiplication_LateBoundDurationTimesNumber() {
    testExpressionTranslationWithContext(
        "(2 * (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "BT-00-Duration * 2");
  }

  @Test
  void testDurationMultiplication_NumberTimesLateBoundDuration_AsAValue() {
    testExpressionTranslationWithContext(
        "(2 * (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "2 * BT-00-Duration");
  }

  @Test
  void testDurationMultiplication_LateBoundDurationTimesLateBoundNumber() {
    testExpressionTranslationWithContext(
        "(PathNode/NumberField/number() * (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "BT-00-Duration * BT-00-Number");
  }

  @Test
  void testBooleanLiteralExpression_Always() {
    testExpressionTranslationWithContext("true()", "BT-00-Text", "ALWAYS");
  }

  @Test
  void testBooleanLiteralExpression_Never() {
    testExpressionTranslationWithContext("false()", "BT-00-Text", "NEVER");
  }

  // #endregion: Boolean expressions

  // #region: Quantified expressions ------------------------------------------

  @Test
  void testQuantifiedExpression_EveryInUpperCase_IsRejected() {
    assertSyntaxError(() -> translateComputeExpressionWithContext("ND-Root",
        "EVERY text:$x in ['a', 'b'] satisfies $x <= 'a'"));
  }

  @Test
  void testStringQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext("every $x in ('a','b','c') satisfies $x <= 'a'", "ND-Root",
        "every text:$x in ['a', 'b', 'c'] satisfies $x <= 'a'");
  }

  @Test
  void testStringQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext("every $x in PathNode/TextField/normalize-space(text()) satisfies $x <= 'a'",
        "ND-Root", "every text:$x in BT-00-Text satisfies $x <= 'a'");
  }

  @Test
  void testBooleanQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext("every $x in (true(),false(),true()) satisfies $x",
        "ND-Root", "every indicator:$x in [TRUE, FALSE, ALWAYS] satisfies $x");
  }

  @Test
  void testBooleanQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext("every $x in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) satisfies $x",
        "ND-Root", "every indicator:$x in BT-00-Indicator satisfies $x");
  }

  @Test
  void testNumericQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext("every $x in (1,2,3) satisfies $x <= 1", "ND-Root",
        "every number:$x in [1, 2, 3] satisfies $x <= 1");
  }

  @Test
  void testNumericQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext("every $x in PathNode/NumberField/number() satisfies $x <= 1",
        "ND-Root", "every number:$x in BT-00-Number satisfies $x <= 1");
  }

  @Test
  void testDateQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext(
        "every $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) satisfies $x <= xs:date('2012-01-01Z')",
        "ND-Root",
        "every date:$x in [2012-01-01Z, 2012-01-02Z, 2012-01-03Z] satisfies $x <= 2012-01-01Z");
  }

  @Test
  void testDateQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "every $x in PathNode/StartDateField/xs:date(text()) satisfies $x <= xs:date('2012-01-01Z')", "ND-Root",
        "every date:$x in BT-00-StartDate satisfies $x <= 2012-01-01Z");
  }

  @Test
  void testDateQuantifiedExpression_UsingMultipleIterators() {
    testExpressionTranslationWithContext(
        "every $x in PathNode/StartDateField/xs:date(text()), $y in ($x,xs:date('2022-02-02Z')), $i in (true(),true()) satisfies $x <= xs:date('2012-01-01Z')",
        "ND-Root",
        "every date:$x in BT-00-StartDate, date:$y in [$x, 2022-02-02Z], indicator:$i in [ALWAYS, TRUE] satisfies $x <= 2012-01-01Z");
  }

  @Test
  void testTimeQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext(
        "every $x in (xs:time('00:00:00Z'),xs:time('00:00:01Z'),xs:time('00:00:02Z')) satisfies $x <= xs:time('00:00:00Z')",
        "ND-Root", "every time:$x in [00:00:00Z, 00:00:01Z, 00:00:02Z] satisfies $x <= 00:00:00Z");
  }

  @Test
  void testTimeQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "every $x in PathNode/StartTimeField/xs:time(text()) satisfies $x <= xs:time('00:00:00Z')", "ND-Root",
        "every time:$x in BT-00-StartTime satisfies $x <= 00:00:00Z");
  }

  @Test
  void testDurationQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext(
        "every $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')) satisfies boolean(for $__T in (current-date()) return ($__T + $x <= $__T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "every duration:$x in [P1D, P2D, P3D] satisfies $x <= P1D");
  }

  @Test
  void testDurationQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "every $x in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) satisfies boolean(for $__T in (current-date()) return ($__T + $x <= $__T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "every duration:$x in BT-00-Duration satisfies $x <= P1D");
  }

  // #endregion: Quantified expressions

  // #region: Conditional expressions -----------------------------------------

  @Test
  void testConditionalExpression() {
    testExpressionTranslationWithContext("(if (1 > 2) then 'a' else 'b')", "ND-Root",
        "if 1 > 2 then 'a' else 'b'");
  }

  @Test
  void testConditionalStringExpression_UsingLiterals() {
    testExpressionTranslationWithContext("(if ('a' > 'b') then 'a' else 'b')", "ND-Root",
        "if 'a' > 'b' then 'a' else 'b'");
  }

  @Test
  void testConditionalStringExpression_UsingFieldReferenceInCondition() {
    testExpressionTranslationWithContext(
        "(if ('a' > PathNode/TextField/normalize-space(text())) then 'a' else 'b')", "ND-Root",
        "if 'a' > BT-00-Text then 'a' else 'b'");
    testExpressionTranslationWithContext(
        "(if (PathNode/TextField/normalize-space(text()) >= 'a') then 'a' else 'b')", "ND-Root",
        "if BT-00-Text >= 'a' then 'a' else 'b'");
    testExpressionTranslationWithContext(
        "(if (PathNode/TextField/normalize-space(text()) >= PathNode/TextField/normalize-space(text())) then 'a' else 'b')",
        "ND-Root", "if BT-00-Text >= BT-00-Text then 'a' else 'b'");
    testExpressionTranslationWithContext(
        "(if (PathNode/StartDateField/xs:date(text()) >= PathNode/EndDateField/xs:date(text())) then 'a' else 'b')",
        "ND-Root", "if BT-00-StartDate >= BT-00-EndDate then 'a' else 'b'");
  }

  @Test
  void testConditionalStringExpression_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "(if ('a' > 'b') then PathNode/TextField/normalize-space(text()) else 'b')", "ND-Root",
        "if 'a' > 'b' then BT-00-Text else 'b'");
    testExpressionTranslationWithContext(
        "(if ('a' > 'b') then 'a' else PathNode/TextField/normalize-space(text()))", "ND-Root",
        "if 'a' > 'b' then 'a' else BT-00-Text");
    testExpressionTranslationWithContext(
        "(if ('a' > 'b') then PathNode/TextField/normalize-space(text()) else PathNode/TextField/normalize-space(text()))",
        "ND-Root", "if 'a' > 'b' then BT-00-Text else BT-00-Text");
  }

  @Test
  void testConditionalStringExpression_UsingFieldReferences_TypeMismatch() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "if 'a' > 'b' then BT-00-StartDate else BT-00-Text"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  @Test
  void testConditionalBooleanExpression() {
    testExpressionTranslationWithContext("(if (PathNode/IndicatorField/(normalize-space(text()) = ('true', '1'))) then true() else false())",
        "ND-Root", "if BT-00-Indicator then TRUE else FALSE");
  }

  @Test
  void testConditionalNumericExpression() {
    testExpressionTranslationWithContext("(if (1 > 2) then 1 else PathNode/NumberField/number())",
        "ND-Root", "if 1 > 2 then 1 else BT-00-Number");
  }

  @Test
  void testConditionalDateExpression() {
    testExpressionTranslationWithContext(
        "(if (xs:date('2012-01-01Z') > PathNode/EndDateField/xs:date(text())) then PathNode/StartDateField/xs:date(text()) else xs:date('2012-01-02Z'))",
        "ND-Root", "if 2012-01-01Z > BT-00-EndDate then BT-00-StartDate else 2012-01-02Z");
  }

  @Test
  void testConditionalTimeExpression() {
    testExpressionTranslationWithContext(
        "(if (PathNode/EndTimeField/xs:time(text()) > xs:time('00:00:01Z')) then PathNode/StartTimeField/xs:time(text()) else xs:time('00:00:01Z'))",
        "ND-Root", "if BT-00-EndTime > 00:00:01Z then BT-00-StartTime else 00:00:01Z");
  }

  @Test
  void testConditionalDurationExpression() {
    assertEquals(
        "(if (boolean(for $__T in (current-date()) return ($__T + xs:dayTimeDuration('P1D') > $__T + (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))))) then xs:dayTimeDuration('P1D') else xs:dayTimeDuration('P2D'))",
        translateExpressionWithContext("ND-Root", "if P1D > BT-00-Duration then P1D else P2D"));
  }

  @Test
  void testConditionalBooleanExpression_UsingVariableInThenBranch() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return (if (1 > 2) then $x else false())", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return if 1 > 2 then $x else FALSE");
  }

  @Test
  void testConditionalBooleanExpression_UsingVariableInElseBranch() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return (if (1 > 2) then true() else $x)", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return if 1 > 2 then TRUE else $x");
  }

  @Test
  void testConditionalNumericExpression_UsingLiterals() {
    testExpressionTranslationWithContext("(if (1 > 2) then 1 else 2)", "ND-Root",
        "if 1 > 2 then 1 else 2");
  }

  @Test
  void testConditionalNumericExpression_UsingFieldReferenceInThenBranch() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then PathNode/NumberField/number() else 2)", "ND-Root",
        "if 1 > 2 then BT-00-Number else 2");
  }

  @Test
  void testConditionalDateExpression_UsingLiterals() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then xs:date('2020-01-01Z') else xs:date('2020-01-02Z'))", "ND-Root",
        "if 1 > 2 then 2020-01-01Z else 2020-01-02Z");
  }

  @Test
  void testConditionalDateExpression_UsingFieldReferenceInElseBranch() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then xs:date('2020-01-01Z') else PathNode/StartDateField/xs:date(text()))",
        "ND-Root", "if 1 > 2 then 2020-01-01Z else BT-00-StartDate");
  }

  @Test
  void testConditionalTimeExpression_UsingLiterals() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then xs:time('10:00:00Z') else xs:time('11:00:00Z'))", "ND-Root",
        "if 1 > 2 then 10:00:00Z else 11:00:00Z");
  }

  @Test
  void testConditionalTimeExpression_UsingFieldReferenceInElseBranch() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then xs:time('10:00:00Z') else PathNode/StartTimeField/xs:time(text()))",
        "ND-Root", "if 1 > 2 then 10:00:00Z else BT-00-StartTime");
  }

  @Test
  void testConditionalDurationExpression_UsingFieldReferenceInThenBranch() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) else xs:dayTimeDuration('P1D'))",
        "ND-Root", "if 1 > 2 then BT-00-Duration else P1D");
  }

  @Test
  void testConditionalDurationExpression_UsingFieldReferenceInElseBranch() {
    testExpressionTranslationWithContext(
        "(if (1 > 2) then xs:dayTimeDuration('P1D') else (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "if 1 > 2 then P1D else BT-00-Duration");
  }

  // The expression after else extends as far to the right as it can, as in XPath.

  @Test
  void testConditionalExpression_ElseBranchTakesTheComparison_Throws() {
    // Read as if TRUE then 1 else (2 == 3): a number and a boolean as branches
    assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "if TRUE then 1 else 2 == 3"));
  }

  @Test
  void testConditionalExpression_ElseBranchTakesTheAddition() {
    testExpressionTranslationWithContext("(if (true()) then 1 else 2 + 3)", "ND-Root",
        "if TRUE then 1 else 2 + 3");
  }

  @Test
  void testConditionalExpression_InParentheses_Compared() {
    testExpressionTranslationWithContext("((if (true()) then 1 else 2)) = 3", "ND-Root",
        "(if TRUE then 1 else 2) == 3");
  }

  // The conditional takes the join of its branches: a sequence if either branch is one, the
  // other promoted to a sequence of one, and a single value if both are.

  @Test
  void testConditionalExpression_SequenceInThenBranch() {
    testExpressionTranslationWithContext("(if (true()) then (1) else 2)", "ND-Root",
        "if TRUE then [1] else 2");
  }

  @Test
  void testConditionalExpression_SequenceInElseBranch() {
    testExpressionTranslationWithContext("(if (true()) then 1 else (2))", "ND-Root",
        "if TRUE then 1 else [2]");
  }

  @Test
  void testConditionalExpression_SequenceInBothBranches() {
    testExpressionTranslationWithContext("(if (true()) then (1) else (2))", "ND-Root",
        "if TRUE then [1] else [2]");
  }

  @Test
  void testConditionalExpression_RepeatableFieldInThenBranch() {
    testExpressionTranslationWithContext(
        "(if (true()) then PathNode/RepeatableNumberField/number() else 2)", "ND-Root",
        "if TRUE then BT-00-Repeatable-Number else 2");
  }

  @Test
  void testConditionalExpression_RepeatableFieldInElseBranch() {
    testExpressionTranslationWithContext(
        "(if (true()) then 2 else PathNode/RepeatableNumberField/number())", "ND-Root",
        "if TRUE then 2 else BT-00-Repeatable-Number");
  }

  @Test
  void testConditionalExpression_RepeatableTextFieldAndList() {
    testExpressionTranslationWithContext(
        "(if (true()) then PathNode/RepeatableTextField/normalize-space(text()) else ('a'))",
        "ND-Root", "if TRUE then BT-00-Repeatable-Text else ['a']");
  }

  @Test
  void testConditionalExpression_DateListAndDateField() {
    testExpressionTranslationWithContext(
        "(if (true()) then (xs:date('2020-01-01Z')) else PathNode/StartDateField/xs:date(text()))",
        "ND-Root", "if TRUE then [2020-01-01Z] else BT-00-StartDate");
  }

  @Test
  void testConditionalExpression_MultilingualFieldInThenBranch() {
    // A multilingual field is a sequence of texts, one per language, as in, for and count take it.
    testExpressionTranslationWithContext(
        "(if (true()) then PathNode/TextMultilingualField/normalize-space(text()) else 'a')",
        "ND-Root", "if TRUE then BT-00-Text-Multilingual else 'a'");
  }

  @Test
  void testConditionalExpression_MultilingualFieldInElseBranch() {
    testExpressionTranslationWithContext(
        "(if (true()) then 'a' else PathNode/TextMultilingualField/normalize-space(text()))",
        "ND-Root", "if TRUE then 'a' else BT-00-Text-Multilingual");
  }

  @Test
  void testConditionalExpression_MultilingualFieldInBranch_IsATextSequence() {
    assertTypeError(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS,
        "line 1:12 Type mismatch. Operator '==' cannot take 'text*' and 'text'."
            + " Its left operand must be 'text'.",
        "(if TRUE then BT-00-Text-Multilingual else 'a') == 'b'");
  }

  @Test
  void testConditionalExpression_CodeFieldInElseBranch() {
    // A code and a text join as a text in either order.
    testExpressionTranslationWithContext(
        "(if (true()) then 'a' else PathNode/CodeField/normalize-space(text()))", "ND-Root",
        "if TRUE then 'a' else BT-00-Code");
  }

  @Test
  void testConditionalExpression_SequenceCounted() {
    testExpressionTranslationWithContext("count((if (true()) then (1,2) else (3)))", "ND-Root",
        "count(if TRUE then [1, 2] else [3])");
  }

  @Test
  void testConditionalExpression_NestedWithASequence() {
    testExpressionTranslationWithContext(
        "(if (true()) then 1 else (if (false()) then (2) else 3))", "ND-Root",
        "if TRUE then 1 else if FALSE then [2] else 3");
  }

  @Test
  void testConditionalExpression_SequenceIteratedOver() {
    testExpressionTranslationWithContext("for $x in (if (true()) then (1) else (2)) return $x",
        "ND-Root", "for number:$x in if TRUE then [1] else [2] return $x");
  }

  @Test
  void testConditionalExpression_SequenceCompared_Throws() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "(if TRUE then [1] else 2) == 3"));
    assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, exception.getErrorCode());
  }

  @Test
  void testSequenceFromIteration_ReturnTakesTheAddition() {
    // The return clause reaches as far to the right too: (for ... return $x) + 1 would add a
    // number to a sequence
    testExpressionTranslationWithContext("for $x in (1,2) return $x + 1", "ND-Root",
        "for number:$x in [1, 2] return $x + 1");
  }

  // #endregion: Conditional expressions

  // #region: Iteration expressions -------------------------------------------

  // Strings from iteration ---------------------------------------------------

  @Test
  void testStringsFromStringIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in ('a','b','c') return concat($x, 'text'))", "ND-Root",
        "'a' in (for text:$x in ['a', 'b', 'c'] return concat($x, 'text'))");
  }

  @Test
  void testStringsSequenceFromIteration_UsingMultipleIterators() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in ('a','b','c'), $y in (1,2), $z in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return concat($x, string($y), 'text'))",
        "ND-Root",
        "'a' in (for text:$x in ['a', 'b', 'c'], number:$y in [1, 2], indicator:$z in BT-00-Indicator return concat($x, string($y), 'text'))");
  }

  @Test
  void testStringsSequenceFromIteration_UsingObjectVariable() {
    testExpressionTranslationWithContext(
        "for $n in PathNode/TextField[exists(../NumberField)], $d in $n/../StartDateField/xs:date(text()) return 'text'",
        "ND-Root",
        "for context:$n in BT-00-Text[BT-00-Number is present], date:$d in $n::BT-00-StartDate return 'text'");
  }

  @Test
  void testStringsSequenceFromIteration_UsingNodeContextVariable() {
    testExpressionTranslationWithContext(
        "for $n in .[PathNode/TextField/normalize-space(text()) = 'a'] return 'text'", "ND-Root",
        "for context:$n in ND-Root[BT-00-Text == 'a'] return 'text'");
  }

  @Test
  void testStringsFromStringIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in PathNode/TextField/normalize-space(text()) return concat($x, 'text'))", "ND-Root",
        "'a' in (for text:$x in BT-00-Text return concat($x, 'text'))");
  }

  @Test
  void testStringsFromStringIteration_UsingMultilingualFieldReference() {
    // This test verifies that iterating over a multilingual text field with a text iterator works correctly
    // The iterator variable should inherit the actual type (multilingual string) from the sequence
    testExpressionTranslationWithContext(
        "'a' = (for $x in PathNode/TextMultilingualField/normalize-space(text()) return concat($x, 'text'))", "ND-Root",
        "'a' in (for text:$x in BT-00-Text-Multilingual return concat($x, 'text'))");
  }


  @Test
  void testStringsFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext("'a' = (for $x in (true(),false()) return 'y')", "ND-Root",
        "'a' in (for indicator:$x in [TRUE, FALSE] return 'y')");
  }

  @Test
  void testStringsFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return 'y')",
        "ND-Root", "'a' in (for indicator:$x in BT-00-Indicator return 'y')");
  }


  @Test
  void testStringsFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext("'a' = (for $x in (1,2,3) return 'y')", "ND-Root",
        "'a' in (for number:$x in [1, 2, 3] return 'y')");
  }

  @Test
  void testStringsFromNumericIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in PathNode/NumberField/number() return 'y')",
        "ND-Root", "'a' in (for number:$x in BT-00-Number return 'y')");
  }

  @Test
  void testStringsFromDateIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return 'y')",
        "ND-Root", "'a' in (for date:$x in [2012-01-01Z, 2012-01-02Z, 2012-01-03Z] return 'y')");
  }

  @Test
  void testStringsFromDateIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in PathNode/StartDateField/xs:date(text()) return 'y')",
        "ND-Root", "'a' in (for date:$x in BT-00-StartDate return 'y')");
  }

  @Test
  void testStringsFromTimeIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return 'y')",
        "ND-Root", "'a' in (for time:$x in [12:00:00Z, 12:00:01Z, 12:00:02Z] return 'y')");
  }

  @Test
  void testStringsFromTimeIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in PathNode/StartTimeField/xs:time(text()) return 'y')",
        "ND-Root", "'a' in (for time:$x in BT-00-StartTime return 'y')");
  }

  @Test
  void testStringsFromDurationIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 'y')",
        "ND-Root", "'a' in (for duration:$x in [P1D, P1Y, P2M] return 'y')");
  }


  @Test
  void testStringsFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return 'y')",
        "ND-Root", "'a' in (for duration:$x in BT-00-Duration return 'y')");
  }

  // Numbers from iteration ---------------------------------------------------

  @Test
  void testNumbersFromStringIteration_UsingLiterals() {
    testExpressionTranslationWithContext("123 = (for $x in ('a','b','c') return number($x))",
        "ND-Root", "123 in (for text:$x in ['a', 'b', 'c'] return number($x))");
  }

  @Test
  void testNumbersFromStringIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/TextField/normalize-space(text()) return number($x))",
        "ND-Root", "123 in (for text:$x in BT-00-Text return number($x))");
  }


  @Test
  void testNumbersFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext("123 = (for $x in (true(),false()) return 0)", "ND-Root",
        "123 in (for indicator:$x in [TRUE, FALSE] return 0)");
  }

  @Test
  void testNumbersFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return 0)",
        "ND-Root", "123 in (for indicator:$x in BT-00-Indicator return 0)");
  }


  @Test
  void testNumbersFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext("123 = (for $x in (1,2,3) return 0)", "ND-Root",
        "123 in (for number:$x in [1, 2, 3] return 0)");
  }

  @Test
  void testNumbersFromNumericIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/NumberField/number() return 0)",
        "ND-Root", "123 in (for number:$x in BT-00-Number return 0)");
  }

  @Test
  void testNumbersFromDateIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "123 = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return 0)",
        "ND-Root", "123 in (for date:$x in [2012-01-01Z, 2012-01-02Z, 2012-01-03Z] return 0)");
  }

  @Test
  void testNumbersFromDateIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/StartDateField/xs:date(text()) return 0)",
        "ND-Root", "123 in (for date:$x in BT-00-StartDate return 0)");
  }

  @Test
  void testNumbersFromTimeIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "123 = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return 0)",
        "ND-Root", "123 in (for time:$x in [12:00:00Z, 12:00:01Z, 12:00:02Z] return 0)");
  }

  @Test
  void testNumbersFromTimeIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/StartTimeField/xs:time(text()) return 0)",
        "ND-Root", "123 in (for time:$x in BT-00-StartTime return 0)");
  }

  @Test
  void testNumbersFromDurationIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "123 = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 0)",
        "ND-Root", "123 in (for duration:$x in [P1D, P1Y, P2M] return 0)");
  }


  @Test
  void testNumbersFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return 0)",
        "ND-Root", "123 in (for duration:$x in BT-00-Duration return 0)");
  }

  // Dates from iteration ---------------------------------------------------

  @Test
  void testDatesFromStringIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in ('a','b','c') return xs:date($x))", "ND-Root",
        "2022-01-01Z in (for text:$x in ['a', 'b', 'c'] return date($x))");
  }

  @Test
  void testDatesFromStringIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in PathNode/TextField/normalize-space(text()) return xs:date($x))", "ND-Root",
        "2022-01-01Z in (for text:$x in BT-00-Text return date($x))");
  }


  @Test
  void testDatesFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in (true(),false()) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for indicator:$x in [TRUE, FALSE] return 2022-01-01Z)");
  }

  @Test
  void testDatesFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for indicator:$x in BT-00-Indicator return 2022-01-01Z)");
  }


  @Test
  void testDatesFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in (1,2,3) return xs:date('2022-01-01Z'))", "ND-Root",
        "2022-01-01Z in (for number:$x in [1, 2, 3] return 2022-01-01Z)");
  }

  @Test
  void testDatesFromNumericIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in PathNode/NumberField/number() return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for number:$x in BT-00-Number return 2022-01-01Z)");
  }

  @Test
  void testDatesFromDateIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return xs:date('2022-01-01Z'))",
        "ND-Root",
        "2022-01-01Z in (for date:$x in [2012-01-01Z, 2012-01-02Z, 2012-01-03Z] return 2022-01-01Z)");
  }

  @Test
  void testDatesFromDateIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in PathNode/StartDateField/xs:date(text()) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for date:$x in BT-00-StartDate return 2022-01-01Z)");
  }

  @Test
  void testDatesFromTimeIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return xs:date('2022-01-01Z'))",
        "ND-Root",
        "2022-01-01Z in (for time:$x in [12:00:00Z, 12:00:01Z, 12:00:02Z] return 2022-01-01Z)");
  }

  @Test
  void testDatesFromTimeIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in PathNode/StartTimeField/xs:time(text()) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for time:$x in BT-00-StartTime return 2022-01-01Z)");
  }

  @Test
  void testDatesFromDurationIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for duration:$x in [P1D, P1Y, P2M] return 2022-01-01Z)");
  }


  @Test
  void testDatesFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:date('2022-01-01Z') = (for $x in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for duration:$x in BT-00-Duration return 2022-01-01Z)");
  }

  // Times from iteration ---------------------------------------------------

  @Test
  void testTimesFromStringIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in ('a','b','c') return xs:time($x))", "ND-Root",
        "12:00:00Z in (for text:$x in ['a', 'b', 'c'] return time($x))");
  }

  @Test
  void testTimesFromStringIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in PathNode/TextField/normalize-space(text()) return xs:time($x))", "ND-Root",
        "12:00:00Z in (for text:$x in BT-00-Text return time($x))");
  }


  @Test
  void testTimesFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in (true(),false()) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for indicator:$x in [TRUE, FALSE] return 12:00:00Z)");
  }

  @Test
  void testTimesFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for indicator:$x in BT-00-Indicator return 12:00:00Z)");
  }


  @Test
  void testTimesFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in (1,2,3) return xs:time('12:00:00Z'))", "ND-Root",
        "12:00:00Z in (for number:$x in [1, 2, 3] return 12:00:00Z)");
  }

  @Test
  void testTimesFromNumericIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in PathNode/NumberField/number() return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for number:$x in BT-00-Number return 12:00:00Z)");
  }

  @Test
  void testTimesFromDateIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return xs:time('12:00:00Z'))",
        "ND-Root",
        "12:00:00Z in (for date:$x in [2012-01-01Z, 2012-01-02Z, 2012-01-03Z] return 12:00:00Z)");
  }

  @Test
  void testTimesFromDateIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in PathNode/StartDateField/xs:date(text()) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for date:$x in BT-00-StartDate return 12:00:00Z)");
  }

  @Test
  void testTimesFromTimeIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return xs:time('12:00:00Z'))",
        "ND-Root",
        "12:00:00Z in (for time:$x in [12:00:00Z, 12:00:01Z, 12:00:02Z] return 12:00:00Z)");
  }

  @Test
  void testTimesFromTimeIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in PathNode/StartTimeField/xs:time(text()) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for time:$x in BT-00-StartTime return 12:00:00Z)");
  }

  @Test
  void testTimesFromDurationIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('12:00:00Z') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for duration:$x in [P1D, P1Y, P2M] return 12:00:00Z)");
  }


  @Test
  void testTimesFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:time('12:00:00Z') = (for $x in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for duration:$x in BT-00-Duration return 12:00:00Z)");
  }

  // Durations from iteration ---------------------------------------------------

  @Test
  void testDurationsFromStringIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')) return $x)",
        "ND-Root", "P1D in (for duration:$x in [P1D, P2D, P1W] return $x)");
  }

  @Test
  void testDurationsFromStringIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/TextField/normalize-space(text()) return xs:dayTimeDuration($x))",
        "ND-Root", "P1D in (for text:$x in BT-00-Text return day-time-duration($x))");
  }


  @Test
  void testDurationsFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (true(),false()) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for indicator:$x in [TRUE, FALSE] return P1D)");
  }

  @Test
  void testDurationsFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for indicator:$x in BT-00-Indicator return P1D)");
  }


  @Test
  void testDurationsFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (1,2,3) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for number:$x in [1, 2, 3] return P1D)");
  }

  @Test
  void testDurationsFromNumericIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/NumberField/number() return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for number:$x in BT-00-Number return P1D)");
  }

  @Test
  void testDurationsFromDateIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for date:$x in [2012-01-01Z, 2012-01-02Z, 2012-01-03Z] return P1D)");
  }

  @Test
  void testDurationsFromDateIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartDateField/xs:date(text()) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for date:$x in BT-00-StartDate return P1D)");
  }

  @Test
  void testDurationsFromTimeIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for time:$x in [12:00:00Z, 12:00:01Z, 12:00:02Z] return P1D)");
  }

  @Test
  void testDurationsFromTimeIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartTimeField/xs:time(text()) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for time:$x in BT-00-StartTime return P1D)");
  }

  @Test
  void testDurationsFromDurationIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for duration:$x in [P1D, P1Y, P2M] return P1D)");
  }

  @Test
  void testDurationsFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:dayTimeDuration('P1D') = (for $x in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for duration:$x in BT-00-Duration return P1D)");
  }

  // Strings from concatenated iterations -----------------------------------

  @Test
  void testStringsFromConcatenatedIterations_UsingLiterals() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in ('a','b','c') return ('x','y'))", "ND-Root",
        "'a' in (for text:$x in ['a', 'b', 'c'] return ['x', 'y'])");
  }

  @Test
  void testStringsFromConcatenatedIterations_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "for $x in PathNode/TextField/normalize-space(text()) return PathNode/RepeatableTextField/normalize-space(text())",
        "ND-Root",
        "for text:$x in BT-00-Text return BT-00-Repeatable-Text");
  }

  @Test
  void testBooleansFromConcatenatedIterations_UsingLiterals() {
    testExpressionTranslationWithContext(
        "true() = (for $x in (true(),false()) return (false(),true()))", "ND-Root",
        "TRUE in (for indicator:$x in [TRUE, FALSE] return [FALSE, TRUE])");
  }

  @Test
  void testNumbersFromConcatenatedIterations_UsingLiterals() {
    testExpressionTranslationWithContext("1 = (for $x in (1,2) return (3,4))", "ND-Root",
        "1 in (for number:$x in [1, 2] return [3, 4])");
  }

  @Test
  void testDatesFromConcatenatedIterations_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:date('2020-01-01Z') = (for $x in (xs:date('2020-01-01Z')) return (xs:date('2020-01-02Z'),xs:date('2020-01-03Z')))",
        "ND-Root", "2020-01-01Z in (for date:$x in [2020-01-01Z] return [2020-01-02Z, 2020-01-03Z])");
  }

  @Test
  void testTimesFromConcatenatedIterations_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:time('10:00:00Z') = (for $x in (xs:time('10:00:00Z')) return (xs:time('11:00:00Z'),xs:time('12:00:00Z')))",
        "ND-Root", "10:00:00Z in (for time:$x in [10:00:00Z] return [11:00:00Z, 12:00:00Z])");
  }

  @Test
  void testDurationsFromConcatenatedIterations_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D')) return (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')))",
        "ND-Root", "P1D in (for duration:$x in [P1D] return [P2D, P3D])");
  }

  // Return distinct (scalar) ------------------------------------------------

  @Test
  void testStringsFromIteration_ReturnDistinct() {
    testExpressionTranslationWithContext(
        "distinct-values(for $x in ('a','b','c') return concat($x, '!'))", "ND-Root",
        "for text:$x in ['a', 'b', 'c'] return distinct concat($x, '!')");
  }

  // Return distinct (concatenated iterations / flatMap) --------------------

  @Test
  void testStringsFromConcatenatedIterations_ReturnDistinct() {
    testExpressionTranslationWithContext(
        "'a' = (distinct-values(for $x in ('a','b','c') return ('x','y')))", "ND-Root",
        "'a' in (for text:$x in ['a', 'b', 'c'] return distinct ['x', 'y'])");
  }

  // #endregion: Iteration expressions

  // #region: Numeric expressions ---------------------------------------------

  @Test
  void testMultiplicationExpression() {
    testExpressionTranslationWithContext("3 * 4", "BT-00-Text", "3 * 4");
  }

  @Test
  void testAdditionExpression() {
    testExpressionTranslationWithContext("4 + 4", "BT-00-Text", "4 + 4");
  }

  @Test
  void testParenthesizedNumericExpression() {
    testExpressionTranslationWithContext("(2 + 2) * 4", "BT-00-Text", "(2 + 2)*4");
  }

  @Test
  void testMultiplicationExpression_UsingFieldReferenceOnTheRight() {
    testExpressionTranslationWithContext("3 * PathNode/NumberField/number()", "ND-Root",
        "3 * BT-00-Number");
  }

  @Test
  void testMultiplicationExpression_UsingFieldReferences() {
    testExpressionTranslationWithContext(
        "PathNode/NumberField/number() * PathNode/NumberField/number()", "ND-Root",
        "BT-00-Number * BT-00-Number");
  }

  @Test
  void testNumericLiteralExpression() {
    testExpressionTranslationWithContext("3.1415", "BT-00-Text", "3.1415");
  }

  // #endregion: Numeric expressions

  // #region: Lists -----------------------------------------------------------

  @Test
  void testStringList() {
    testExpressionTranslationWithContext("'a' = ('a','b','c')", "BT-00-Text",
        "'a' in ['a', 'b', 'c']");
  }

  @Test
  void testNumericList_UsingNumericLiterals() {
    testExpressionTranslationWithContext("4 = (1,2,3)", "BT-00-Text", "4 in [1, 2, 3]");
  }

  @Test
  void testNumericList_UsingNumericField() {
    testExpressionTranslationWithContext("4 = (1,../NumberField/number(),3)", "BT-00-Text",
        "4 in [1, BT-00-Number, 3]");
  }

  @Test
  void testNumericList_UsingTextField() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("BT-00-Text", "4 in [1, BT-00-Text, 3]"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  @Test
  void testNumericList_UsingOnlyFieldReferences() {
    testExpressionTranslationWithContext(
        "4 = (PathNode/NumberField/number(),PathNode/IntegerField/number())", "ND-Root",
        "4 in [BT-00-Number, BT-00-Integer]");
  }

  @Test
  void testBooleanList() {
    testExpressionTranslationWithContext("false() = (true(),PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')),true())",
        "ND-Root", "NEVER in [TRUE, BT-00-Indicator, ALWAYS]");
  }

  @Test
  void testDateList() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (xs:date('2022-01-02Z'),PathNode/StartDateField/xs:date(text()),xs:date('2022-02-02Z'))",
        "ND-Root", "2022-01-01Z in [2022-01-02Z, BT-00-StartDate, 2022-02-02Z]");
  }

  @Test
  void testTimeList() {
    testExpressionTranslationWithContext(
        "xs:time('12:20:21Z') = (xs:time('12:30:00Z'),PathNode/StartTimeField/xs:time(text()),xs:time('13:40:00Z'))",
        "ND-Root", "12:20:21Z in [12:30:00Z, BT-00-StartTime, 13:40:00Z]");
  }

  @Test
  void testDurationList_UsingDurationLiterals() {
    testExpressionTranslationWithContext(
        "xs:yearMonthDuration('P3M') = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        "BT-00-Text", "P3M in [P1M, P3M, P6M]");
  }



  @Test
  void testDurationList_UsingDurationField() {
    assertEquals(
        "(for $__F in ../DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        translateExpressionWithContext("BT-00-Text", "BT-00-Duration in [P1M, P3M, P6M]"));
  }

  @Test
  void testCodeList() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in [...accessibility]");
  }

  @Test
  void testCodeList_WithNumericSuffix() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in [...legal-basis-1]");
  }

  @Test
  void testCodeList_WithKeywordName() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in [...indicator]");
  }

  // #endregion: Lists

  // #region: References ------------------------------------------------------

  @Test
  void testFieldAttributeValueReference() {
    testExpressionTranslationWithContext("PathNode/TextField/@Attribute = 'text'", "ND-Root",
        "BT-00-Attribute == 'text'");
  }

  @Test
  void testFieldAttributeValueReference_SameElementContext() {
    testExpressionTranslationWithContext("./@Attribute = 'text'", "BT-00-Text",
        "BT-00-Attribute == 'text'");
  }

  @Test
  void testScalarFromAttributeReference() {
    testExpressionTranslationWithContext("PathNode/CodeField/@listName", "ND-Root",
        "BT-00-Code/@listName");
  }

  @Test
  void testScalarFromAttributeReference_SameElementContext() {
    testExpressionTranslationWithContext("./@listName", "BT-00-Code", "BT-00-Code/@listName");
  }

  @Test
  void testSequenceFromAttributeReference() {
    testExpressionTranslationWithContext("count(PathNode/CodeField/@listName)", "ND-Root",
        "count(BT-00-Code/@listName)");
  }

  @Test
  void testAttributeReference_AfterIndexer() {
    testExpressionTranslationWithContext("(PathNode/CodeField)[1]/@listName", "ND-Root",
        "BT-00-Code[at 1]/@listName");
  }

  @Test
  void testAttributeReference_WithIndexer() {
    testExpressionTranslationWithContext("(PathNode/CodeField/@listName)[1]", "ND-Root",
        "BT-00-Code/@listName[at 1]");
  }

  @Test
  void testFieldReferenceWithPredicate() {
    testExpressionTranslationWithContext("PathNode/IndicatorField['a' = 'a']/(normalize-space(text()) = ('true', '1'))", "ND-Root",
        "BT-00-Indicator['a' == 'a']");
  }

  @Test
  void testFieldReferenceWithPredicate_WithFieldReferenceInPredicate() {
    testExpressionTranslationWithContext(
        "PathNode/IndicatorField[../CodeField/normalize-space(text()) = 'a']/(normalize-space(text()) = ('true', '1'))", "ND-Root",
        "BT-00-Indicator[BT-00-Code == 'a']");
  }


  @Test
  void testAbsoluteFieldReference() {
    testExpressionTranslationWithContext("/*/PathNode/IndicatorField/(normalize-space(text()) = ('true', '1'))", "BT-00-Text",
        "/BT-00-Indicator");
  }

  @Test
  void testSimpleFieldReference() {
    testExpressionTranslationWithContext("../IndicatorField/(normalize-space(text()) = ('true', '1'))", "BT-00-Text", "BT-00-Indicator");
  }

  @Test
  void testFieldReference_ForDurationFields() {
    testExpressionTranslationWithContext(
        "(for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))",
        "ND-Root", "BT-00-Duration");
  }

  /**
   * Unlike EFX-1, where any reference to a text-multilingual field, is automatically translated to
   * an expression that returns the value of the field in the preferred language, in EFX-2 there are
   * no such implicit assumptions made. In EFX-2 a reference to a text-multilingual field behaves just 
   * like any other field reference. To get the value of the field in a specific language you either need
   * to add a predicate that selects it or use the preferred-language-text function. 
   */
  @Test
  void testMultilingualTextFieldReference() {
    testExpressionTranslationWithContext("PathNode/TextMultilingualField/normalize-space(text())",
        "ND-Root", "BT-00-Text-Multilingual");
  }

  @Test
  void testMultilingualTextFieldReference_WithLanguagePredicate() {
    testExpressionTranslationWithContext("PathNode/TextMultilingualField[./@languageID = 'eng']/normalize-space(text())",
        "ND-Root", "BT-00-Text-Multilingual[BT-00-Text-Multilingual/@languageID == 'eng']");
  }

  @Test
  void testMultilingualTextFieldReference_InScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text-Multilingual == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL, ex.getErrorCode());
  }

  @Test
  void testMultilingualTextFieldReference_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextMultilingualField/normalize-space(text())",
        "ND-Root",
        "'test' in BT-00-Text-Multilingual");
  }

  @Test
  void testMultilingualTextFieldReference_FromOwnContext_InScalarContext() {
    testExpressionTranslationWithContext(
        "string-length(./normalize-space(text())) <= 400",
        "BT-00-Text-Multilingual",
        "string-length(BT-00-Text-Multilingual) <= 400");
  }

  @Test
  void testMultilingualTextFieldReference_InOwnPredicate() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextMultilingualField[./normalize-space(text()) != '']/normalize-space(text())",
        "ND-Root",
        "'test' in BT-00-Text-Multilingual[BT-00-Text-Multilingual != '']");
  }

  @Test
  void testMultilingualTextFieldReference_AbsoluteFromOwnContext_InScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("BT-00-Text-Multilingual",
            "/BT-00-Text-Multilingual == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL, ex.getErrorCode());
  }

  /*
   * A reference that names another context, by an absolute slash or a context override, is a
   * sequence if the path to that context or the path from it to the field may select several nodes.
   */

  @Test
  void testAbsoluteFieldReference_FromInsideRepeatableNode_InScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-RepeatableNode",
            "/BT-00-Text-In-Repeatable-Node == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testAbsoluteFieldReference_FromInsideRepeatableNode_InSequenceContext() {
    testExpressionTranslationWithContext("count(/*/RepeatableNode/TextField/normalize-space(text()))",
        "ND-RepeatableNode", "count(/BT-00-Text-In-Repeatable-Node)");
  }

  @Test
  void testAbsoluteAttributeFieldReference_FromInsideRepeatableNode_InScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-RepeatableNode",
            "/BT-00-Attribute-In-Repeatable-Node == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_InScalarContext() {
    testExpressionTranslationWithContext(
        "some $c in RepeatableNode satisfies $c/TextField/normalize-space(text()) = 'test'",
        "ND-Root",
        "some context:$c in ND-RepeatableNode satisfies $c::BT-00-Text-In-Repeatable-Node == 'test'");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_InIteration() {
    testExpressionTranslationWithContext(
        "for $c in RepeatableNode return $c/TextField/normalize-space(text())",
        "ND-Root",
        "for context:$c in ND-RepeatableNode return $c::BT-00-Text-In-Repeatable-Node");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_FromAFieldContextVariable() {
    testExpressionTranslationWithContext(
        "for $c in PathNode/ChildNode/SubLevelTextField return $c/../../TextField/normalize-space(text())",
        "ND-Root", "for context:$c in BT-01-SubLevel-Text return $c::BT-00-Text");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_FromAPredicatedNode() {
    testExpressionTranslationWithContext(
        "for $c in .[PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) = true()] return $c/PathNode/IntegerField/number()",
        "ND-Root", "for context:$c in ND-Root[BT-00-Indicator == TRUE] return $c::BT-00-Integer");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_FromAnIndexedField() {
    testExpressionTranslationWithContext(
        "for $c in (PathNode/ChildNode/SubLevelTextField)[1] return $c/../../TextField/normalize-space(text())",
        "ND-Root", "for context:$c in BT-01-SubLevel-Text[at 1] return $c::BT-00-Text");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_FromANodeWithAPredicateFromTheSdk() {
    // The variable's path carries a predicate that comes from the SDK rather than from the
    // expression. It has to survive for the same reason a written one does.
    testExpressionTranslationWithContext(
        "for $c in PathNode/FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv'] return exists($c/ReasonCode)",
        "ND-Root",
        "for context:$c in ND-PrivacyForRepeatableText return $c::BT-197(BT-00)-Repeatable-Text is present");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_WithAnIndex() {
    testExpressionTranslationWithContext(
        "for $c in RepeatableNode return ($c/TextField)[1]/normalize-space(text())",
        "ND-Root",
        "for context:$c in ND-RepeatableNode return $c::BT-00-Text-In-Repeatable-Node[at 1]");
  }

  @Test
  void testFieldReferenceWithVariableContextOverride_IntoRepeatableNode_InScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "for context:$c in ND-Root return $c::BT-00-Text-In-Repeatable-Node == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testContextOverride_AfterAVariableThatHoldsNoContext_Throws() {
    InvalidIdentifierException ex = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "for text:$t in ['a'] return $t::BT-00-Text"));
    assertEquals("line 1:40 Variable '$t' is not a context variable.", ex.getMessage());
  }

  @Test
  void testContextOverride_AfterAnythingButAContextVariable_IsASyntaxError() {
    for (final String expression : List.of("ND-Root::BT-00-Text", "BT-00-Text::BT-00-Number",
        "ND-Root[BT-00-Indicator == TRUE]::BT-00-Text", "BT-01-SubLevel-Text[at 1]::BT-00-Text",
        "/ND-Root::BT-00-Text", "'a'::BT-00-Text", "(BT-00-Text)::BT-00-Number",
        "count(ND-Root::BT-00-Text)",
        "for context:$n in ND-SubNode return $n::ND-Root::BT-00-Text")) {
      assertSyntaxError(() -> translateExpressionWithContext("ND-Root", expression));
    }
  }

  @Test
  void testContextOverride_AfterAField_SuggestsAContextVariableOrAPredicate() {
    final ParseCancellationException error = assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text::BT-00-Number"));
    assertEquals("line 1:22 Syntax error: unexpected '::' in expression block; expected '}'."
        + " Only a context variable can stand before '::', as in"
        + " 'for context:$c in ND-1 return $c::BT-1'. To keep the BT-2 next to a BT-1,"
        + " write 'BT-2[BT-1 is present]'.", error.getMessage());
  }

  @Test
  void testContextOverride_AfterAnExpressionInsideAnotherConstruct_SuggestsAContextVariable() {
    for (final String expression : List.of(
        "WITH ND-Root COMPUTE for text:$t in ND-Root::BT-00-Text return $t",
        "WITH ND-Root COMPUTE every text:$t in ND-Root::BT-00-Text satisfies $t == 'a'",
        "WITH ND-Root COMPUTE BT-00-Text[ND-Root::BT-00-Text == 'a']",
        "WITH ND-Root COMPUTE upper-case(ND-Root::BT-00-Text)")) {
      final ParseCancellationException error = assertThrows(ParseCancellationException.class,
          () -> translateExpression(expression));
      assertTrue(error.getMessage().endsWith(" write 'BT-2[BT-1 is present]'."),
          error::getMessage);
    }
  }

  @Test
  void testDoubleColon_OutsideAnExpression_GetsNoContextOverrideHint() {
    for (final String expression : List.of("WITH ND-Root::BT-00-Text COMPUTE TRUE",
        "WITH ND-Root, text::$t COMPUTE TRUE")) {
      final ParseCancellationException error = assertThrows(ParseCancellationException.class,
          () -> translateExpression(expression));
      assertTrue(error.getMessage().endsWith("."), error::getMessage);
      assertTrue(!error.getMessage().contains("context variable"), error::getMessage);
    }
  }

  @Test
  void testAlias_IsASyntaxError() {
    for (final String expression : List.of("WITH ND-Root COMPUTE textField == 'a'",
        "WITH textField COMPUTE 1", "WITH Root COMPUTE 1", "{ND-Root} ${textField}",
        "WITH ND-Root COMPUTE textField:publicationDate is present",
        "WITH ND-Root COMPUTE textField:isWithheld")) {
      assertSyntaxError(() -> translateExpression(expression));
    }
  }

  @Test
  void testFieldNextToAnotherField_IsWrittenWithAPredicate() {
    testExpressionTranslationWithContext("PathNode/NumberField[exists(../TextField)]/number()",
        "ND-Root", "BT-00-Number[BT-00-Text is present]");
  }

  @Test
  void testUnknownFieldAfterContextOverride_OnTheNextLine_IsPlacedAtTheField() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateExpression(
            "WITH ND-Root COMPUTE for context:$c in ND-Root return $c::\nBT-99-Nonexistent == 'a'"));
    assertEquals("line 2:0 Unknown symbol 'BT-99-Nonexistent'.", ex.getMessage());
  }

  @Test
  void testUnknownNodeAfterContextOverride_OnTheNextLine_IsPlacedAtTheNode() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateExpression(
            "WITH ND-Root COMPUTE for context:$c in ND-Root return $c::\nND-NONEXISTENT is present"));
    assertEquals("line 2:0 Unknown symbol 'ND-NONEXISTENT'.", ex.getMessage());
  }

  @Test
  void testUnknownLinkedFieldAfterContextOverride_OnTheNextLine_IsPlacedAtTheField() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateExpression("WITH ND-Root COMPUTE for context:$c in ND-Root return $c::\n"
            + "BT-99-Nonexistent:publicationDate is present"));
    assertEquals("line 2:0 Unknown symbol 'BT-99-Nonexistent'.", ex.getMessage());
  }

  // #endregion: References

  // #region: Boolean functions -----------------------------------------------

  @Test
  void testNotFunction() {
    testExpressionTranslationWithContext("not(true())", "BT-00-Text", "not(ALWAYS)");
    testExpressionTranslationWithContext("not(1 + 1 = 2)", "BT-00-Text", "not(1 + 1 == 2)");
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("BT-00-Text", "not('text')"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  @Test
  void testBooleanFromNumberFunction() {
    testExpressionTranslationWithContext("boolean(0)", "ND-Root", "indicator(0)");
  }

  @Test
  void testBooleanFromNumberFunction_WithFieldReference() {
    testExpressionTranslationWithContext("boolean(PathNode/NumberField/number())", "ND-Root",
        "indicator(BT-00-Number)");
  }

  @Test
  void testContainsFunction() {
    testExpressionTranslationWithContext(
        "contains(PathNode/TextField/normalize-space(text()), 'xyz')", "ND-Root",
        "contains(BT-00-Text, 'xyz')");
  }

  @Test
  void testStartsWithFunction() {
    testExpressionTranslationWithContext(
        "starts-with(PathNode/TextField/normalize-space(text()), 'abc')", "ND-Root",
        "starts-with(BT-00-Text, 'abc')");
  }

  @Test
  void testEndsWithFunction() {
    testExpressionTranslationWithContext(
        "ends-with(PathNode/TextField/normalize-space(text()), 'abc')", "ND-Root",
        "ends-with(BT-00-Text, 'abc')");
  }

  // Linked field property tests

  @Test
  void testLinkedFieldProperty_publicationDate() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        "exists(" + privacyPath + "/PublicationDate)",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:publicationDate is present");
  }

  @Test
  void testLinkedFieldProperty_justificationCode() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        "exists(" + privacyPath + "/ReasonCode)",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:justificationCode is present");
  }

  @Test
  void testLinkedFieldProperty_justificationDescription() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        "exists(" + privacyPath + "/ReasonDescription)",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:justificationDescription is present");
  }

  @Test
  void testLinkedFieldProperty_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:publicationDate is present"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  @Test
  void testLinkedFieldProperty_justificationCode_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:justificationCode is present"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  @Test
  void testLinkedFieldProperty_justificationDescription_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:justificationDescription is present"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  @Test
  void testLinkedFieldProperty_inComparison() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/PublicationDate/xs:date(text()) > xs:date('2025-01-01Z')",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:publicationDate > 2025-01-01Z");
  }

  // Computed property tests

  @Test
  void testComputedProperty_wasWithheld() {
    testExpressionTranslationWithContext(
        "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']/FieldIdentifierCode/normalize-space(text()) = 'test-priv'",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:wasWithheld");
  }

  @Test
  void testComputedProperty_wasWithheld_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:wasWithheld"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
    assertTrue(exception.getMessage().startsWith("line "), "Error message should include source position");
  }

  @Test
  void testComputedProperty_isWithheld() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "(not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date())",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:isWithheld");
  }

  @Test
  void testComputedProperty_isWithheld_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:isWithheld"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  @Test
  void testComputedProperty_isWithholdable_true() {
    testExpressionTranslationWithContext(
        "true()",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:isWithholdable");
  }

  @Test
  void testComputedProperty_isWithholdable_false() {
    testExpressionTranslationWithContext(
        "false()", "BT-00-Text",
        "BT-00-Text:isWithholdable");
  }

  @Test
  void testComputedProperty_isDisclosed() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "not("
                + "(not(exists(" + privacyPath + "/PublicationDate))"
                + " or "
                + privacyPath + "/PublicationDate/xs:date(text()) > current-date())"
            + ")"
            + " and "
            + "not(./normalize-space(text()) = 'unpublished')",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:isDisclosed");
  }

  @Test
  void testComputedProperty_isDisclosed_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:isDisclosed"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  @Test
  void testComputedProperty_isMasked() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "./normalize-space(text()) = 'unpublished'",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:isMasked");
  }

  @Test
  void testComputedProperty_isMasked_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:isMasked"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  @Test
  void testComputedProperty_isMasked_numericField() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='num-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'num-priv'"
            + " and "
            + "./number() = -1",
        "BT-00-Number-In-Repeatable-Node",
        "BT-00-Number-In-Repeatable-Node:isMasked");
  }

  @Test
  void testComputedProperty_isMasked_repeatingFieldFromContext() {
    final String privacyPath = "RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "RepeatableNode/TextField/normalize-space(text()) = 'unpublished'",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:isMasked");
  }

  @Test
  void testComputedProperty_wasWithheld_repeatableFieldFromOwnContext() {
    testExpressionTranslationWithContext(
        "../FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:wasWithheld");
  }

  @Test
  void testComputedProperty_isWithheld_repeatableFieldFromOwnContext() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'"
            + " and "
            + "(not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date())",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:isWithheld");
  }

  @Test
  void testComputedProperty_isDisclosed_repeatableFieldFromOwnContext() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'"
            + " and "
            + "not("
                + "(not(exists(" + privacyPath + "/PublicationDate))"
                + " or "
                + privacyPath + "/PublicationDate/xs:date(text()) > current-date())"
            + ")"
            + " and "
            + "not(./normalize-space(text()) = 'unpublished')",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:isDisclosed");
  }

  @Test
  void testComputedProperty_isMasked_repeatableFieldFromOwnContext() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'"
            + " and "
            + "./normalize-space(text()) = 'unpublished'",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:isMasked");
  }

  @Test
  void testComputedProperty_isDisclosed_numericField() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='num-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'num-priv'"
            + " and "
            + "not("
                + "(not(exists(" + privacyPath + "/PublicationDate))"
                + " or "
                + privacyPath + "/PublicationDate/xs:date(text()) > current-date())"
            + ")"
            + " and "
            + "not(./number() = -1)",
        "BT-00-Number-In-Repeatable-Node",
        "BT-00-Number-In-Repeatable-Node:isDisclosed");
  }

  @Test
  void testComputedProperty_isMasked_dateField() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "./xs:date(text()) = xs:date('1970-01-01Z')",
        "BT-00-Date-In-Repeatable-Node",
        "BT-00-Date-In-Repeatable-Node:isMasked");
  }

  @Test
  void testComputedProperty_isDisclosed_dateField() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "not("
                + "(not(exists(" + privacyPath + "/PublicationDate))"
                + " or "
                + privacyPath + "/PublicationDate/xs:date(text()) > current-date())"
            + ")"
            + " and "
            + "not(./xs:date(text()) = xs:date('1970-01-01Z'))",
        "BT-00-Date-In-Repeatable-Node",
        "BT-00-Date-In-Repeatable-Node:isDisclosed");
  }

  // Metadata property tests

  @Test
  void testMetadataProperty_privacyCode() {
    testExpressionTranslationWithContext(
        "'test-priv'",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:privacyCode");
  }

  @Test
  void testMetadataProperty_privacyCode_onNonWithholdableField() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:privacyCode"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, exception.getErrorCode());
  }

  // rawValue property tests

  @Test
  void testFieldRawValue_Duration() {
    testExpressionTranslationWithContext(
        "PathNode/DurationField/text()",
        "ND-Root",
        "BT-00-Duration:rawValue");
  }

  @Test
  void testFieldRawValue_Text() {
    testExpressionTranslationWithContext(
        "PathNode/TextField/text()",
        "ND-Root",
        "BT-00-Text:rawValue");
  }

  @Test
  void testFieldRawValue_Number() {
    testExpressionTranslationWithContext(
        "PathNode/NumberField/text()",
        "ND-Root",
        "BT-00-Number:rawValue");
  }

  @Test
  void testFieldRawValue_RepeatableFieldFromOwnContext() {
    testExpressionTranslationWithContext(
        "./text()",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:rawValue");
  }

  @Test
  void testFieldRawValue_ScalarFieldInSequenceContext() {
    // rawValue of a non-repeatable field used in a sequence context
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextField/text()",
        "ND-Root",
        "'test' in BT-00-Text:rawValue");
  }

  @Test
  void testFieldRawValue_RepeatableFieldInSequenceContext() {
    // rawValue of a repeatable field used in a sequence context (the 'in' operator expects a sequence)
    testExpressionTranslationWithContext(
        "'test' = PathNode/RepeatableTextField/text()",
        "ND-Root",
        "'test' in BT-00-Repeatable-Text:rawValue");
  }

  @Test
  void testFieldRawValue_WithPredicateInScalarContext() {
    testExpressionTranslationWithContext(
        "PathNode/TextField['a' = 'a']/text()",
        "ND-Root",
        "BT-00-Text['a' == 'a']:rawValue");
  }

  @Test
  void testFieldRawValue_WithPredicateInSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextField['a' = 'a']/text()",
        "ND-Root",
        "'test' in BT-00-Text['a' == 'a']:rawValue");
  }

  @Test
  void testFieldRawValue_RepeatableWithPredicateInSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/RepeatableTextField['a' = 'a']/text()",
        "ND-Root",
        "'test' in BT-00-Repeatable-Text['a' == 'a']:rawValue");
  }

  @Test
  void testFieldRawValue_RepeatableWithPredicateInScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Repeatable-Text['a' == 'a']:rawValue == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testFieldRawValue_RepeatableFieldInScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Repeatable-Text:rawValue == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testFieldRawValue_MultilingualFieldInScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text-Multilingual:rawValue == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL, ex.getErrorCode());
  }

  @Test
  void testFieldRawValue_MultilingualFieldInSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextMultilingualField/text()",
        "ND-Root",
        "'test' in BT-00-Text-Multilingual:rawValue");
  }

  // #endregion: Boolean functions

  // #region: Numeric functions -----------------------------------------------

  @Test
  void testCountFunction_UsingFieldReference() {
    testExpressionTranslationWithContext("count(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "count(BT-00-Text)");
  }

  @Test
  void testCountFunction_UsingRepeatableNodeReference() {
    testExpressionTranslationWithContext("count(RepeatableNode)", "ND-Root",
        "count(ND-RepeatableNode)");
  }

  @Test
  void testCountFunction_UsingNodeReferenceWithPredicate() {
    testExpressionTranslationWithContext(
        "count(RepeatableNode[TextField/normalize-space(text()) = 'a'])", "ND-Root",
        "count(ND-RepeatableNode[BT-00-Text-In-Repeatable-Node == 'a'])");
  }

  @Test
  void testCountFunction_UsingNonRepeatableNodeReference() {
    testExpressionTranslationWithContext("count(SubNode)", "ND-Root", "count(ND-SubNode)");
  }

  @Test
  void testCountFunction_UsingSequenceFromIteration() {
    testExpressionTranslationWithContext(
        "count(for $x in PathNode/TextField/normalize-space(text()) return concat($x, '-xyz'))", "ND-Root",
        "count(for text:$x in BT-00-Text return concat($x, '-xyz'))");
  }

  @Test
  void testCountFunction_UsingBooleanList() {
    testExpressionTranslationWithContext("count((true(),false()))", "ND-Root",
        "count([TRUE, FALSE])");
  }

  @Test
  void testCountFunction_UsingNumericList() {
    testExpressionTranslationWithContext("count((1,2,3))", "ND-Root", "count([1, 2, 3])");
  }

  @Test
  void testCountFunction_UsingDateList() {
    testExpressionTranslationWithContext(
        "count((xs:date('2020-01-01Z'),xs:date('2020-01-02Z')))", "ND-Root",
        "count([2020-01-01Z, 2020-01-02Z])");
  }

  @Test
  void testCountFunction_UsingTimeList() {
    testExpressionTranslationWithContext(
        "count((xs:time('10:00:00Z'),xs:time('11:00:00Z')))", "ND-Root",
        "count([10:00:00Z, 11:00:00Z])");
  }

  @Test
  void testCountFunction_UsingDurationList() {
    testExpressionTranslationWithContext(
        "count((xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P2M')))", "ND-Root",
        "count([P1D, P2M])");
  }

  @Test
  void testNumberFunction() {
    testExpressionTranslationWithContext("number(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "number(BT-00-Text)");
  }

  @Test
  void testNumberFromStringFunction() {
    testExpressionTranslationWithContext("number('12.5')", "ND-Root", "number('12.5')");
  }

  @Test
  void testNumberFromBooleanFunction() {
    testExpressionTranslationWithContext("number(true())", "ND-Root", "number(TRUE)");
  }

  @Test
  void testNumberFromBooleanFunction_WithFieldReference() {
    testExpressionTranslationWithContext("number(PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')))", "ND-Root",
        "number(BT-00-Indicator)");
  }

  @Test
  void testNumberFromDurationField_ExplicitRawValue() {
    testExpressionTranslationWithContext(
        "number(PathNode/DurationField/text())",
        "ND-Root",
        "number(BT-00-Duration:rawValue)");
  }

  @Test
  void testFieldRawValue_NumberLikePattern() {
    testExpressionTranslationWithContext(
        "fn:matches(normalize-space(PathNode/NumberField/text()), '[0-9]+\\.[0-9]+')",
        "ND-Root",
        "BT-00-Number:rawValue like '[0-9]+\\.[0-9]+'");
  }

  @Test
  void testNumberFromDurationRawValue_Comparison() {
    testExpressionTranslationWithContext(
        "number(PathNode/DurationField/text()) > 3",
        "ND-Root",
        "number(BT-00-Duration:rawValue) > 3");
  }

  @Test
  void testSumFunction_UsingFieldReference() {
    testExpressionTranslationWithContext("sum(PathNode/NumberField/number())", "ND-Root",
        "sum(BT-00-Number)");
  }

  @Test
  void testSumFunction_UsingNumericSequenceFromIteration() {
    testExpressionTranslationWithContext("sum(for $v in PathNode/NumberField/number() return $v + 1)",
        "ND-Root", "sum(for number:$v in BT-00-Number return $v +1)");
  }

  @Test
  void testMinFunction_UsingFieldReference() {
    testExpressionTranslationWithContext("min(PathNode/NumberField/number())", "ND-Root",
        "min(BT-00-Number)");
  }

  @Test
  void testMaxFunction_UsingFieldReference() {
    testExpressionTranslationWithContext("max(PathNode/NumberField/number())", "ND-Root",
        "max(BT-00-Number)");
  }

  @Test
  void testAverageFunction_UsingFieldReference() {
    testExpressionTranslationWithContext("avg(PathNode/NumberField/number())", "ND-Root",
        "average(BT-00-Number)");
  }

  @Test
  void testAverageFunction_UsingNumericSequenceFromIteration() {
    testExpressionTranslationWithContext("avg(for $v in PathNode/NumberField/number() return $v + 1)",
        "ND-Root", "average(for number:$v in BT-00-Number return $v +1)");
  }

  @Test
  void testStringLengthFunction() {
    testExpressionTranslationWithContext(
        "string-length(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "string-length(BT-00-Text)");
  }

  @Test
  void testStringLengthFunction_MultilingualFieldInItsOwnContext() {
    testExpressionTranslationWithContext("string-length(./normalize-space(text()))",
        "BT-00-Text-Multilingual",
        "string-length(BT-00-Text-Multilingual)");
  }

  @Test
  void testYearFromDateFunction() {
    testExpressionTranslationWithContext("year-from-date(xs:date('2024-03-15Z'))", "ND-Root",
        "year(date('2024-03-15Z'))");
  }

  @Test
  void testYearFromDateFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "year-from-date(PathNode/StartDateField/xs:date(text()))", "ND-Root",
        "year(BT-00-StartDate)");
  }

  @Test
  void testMonthFromDateFunction() {
    testExpressionTranslationWithContext("month-from-date(xs:date('2024-03-15Z'))", "ND-Root",
        "month(date('2024-03-15Z'))");
  }

  @Test
  void testMonthFromDateFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "month-from-date(PathNode/StartDateField/xs:date(text()))", "ND-Root",
        "month(BT-00-StartDate)");
  }

  @Test
  void testDayFromDateFunction() {
    testExpressionTranslationWithContext("day-from-date(xs:date('2024-03-15Z'))", "ND-Root",
        "day(date('2024-03-15Z'))");
  }

  @Test
  void testDayFromDateFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "day-from-date(PathNode/StartDateField/xs:date(text()))", "ND-Root",
        "day(BT-00-StartDate)");
  }

  @Test
  void testHoursFromTimeFunction() {
    testExpressionTranslationWithContext("hours-from-time(xs:time('14:30:00Z'))", "ND-Root",
        "hours(time('14:30:00Z'))");
  }

  @Test
  void testHoursFromTimeFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "hours-from-time(PathNode/StartTimeField/xs:time(text()))", "ND-Root",
        "hours(BT-00-StartTime)");
  }

  @Test
  void testMinutesFromTimeFunction() {
    testExpressionTranslationWithContext("minutes-from-time(xs:time('14:30:00Z'))", "ND-Root",
        "minutes(time('14:30:00Z'))");
  }

  @Test
  void testMinutesFromTimeFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "minutes-from-time(PathNode/StartTimeField/xs:time(text()))", "ND-Root",
        "minutes(BT-00-StartTime)");
  }

  @Test
  void testSecondsFromTimeFunction() {
    testExpressionTranslationWithContext("seconds-from-time(xs:time('14:30:45Z'))", "ND-Root",
        "seconds(time('14:30:45Z'))");
  }

  @Test
  void testSecondsFromTimeFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "seconds-from-time(PathNode/StartTimeField/xs:time(text()))", "ND-Root",
        "seconds(BT-00-StartTime)");
  }

  @Test
  void testYearsFromDurationFunction() {
    testExpressionTranslationWithContext(
        "years-from-duration(xs:yearMonthDuration('P2Y'))", "ND-Root",
        "years(P2Y)");
  }

  @Test
  void testYearsFromDurationFunction_WithDayTimeDuration() {
    testExpressionTranslationWithContext(
        "years-from-duration(xs:dayTimeDuration('P10D'))", "ND-Root",
        "years(P10D)");
  }

  @Test
  void testYearsFromDurationFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "years-from-duration((for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "years(BT-00-Duration)");
  }

  @Test
  void testMonthsFromDurationFunction() {
    testExpressionTranslationWithContext(
        "(years-from-duration(xs:yearMonthDuration('P3M')) * 12 + months-from-duration(xs:yearMonthDuration('P3M')))",
        "ND-Root", "months(P3M)");
  }

  @Test
  void testMonthsFromDurationFunction_WithYears() {
    testExpressionTranslationWithContext(
        "(years-from-duration(xs:yearMonthDuration('P2Y')) * 12 + months-from-duration(xs:yearMonthDuration('P2Y')))",
        "ND-Root", "months(P2Y)");
  }

  @Test
  void testMonthsFromDurationFunction_TotalMonths() {
    testExpressionTranslationWithContext(
        "(years-from-duration(xs:yearMonthDuration('P12M')) * 12 + months-from-duration(xs:yearMonthDuration('P12M')))",
        "ND-Root", "months(P12M)");
  }

  @Test
  void testDaysFromDurationFunction() {
    testExpressionTranslationWithContext(
        "days-from-duration(xs:dayTimeDuration('P10D'))", "ND-Root",
        "days(P10D)");
  }

  @Test
  void testDaysFromDurationFunction_WithWeeks() {
    testExpressionTranslationWithContext(
        "days-from-duration(xs:dayTimeDuration('P21D'))", "ND-Root",
        "days(P3W)");
  }

  @Test
  void testAbsoluteFunction() {
    testExpressionTranslationWithContext("abs(-5)", "ND-Root", "absolute(-5)");
  }

  @Test
  void testAbsoluteFunction_WithFieldReference() {
    testExpressionTranslationWithContext("abs(PathNode/NumberField/number())", "ND-Root",
        "absolute(BT-00-Number)");
  }

  @Test
  void testRoundFunction() {
    testExpressionTranslationWithContext("round(3.7)", "ND-Root", "round(3.7)");
  }


  @Test
  void testRoundFunction_WithFieldReference() {
    testExpressionTranslationWithContext("round(PathNode/NumberField/number())", "ND-Root",
        "round(BT-00-Number)");
  }

  @Test
  void testRoundDownFunction() {
    testExpressionTranslationWithContext("floor(3.7)", "ND-Root", "round-down(3.7)");
  }

  @Test
  void testRoundDownFunction_WithFieldReference() {
    testExpressionTranslationWithContext("floor(PathNode/NumberField/number())", "ND-Root",
        "round-down(BT-00-Number)");
  }

  @Test
  void testRoundUpFunction() {
    testExpressionTranslationWithContext("ceiling(3.2)", "ND-Root", "round-up(3.2)");
  }

  @Test
  void testRoundUpFunction_WithFieldReference() {
    testExpressionTranslationWithContext("ceiling(PathNode/NumberField/number())", "ND-Root",
        "round-up(BT-00-Number)");
  }

  // #endregion: Numeric functions

  // #region: String functions ------------------------------------------------

  @Test
  void testSubstringFunction() {
    testExpressionTranslationWithContext(
        "substring(PathNode/TextField/normalize-space(text()), 1, 3)", "ND-Root",
        "substring(BT-00-Text, 1, 3)");
    testExpressionTranslationWithContext("substring(PathNode/TextField/normalize-space(text()), 4)",
        "ND-Root", "substring(BT-00-Text, 4)");
  }

  @Test
  void testUpperCaseFunction() {
    testExpressionTranslation(
        "upper-case(PathNode/TextField/normalize-space(text()))", 
        "{ND-Root} ${upper-case(BT-00-Text)}");
  }

  @Test
  void testLowerCaseFunction() {
    testExpressionTranslation(
        "lower-case(PathNode/TextField/normalize-space(text()))",
        "{ND-Root} ${lower-case(BT-00-Text)}");
  }

  @Test
  void testNormalizeSpaceFunction() {
    testExpressionTranslation(
        "normalize-space(PathNode/TextField/normalize-space(text()))",
        "{ND-Root} ${normalize-space(BT-00-Text)}");
  }

  @Test
  void testNormalizeSpaceFunction_WithLiteral() {
    testExpressionTranslationWithContext("normalize-space('  hello   world  ')", "ND-Root",
        "normalize-space('  hello   world  ')");
  }

  @Test
  void testTrimFunction() {
    testExpressionTranslation(
        "replace(replace(PathNode/TextField/normalize-space(text()), '^\\s+', ''), '\\s+$', '')",
        "{ND-Root} ${trim(BT-00-Text)}");
  }

  @Test
  void testTrimFunction_WithLiteral() {
    testExpressionTranslationWithContext("replace(replace('  hello  ', '^\\s+', ''), '\\s+$', '')",
        "ND-Root", "trim('  hello  ')");
  }

  @Test
  void testTrimLeftFunction() {
    testExpressionTranslation(
        "replace(PathNode/TextField/normalize-space(text()), '^\\s+', '')",
        "{ND-Root} ${trim-left(BT-00-Text)}");
  }

  @Test
  void testTrimLeftFunction_WithLiteral() {
    testExpressionTranslationWithContext("replace('  hello  ', '^\\s+', '')", "ND-Root",
        "trim-left('  hello  ')");
  }

  @Test
  void testTrimRightFunction() {
    testExpressionTranslation(
        "replace(PathNode/TextField/normalize-space(text()), '\\s+$', '')",
        "{ND-Root} ${trim-right(BT-00-Text)}");
  }

  @Test
  void testTrimRightFunction_WithLiteral() {
    testExpressionTranslationWithContext("replace('  hello  ', '\\s+$', '')", "ND-Root",
        "trim-right('  hello  ')");
  }

  @Test
  void testTrimFunction_WithRepeatableField_Throws() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "trim(BT-00-Repeatable-Text)"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, exception.getErrorCode());
  }

  @Test
  void testPadLeftFunction() {
    testExpressionTranslationWithContext(
        "(for $__s in '42' return concat(substring(string-join(for $__i in 1 to 5 return '0', ''), 1, 5 - string-length($__s)), $__s))",
        "ND-Root", "pad-left('42', 5, '0')");
  }

  @Test
  void testPadLeftFunction_WithFieldReference() {
    testExpressionTranslation(
        "(for $__s in PathNode/TextField/normalize-space(text()) return concat(substring(string-join(for $__i in 1 to 10 return ' ', ''), 1, 10 - string-length($__s)), $__s))",
        "{ND-Root} ${pad-left(BT-00-Text, 10, ' ')}");
  }

  @Test
  void testPadRightFunction() {
    testExpressionTranslationWithContext(
        "(for $__s in '42' return concat($__s, substring(string-join(for $__i in 1 to 5 return '0', ''), 1, 5 - string-length($__s))))",
        "ND-Root", "pad-right('42', 5, '0')");
  }

  @Test
  void testPadRightFunction_WithFieldReference() {
    testExpressionTranslation(
        "(for $__s in PathNode/TextField/normalize-space(text()) return concat($__s, substring(string-join(for $__i in 1 to 10 return ' ', ''), 1, 10 - string-length($__s))))",
        "{ND-Root} ${pad-right(BT-00-Text, 10, ' ')}");
  }

  @Test
  void testRepeatFunction() {
    testExpressionTranslationWithContext(
        "(for $__s in 'abc' return string-join(for $__i in 1 to 3 return $__s, ''))",
        "ND-Root", "repeat('abc', 3)");
  }

  @Test
  void testRepeatFunction_WithFieldReference() {
    testExpressionTranslation(
        "(for $__s in PathNode/TextField/normalize-space(text()) return string-join(for $__i in 1 to 4 return $__s, ''))",
        "{ND-Root} ${repeat(BT-00-Text, 4)}");
  }

  @Test
  void testReplaceFunction() {
    testExpressionTranslationWithContext(
        "(for $__s in 'world', $__t in 'hello world' return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('there', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "ND-Root", "replace('hello world', 'world', 'there')");
  }

  @Test
  void testReplaceFunction_WithFieldReference() {
    testExpressionTranslation(
        "(for $__s in '-', $__t in PathNode/TextField/normalize-space(text()) return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('_', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "{ND-Root} ${replace(BT-00-Text, '-', '_')}");
  }

  @Test
  void testReplaceFunction_WithRegexMetacharSearch() {
    testExpressionTranslationWithContext(
        "(for $__s in '.', $__t in 'a.b.c' return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('-', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "ND-Root", "replace('a.b.c', '.', '-')");
  }

  @Test
  void testReplaceFunction_WithDollarInSearch() {
    testExpressionTranslationWithContext(
        "(for $__s in '$', $__t in 'a$b' return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('X', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "ND-Root", "replace('a$b', '$', 'X')");
  }

  @Test
  void testReplaceFunction_WithDollarInReplacement() {
    testExpressionTranslationWithContext(
        "(for $__s in 'b', $__t in 'ab' return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('$1', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "ND-Root", "replace('ab', 'b', '$1')");
  }

  @Test
  void testReplaceFunction_WithBackslashInReplacement() {
    testExpressionTranslationWithContext(
        "(for $__s in 'b', $__t in 'ab' return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('\\\\', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "ND-Root", "replace('ab', 'b', '\\\\')");
  }

  @Test
  void testReplaceFunction_WithEmptySearch() {
    testExpressionTranslationWithContext(
        "(for $__s in '', $__t in 'text' return if ($__s = '') then $__t else replace($__t, replace($__s, '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'), replace(replace('x', '\\\\', '\\\\\\\\'), '\\$', '\\\\\\$')))",
        "ND-Root", "replace('text', '', 'x')");
  }

  @Test
  void testReplaceRegexFunction() {
    testExpressionTranslationWithContext(
        "replace('hello 123 world', '[0-9]+', 'NUM')",
        "ND-Root", "replace-regex('hello 123 world', '[0-9]+', 'NUM')");
  }

  @Test
  void testReplaceRegexFunction_WithFieldReference() {
    testExpressionTranslation(
        "replace(PathNode/TextField/normalize-space(text()), '[ \\t]+', ' ')",
        "{ND-Root} ${replace-regex(BT-00-Text, '[ \\t]+', ' ')}");
  }

  @Test
  void testReplaceRegexFunction_WithShorthandPattern_ThrowsError() {
    assertThrows(InvalidUsageException.class, () ->
        testExpressionTranslationWithContext(
            "", "ND-Root", "replace-regex('hello', '\\w+', 'x')"));
  }

  @Test
  void testReplaceRegexFunction_WithDynamicPattern_DoesNotThrow() {
    // Pattern is a field reference (non-literal) — static regex validation is skipped
    testExpressionTranslation(
        "replace('hello', PathNode/TextField/normalize-space(text()), 'x')",
        "{ND-Root} ${replace-regex('hello', BT-00-Text, 'x')}");
  }

  @Test
  void testUrlEncodeFunction() {
    testExpressionTranslationWithContext(
        "encode-for-uri('hello world')",
        "ND-Root", "url-encode('hello world')");
  }

  @Test
  void testUrlEncodeFunction_WithFieldReference() {
    testExpressionTranslation(
        "encode-for-uri(PathNode/TextField/normalize-space(text()))",
        "{ND-Root} ${url-encode(BT-00-Text)}");
  }

  @Test
  void testUrlEncodeFunction_WithRepeatableField_Throws() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpression("{ND-Root} ${url-encode(ND-Root)}"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  @Test
  void testCapitalizeFirstFunction() {
    testExpressionTranslationWithContext(
        "(for $__s in 'hello' return concat(upper-case(substring($__s, 1, 1)), substring($__s, 2)))",
        "ND-Root", "capitalize-first('hello')");
  }

  @Test
  void testCapitalizeFirstFunction_WithFieldReference() {
    testExpressionTranslation(
        "(for $__s in PathNode/TextField/normalize-space(text()) return concat(upper-case(substring($__s, 1, 1)), substring($__s, 2)))",
        "{ND-Root} ${capitalize-first(BT-00-Text)}");
  }

  @Test
  void testCapitalizeFirstFunction_WithRepeatableField_Throws() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpression("{ND-Root} ${capitalize-first(ND-Root)}"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  @Test
  void testSplitFunction() {
    testExpressionTranslationWithContext(
        "tokenize('a,b,c', replace(',', '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'))",
        "ND-Root", "split('a,b,c', ',')");
  }

  @Test
  void testSplitFunction_WithFieldReference() {
    testExpressionTranslation(
        "tokenize(PathNode/TextField/normalize-space(text()), replace(';', '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'))",
        "{ND-Root} ${split(BT-00-Text, ';')}");
  }

  @Test
  void testSplitFunction_WithRegexMetacharDelimiter() {
    testExpressionTranslationWithContext(
        "tokenize('a.b.c', replace('.', '([.\\\\?*+{}\\[\\]()^$|])', '\\\\$1'))",
        "ND-Root", "split('a.b.c', '.')");
  }

  @Test
  void testSubstringBeforeFunction() {
    testExpressionTranslationWithContext("substring-before('hello-world', '-')", "ND-Root",
        "substring-before('hello-world', '-')");
  }

  @Test
  void testSubstringBeforeFunction_WithFieldReference() {
    testExpressionTranslation(
        "substring-before(PathNode/TextField/normalize-space(text()), '-')",
        "{ND-Root} ${substring-before(BT-00-Text, '-')}");
  }

  @Test
  void testSubstringAfterFunction() {
    testExpressionTranslationWithContext("substring-after('hello-world', '-')", "ND-Root",
        "substring-after('hello-world', '-')");
  }

  @Test
  void testSubstringAfterFunction_WithFieldReference() {
    testExpressionTranslation(
        "substring-after(PathNode/TextField/normalize-space(text()), '-')",
        "{ND-Root} ${substring-after(BT-00-Text, '-')}");
  }

  @Test
  void testIndexOfSubstringFunction() {
    testExpressionTranslationWithContext(
        "(for $__s in 'hello world', $__sub in 'world' return if (contains($__s, $__sub)) then string-length(substring-before($__s, $__sub)) + 1 else 0)",
        "ND-Root", "index-of-substring('hello world', 'world')");
  }

  @Test
  void testIndexOfSubstringFunction_WithFieldReference() {
    testExpressionTranslation(
        "(for $__s in PathNode/TextField/normalize-space(text()), $__sub in '-' return if (contains($__s, $__sub)) then string-length(substring-before($__s, $__sub)) + 1 else 0)",
        "{ND-Root} ${index-of-substring(BT-00-Text, '-')}");
  }

  @Test
  void testNumberToStringFunction() {
    testExpressionTranslationWithContext("string(123)", "ND-Root", "string(123)");
  }

  @Test
  void testNumberToStringFunction_WithFieldReference() {
    testExpressionTranslationWithContext("string(PathNode/NumberField/number())", "ND-Root",
        "string(BT-00-Number)");
  }

  @Test
  void testBooleanToStringFunction() {
    testExpressionTranslationWithContext("string(true())", "ND-Root", "string(TRUE)");
  }

  @Test
  void testBooleanToStringFunction_WithFieldReference() {
    testExpressionTranslationWithContext("string(PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')))", "ND-Root",
        "string(BT-00-Indicator)");
  }

  @Test
  void testDateToStringFunction() {
    testExpressionTranslationWithContext("string(xs:date('2024-01-15Z'))", "ND-Root",
        "string(2024-01-15Z)");
  }

  @Test
  void testDateToStringFunction_WithFieldReference() {
    testExpressionTranslationWithContext("string(PathNode/StartDateField/xs:date(text()))", "ND-Root",
        "string(BT-00-StartDate)");
  }

  @Test
  void testTimeToStringFunction() {
    testExpressionTranslationWithContext("string(xs:time('14:30:00Z'))", "ND-Root",
        "string(14:30:00Z)");
  }

  @Test
  void testTimeToStringFunction_WithFieldReference() {
    testExpressionTranslationWithContext("string(PathNode/StartTimeField/xs:time(text()))", "ND-Root",
        "string(BT-00-StartTime)");
  }

  @Test
  void testDurationToStringFunction() {
    testExpressionTranslationWithContext("string(xs:dayTimeDuration('P30D'))", "ND-Root",
        "string(P30D)");
  }

  @Test
  void testDurationToStringFunction_WithFieldReference() {
    testExpressionTranslationWithContext(
        "string((for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))",
        "ND-Root", "string(BT-00-Duration)");
  }

  // text() variants - verify that 'text' keyword works as alias for 'string' conversion
  @Test
  void testTextFromNumberFunction() {
    testExpressionTranslationWithContext("string(123)", "ND-Root", "text(123)");
  }

  @Test
  void testTextFromBooleanFunction() {
    testExpressionTranslationWithContext("string(true())", "ND-Root", "text(TRUE)");
  }

  @Test
  void testTextFromDateFunction() {
    testExpressionTranslationWithContext("string(xs:date('2024-01-15Z'))", "ND-Root",
        "text(2024-01-15Z)");
  }

  @Test
  void testTextFromTimeFunction() {
    testExpressionTranslationWithContext("string(xs:time('14:30:00Z'))", "ND-Root",
        "text(14:30:00Z)");
  }

  @Test
  void testTextFromDurationFunction() {
    testExpressionTranslationWithContext("string(xs:dayTimeDuration('P30D'))", "ND-Root",
        "text(P30D)");
  }

  @Test
  void testConcatFunction() {
    testExpressionTranslationWithContext("concat('abc', 'def')", "ND-Root", "concat('abc', 'def')");
  };

  @Test
  void testStringJoinFunction_withLiterals() {
    testExpressionTranslationWithContext("string-join(('abc','def'), ',')", "ND-Root",
        "string-join(['abc', 'def'], ',')");
  }

  @Test
  void testStringJoinFunction_withFieldReference() {
    testExpressionTranslationWithContext("string-join(PathNode/TextField/normalize-space(text()), ',')", "ND-Root",
        "string-join(BT-00-Text, ',')");
  }

  @Test
  void testFormatNumber_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-number(BT-00-Number, '#,##0.00')"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatShort_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-short(date('2026-02-15'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatMedium_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-medium(date('2026-02-15'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatLong_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-long(date('2026-02-15'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatShort_WithTime_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-short(time('14:30:00Z'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatShort_WithDateAndTime_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root",
            "format-short(date('2026-02-15'), time('14:30:00Z'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatShort_WithFieldReference_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-short(BT-00-StartDate)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatMedium_WithTime_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-medium(time('14:30:00Z'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatMedium_WithDateAndTime_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root",
            "format-medium(date('2026-02-15'), time('14:30:00Z'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatMedium_WithFieldReference_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-medium(BT-00-StartDate)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatLong_WithTime_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-long(time('14:30:00Z'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatLong_WithDateAndTime_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root",
            "format-long(date('2026-02-15'), time('14:30:00Z'))"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFormatLong_WithFieldReference_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "format-long(BT-00-StartDate)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  // #endregion: String functions

  // #region: Date functions --------------------------------------------------

  @Test
  void testDateFromStringFunction() {
    testExpressionTranslationWithContext("xs:date(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "date(BT-00-Text)");
  }

  @Test
  void testCurrentDateFunction() {
    testExpressionTranslationWithContext("current-date()", "ND-Root", "current-date()");
  }

  @Test
  void testCurrentDateComparedToField() {
    testExpressionTranslationWithContext(
        "current-date() > PathNode/StartDateField/xs:date(text())",
        "ND-Root", "current-date() > BT-00-StartDate");
  }

  @Test
  void testCurrentDatePlusDuration() {
    testExpressionTranslationWithContext(
        "(current-date() + xs:dayTimeDuration('P30D'))",
        "ND-Root", "add-duration(current-date(), P30D)");
  }

  @Test
  void testCurrentDateMinusDuration() {
    testExpressionTranslationWithContext("(current-date() - xs:dayTimeDuration('P30D'))", "ND-Root",
        "subtract-duration(current-date(), P30D)");
  }

  @Test
  void testYearFromCurrentDate() {
    testExpressionTranslationWithContext("year-from-date(current-date())",
        "ND-Root", "year(current-date())");
  }

  @Test
  void testMonthFromCurrentDate() {
    testExpressionTranslationWithContext("month-from-date(current-date())",
        "ND-Root", "month(current-date())");
  }

  // #endregion: Date functions

  // #region: Time functions --------------------------------------------------

  @Test
  void testTimeFromStringFunction() {
    testExpressionTranslationWithContext("xs:time(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "time(BT-00-Text)");
  }

  @Test
  void testCurrentTimeFunction() {
    testExpressionTranslationWithContext("current-time()", "ND-Root", "current-time()");
  }

  @Test
  void testCurrentTimeComparedToField() {
    testExpressionTranslationWithContext(
        "current-time() > PathNode/StartTimeField/xs:time(text())",
        "ND-Root", "current-time() > BT-00-StartTime");
  }

  @Test
  void testHoursFromCurrentTime() {
    testExpressionTranslationWithContext("hours-from-time(current-time())",
        "ND-Root", "hours(current-time())");
  }

  // #endregion: Time functions

  // #region Duration functions

  @Test
  void testDayTimeDurationFromStringFunction() {
    testExpressionTranslationWithContext("xs:yearMonthDuration(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "year-month-duration(BT-00-Text)");
  }

  @Test
  void testYearMonthDurationFromStringFunction() {
    testExpressionTranslationWithContext("xs:dayTimeDuration(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "day-time-duration(BT-00-Text)");
  }

  // #endregion Duration functions

  // #region: Sequence Functions ----------------------------------------------

  @Test
  void testDistinctValuesFunction_WithStringSequences() {
    testExpressionTranslationWithContext("distinct-values(('one','two','one'))", "ND-Root",
        "distinct-values(['one', 'two', 'one'])");
  }

  @Test
  void testDistinctValuesFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values((1,2,3,2,3,4))", "ND-Root",
        "distinct-values([1, 2, 3, 2, 3, 4])");
  }

  @Test
  void testDistinctValuesFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'),xs:date('2022-01-02Z')))",
        "ND-Root", "distinct-values([2018-01-01Z, 2020-01-01Z, 2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testDistinctValuesFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'),xs:time('14:00:00Z')))",
        "ND-Root", "distinct-values([12:00:00Z, 13:00:00Z, 12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testDistinctValuesFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')))",
        "ND-Root", "distinct-values([P1W, P2D, P2D, P5D])");
  }

  @Test
  void testDistinctValuesFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values((true(),false(),false(),false()))",
        "ND-Root", "distinct-values([TRUE, FALSE, FALSE, NEVER])");
  }

  @Test
  void testDistinctValuesFunction_WithFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "distinct-values(BT-00-Text)");
  }

  // #region: Count-duplicates

  @Test
  void testCountDuplicatesFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
        "count(('one','two','one')) - count(distinct-values(('one','two','one')))", "ND-Root",
        "count-duplicates(['one', 'two', 'one'])");
  }

  @Test
  void testCountDuplicatesFunction_WithNumberSequences() {
    testExpressionTranslationWithContext(
        "count((1,2,3,2,3,4)) - count(distinct-values((1,2,3,2,3,4)))", "ND-Root",
        "count-duplicates([1, 2, 3, 2, 3, 4])");
  }

  @Test
  void testCountDuplicatesFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
        "count((true(),false(),false(),false())) - count(distinct-values((true(),false(),false(),false())))",
        "ND-Root", "count-duplicates([TRUE, FALSE, FALSE, NEVER])");
  }

  @Test
  void testCountDuplicatesFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "count((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'))) - count(distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'))))",
        "ND-Root", "count-duplicates([2018-01-01Z, 2020-01-01Z, 2018-01-01Z])");
  }

  @Test
  void testCountDuplicatesFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "count((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'))) - count(distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'))))",
        "ND-Root", "count-duplicates([12:00:00Z, 13:00:00Z, 12:00:00Z])");
  }

  @Test
  void testCountDuplicatesFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "count((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P2D'))) - count(distinct-values((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P2D'))))",
        "ND-Root", "count-duplicates([P1W, P2D, P2D])");
  }

  @Test
  void testCountDuplicatesFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
        "count(PathNode/TextField/normalize-space(text())) - count(distinct-values(PathNode/TextField/normalize-space(text())))",
        "ND-Root", "count-duplicates(BT-00-Text)");
  }

  // #endregion: Count-duplicates

  // #region: Get-duplicates

  @Test
  void testGetDuplicatesFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values(('one','two','one')) return if (count(('one','two','one')[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates(['one', 'two', 'one'])");
  }

  @Test
  void testGetDuplicatesFunction_WithNumberSequences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values((1,2,3,2,3,4)) return if (count((1,2,3,2,3,4)[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates([1, 2, 3, 2, 3, 4])");
  }

  @Test
  void testGetDuplicatesFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values((true(),false(),false(),false())) return if (count((true(),false(),false(),false())[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates([TRUE, FALSE, FALSE, NEVER])");
  }

  @Test
  void testGetDuplicatesFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'))) return if (count((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'))[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates([2018-01-01Z, 2020-01-01Z, 2018-01-01Z])");
  }

  @Test
  void testGetDuplicatesFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'))) return if (count((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'))[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates([12:00:00Z, 13:00:00Z, 12:00:00Z])");
  }

  @Test
  void testGetDuplicatesFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P2D'))) return if (count((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P2D'))[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates([P1W, P2D, P2D])");
  }

  @Test
  void testGetDuplicatesFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
        "for $__v in distinct-values(PathNode/TextField/normalize-space(text())) return if (count(PathNode/TextField/normalize-space(text())[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "get-duplicates(BT-00-Text)");
  }

  // #endregion: Get-duplicates

  // #region: Union

  @Test
  void testUnionFunction_WithStringSequences() {
    testExpressionTranslationWithContext("distinct-values((('one','two'), ('two','three','four')))",
        "ND-Root", "value-union(['one', 'two'], ['two', 'three', 'four'])");
  }

  @Test
  void testUnionFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(((1,2,3), (2,3,4)))", "ND-Root",
        "value-union([1, 2, 3], [2, 3, 4])");
  }

  @Test
  void testUnionFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:date('2018-01-01Z'),xs:date('2020-01-01Z')), (xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))))",
        "ND-Root", "value-union([2018-01-01Z, 2020-01-01Z], [2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testUnionFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:time('12:00:00Z'),xs:time('13:00:00Z')), (xs:time('12:00:00Z'),xs:time('14:00:00Z'))))",
        "ND-Root", "value-union([12:00:00Z, 13:00:00Z], [12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testUnionFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values(((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')), (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D'))))",
        "ND-Root", "value-union([P1W, P2D], [P2D, P5D])");
  }

  @Test
  void testUnionFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values(((true(),false()), (false(),false())))",
        "ND-Root", "value-union([TRUE, FALSE], [FALSE, NEVER])");
  }

  @Test
  void testUnionFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
        "distinct-values((PathNode/TextField/normalize-space(text()), PathNode/TextField/normalize-space(text())))", "ND-Root",
        "value-union(BT-00-Text, BT-00-Text)");
  }

  @Test
  void testUnionFunction_WithFieldReferenceAndStringSequence() {
    testExpressionTranslationWithContext(
        "distinct-values((PathNode/RepeatableTextField/normalize-space(text()), ('a','b')))",
        "ND-Root", "value-union(BT-00-Repeatable-Text, ['a', 'b'])");
  }

  @Test
  void testUnionFunction_WithStringSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values((('a','b'), PathNode/RepeatableTextField/normalize-space(text())))",
        "ND-Root", "value-union(['a', 'b'], BT-00-Repeatable-Text)");
  }

  @Test
  void testUnionFunction_WithVariableAndBooleanSequence() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return distinct-values((($x), (false())))", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return value-union([$x], [FALSE])");
  }

  @Test
  void testUnionFunction_WithBooleanSequenceAndVariable() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return distinct-values(((false()), ($x)))", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return value-union([FALSE], [$x])");
  }

  @Test
  void testUnionFunction_WithFieldReferenceAndNumberSequence() {
    testExpressionTranslationWithContext(
        "distinct-values((PathNode/RepeatableNumberField/number(), (1,2)))", "ND-Root",
        "value-union(BT-00-Repeatable-Number, [1, 2])");
  }

  @Test
  void testUnionFunction_WithNumberSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(((1,2), PathNode/RepeatableNumberField/number()))", "ND-Root",
        "value-union([1, 2], BT-00-Repeatable-Number)");
  }

  @Test
  void testUnionFunction_WithFieldReferenceAndDateSequence() {
    testExpressionTranslationWithContext(
        "distinct-values((PathNode/RepeatableDateField/xs:date(text()), (xs:date('2018-01-01Z'))))",
        "ND-Root", "value-union(BT-00-Repeatable-Date, [2018-01-01Z])");
  }

  @Test
  void testUnionFunction_WithDateSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:date('2018-01-01Z')), PathNode/RepeatableDateField/xs:date(text())))",
        "ND-Root", "value-union([2018-01-01Z], BT-00-Repeatable-Date)");
  }

  @Test
  void testUnionFunction_WithFieldReferenceAndTimeSequence() {
    testExpressionTranslationWithContext(
        "distinct-values((PathNode/RepeatableTimeField/xs:time(text()), (xs:time('12:00:00Z'))))",
        "ND-Root", "value-union(BT-00-Repeatable-Time, [12:00:00Z])");
  }

  @Test
  void testUnionFunction_WithTimeSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:time('12:00:00Z')), PathNode/RepeatableTimeField/xs:time(text())))",
        "ND-Root", "value-union([12:00:00Z], BT-00-Repeatable-Time)");
  }

  @Test
  void testUnionFunction_WithFieldReferenceAndDurationSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(((for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())), (xs:dayTimeDuration('P2D'))))",
        "ND-Root", "value-union(BT-00-Repeatable-Duration, [P2D])");
  }

  @Test
  void testUnionFunction_WithDurationSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:dayTimeDuration('P2D')), (for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))))",
        "ND-Root", "value-union([P2D], BT-00-Repeatable-Duration)");
  }

  @Test
  void testUnionFunction_WithTypeMismatch() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "value-union(BT-00-Text, BT-00-Number)"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  // #endregion: Union

  // #region: Intersect

  @Test
  void testIntersectFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in ('one','two') return if (some $__L2 in ('two','three','four') satisfies $__L1 = $__L2) then $__L1 else ())", "ND-Root",
        "value-intersect(['one', 'two'], ['two', 'three', 'four'])");
  }

  @Test
  void testIntersectFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in (1,2,3) return if (some $__L2 in (2,3,4) satisfies $__L1 = $__L2) then $__L1 else ())", "ND-Root",
        "value-intersect([1, 2, 3], [2, 3, 4])");
  }

  @Test
  void testIntersectFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in (xs:date('2018-01-01Z'),xs:date('2020-01-01Z')) return if (some $__L2 in (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([2018-01-01Z, 2020-01-01Z], [2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testIntersectFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in (xs:time('12:00:00Z'),xs:time('13:00:00Z')) return if (some $__L2 in (xs:time('12:00:00Z'),xs:time('14:00:00Z')) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([12:00:00Z, 13:00:00Z], [12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testIntersectFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in (xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')) return if (some $__L2 in (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([P1W, P2D], [P2D, P5D])");
  }

  @Test
  void testIntersectFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in (true(),false()) return if (some $__L2 in (false(),false()) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([TRUE, FALSE], [FALSE, NEVER])");
  }

  @Test
  void testIntersectFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in PathNode/TextField/normalize-space(text()) return if (some $__L2 in PathNode/TextField/normalize-space(text()) satisfies $__L1 = $__L2) then $__L1 else ())", "ND-Root",
        "value-intersect(BT-00-Text, BT-00-Text)");
  }

  @Test
  void testIntersectFunction_WithFieldReferenceAndStringSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableTextField/normalize-space(text()) return if (some $__L2 in ('a','b') satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect(BT-00-Repeatable-Text, ['a', 'b'])");
  }

  @Test
  void testIntersectFunction_WithStringSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in ('a','b') return if (some $__L2 in PathNode/RepeatableTextField/normalize-space(text()) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect(['a', 'b'], BT-00-Repeatable-Text)");
  }

  @Test
  void testIntersectFunction_WithVariableAndBooleanSequence() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return distinct-values(for $__L1 in ($x) return if (some $__L2 in (false()) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "for indicator:$x in [TRUE, FALSE] return value-intersect([$x], [FALSE])");
  }

  @Test
  void testIntersectFunction_WithBooleanSequenceAndVariable() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return distinct-values(for $__L1 in (false()) return if (some $__L2 in ($x) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "for indicator:$x in [TRUE, FALSE] return value-intersect([FALSE], [$x])");
  }

  @Test
  void testIntersectFunction_WithFieldReferenceAndNumberSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableNumberField/number() return if (some $__L2 in (1,2) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect(BT-00-Repeatable-Number, [1, 2])");
  }

  @Test
  void testIntersectFunction_WithNumberSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (1,2) return if (some $__L2 in PathNode/RepeatableNumberField/number() satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([1, 2], BT-00-Repeatable-Number)");
  }

  @Test
  void testIntersectFunction_WithFieldReferenceAndDateSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableDateField/xs:date(text()) return if (some $__L2 in (xs:date('2018-01-01Z')) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect(BT-00-Repeatable-Date, [2018-01-01Z])");
  }

  @Test
  void testIntersectFunction_WithDateSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:date('2018-01-01Z')) return if (some $__L2 in PathNode/RepeatableDateField/xs:date(text()) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([2018-01-01Z], BT-00-Repeatable-Date)");
  }

  @Test
  void testIntersectFunction_WithFieldReferenceAndTimeSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableTimeField/xs:time(text()) return if (some $__L2 in (xs:time('12:00:00Z')) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect(BT-00-Repeatable-Time, [12:00:00Z])");
  }

  @Test
  void testIntersectFunction_WithTimeSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:time('12:00:00Z')) return if (some $__L2 in PathNode/RepeatableTimeField/xs:time(text()) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([12:00:00Z], BT-00-Repeatable-Time)");
  }

  @Test
  void testIntersectFunction_WithFieldReferenceAndDurationSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return if (some $__L2 in (xs:dayTimeDuration('P2D')) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect(BT-00-Repeatable-Duration, [P2D])");
  }

  @Test
  void testIntersectFunction_WithDurationSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:dayTimeDuration('P2D')) return if (some $__L2 in (for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "value-intersect([P2D], BT-00-Repeatable-Duration)");
  }

  @Test
  void testIntersectFunction_WithTypeMismatch() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "value-intersect(BT-00-Text, BT-00-Number)"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  // #endregion: Intersect

  // #region: Except

  @Test
  void testExceptFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in ('one','two') return if (every $__L2 in ('two','three','four') satisfies $__L1 != $__L2) then $__L1 else ())", "ND-Root",
        "value-except(['one', 'two'], ['two', 'three', 'four'])");
  }

  @Test
  void testExceptFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in (1,2,3) return if (every $__L2 in (2,3,4) satisfies $__L1 != $__L2) then $__L1 else ())", "ND-Root",
        "value-except([1, 2, 3], [2, 3, 4])");
  }

  @Test
  void testExceptFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in (xs:date('2018-01-01Z'),xs:date('2020-01-01Z')) return if (every $__L2 in (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([2018-01-01Z, 2020-01-01Z], [2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testExceptFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in (xs:time('12:00:00Z'),xs:time('13:00:00Z')) return if (every $__L2 in (xs:time('12:00:00Z'),xs:time('14:00:00Z')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([12:00:00Z, 13:00:00Z], [12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testExceptFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')) return if (every $__L2 in (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([P1W, P2D], [P2D, P5D])");
  }

  @Test
  void testExceptFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in (true(),false()) return if (every $__L2 in (false(),false()) satisfies $__L1 != $__L2) then $__L1 else ())", "ND-Root",
        "value-except([TRUE, FALSE], [FALSE, NEVER])");
  }

  @Test
  void testExceptFunction_WithTextFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in PathNode/TextField/normalize-space(text()) return if (every $__L2 in PathNode/TextField/normalize-space(text()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Text, BT-00-Text)");
  }

  @Test
  void testExceptFunction_WithNumberFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in PathNode/IntegerField/number() return if (every $__L2 in PathNode/IntegerField/number() satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Integer, BT-00-Integer)");
  }

  @Test
  void testExceptFunction_WithBooleanFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) return if (every $__L2 in PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Indicator, BT-00-Indicator)");
  }

    @Test
  void testExceptFunction_WithDateFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in PathNode/StartDateField/xs:date(text()) return if (every $__L2 in PathNode/StartDateField/xs:date(text()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-StartDate, BT-00-StartDate)");
  }

  @Test
  void testExceptFunction_WithTimeFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $__L1 in PathNode/StartTimeField/xs:time(text()) return if (every $__L2 in PathNode/StartTimeField/xs:time(text()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-StartTime, BT-00-StartTime)");
  }

  @Test
  void testExceptFunction_WithDurationFieldReferences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $__L1 in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return if (every $__L2 in (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) satisfies $__L1 != $__L2) then $__L1 else ())", "ND-Root", "value-except(BT-00-Duration, BT-00-Duration)");
  }

  @Test
  void testExceptFunction_WithFieldReferenceAndStringSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableTextField/normalize-space(text()) return if (every $__L2 in ('a','b') satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Repeatable-Text, ['a', 'b'])");
  }

  @Test
  void testExceptFunction_WithStringSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in ('a','b') return if (every $__L2 in PathNode/RepeatableTextField/normalize-space(text()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(['a', 'b'], BT-00-Repeatable-Text)");
  }

  @Test
  void testExceptFunction_WithVariableAndBooleanSequence() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return distinct-values(for $__L1 in ($x) return if (every $__L2 in (false()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "for indicator:$x in [TRUE, FALSE] return value-except([$x], [FALSE])");
  }

  @Test
  void testExceptFunction_WithBooleanSequenceAndVariable() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return distinct-values(for $__L1 in (false()) return if (every $__L2 in ($x) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "for indicator:$x in [TRUE, FALSE] return value-except([FALSE], [$x])");
  }

  @Test
  void testExceptFunction_WithFieldReferenceAndNumberSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableNumberField/number() return if (every $__L2 in (1,2) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Repeatable-Number, [1, 2])");
  }

  @Test
  void testExceptFunction_WithNumberSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (1,2) return if (every $__L2 in PathNode/RepeatableNumberField/number() satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([1, 2], BT-00-Repeatable-Number)");
  }

  @Test
  void testExceptFunction_WithFieldReferenceAndDateSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableDateField/xs:date(text()) return if (every $__L2 in (xs:date('2018-01-01Z')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Repeatable-Date, [2018-01-01Z])");
  }

  @Test
  void testExceptFunction_WithDateSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:date('2018-01-01Z')) return if (every $__L2 in PathNode/RepeatableDateField/xs:date(text()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([2018-01-01Z], BT-00-Repeatable-Date)");
  }

  @Test
  void testExceptFunction_WithFieldReferenceAndTimeSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in PathNode/RepeatableTimeField/xs:time(text()) return if (every $__L2 in (xs:time('12:00:00Z')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Repeatable-Time, [12:00:00Z])");
  }

  @Test
  void testExceptFunction_WithTimeSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:time('12:00:00Z')) return if (every $__L2 in PathNode/RepeatableTimeField/xs:time(text()) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([12:00:00Z], BT-00-Repeatable-Time)");
  }

  @Test
  void testExceptFunction_WithFieldReferenceAndDurationSequence() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) return if (every $__L2 in (xs:dayTimeDuration('P2D')) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except(BT-00-Repeatable-Duration, [P2D])");
  }

  @Test
  void testExceptFunction_WithDurationSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "distinct-values(for $__L1 in (xs:dayTimeDuration('P2D')) return if (every $__L2 in (for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "value-except([P2D], BT-00-Repeatable-Duration)");
  }

  @Test
  void testExceptFunction_WithTypeMismatch() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "value-except(BT-00-Text, BT-00-Number)"));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  // #endregion: Except

  // #region: Variables of the XPath ------------------------------------------
  // The XPath binds variables of its own. Their names start with `__`, which an EFX name cannot,
  // so a variable of the same name in EFX keeps its value inside them.

  @Test
  void testIntersectFunction_WithVariableOfTheSameName() {
    testExpressionTranslationWithContext(
        "for $L1 in ('a') return distinct-values(for $__L1 in ('a','b')"
            + " return if (some $__L2 in ($L1) satisfies $__L1 = $__L2) then $__L1 else ())",
        "ND-Root", "for text:$L1 in ['a'] return value-intersect(['a', 'b'], [$L1])");
  }

  @Test
  void testExceptFunction_WithVariableOfTheSameName() {
    testExpressionTranslationWithContext(
        "for $L1 in ('a') return distinct-values(for $__L1 in ('a','b')"
            + " return if (every $__L2 in ($L1) satisfies $__L1 != $__L2) then $__L1 else ())",
        "ND-Root", "for text:$L1 in ['a'] return value-except(['a', 'b'], [$L1])");
  }

  @Test
  void testGetDuplicatesFunction_WithVariableOfTheSameName() {
    testExpressionTranslationWithContext(
        "for $v in ('a') return for $__v in distinct-values(($v,'b','b'))"
            + " return if (count(($v,'b','b')[. = $__v]) > 1) then $__v else ()",
        "ND-Root", "for text:$v in ['a'] return get-duplicates([$v, 'b', 'b'])");
  }

  @Test
  void testStringUniqueValueCondition_WithVariableOfTheSameName() {
    testExpressionTranslationWithContext(
        "for $n in ('a') return count(for $__n in 'a', $__x in ($n,'b')[. = $__n] return $__x) = 1",
        "ND-Root", "for text:$n in ['a'] return 'a' is unique in [$n, 'b']");
  }

  @Test
  void testDurationComparison_WithVariableOfTheSameName() {
    testExpressionTranslationWithContext(
        "for $T in (xs:dayTimeDuration('P1D')) return boolean(for $__T in (current-date())"
            + " return ($__T + $T > $__T + xs:dayTimeDuration('P2D')))",
        "ND-Root", "for duration:$T in (P1D) return $T > P2D");
  }

  // #endregion: Variables of the XPath

  // #region: Sort

  @Test
  void testSortFunction_WithStringSequences() {
    testExpressionTranslationWithContext("sort(('banana','apple','cherry'))", "ND-Root",
        "sort(['banana', 'apple', 'cherry'])");
  }

  @Test
  void testSortFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("sort((3,1,2))", "ND-Root",
        "sort([3, 1, 2])");
  }

  @Test
  void testSortFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "sort((xs:date('2022-01-01Z'),xs:date('2018-01-01Z'),xs:date('2020-01-01Z')))",
        "ND-Root", "sort([2022-01-01Z, 2018-01-01Z, 2020-01-01Z])");
  }

  @Test
  void testSortFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "sort((xs:time('14:00:00Z'),xs:time('12:00:00Z'),xs:time('13:00:00Z')))",
        "ND-Root", "sort([14:00:00Z, 12:00:00Z, 13:00:00Z])");
  }

  @Test
  void testSortFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("sort((xs:dayTimeDuration('P5D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')))",
        "ND-Root", "sort([P5D, P2D, P1W])");
  }

  @Test
  void testSortFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("sort((true(),false(),true()))",
        "ND-Root", "sort([TRUE, FALSE, TRUE])");
  }

  @Test
  void testSortFunction_WithFieldReferences() {
    testExpressionTranslationWithContext("sort(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "sort(BT-00-Text)");
  }

  @Test
  void testSortFunction_WithRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "sort(PathNode/RepeatableTextField/normalize-space(text()))", "ND-Root",
        "sort(BT-00-Repeatable-Text)");
  }

  // #endregion: Sort

  // #region: Reverse

  @Test
  void testReverseFunction_WithStringSequences() {
    testExpressionTranslationWithContext("reverse(('banana','apple','cherry'))", "ND-Root",
        "reverse(['banana', 'apple', 'cherry'])");
  }

  @Test
  void testReverseFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("reverse((3,1,2))", "ND-Root",
        "reverse([3, 1, 2])");
  }

  @Test
  void testReverseFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "reverse((xs:date('2022-01-01Z'),xs:date('2018-01-01Z'),xs:date('2020-01-01Z')))",
        "ND-Root", "reverse([2022-01-01Z, 2018-01-01Z, 2020-01-01Z])");
  }

  @Test
  void testReverseFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "reverse((xs:time('14:00:00Z'),xs:time('12:00:00Z'),xs:time('13:00:00Z')))",
        "ND-Root", "reverse([14:00:00Z, 12:00:00Z, 13:00:00Z])");
  }

  @Test
  void testReverseFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("reverse((xs:dayTimeDuration('P5D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')))",
        "ND-Root", "reverse([P5D, P2D, P1W])");
  }

  @Test
  void testReverseFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("reverse((true(),false(),true()))",
        "ND-Root", "reverse([TRUE, FALSE, TRUE])");
  }

  @Test
  void testReverseFunction_WithFieldReferences() {
    testExpressionTranslationWithContext("reverse(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "reverse(BT-00-Text)");
  }

  @Test
  void testReverseFunction_WithRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "reverse(PathNode/RepeatableTextField/normalize-space(text()))", "ND-Root",
        "reverse(BT-00-Repeatable-Text)");
  }

  // #endregion: Reverse

  // #region: Subsequence

  @Test
  void testSubsequenceFunction_WithStringSequences() {
    testExpressionTranslationWithContext("subsequence(('a','b','c','d'), 2)", "ND-Root",
        "subsequence(['a', 'b', 'c', 'd'], 2)");
  }

  @Test
  void testSubsequenceFunction_WithStringSequences_AndLength() {
    testExpressionTranslationWithContext("subsequence(('a','b','c','d'), 2, 2)", "ND-Root",
        "subsequence(['a', 'b', 'c', 'd'], 2, 2)");
  }

  @Test
  void testSubsequenceFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("subsequence((10,20,30,40), 2, 2)", "ND-Root",
        "subsequence([10, 20, 30, 40], 2, 2)");
  }

  @Test
  void testSubsequenceFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "subsequence((xs:date('2022-01-01Z'),xs:date('2023-01-01Z'),xs:date('2024-01-01Z')), 1, 2)",
        "ND-Root", "subsequence([2022-01-01Z, 2023-01-01Z, 2024-01-01Z], 1, 2)");
  }

  @Test
  void testSubsequenceFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("subsequence((true(),false(),true()), 2)", "ND-Root",
        "subsequence([TRUE, FALSE, TRUE], 2)");
  }

  @Test
  void testSubsequenceFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "subsequence((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('14:00:00Z')), 2, 1)",
        "ND-Root", "subsequence([12:00:00Z, 13:00:00Z, 14:00:00Z], 2, 1)");
  }

  @Test
  void testSubsequenceFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "subsequence((xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')), 2, 1)",
        "ND-Root", "subsequence([P1D, P2D, P3D], 2, 1)");
  }

  @Test
  void testSubsequenceFunction_WithRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "subsequence(PathNode/RepeatableTextField/normalize-space(text()), 2)", "ND-Root",
        "subsequence(BT-00-Repeatable-Text, 2)");
  }

  @Test
  void testSubsequenceFunction_WithRepeatableFieldReference_AndLength() {
    testExpressionTranslationWithContext(
        "subsequence(PathNode/RepeatableTextField/normalize-space(text()), 1, 3)", "ND-Root",
        "subsequence(BT-00-Repeatable-Text, 1, 3)");
  }

  // #endregion: Subsequence

  // #region: Index-of

  @Test
  void testIndexOfFunction_WithStringSequences() {
    testExpressionTranslationWithContext("index-of(('a','b','c','b'), 'b')[1]", "ND-Root",
        "index-of(['a', 'b', 'c', 'b'], 'b')");
  }

  @Test
  void testIndexOfFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("index-of((10,20,30,20), 20)[1]", "ND-Root",
        "index-of([10, 20, 30, 20], 20)");
  }

  @Test
  void testIndexOfFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "index-of((xs:date('2022-01-01Z'),xs:date('2023-01-01Z'),xs:date('2022-01-01Z')), xs:date('2022-01-01Z'))[1]",
        "ND-Root", "index-of([2022-01-01Z, 2023-01-01Z, 2022-01-01Z], 2022-01-01Z)");
  }

  @Test
  void testIndexOfFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("index-of((true(),false(),true()), true())[1]",
        "ND-Root", "index-of([TRUE, FALSE, TRUE], TRUE)");
  }

  @Test
  void testIndexOfFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "index-of((xs:time('14:00:00Z'),xs:time('12:00:00Z'),xs:time('14:00:00Z')), xs:time('14:00:00Z'))[1]",
        "ND-Root", "index-of([14:00:00Z, 12:00:00Z, 14:00:00Z], 14:00:00Z)");
  }

  @Test
  void testIndexOfFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "index-of((xs:dayTimeDuration('P5D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')), xs:dayTimeDuration('P5D'))[1]",
        "ND-Root", "index-of([P5D, P2D, P5D], P5D)");
  }

  @Test
  void testIndexOfFunction_WithRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "index-of(PathNode/RepeatableTextField/normalize-space(text()), 'hello')[1]", "ND-Root",
        "index-of(BT-00-Repeatable-Text, 'hello')");
  }

  @Test
  void testIndexOfFunction_WithRepeatableFieldReferenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "index-of(PathNode/RepeatableTextField/normalize-space(text()), PathNode/TextField/normalize-space(text()))[1]",
        "ND-Root", "index-of(BT-00-Repeatable-Text, BT-00-Text)");
  }

  @Test
  void testIndexOfFunction_WithStringSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "index-of(('a','b'), PathNode/TextField/normalize-space(text()))[1]", "ND-Root",
        "index-of(['a', 'b'], BT-00-Text)");
  }

  @Test
  void testIndexOfFunction_WithVariableAndBoolean() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return index-of(($x), true())[1]", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return index-of([$x], TRUE)");
  }

  @Test
  void testIndexOfFunction_WithBooleanSequenceAndVariable() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return index-of((true(),false()), $x)[1]", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return index-of([TRUE, FALSE], $x)");
  }

  @Test
  void testIndexOfFunction_WithRepeatableFieldReferenceAndNumber() {
    testExpressionTranslationWithContext(
        "index-of(PathNode/RepeatableNumberField/number(), 20)[1]", "ND-Root",
        "index-of(BT-00-Repeatable-Number, 20)");
  }

  @Test
  void testIndexOfFunction_WithNumberSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "index-of((10,20), PathNode/NumberField/number())[1]", "ND-Root",
        "index-of([10, 20], BT-00-Number)");
  }

  @Test
  void testIndexOfFunction_WithRepeatableFieldReferenceAndDate() {
    testExpressionTranslationWithContext(
        "index-of(PathNode/RepeatableDateField/xs:date(text()), xs:date('2022-01-01Z'))[1]",
        "ND-Root", "index-of(BT-00-Repeatable-Date, 2022-01-01Z)");
  }

  @Test
  void testIndexOfFunction_WithDateSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "index-of((xs:date('2022-01-01Z')), PathNode/StartDateField/xs:date(text()))[1]", "ND-Root",
        "index-of([2022-01-01Z], BT-00-StartDate)");
  }

  @Test
  void testIndexOfFunction_WithRepeatableFieldReferenceAndTime() {
    testExpressionTranslationWithContext(
        "index-of(PathNode/RepeatableTimeField/xs:time(text()), xs:time('14:00:00Z'))[1]",
        "ND-Root", "index-of(BT-00-Repeatable-Time, 14:00:00Z)");
  }

  @Test
  void testIndexOfFunction_WithTimeSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "index-of((xs:time('14:00:00Z')), PathNode/StartTimeField/xs:time(text()))[1]", "ND-Root",
        "index-of([14:00:00Z], BT-00-StartTime)");
  }

  @Test
  void testIndexOfFunction_WithRepeatableFieldReferenceAndDuration() {
    testExpressionTranslationWithContext(
        "index-of((for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())), xs:dayTimeDuration('P5D'))[1]",
        "ND-Root", "index-of(BT-00-Repeatable-Duration, P5D)");
  }

  @Test
  void testIndexOfFunction_WithDurationSequenceAndFieldReference() {
    testExpressionTranslationWithContext(
        "index-of((xs:dayTimeDuration('P5D')), (for $__F in PathNode/DurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ())))[1]",
        "ND-Root", "index-of([P5D], BT-00-Duration)");
  }

  // #endregion: Index-of

  // #region: Compare sequences

  @Test
  void testSequenceEqualFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(('one','two')), sort(('two','three','four')))", "ND-Root",
        "sequence-equal(['one', 'two'], ['two', 'three', 'four'])");
  }

  @Test
  void testSequenceEqualFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("deep-equal(sort((1,2,3)), sort((2,3,4)))", "ND-Root",
        "sequence-equal([1, 2, 3], [2, 3, 4])");
  }

  @Test
  void testSequenceEqualFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'))), sort((xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))))",
        "ND-Root", "sequence-equal([2018-01-01Z, 2020-01-01Z], [2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testSequenceEqualFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:time('12:00:00Z'),xs:time('13:00:00Z'))), sort((xs:time('12:00:00Z'),xs:time('14:00:00Z'))))",
        "ND-Root", "sequence-equal([12:00:00Z, 13:00:00Z], [12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testSequenceEqualFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((true(),false())), sort((false(),false())))", "ND-Root",
        "sequence-equal([TRUE, FALSE], [FALSE, NEVER])");
  }

  @Test
  void testSequenceEqualFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2Y'))), sort((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P3Y'))))",
        "ND-Root", "sequence-equal([P1Y, P2Y], [P1Y, P3Y])");
  }

  @Test
  void testSequenceEqualFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(PathNode/TextField/normalize-space(text())), sort(PathNode/TextField/normalize-space(text())))", "ND-Root",
        "sequence-equal(BT-00-Text, BT-00-Text)");
  }

  @Test
  void testSequenceEqualFunction_WithRepeatableFieldReferenceAndStringSequence() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(PathNode/RepeatableTextField/normalize-space(text())), sort(('one','two')))",
        "ND-Root", "sequence-equal(BT-00-Repeatable-Text, ['one', 'two'])");
  }

  @Test
  void testSequenceEqualFunction_WithStringSequenceAndRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(('one','two')), sort(PathNode/RepeatableTextField/normalize-space(text())))",
        "ND-Root", "sequence-equal(['one', 'two'], BT-00-Repeatable-Text)");
  }

  @Test
  void testSequenceEqualFunction_WithVariableAndBooleanSequence() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return deep-equal(sort(($x)), sort((true())))", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return sequence-equal([$x], [TRUE])");
  }

  @Test
  void testSequenceEqualFunction_WithBooleanSequenceAndVariable() {
    testExpressionTranslationWithContext(
        "for $x in (true(),false()) return deep-equal(sort((true())), sort(($x)))", "ND-Root",
        "for indicator:$x in [TRUE, FALSE] return sequence-equal([TRUE], [$x])");
  }

  @Test
  void testSequenceEqualFunction_WithRepeatableFieldReferenceAndNumberSequence() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(PathNode/RepeatableNumberField/number()), sort((1,2)))", "ND-Root",
        "sequence-equal(BT-00-Repeatable-Number, [1, 2])");
  }

  @Test
  void testSequenceEqualFunction_WithNumberSequenceAndRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((1,2)), sort(PathNode/RepeatableNumberField/number()))", "ND-Root",
        "sequence-equal([1, 2], BT-00-Repeatable-Number)");
  }

  @Test
  void testSequenceEqualFunction_WithRepeatableFieldReferenceAndDateSequence() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(PathNode/RepeatableDateField/xs:date(text())), sort((xs:date('2018-01-01Z'))))",
        "ND-Root", "sequence-equal(BT-00-Repeatable-Date, [2018-01-01Z])");
  }

  @Test
  void testSequenceEqualFunction_WithDateSequenceAndRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:date('2018-01-01Z'))), sort(PathNode/RepeatableDateField/xs:date(text())))",
        "ND-Root", "sequence-equal([2018-01-01Z], BT-00-Repeatable-Date)");
  }

  @Test
  void testSequenceEqualFunction_WithRepeatableFieldReferenceAndTimeSequence() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(PathNode/RepeatableTimeField/xs:time(text())), sort((xs:time('12:00:00Z'))))",
        "ND-Root", "sequence-equal(BT-00-Repeatable-Time, [12:00:00Z])");
  }

  @Test
  void testSequenceEqualFunction_WithTimeSequenceAndRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:time('12:00:00Z'))), sort(PathNode/RepeatableTimeField/xs:time(text())))",
        "ND-Root", "sequence-equal([12:00:00Z], BT-00-Repeatable-Time)");
  }

  @Test
  void testSequenceEqualFunction_WithRepeatableFieldReferenceAndDurationSequence() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))), sort((xs:yearMonthDuration('P1Y'))))",
        "ND-Root", "sequence-equal(BT-00-Repeatable-Duration, [P1Y])");
  }

  @Test
  void testSequenceEqualFunction_WithDurationSequenceAndRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:yearMonthDuration('P1Y'))), sort((for $__F in PathNode/RepeatableDurationField return (if ($__F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D')) else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D')) else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y')) else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))))",
        "ND-Root", "sequence-equal([P1Y], BT-00-Repeatable-Duration)");
  }

  // #endregion: Compare sequences

  // #region: Sequence emptiness

  @Test
  void testSequenceEmptiness_WithNonRepeatableField() {
    // Field references always go through sequence emptiness, regardless of repeatability.
    testExpressionTranslationWithContext("empty(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "BT-00-Text is empty");
  }

  @Test
  void testSequenceEmptiness_WithStringSequence() {
    testExpressionTranslationWithContext("empty(('a','b','c'))", "ND-Root",
        "['a', 'b', 'c'] is empty");
  }

  @Test
  void testSequenceEmptiness_WithStringSequence_Negated() {
    testExpressionTranslationWithContext("not(empty(('a','b','c')))", "ND-Root",
        "['a', 'b', 'c'] is not empty");
  }

  @Test
  void testSequenceEmptiness_WithNumericSequence() {
    testExpressionTranslationWithContext("empty((1,2,3))", "ND-Root",
        "[1, 2, 3] is empty");
  }

  @Test
  void testSequenceEmptiness_WithBooleanSequence() {
    testExpressionTranslationWithContext("empty((true(),false()))", "ND-Root",
        "[TRUE, FALSE] is empty");
  }

  @Test
  void testSequenceEmptiness_WithDateSequence() {
    testExpressionTranslationWithContext(
        "empty((xs:date('2024-01-01Z'),xs:date('2024-12-31Z')))", "ND-Root",
        "[2024-01-01Z, 2024-12-31Z] is empty");
  }

  @Test
  void testSequenceEmptiness_WithTimeSequence() {
    testExpressionTranslationWithContext(
        "empty((xs:time('12:00:00Z'),xs:time('13:00:00Z')))", "ND-Root",
        "[12:00:00Z, 13:00:00Z] is empty");
  }

  @Test
  void testSequenceEmptiness_WithDurationSequence() {
    testExpressionTranslationWithContext(
        "empty((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2Y')))", "ND-Root",
        "[P1Y, P2Y] is empty");
  }

  @Test
  void testSequenceEmptiness_WithRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "empty(PathNode/RepeatableTextField/normalize-space(text()))", "ND-Root",
        "BT-00-Repeatable-Text is empty");
  }

  @Test
  void testSequenceEmptiness_WithRepeatableFieldReference_Negated() {
    testExpressionTranslationWithContext(
        "not(empty(PathNode/RepeatableTextField/normalize-space(text())))", "ND-Root",
        "BT-00-Repeatable-Text is not empty");
  }

  // #endregion: Sequence emptiness

  // #region: String empty function

  @Test
  void testStringEmptyFunction() {
    testExpressionTranslationWithContext("'hello' = ''", "ND-Root", "empty('hello')");
  }

  @Test
  void testStringEmptyFunction_WithFieldReference() {
    testExpressionTranslationWithContext("PathNode/TextField/normalize-space(text()) = ''",
        "ND-Root", "empty(BT-00-Text)");
  }

  // #endregion: String empty function

  // #region: Sequence duplicates

  @Test
  void testSequenceDuplicates_WithNonRepeatableField() {
    // Field references always go through sequence duplicates, regardless of repeatability.
    testExpressionTranslationWithContext(
        "not(count(PathNode/TextField/normalize-space(text())) = count(distinct-values(PathNode/TextField/normalize-space(text()))))",
        "ND-Root", "BT-00-Text has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithStringSequence() {
    testExpressionTranslationWithContext(
        "not(count(('a','b','a')) = count(distinct-values(('a','b','a'))))", "ND-Root",
        "['a', 'b', 'a'] has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithStringSequence_Negated() {
    testExpressionTranslationWithContext(
        "count(('a','b','c')) = count(distinct-values(('a','b','c')))", "ND-Root",
        "['a', 'b', 'c'] has no duplicates");
  }

  @Test
  void testSequenceDuplicates_WithNumericSequence() {
    testExpressionTranslationWithContext(
        "not(count((1,2,3)) = count(distinct-values((1,2,3))))", "ND-Root",
        "[1, 2, 3] has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithBooleanSequence() {
    testExpressionTranslationWithContext(
        "not(count((true(),false())) = count(distinct-values((true(),false()))))", "ND-Root",
        "[TRUE, FALSE] has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithDateSequence() {
    testExpressionTranslationWithContext(
        "not(count((xs:date('2024-01-01Z'),xs:date('2024-12-31Z'))) = count(distinct-values((xs:date('2024-01-01Z'),xs:date('2024-12-31Z')))))",
        "ND-Root",
        "[2024-01-01Z, 2024-12-31Z] has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithTimeSequence() {
    testExpressionTranslationWithContext(
        "not(count((xs:time('12:00:00Z'),xs:time('13:00:00Z'))) = count(distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z')))))",
        "ND-Root",
        "[12:00:00Z, 13:00:00Z] has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithDurationSequence() {
    testExpressionTranslationWithContext(
        "not(count((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2Y'))) = count(distinct-values((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2Y')))))",
        "ND-Root",
        "[P1Y, P2Y] has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithRepeatableFieldReference() {
    testExpressionTranslationWithContext(
        "not(count(PathNode/RepeatableTextField/normalize-space(text())) = count(distinct-values(PathNode/RepeatableTextField/normalize-space(text()))))",
        "ND-Root",
        "BT-00-Repeatable-Text has duplicates");
  }

  @Test
  void testSequenceDuplicates_WithRepeatableFieldReference_Negated() {
    testExpressionTranslationWithContext(
        "count(PathNode/RepeatableTextField/normalize-space(text())) = count(distinct-values(PathNode/RepeatableTextField/normalize-space(text())))",
        "ND-Root",
        "BT-00-Repeatable-Text has no duplicates");
  }

  // #endregion: Sequence duplicates

  @Test
  void testParameterizedExpression_WithStringParameter() {
    testExpressionTranslation("'hello' = 'world'", "{ND-Root, text:$p1, text:$p2} ${$p1 == $p2}",
        "'hello'", "'world'");
  }

  @Test
  void testParameterizedExpression_MissingArgument_Throws() {
    InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
        () -> translateExpression("{ND-Root, text:$p} ${$p}"));
    assertEquals(InvalidArgumentException.ErrorCode.MISSING_ARGUMENT, exception.getErrorCode());
    assertEquals("line 1:10 No argument passed for Parameter '$p'.", exception.getMessage());
  }

  @Test
  void testParameterizedExpression_NullArgument_IsMissing() {
    InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
        () -> translateExpression("WITH ND-Root, text:$p COMPUTE $p", (String) null));
    assertEquals(InvalidArgumentException.ErrorCode.MISSING_ARGUMENT, exception.getErrorCode());
    assertEquals("line 1:14 No argument passed for Parameter '$p'.", exception.getMessage());
  }

  @Test
  void testParameterizedExpression_WithUnquotedStringParameter() {
    assertSyntaxError(
        () -> translateExpression("{ND-Root, text:$p1, text:$p2} ${$p1 == $p2}", "hello", "world"));
  }

  @Test
  void testParameterizedExpression_WithNumberParameter() {
    testExpressionTranslation("1 = 2", "{ND-Root, number:$p1, number:$p2} ${$p1 == $p2}", "1", "2");
  }

  @Test
  void testParameterizedExpression_WithDateParameter() {
    testExpressionTranslation("xs:date('2018-01-01Z') = xs:date('2020-01-01Z')",
        "{ND-Root, date:$p1, date:$p2} ${$p1 == $p2}", "2018-01-01Z", "2020-01-01Z");
  }

  @Test
  void testParameterizedExpression_WithTimeParameter() {
    testExpressionTranslation("xs:time('12:00:00Z') = xs:time('13:00:00Z')",
        "{ND-Root, time:$p1, time:$p2} ${$p1 == $p2}", "12:00:00Z", "13:00:00Z");
  }

  @Test
  void testParameterizedExpression_WithBooleanParameter() {
    testExpressionTranslation("true() = false()",
        "{ND-Root, indicator:$p1, indicator:$p2} ${$p1 == $p2}", "ALWAYS", "FALSE");
  }

  @Test
  void testParameterizedExpression_WithDurationParameter() {
    testExpressionTranslation(
        "boolean(for $__T in (current-date()) return ($__T + xs:yearMonthDuration('P1Y') = $__T + xs:yearMonthDuration('P2Y')))",
        "{ND-Root, duration:$p1, duration:$p2} ${$p1 == $p2}", "P1Y", "P2Y");
  }

  @Test
  void testParameterizedExpression_WithTextSequenceParameter() {
    testExpressionTranslation("count(('a','b','c'))",
        "{ND-Root, text*:$items} ${count($items)}", "['a', 'b', 'c']");
  }

  @Test
  void testParameterizedExpression_WithNumericSequenceParameter() {
    testExpressionTranslation("count((1,2,3))",
        "{ND-Root, number*:$items} ${count($items)}", "[1, 2, 3]");
  }

  @Test
  void testParameterizedExpression_WithBooleanSequenceParameter() {
    testExpressionTranslation("count((true(),false(),true()))",
        "{ND-Root, indicator*:$items} ${count($items)}", "[TRUE, FALSE, ALWAYS]");
  }

  @Test
  void testParameterizedExpression_WithDateSequenceParameter() {
    testExpressionTranslation("count((xs:date('2024-01-01Z'),xs:date('2024-12-31Z')))",
        "{ND-Root, date*:$items} ${count($items)}", "[2024-01-01Z, 2024-12-31Z]");
  }

  @Test
  void testParameterizedExpression_WithTimeSequenceParameter() {
    testExpressionTranslation("count((xs:time('10:00:00Z'),xs:time('18:00:00Z')))",
        "{ND-Root, time*:$items} ${count($items)}", "[10:00:00Z, 18:00:00Z]");
  }

  @Test
  void testParameterizedExpression_WithDurationSequenceParameter() {
    testExpressionTranslation("count((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')))",
        "{ND-Root, duration*:$items} ${count($items)}", "[P1Y, P2M]");
  }

  // #endregion: Compare sequences

  // #endregion Sequence Functions

  // #region: Indexers --------------------------------------------------------

  @Test
  void testIndexer_WithNonRepeatableField() {
    // Indexing a non-repeatable field - currently allowed (semantically odd but syntactically valid)
    testExpressionTranslationWithContext("(PathNode/TextField)[1]/normalize-space(text())", "ND-Root",
        "BT-00-Text[at 1]");
  }

  @Test
  void testIndexer_WithNonRepeatableFieldAndPredicate() {
    // Indexing a non-repeatable field with predicate
    testExpressionTranslationWithContext(
        "(PathNode/TextField[./normalize-space(text()) = 'hello'])[1]/normalize-space(text())", "ND-Root",
        "BT-00-Text[BT-00-Text == 'hello'][at 1]");
  }

  @Test
  void testIndexer_WithRepeatableField() {
    // Indexing a repeatable field should work - BT-00-Repeatable-Text is repeatable
    // No explicit cast - preprocessor inserts (text*) making it stringSequence[indexer]
    testExpressionTranslationWithContext("(PathNode/RepeatableTextField)[1]/normalize-space(text())", "ND-Root",
        "BT-00-Repeatable-Text[at 1]");
  }

  @Test
  void testIndexer_WithRepeatableFieldAndPredicate() {
    // Indexing a repeatable field with predicate should work
    // No explicit cast - preprocessor handles typing
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField[(.)[1]/normalize-space(text()) = 'hello'])[1]/normalize-space(text())", "ND-Root",
        "BT-00-Repeatable-Text[BT-00-Repeatable-Text[at 1] == 'hello'][at 1]");
  }

  @Test
  void testIndexer_WithTextSequence() {
    testExpressionTranslationWithContext("('a','b','c')[1]", "ND-Root", "['a', 'b','c'][at 1]");
  }

  @Test
  void testIndexer_WithBooleanSequence() {
    testExpressionTranslationWithContext("(true(),false())[2]", "ND-Root", "[TRUE, FALSE][at 2]");
  }

  @Test
  void testIndexer_WithNumericSequence() {
    testExpressionTranslationWithContext("(1,2,3)[2]", "ND-Root", "[1, 2, 3][at 2]");
  }

  @Test
  void testIndexer_WithDateSequence() {
    testExpressionTranslationWithContext(
        "(xs:date('2020-01-01Z'),xs:date('2020-01-02Z'))[2]", "ND-Root",
        "[2020-01-01Z, 2020-01-02Z][at 2]");
  }

  @Test
  void testIndexer_WithTimeSequence() {
    testExpressionTranslationWithContext(
        "(xs:time('10:00:00Z'),xs:time('11:00:00Z'))[2]", "ND-Root",
        "[10:00:00Z, 11:00:00Z][at 2]");
  }

  @Test
  void testIndexer_WithDurationSequence() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P2M'))[2]", "ND-Root",
        "[P1D, P2M][at 2]");
  }

  // An index is computed once, where the expression stands. A position that reads no field or
  // node from the context means the same wherever it is evaluated; one that does is bound first.

  @Test
  void testIndexer_WithVariable() {
    testExpressionTranslationWithContext(
        "for $n in PathNode/NumberField/number() return (PathNode/RepeatableTextField)[$n]/normalize-space(text())",
        "ND-Root", "for number:$n in BT-00-Number return BT-00-Repeatable-Text[at $n]");
  }

  @Test
  void testIndexer_WithArithmeticOnLiterals() {
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField)[1 + 1]/normalize-space(text())", "ND-Root",
        "BT-00-Repeatable-Text[at 1 + 1]");
  }

  @Test
  void testIndexer_WithAbsoluteFieldReference() {
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField)[/*/PathNode/NumberField/number()]/normalize-space(text())",
        "ND-Root", "BT-00-Repeatable-Text[at /BT-00-Number]");
  }

  @Test
  void testIndexer_WithFieldOfContextVariable() {
    testExpressionTranslationWithContext(
        "for $c in RepeatableNode return (PathNode/RepeatableTextField)[count($c/TextField/normalize-space(text()))]/normalize-space(text())",
        "ND-Root",
        "for context:$c in ND-RepeatableNode return BT-00-Repeatable-Text[at count($c::BT-00-Text-In-Repeatable-Node)]");
  }

  @Test
  void testIndexer_ReadingAField() {
    testExpressionTranslationWithContext(
        "(for $__index in PathNode/NumberField/number() return (PathNode/RepeatableTextField)[$__index])/normalize-space(text())",
        "ND-Root", "BT-00-Repeatable-Text[at BT-00-Number]");
  }

  @Test
  void testIndexer_ComputedFromAField() {
    testExpressionTranslationWithContext(
        "(for $__index in PathNode/NumberField/number() + 1 return (PathNode/RepeatableTextField)[$__index])/normalize-space(text())",
        "ND-Root", "BT-00-Repeatable-Text[at BT-00-Number + 1]");
  }

  @Test
  void testIndexer_ComputedByAFunctionOfAField() {
    testExpressionTranslationWithContext(
        "(for $__index in count(PathNode/RepeatableNumberField/number()) return (PathNode/RepeatableTextField)[$__index])/normalize-space(text())",
        "ND-Root", "BT-00-Repeatable-Text[at count(BT-00-Repeatable-Number)]");
  }

  @Test
  void testIndexer_OnNodeReadingAField() {
    testExpressionTranslationWithContext(
        "exists((for $__index in PathNode/NumberField/number() return (RepeatableNode)[$__index]))",
        "ND-Root", "ND-RepeatableNode[at BT-00-Number] is present");
  }

  @Test
  void testIndexer_OnNodeComputedFromAField() {
    testExpressionTranslationWithContext(
        "exists((for $__index in PathNode/NumberField/number() - 1 return (RepeatableNode)[$__index]))",
        "ND-Root", "ND-RepeatableNode[at BT-00-Number - 1] is present");
  }

  @Test
  void testIndexer_OnNodeComputedByAFunctionOfAField() {
    testExpressionTranslationWithContext(
        "exists((for $__index in count(RepeatableNode/TextField/normalize-space(text())) return (RepeatableNode)[$__index]))",
        "ND-Root", "ND-RepeatableNode[at count(BT-00-Text-In-Repeatable-Node)] is present");
  }

  @Test
  void testIndexer_OnListReadingAField() {
    testExpressionTranslationWithContext(
        "(for $__index in PathNode/NumberField/number() return (1,2,3)[$__index])", "ND-Root",
        "[1, 2, 3][at BT-00-Number]");
  }

  @Test
  void testIndexer_OnListComputedFromAField() {
    testExpressionTranslationWithContext(
        "(for $__index in PathNode/NumberField/number() + 1 return (1,2,3)[$__index])", "ND-Root",
        "[1, 2, 3][at BT-00-Number + 1]");
  }

  @Test
  void testIndexer_OnListComputedByAFunctionOfAField() {
    testExpressionTranslationWithContext(
        "(for $__index in count(PathNode/RepeatableTextField/normalize-space(text())) return (1,2,3)[$__index])",
        "ND-Root", "[1, 2, 3][at count(BT-00-Repeatable-Text)]");
  }

  @Test
  void testIndexer_OnListReadingAnAbsoluteField() {
    // The target language would read the position for each value, which has no root to start from
    testExpressionTranslationWithContext(
        "(for $__index in /*/PathNode/NumberField/number() return (1,2,3)[$__index])", "ND-Root",
        "[1, 2, 3][at /BT-00-Number]");
  }

  @Test
  void testIndexer_OnListWithFieldOfContextVariable() {
    testExpressionTranslationWithContext(
        "for $c in . return (1,2,3)[$c/PathNode/NumberField/number()]",
        "ND-Root",
        "for context:$c in ND-Root return [1, 2, 3][at $c::BT-00-Number]");
  }

  @Test
  void testIndexer_OnListWithFieldContextVariable() {
    // The variable holds an element of the field: the position is the field's value read from it
    testExpressionTranslationWithContext(
        "for $c in PathNode/NumberField return (1,2,3)[$c/number()]",
        "ND-Root",
        "for context:$c in BT-00-Number return [1, 2, 3][at $c]");
  }

  @Test
  void testIndexer_OnFieldWithFieldContextVariable() {
    testExpressionTranslationWithContext(
        "for $c in PathNode/NumberField return (PathNode/RepeatableTextField)[$c/number()]/normalize-space(text())",
        "ND-Root",
        "for context:$c in BT-00-Number return BT-00-Repeatable-Text[at $c]");
  }

  @Test
  void testIndexer_OnListWithAbsoluteFieldInAFilterOfItsOwn() {
    // The filter reads the root from each of its own elements, not from the values of the list
    testExpressionTranslationWithContext(
        "for $c in . return (1,2)[count($c/PathNode/TextField[/*/PathNode/NumberField/number() = 1]/normalize-space(text()))]",
        "ND-Root",
        "for context:$c in ND-Root return [1, 2][at count($c::BT-00-Text[/BT-00-Number == 1])]");
  }

  @Test
  void testIndexer_OnListWithAbsoluteFieldBesideAFilter() {
    testExpressionTranslationWithContext(
        "for $c in . return (for $__index in count($c/PathNode/TextField[/*/PathNode/NumberField/number() = 1]/normalize-space(text())) + /*/PathNode/NumberField/number() return (1,2)[$__index])",
        "ND-Root",
        "for context:$c in ND-Root return [1, 2][at count($c::BT-00-Text[/BT-00-Number == 1]) + /BT-00-Number]");
  }

  @Test
  void testIndexer_Nested() {
    testExpressionTranslationWithContext(
        "(for $__index in (for $__index in PathNode/NumberField/number() return (PathNode/RepeatableNumberField)[$__index])/number() return (1,2,3)[$__index])",
        "ND-Root", "[1, 2, 3][at BT-00-Repeatable-Number[at BT-00-Number]]");
  }

  @Test
  void testIndexer_FollowedByAPredicate() {
    // The predicate filters the element the index selects
    testExpressionTranslationWithContext(
        "(for $__index in PathNode/NumberField/number() return (PathNode/RepeatableTextField)[$__index])[./normalize-space(text()) = 'b']/normalize-space(text())",
        "ND-Root", "BT-00-Repeatable-Text[at BT-00-Number][BT-00-Repeatable-Text == 'b']");
  }

  @Test
  void testIndexer_FollowedByAnOperator() {
    // The operator takes the element the index selects, or nothing if the position is missing
    testExpressionTranslationWithContext(
        "(for $__index in PathNode/NumberField/number() return (PathNode/RepeatableTextField)[$__index])/normalize-space(text()) = 'x' or true()",
        "ND-Root", "BT-00-Repeatable-Text[at BT-00-Number] == 'x' or TRUE");
  }

  @Test
  void testIndexer_InAPredicateReadingItsElement() {
    // The position is read where the index stands: from each element of the predicate's field
    testExpressionTranslationWithContext(
        "exists(RepeatableNode[(for $__index in NumberField/number() return (../PathNode/RepeatableTextField)[$__index])/normalize-space(text()) = 'b'])",
        "ND-Root",
        "ND-RepeatableNode[BT-00-Repeatable-Text[at BT-00-Number-In-Repeatable-Node] == 'b'] is present");
  }

  @Test
  void testIndexer_ReadingAFieldOfTheContextThatRepeats() {
    // The position is read once, from the context, before the field it selects is read
    testExpressionTranslationWithContext(
        "(for $__index in NumberField/number() + 1 return (../PathNode/RepeatableTextField)[$__index])/normalize-space(text())",
        "ND-RepeatableNode", "BT-00-Repeatable-Text[at BT-00-Number-In-Repeatable-Node + 1]");
  }

  @Test
  void testIteratorVariable_ReservedName_Throws() {
    // Names that start with __ are kept for the variables the translator generates, such as the
    // one an index binds its position to
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root",
            "for number:$__index in [1, 2] return $__index"));
    assertEquals(InvalidIdentifierException.ErrorCode.IDENTIFIER_IS_RESERVED,
        exception.getErrorCode());
    assertEquals("line 1:16 Variable '$__index' is reserved: the translator generates the names"
        + " that start with '__'.", exception.getMessage());
  }

  @Test
  void testIteratorVariable_DeclaredTwice_NamesItAsWritten() {
    final InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Text, text:$x in BT-00-Text return $x"));
    assertEquals(InvalidIdentifierException.ErrorCode.IDENTIFIER_ALREADY_DECLARED,
        exception.getErrorCode());
    assertEquals("line 1:39 Variable '$x' is already declared in this scope.",
        exception.getMessage());
  }

  @Test
  void testUndeclaredVariable_NamesItAsWritten() {
    final InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "$undeclared + 1"));
    assertEquals("line 1:12 Variable '$undeclared' is not declared.", exception.getMessage());
  }

  @Test
  void testUndeclaredFunction_NamesItAsWritten() {
    final InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "?undeclared(1)"));
    assertEquals("line 1:12 Function '?undeclared' is not declared.", exception.getMessage());
  }

  @Test
  void testNotAContextVariable_NamesItAsWritten() {
    final InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "$y::BT-00-Number"));
    assertEquals("line 1:12 Variable '$y' is not a context variable.", exception.getMessage());
  }

  // #endregion: Indexers

  // #region: Scalar/Sequence Validation --------------------------------------

  @Test
  void testScalarFromRepeatableField_ThrowsError() {
    // A repeatable field used as scalar should throw TypeMismatchException.fieldMayRepeat()
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
    assertTrue(ex.getMessage().startsWith("line "), "Error message should include source position");
  }

  @Test
  void testScalarFromFieldInRepeatableNode_ThrowsErrorFromRootContext() {
    // Field in ND-RepeatableNode (repeatable) used as scalar from ND-Root should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text-In-Repeatable-Node == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInRepeatableNode_OkFromSameContext() {
    // Field in ND-RepeatableNode used as scalar from ND-RepeatableNode context should NOT throw
    testExpressionTranslationWithContext("TextField/normalize-space(text()) = 'test'",
        "ND-RepeatableNode", "BT-00-Text-In-Repeatable-Node == 'test'");
  }

  @Test
  void testScalarFromFieldInNestedRepeatableNode_ThrowsErrorFromRootContext() {
    // Field in ND-RepeatableSubSubNode (inside ND-NonRepeatableSubNode inside ND-RepeatableNode) used from root should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text-In-RepeatableSubSubNode == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInNestedRepeatableNode_ThrowsErrorFromRepeatableNodeContext() {
    // Field in ND-RepeatableSubSubNode used from ND-RepeatableNode should still throw (ND-RepeatableSubSubNode is also repeatable)
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-RepeatableNode", "BT-00-Text-In-RepeatableSubSubNode == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInNestedRepeatableNode_OkFromRepeatableSubSubNodeContext() {
    // Field in ND-RepeatableSubSubNode used from ND-RepeatableSubSubNode context should NOT throw
    testExpressionTranslationWithContext("TextField/normalize-space(text()) = 'test'",
        "ND-RepeatableSubSubNode", "BT-00-Text-In-RepeatableSubSubNode == 'test'");
  }

  @Test
  void testScalarFromFieldInNonRepeatableNestedInRepeatable_ThrowsErrorFromRootContext() {
    // Field in ND-NonRepeatableSubNode (non-repeatable) inside ND-RepeatableNode (repeatable) used from root should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text-In-NonRepeatableSubNode == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInNonRepeatableNestedInRepeatable_OkFromRepeatableNodeContext() {
    // Field in ND-NonRepeatableSubNode used from ND-RepeatableNode context should NOT throw
    testExpressionTranslationWithContext("NonRepeatableSubNode/TextField/normalize-space(text()) = 'test'",
        "ND-RepeatableNode", "BT-00-Text-In-NonRepeatableSubNode == 'test'");
  }

  @Test
  void testScalarFromAttributeFieldInRepeatableNode_ThrowsErrorFromRootContext() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Attribute-In-Repeatable-Node == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testScalarFromAttributeFieldInRepeatableNode_OkFromSameContext() {
    testExpressionTranslationWithContext("TextField/@attribute = 'test'",
        "ND-RepeatableNode", "BT-00-Attribute-In-Repeatable-Node == 'test'");
  }

  @Test
  void testScalarFromAttributeOfRepeatableField_ThrowsError() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text/@attr == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testRepeatableFieldInUniqueCondition_ThrowsError() {
    // A repeatable field used as needle (left side) in uniqueness condition should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text is unique in /BT-00-Text"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  // #endregion: Scalar/Sequence Validation

  // #region: InvalidIdentifierException Tests --------------------------------

  @Test
  void testContextSpecifier_WithRegularVariable_ThrowsNotAContextVariable() {
    // A regular variable (not declared with context:) used as context specifier should throw
    InvalidIdentifierException ex = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "for text:$x in BT-00-Text return $x::BT-00-Number"));
    assertEquals(InvalidIdentifierException.ErrorCode.NOT_A_CONTEXT_VARIABLE, ex.getErrorCode());
    assertTrue(ex.getMessage().startsWith("line "), "Error message should include source position");
  }

  @Test
  void testContextSpecifier_UndeclaredVariable_ThrowsNotAContextVariable() {
    // An undeclared variable used as context specifier should throw (NOT_A_CONTEXT_VARIABLE
    // because the lookup for context variables fails before checking if the variable exists)
    InvalidIdentifierException ex = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "$undefined::BT-00-Text"));
    assertEquals(InvalidIdentifierException.ErrorCode.NOT_A_CONTEXT_VARIABLE, ex.getErrorCode());
  }

  // #endregion: InvalidIdentifierException Tests

  // #region: Errors raised with no position ----------------------------------

  @Test
  void testUnknownField_IsPlacedAtTheField() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateComputeExpressionWithContext("ND-Root", "BT-99-Nonexistent == 'a'"));
    assertEquals("line 1:21 Unknown symbol 'BT-99-Nonexistent'.", ex.getMessage());
  }

  @Test
  void testUnknownNode_IsPlacedAtTheNode() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateComputeExpressionWithContext("ND-Root", "ND-NONEXISTENT is present"));
    assertEquals("line 1:21 Unknown symbol 'ND-NONEXISTENT'.", ex.getMessage());
  }

  @Test
  void testUnknownCodelist_IsPlacedAtTheCodelist() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateComputeExpressionWithContext("ND-Root",
            "BT-00-Text in #nonexistent-codelist"));
    assertEquals("line 1:35 Unknown codelist 'nonexistent-codelist'.", ex.getMessage());
  }

  @Test
  void testUnsupportedRegexPattern_IsPlacedAtTheCall() {
    InvalidUsageException ex = assertThrows(InvalidUsageException.class,
        () -> translateComputeExpressionWithContext("ND-Root", "replace-regex('a', '\\w+', 'b')"));
    assertTrue(ex.getMessage().startsWith("line 1:21 Invalid regex pattern '\\w+' at position 1: "),
        ex.getMessage());
  }

  @Test
  void testUnknownFieldAsContext_IsPlacedAtTheExpression() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateExpression("WITH BT-99-Nonexistent COMPUTE 1"));
    assertEquals("line 1:0 Unknown symbol 'BT-99-Nonexistent'.", ex.getMessage());
  }

  @Test
  void testUnknownAbsoluteField_OnTheNextLine_IsPlacedAtTheField() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateExpression("WITH ND-Root COMPUTE /\nBT-99-Nonexistent is present"));
    assertEquals("line 2:0 Unknown symbol 'BT-99-Nonexistent'.", ex.getMessage());
  }

  @Test
  void testUnknownAbsoluteNode_OnTheNextLine_IsPlacedAtTheNode() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateExpression("WITH ND-Root COMPUTE /\nND-NONEXISTENT is present"));
    assertEquals("line 2:0 Unknown symbol 'ND-NONEXISTENT'.", ex.getMessage());
  }

  @Test
  void testIteratorDeclaredTwice_IsPlacedAtTheSecondIterator() {
    InvalidIdentifierException ex = assertThrows(InvalidIdentifierException.class,
        () -> translateExpression(
            "WITH ND-Root COMPUTE for text:$x in ['a'],\n text:$x in ['b'] return $x"));
    assertEquals("line 2:1 Variable '$x' is already declared in this scope.", ex.getMessage());
  }

  @Test
  void testIteratorThatCannotBeComposed_IsPlacedAtTheIterator() {
    EfxCompilationException ex = assertThrows(EfxCompilationException.class,
        () -> new EfxExpressionTranslatorV2(
            DependencyFactoryMock.INSTANCE.createSymbolResolver(getSdkVersion(), ""),
            scriptThatCannotComposeTheIteratorOf("$y"), ThrowingErrorListener.INSTANCE)
                .translateExpression(
                    "WITH ND-Root COMPUTE for text:$x in ['a'],\n text:$y in ['b'] return $x"));
    assertEquals("line 2:1 The iterator cannot be composed.", ex.getMessage());
  }

  /**
   * Gives the XPath script generator, except that it rejects the iterator of one variable with an
   * error of no position. It is not a subclass of the XPath generator, which the component factory
   * would find as a second script generator.
   */
  private static ScriptGenerator scriptThatCannotComposeTheIteratorOf(final String variable) {
    final ScriptGenerator xpath = new XPathScriptGenerator(DEFAULT_OPTIONS);
    return (ScriptGenerator) Proxy.newProxyInstance(ScriptGenerator.class.getClassLoader(),
        new Class<?>[] {ScriptGenerator.class}, (proxy, method, arguments) -> {
          if (method.getName().equals("composeIteratorExpression")
              && ((Expression) arguments[0]).getScript().equals(variable)) {
            throw new EfxCompilationException("The iterator cannot be composed.") {};
          }
          try {
            return method.invoke(xpath, arguments);
          } catch (final InvocationTargetException e) {
            throw e.getCause();
          }
        });
  }

  // #endregion: Errors raised with no position

  // #region: TypeMismatchException - nodeCannotBeValue -------------------------

  @Test
  void testScalarFromNodeContextVariable_ThrowsNodeCannotBeValue() {
    // A node context variable used as scalar value should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "for context:$n in ND-SubNode return $n == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.NODE_CONTEXT_AS_VALUE, ex.getErrorCode());
  }

  @Test
  void testCountOfNodeContextVariable_CountsItsNode() {
    // count takes nodes, so a node context variable gives its node, as count(ND-SubNode) does
    testExpressionTranslationWithContext("for $n in SubNode return count($n)", "ND-Root",
        "for context:$n in ND-SubNode return count($n)");
  }

  // #endregion: TypeMismatchException - nodeCannotBeValue

  // #region: TypeMismatchException - fieldMayRepeat (Context Variables) --------

  @Test
  void testScalarFromFieldContextVariable_NonRepeatable_Works() {
    // A non-repeatable field context variable used as scalar should work
    testExpressionTranslationWithContext(
        "for $f in PathNode/TextField return $f/normalize-space(text()) = 'test'",
        "ND-Root",
        "for context:$f in BT-00-Text return $f == 'test'");
  }

  // #endregion: TypeMismatchException - fieldMayRepeat (Context Variables)

  // #region: TypeMismatchException - messages ---------------------------------

  // Each type error says what its construct was given, and what the operand at fault must be. One
  // test for each construct pins its whole message.

  @Test
  void testTypeErrorMessage_ComparisonOperator() {
    assertTypeError(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS,
        "line 1:12 Type mismatch. Operator '==' cannot take 'text' and 'number'."
            + " Its right operand must be 'text'.",
        "'a' == 1");
  }

  @Test
  void testTypeErrorMessage_ArithmeticOperator_GivesEveryTypeThePositionTakes() {
    assertTypeError(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS,
        "line 1:12 Type mismatch. Operator '+' cannot take 'text' and 'number'."
            + " Its left operand must be 'number', 'duration' or 'date'.",
        "'a' + 1");
  }

  @Test
  void testTypeErrorMessage_Function() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Function 'number' cannot take 'date'."
            + " Its argument must be 'text' or 'indicator'.",
        "number(2020-01-01Z)");
  }

  @Test
  void testTypeErrorMessage_Function_Sequence() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Function 'upper-case' cannot take 'text*'."
            + " Its argument must be 'text'.",
        "upper-case(['a'])");
  }

  @Test
  void testTypeErrorMessage_Function_NamedAsWritten() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Function 'string' cannot take 'text'."
            + " Its argument must be 'number', 'indicator', 'date', 'time' or 'duration'.",
        "string('a')");
  }

  @Test
  void testTypeErrorMessage_ConditionOperator() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Operator 'is present' cannot take 'text'."
            + " Its operand must be a field or node reference.",
        "'a' is present");
  }

  @Test
  void testTypeErrorMessage_Conditional() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:15 Type mismatch. Conditional 'if' cannot take 'text', 'number' and 'number'."
            + " Its condition must be 'indicator'.",
        "if 'a' then 1 else 2");
  }

  @Test
  void testTypeErrorMessage_ConditionalBranches() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:34 Type mismatch. Conditional 'if' cannot take 'indicator', 'number*' and 'text*'."
            + " Its else branch must be 'number' or 'number*'.",
        "if TRUE then [1] else ['a']");
  }

  @Test
  void testTypeErrorMessage_ListWithASequenceFirst_IsReportedAtTheItem() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:13 Type mismatch. List cannot take 'number*' and 'number'."
            + " Its 1st item must be 'number'.",
        "[[1, 2], 1]");
  }

  @Test
  void testTypeErrorMessage_ListWithANodeFirst_IsReportedAtTheList() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. List cannot take 'node' and 'number'."
            + " Its 1st item must be a value.",
        "[ND-SubNode, 1]");
  }

  @Test
  void testTypeErrorMessage_List() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:16 Type mismatch. List cannot take 'number' and 'text'."
            + " Its 2nd item must be 'number'.",
        "[1, 'a']");
  }

  @Test
  void testTypeErrorMessage_Iterator() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:29 Type mismatch. Iterator 'number:$n' cannot take 'text*'."
            + " Its sequence must be 'number*'.",
        "for number:$n in ['a'] return $n");
  }

  @Test
  void testTypeErrorMessage_Predicate() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:34 Type mismatch. Predicate cannot take 'text*' and 'text'."
            + " Its condition must be 'indicator'.",
        "BT-00-Repeatable-Text[BT-00-Text]");
  }

  @Test
  void testTypeErrorMessage_PredicateWithNumber_SuggestsTheIndex() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:34 Type mismatch. Predicate cannot take 'text*' and 'number'."
            + " Its condition must be 'indicator'. To select by position, write [at n].",
        "BT-00-Repeatable-Text[1]");
  }

  @Test
  void testTypeErrorMessage_PredicateOnValue() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Predicate cannot take 'text*' and 'text'."
            + " Its sequence must be a field or node reference.",
        "['a', 'b']['x']");
  }

  @Test
  void testTypeErrorMessage_PredicateOnValueWithNumber_SuggestsTheIndex() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Predicate cannot take 'text*' and 'number'."
            + " Its sequence must be a field or node reference. To select by position, write"
            + " [at n].",
        "['a', 'b'][1]");
  }

  @Test
  void testTypeErrorMessage_Index() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:37 Type mismatch. Index cannot take 'text*' and 'text'."
            + " Its position must be 'number'.",
        "BT-00-Repeatable-Text[at 'x']");
  }

  @Test
  void testTypeErrorMessage_IndexAfterASingleValuedField_NamesItsType() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:26 Type mismatch. Index cannot take 'text' and 'indicator'."
            + " Its position must be 'number'.",
        "BT-00-Text[at TRUE]");
  }

  @Test
  void testTypeErrorMessage_IndexAfterANode_NamesItsType() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:26 Type mismatch. Index cannot take 'node' and 'indicator'."
            + " Its position must be 'number'.",
        "ND-SubNode[at TRUE] is present");
  }

  @Test
  void testTypeErrorMessage_IndexReadingARepeatableField() {
    assertTypeError(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT,
        "line 1:33 Type mismatch. Index cannot take 'node*' and 'number*'."
            + " Its position must be 'number'. Field 'BT-00-Number-In-Repeatable-Node' may return"
            + " multiple values from context 'ND-Root': use a sequence expression or change the"
            + " context.",
        "ND-RepeatableNode[at BT-00-Number-In-Repeatable-Node] is present");
  }

  @Test
  void testTypeErrorMessage_Parentheses() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Parenthesised expression cannot take 'node'."
            + " Its content must be a value or a sequence of values.",
        "(ND-SubNode)");
  }

  @Test
  void testTypeErrorMessage_StepOfAField() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Operator ':rawValue' cannot take 'text'."
            + " Its operand must be a field reference.",
        "'a':rawValue");
  }

  @Test
  void testTypeErrorMessage_FieldMayRepeat() {
    assertTypeError(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT,
        "line 1:12 Type mismatch. Operator '==' cannot take 'text*' and 'text'."
            + " Its left operand must be 'text'. Field 'BT-00-Repeatable-Text' may return"
            + " multiple values from context 'ND-Root': use a sequence expression or change the"
            + " context.",
        "BT-00-Repeatable-Text == 'x'");
  }

  @Test
  void testTypeErrorMessage_FieldMayRepeat_AbsoluteReference_NamesTheRoot() {
    assertTypeError(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT,
        "line 1:12 Type mismatch. Operator '==' cannot take 'text*' and 'text'."
            + " Its left operand must be 'text'. Field 'BT-00-Repeatable-Text' may return"
            + " multiple values from the root: use a sequence expression or change the context.",
        "/BT-00-Repeatable-Text == 'x'");
  }

  @Test
  void testTypeErrorMessage_FieldIsMultilingual() {
    assertTypeError(TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL,
        "line 1:12 Type mismatch. Operator '==' cannot take 'text*' and 'text'."
            + " Its left operand must be 'text'. Field 'BT-00-Text-Multilingual' is multilingual"
            + " and has one value per language: use :preferredLanguageText to select the"
            + " preferred language, or an index [at n] to select a specific value.",
        "BT-00-Text-Multilingual == 'x'");
  }

  @Test
  void testTypeErrorMessage_NodeContextVariable() {
    assertTypeError(TypeMismatchException.ErrorCode.NODE_CONTEXT_AS_VALUE,
        "line 1:48 Type mismatch. Operator '==' cannot take 'node' and 'text'."
            + " Its left operand must be a value. Variable '$n' refers to node"
            + " 'ND-SubNode', which has no value: only field context variables can be used as"
            + " values.",
        "for context:$n in ND-SubNode return $n == 'test'");
  }

  @Test
  void testTypeErrorCode_PairWrongInPrimitiveAndCardinality_WhicheverSideTheSequenceIsOn() {
    for (String expression : List.of("BT-00-Repeatable-Text == 1", "1 == BT-00-Repeatable-Text")) {
      TypeMismatchException ex = assertThrows(TypeMismatchException.class,
          () -> translateExpressionWithContext("ND-Root", expression));
      assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, ex.getErrorCode(),
          expression);
    }
  }

  private void assertTypeError(final TypeMismatchException.ErrorCode code, final String message,
      final String expression) {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", expression));
    assertEquals(code, ex.getErrorCode());
    assertEquals(message, ex.getMessage());
  }

  // #endregion: TypeMismatchException - messages

  // #region: Context Override Syntax Tests ------------------------------------

  @Test
  void testContextOverride_NodeContextVariable_Works() {
    // A node context variable used with context specifier syntax should work
    testExpressionTranslationWithContext(
        "for $n in SubNode return $n/SubTextField/normalize-space(text())",
        "ND-Root",
        "for context:$n in ND-SubNode return $n::BT-01-SubNode-Text");
  }

  // Note: Test 4.2 (regular variable as context specifier) is already covered by
  // testContextSpecifier_WithRegularVariable_ThrowsNotAContextVariable above

  // #endregion: Context Override Syntax Tests

  // #region: For Loop Iterator Sequence Validation -----------------------------
  // These tests verify that repeatable fields are correctly allowed when used as
  // sequences in for loop iterators, even though they would fail as scalars in return.

  @Test
  void testForLoopIterator_RepeatableFieldAsSequence_Works() {
    // A repeatable field used as a sequence in a for loop iterator should work.
    // The return expression uses the iterator variable which is scalar.
    // This is the correct way to handle repeatable fields in for loops.
    testExpressionTranslationWithContext(
        "for $b in PathNode/RepeatableTextField/normalize-space(text()) return $b",
        "ND-Root",
        "for text:$b in BT-00-Repeatable-Text return $b");
  }

  @Test
  void testForLoopIterator_RepeatableFieldWithPredicate_Works() {
    // A repeatable field with a predicate used as sequence in iterator should work.
    // This pattern is equivalent to the problematic SDK template rewritten correctly.
    testExpressionTranslationWithContext(
        "for $a in PathNode/TextField/normalize-space(text()), $b in SubNode/RepeatableInSubNode/Text[./normalize-space(text()) = $a]/normalize-space(text()) return $b",
        "ND-Root",
        "for text:$a in BT-00-Text, text:$b in BT-13-Text[BT-13-Text == $a] return $b");
  }

  // #endregion: For Loop Iterator Sequence Validation

  // #region: Predicate Comparison with Repeatable Fields -------------------------
  // These tests verify that repeatable fields in predicate comparisons are handled correctly.

  @Test
  void testPredicateComparison_RepeatableFieldAsScalar_ThrowsError() {
    // A repeatable field used as scalar in a predicate comparison should throw.
    // Pattern: FIELD[REPEATABLE_FIELD == $var] - the repeatable field is used as scalar
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text[BT-00-Repeatable-Text == 'test']"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testPredicateComparison_RepeatableFieldWithSomeSatisfies_Works() {
    // Using "some ... satisfies" to check if any value matches should work.
    // This is the correct way to compare against a repeatable field.
    // Pattern: FIELD[some text:$x in REPEATABLE_FIELD satisfies $x == $var]
    testExpressionTranslationWithContext(
        "PathNode/TextField[some $x in ../RepeatableTextField/normalize-space(text()) satisfies $x = 'test']/normalize-space(text())",
        "ND-Root",
        "BT-00-Text[some text:$x in BT-00-Repeatable-Text satisfies $x == 'test']");
  }

  // #endregion: Predicate Comparison with Repeatable Fields

  // #region: B1 Grammar Issue - Parentheses in some...in expressions --------
  // These tests verify that parentheses around field references in some...in
  // expressions are correctly parsed as sequences, not as single-element lists.
  //
  // The issue: (FIELD) in "some text:$x in (FIELD) satisfies ..." was previously
  // parsed as a scalar list instead of a parenthesized sequence reference.
  // This has been fixed by the scalar/sequence grammar separation (TEDEFO-4808).

  @Test
  void testSomeSatisfies_ParenthesizedFieldReference_String() {
    // Parentheses around a string field reference should still be treated as a sequence.
    // The parentheses are preserved in the XPath output.
    testExpressionTranslationWithContext(
        "some $x in (PathNode/RepeatableTextField/normalize-space(text())) satisfies $x = 'test'",
        "ND-Root",
        "some text:$x in (BT-00-Repeatable-Text) satisfies $x == 'test'");
  }

  @Test
  void testSomeSatisfies_ParenthesizedFieldReferenceWithPredicate_String() {
    // Parentheses around a field reference with predicate should work as sequence.
    // Note: The predicate comparison still needs some...satisfies pattern for repeatable fields.
    testExpressionTranslationWithContext(
        "some $x in (PathNode/TextField[some $y in ../RepeatableTextField/normalize-space(text()) satisfies $y = 'filter']/normalize-space(text())) satisfies $x = 'test'",
        "ND-Root",
        "some text:$x in (BT-00-Text[some text:$y in BT-00-Repeatable-Text satisfies $y == 'filter']) satisfies $x == 'test'");
  }

  @Test
  void testSomeSatisfies_ParenthesizedFieldFromRepeatableNode_String() {
    // A field from a repeatable node, wrapped in parentheses, should work as sequence.
    testExpressionTranslationWithContext(
        "some $x in (RepeatableNode/TextField/normalize-space(text())) satisfies $x = 'test'",
        "ND-Root",
        "some text:$x in (BT-00-Text-In-Repeatable-Node) satisfies $x == 'test'");
  }

  @Test
  void testSomeSatisfies_NestedParenthesizedFieldReference_String() {
    // Double parentheses should also work.
    testExpressionTranslationWithContext(
        "some $x in ((PathNode/RepeatableTextField/normalize-space(text()))) satisfies $x = 'test'",
        "ND-Root",
        "some text:$x in ((BT-00-Repeatable-Text)) satisfies $x == 'test'");
  }

  @Test
  void testForLoop_ParenthesizedFieldReference_String() {
    // Parentheses around a field reference in a for loop should work as sequence.
    testExpressionTranslationWithContext(
        "for $x in (PathNode/RepeatableTextField/normalize-space(text())) return $x",
        "ND-Root",
        "for text:$x in (BT-00-Repeatable-Text) return $x");
  }

  @Test
  void testEverySatisfies_ParenthesizedFieldReference_String() {
    // "every" quantifier with parenthesized field reference should work as sequence.
    testExpressionTranslationWithContext(
        "every $x in (PathNode/RepeatableTextField/normalize-space(text())) satisfies $x != ''",
        "ND-Root",
        "every text:$x in (BT-00-Repeatable-Text) satisfies $x != ''");
  }

  @Test
  void testCount_ParenthesizedFieldReference() {
    // count() with parenthesized field reference should work as sequence.
    testExpressionTranslationWithContext(
        "count((PathNode/RepeatableTextField/normalize-space(text())))",
        "ND-Root",
        "count((BT-00-Repeatable-Text))");
  }

  @Test
  void testDistinctValues_ParenthesizedFieldReference_String() {
    // distinct-values() with parenthesized field reference should work as sequence.
    testExpressionTranslationWithContext(
        "distinct-values((PathNode/RepeatableTextField/normalize-space(text())))",
        "ND-Root",
        "distinct-values((BT-00-Repeatable-Text))");
  }

  @Test
  void testStringJoin_ParenthesizedFieldReference() {
    // string-join() with parenthesized field reference should work as sequence.
    testExpressionTranslationWithContext(
        "string-join((PathNode/RepeatableTextField/normalize-space(text())), ', ')",
        "ND-Root",
        "string-join((BT-00-Repeatable-Text), ', ')");
  }

  // #endregion: B1 Grammar Issue

  // #region: Field Reference Structural Combinations -------------------------
  //   Tests covering fieldReference grammar structural forms
  //   (predicates, absolute refs, context overrides, indexers)
  //   in different consumer contexts (scalar, sequence, presence).

  // #region: Absolute and predicate combinations

  @Test
  void testAbsoluteFieldReference_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = /*/PathNode/TextField/normalize-space(text())",
        "BT-00-Text",
        "'test' in /BT-00-Text");
  }

  @Test
  void testAbsoluteFieldReference_InPresenceCondition() {
    testExpressionTranslationWithContext(
        "exists(/*/PathNode/TextField)",
        "BT-00-Text",
        "/BT-00-Text is present");
  }

  @Test
  void testAbsoluteFieldReference_WithPredicate() {
    testExpressionTranslationWithContext(
        "/*/PathNode/IndicatorField['a' = 'a']/(normalize-space(text()) = ('true', '1'))",
        "BT-00-Text",
        "/BT-00-Indicator['a' == 'a']");
  }

  @Test
  void testFieldReferenceWithPredicate_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextField['a' = 'a']/normalize-space(text())",
        "ND-Root",
        "'test' in BT-00-Text['a' == 'a']");
  }

  @Test
  void testFieldReferenceWithPredicate_InPresenceCondition() {
    testExpressionTranslationWithContext(
        "exists(PathNode/TextField['a' = 'a'])",
        "ND-Root",
        "BT-00-Text['a' == 'a'] is present");
  }

  @Test
  void testLinkedProperty_InScalarComparison() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/PublicationDate/xs:date(text()) = xs:date('2025-01-01Z')",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:publicationDate == 2025-01-01Z");
  }

  // Row 2 ATTR
  @Test
  void testFieldReferenceWithPredicate_InAttributeContext() {
    testExpressionTranslationWithContext(
        "PathNode/CodeField['a' = 'a']/@listName",
        "ND-Root",
        "BT-00-Code['a' == 'a']/@listName");
  }

  // Row 3 ATTR
  @Test
  void testAbsoluteFieldReference_InAttributeContext() {
    testExpressionTranslationWithContext(
        "/*/PathNode/CodeField/@listName",
        "BT-00-Text",
        "/BT-00-Code/@listName");
  }

  // Row 4 SC
  @Test
  void testAbsoluteFieldReferenceWithPredicate_InScalarContext() {
    testExpressionTranslationWithContext(
        "/*/PathNode/TextField['a' = 'a']/normalize-space(text()) = 'test'",
        "BT-00-Text",
        "/BT-00-Text['a' == 'a'] == 'test'");
  }

  // Row 4 SEQ
  @Test
  void testAbsoluteFieldReferenceWithPredicate_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = /*/PathNode/TextField['a' = 'a']/normalize-space(text())",
        "BT-00-Text",
        "'test' in /BT-00-Text['a' == 'a']");
  }

  // Row 4 ATTR
  @Test
  void testAbsoluteFieldReferenceWithPredicate_InAttributeContext() {
    testExpressionTranslationWithContext(
        "/*/PathNode/CodeField['a' = 'a']/@listName",
        "BT-00-Text",
        "/BT-00-Code['a' == 'a']/@listName");
  }

  // Row 5 SEQ
  @Test
  void testLinkedProperty_InSequenceContext() {
    final String privacyPath = "RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        "xs:date('2025-01-01Z') = " + privacyPath + "/PublicationDate/xs:date(text())",
        "ND-Root",
        "2025-01-01Z in BT-00-Text-In-Repeatable-Node:publicationDate");
  }

  // #endregion: Absolute and predicate combinations

  // #region: Context override combinations (field::, node::, $var::, stacked)

  // Row 11 SEQ
  @Test
  void testVariableContextOverride_InSequenceContext() {
    testExpressionTranslationWithContext(
        "for $n in SubNode return 'test' = $n/SubTextField/normalize-space(text())",
        "ND-Root",
        "for context:$n in ND-SubNode return 'test' in $n::BT-01-SubNode-Text");
  }

  // Row 11 PRES
  @Test
  void testVariableContextOverride_InPresenceCondition() {
    testExpressionTranslationWithContext(
        "for $n in SubNode return exists($n/SubTextField)",
        "ND-Root",
        "for context:$n in ND-SubNode return $n::BT-01-SubNode-Text is present");
  }

  // Row 11 ATTR
  @Test
  void testVariableContextOverride_InAttributeContext() {
    testExpressionTranslationWithContext(
        "for $n in . return $n/PathNode/CodeField/@listName",
        "ND-Root",
        "for context:$n in ND-Root return $n::BT-00-Code/@listName");
  }

  @Test
  void testVariableContextOverride_OfNodeReference_InCount() {
    testExpressionTranslationWithContext("for $n in SubNode return count($n/..)", "ND-Root",
        "for context:$n in ND-SubNode return count($n::ND-Root)");
  }

  @Test
  void testVariableContextOverride_OfNodeReference_InPresenceCondition() {
    testExpressionTranslationWithContext("every $n in SubNode satisfies exists($n/..)", "ND-Root",
        "every context:$n in ND-SubNode satisfies $n::ND-Root is present");
  }

  // #endregion: Context override combinations

  // #region: Indexer interactions

  @Test
  void testIndexer_MultilingualField_InScalarContext() {
    // Indexer narrows multilingual field to a single element, allowing scalar use
    testExpressionTranslationWithContext(
        "(PathNode/TextMultilingualField)[1]/normalize-space(text()) = 'test'",
        "ND-Root",
        "BT-00-Text-Multilingual[at 1] == 'test'");
  }

  // Note: testIndexer_MultilingualField_InSequenceContext is not possible because
  // the 'in' operator uses fieldReference (not fieldContext), and [indexer] is only
  // available on fieldContext. This is a grammar-level limitation.

  @Test
  void testIndexer_MultilingualFieldWithPredicate_InScalarContext() {
    testExpressionTranslationWithContext(
        "(PathNode/TextMultilingualField[./@languageID = 'eng'])[1]/normalize-space(text()) = 'test'",
        "ND-Root",
        "BT-00-Text-Multilingual[BT-00-Text-Multilingual/@languageID == 'eng'][at 1] == 'test'");
  }

  // #endregion: Indexer interactions

  // #endregion: Field Reference Structural Combinations

  // #region: fieldContext Consumer Coverage -----------------------------------
  //   Tests covering fieldContext forms (predicate, indexer, absolute)
  //   in rawValue, context iterator, and template context declaration contexts.

  // #region: rawValue with structural variations

  @Test
  void testFieldRawValue_WithIndexer() {
    // Indexer selects from the whole sequence, then rawValue appends /text()
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField)[1]/text() = 'test'",
        "ND-Root",
        "BT-00-Repeatable-Text[at 1]:rawValue == 'test'");
  }

  @Test
  // @Disabled("BUG: composeIndexer maps MultilingualStringPath back to MultilingualStringPath instead of narrowing to StringPath")
  void testFieldRawValue_MultilingualWithIndexer() {
    // Indexer should narrow multilingual to scalar, then rawValue appends /text()
    testExpressionTranslationWithContext(
        "(PathNode/TextMultilingualField)[1]/text() = 'test'",
        "ND-Root",
        "BT-00-Text-Multilingual[at 1]:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_AbsoluteReference() {
    testExpressionTranslationWithContext(
        "/*/PathNode/TextField/text() = 'test'",
        "BT-00-Text",
        "/BT-00-Text:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_FieldInRepeatableNode_WithIndexer() {
    // Indexer resolves the repeatability, rawValue appends /text()
    testExpressionTranslationWithContext(
        "(RepeatableNode/TextField)[1]/text() = 'test'",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node[at 1]:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_WithPredicateAndIndexer() {
    // Predicate, then indexer on the whole filtered sequence, then rawValue appends /text()
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField['a' = 'a'])[1]/text() = 'test'",
        "ND-Root",
        "BT-00-Repeatable-Text['a' == 'a'][at 1]:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_WithIndexer_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = (PathNode/RepeatableTextField)[1]/text()",
        "ND-Root",
        "'test' in BT-00-Repeatable-Text[at 1]:rawValue");
  }

  @Test
  void testFieldRawValue_WithPredicateAndIndexer_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = (PathNode/RepeatableTextField['a' = 'a'])[1]/text()",
        "ND-Root",
        "'test' in BT-00-Repeatable-Text['a' == 'a'][at 1]:rawValue");
  }

  @Test
  void testFieldRawValue_AbsoluteReference_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = /*/PathNode/TextField/text()",
        "BT-00-Text",
        "'test' in /BT-00-Text:rawValue");
  }

  @Test
  void testFieldRawValue_AbsoluteReferenceWithPredicate() {
    testExpressionTranslationWithContext(
        "/*/PathNode/TextField['a' = 'a']/text() = 'test'",
        "BT-00-Text",
        "/BT-00-Text['a' == 'a']:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_AbsoluteReferenceWithPredicate_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = /*/PathNode/TextField['a' = 'a']/text()",
        "BT-00-Text",
        "'test' in /BT-00-Text['a' == 'a']:rawValue");
  }

  @Test
  void testFieldRawValue_AbsoluteReferenceWithIndexer() {
    testExpressionTranslationWithContext(
        "(/*/PathNode/RepeatableTextField)[1]/text() = 'test'",
        "BT-00-Repeatable-Text",
        "/BT-00-Repeatable-Text[at 1]:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_AbsoluteReferenceWithIndexer_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = (/*/PathNode/RepeatableTextField)[1]/text()",
        "BT-00-Repeatable-Text",
        "'test' in /BT-00-Repeatable-Text[at 1]:rawValue");
  }

  // #endregion: rawValue with structural variations

  // #region: Context iterator with structural variations

  @Test
  void testContextIterator_WithIndexer() {
    // Indexer selects from the whole sequence in context iterator
    testExpressionTranslationWithContext(
        "for $x in (PathNode/RepeatableTextField)[1] return $x/normalize-space(text()) = 'test'",
        "ND-Root",
        "for context:$x in BT-00-Repeatable-Text[at 1] return $x == 'test'");
  }

  @Test
  void testContextIterator_WithPredicateAndIndexer() {
    testExpressionTranslationWithContext(
        "for $x in (PathNode/TextField['a' = 'a'])[1] return $x/normalize-space(text()) = 'test'",
        "ND-Root",
        "for context:$x in BT-00-Text['a' == 'a'][at 1] return $x == 'test'");
  }

  @Test
  void testContextIterator_WithIndexerOnTheVariable() {
    // The variable holds one value, a sequence of one
    testExpressionTranslationWithContext(
        "for $x in PathNode/RepeatableTextField return ($x/normalize-space(text()))[1] = 'test'",
        "ND-Root",
        "for context:$x in BT-00-Repeatable-Text return $x[at 1] == 'test'");
  }

  @Test
  void testContextIterator_AbsoluteReference() {
    testExpressionTranslationWithContext(
        "for $x in /*/PathNode/TextField return $x/normalize-space(text()) = 'test'",
        "BT-00-Text",
        "for context:$x in /BT-00-Text return $x == 'test'");
  }

  @Test
  void testContextIterator_AbsoluteReferenceWithPredicate() {
    testExpressionTranslationWithContext(
        "for $x in /*/PathNode/TextField['a' = 'a'] return $x/normalize-space(text()) = 'test'",
        "BT-00-Text",
        "for context:$x in /BT-00-Text['a' == 'a'] return $x == 'test'");
  }

  @Test
  void testContextIterator_AbsoluteReferenceWithIndexer() {
    testExpressionTranslationWithContext(
        "for $x in (/*/PathNode/RepeatableTextField)[1] return $x/normalize-space(text()) = 'test'",
        "BT-00-Repeatable-Text",
        "for context:$x in /BT-00-Repeatable-Text[at 1] return $x == 'test'");
  }

  @Test
  void testContextIterator_NameReusedByValueIterator() {
    // The context variable of the first iteration is out of scope in the second, where the name
    // is a text variable
    testExpressionTranslationWithContext(
        "(every $x in PathNode/RepeatableTextField satisfies $x/normalize-space(text()) = 'a') and (every $x in PathNode/RepeatableTextField/normalize-space(text()) satisfies $x = 'b')",
        "ND-Root",
        "(every context:$x in BT-00-Repeatable-Text satisfies $x == 'a') and (every text:$x in BT-00-Repeatable-Text satisfies $x == 'b')");
  }

  @Test
  void testContextIterator_AttributeFieldVariable_IsItsAttribute() {
    // The element of an attribute field is its attribute, which is its value
    testExpressionTranslationWithContext(
        "for $c in PathNode/TextField/@Attribute return $c = 'a'",
        "ND-Root",
        "for context:$c in BT-00-Attribute return $c == 'a'");
  }

  @Test
  void testContextIterator_FieldVariableInPresenceCondition_IsItsElement() {
    testExpressionTranslationWithContext(
        "for $c in PathNode/TextField return exists($c)",
        "ND-Root",
        "for context:$c in BT-00-Text return $c is present");
  }

  // #endregion: Context iterator with structural variations

  // #endregion: fieldContext Consumer Coverage

  // #region: Cardinality x Consumer Gaps -------------------------------------
  //   Tests covering cardinality (repeatable, in-rep-node, multilingual)
  //   crossed with consumer contexts where coverage was missing.

  // #region: rawValue cardinality

  @Test
  void testFieldRawValue_FieldInRepeatableNode_ScalarContext_Throws() {
    // Field in repeatable node used as rawValue scalar from root should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text-In-Repeatable-Node:rawValue == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testFieldRawValue_FieldInRepeatableNode_SequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = RepeatableNode/TextField/text()",
        "ND-Root",
        "'test' in BT-00-Text-In-Repeatable-Node:rawValue");
  }

  @Test
  void testFieldRawValue_FieldInRepeatableNode_FromOwnContext() {
    testExpressionTranslationWithContext(
        "TextField/text() = 'test'",
        "ND-RepeatableNode",
        "BT-00-Text-In-Repeatable-Node:rawValue == 'test'");
  }

  @Test
  void testFieldRawValue_RepeatableField_FromOwnContext_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = ./text()",
        "BT-00-Repeatable-Text",
        "'test' in BT-00-Repeatable-Text:rawValue");
  }

  // Matrix 3, Row 3 SEQ: Repeatable field from own context in sequence context
  @Test
  void testSequence_RepeatableFieldFromOwnContext() {
    testExpressionTranslationWithContext(
        "'test' = ./normalize-space(text())",
        "BT-00-Repeatable-Text",
        "'test' in BT-00-Repeatable-Text");
  }

  // Matrix 3, Row 4 SEQ: Field in repeatable node from root in sequence context
  @Test
  void testSequence_FieldInRepeatableNode_FromRoot() {
    testExpressionTranslationWithContext(
        "'test' = RepeatableNode/TextField/normalize-space(text())",
        "ND-Root",
        "'test' in BT-00-Text-In-Repeatable-Node");
  }

  // Matrix 3, Row 5 SEQ: Field in repeatable node from own node in sequence context
  @Test
  void testSequence_FieldInRepeatableNode_FromOwnNode() {
    testExpressionTranslationWithContext(
        "'test' = TextField/normalize-space(text())",
        "ND-RepeatableNode",
        "'test' in BT-00-Text-In-Repeatable-Node");
  }

  // Matrix 3, Row 5 RV-SEQ: Field in repeatable node from own node rawValue in sequence context
  @Test
  void testFieldRawValue_FieldInRepeatableNode_FromOwnNode_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = TextField/text()",
        "ND-RepeatableNode",
        "'test' in BT-00-Text-In-Repeatable-Node:rawValue");
  }

  // Matrix 3, Row 7 SEQ: Multilingual[pred] in sequence context
  @Test
  void testSequence_MultilingualWithPredicate() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextMultilingualField['a' = 'a']/normalize-space(text())",
        "ND-Root",
        "'test' in BT-00-Text-Multilingual['a' == 'a']");
  }

  // Matrix 3, Row 7 RV-SC: Multilingual[pred]:rawValue in scalar context — should throw FIELD_IS_MULTILINGUAL
  @Test
  void testFieldRawValue_MultilingualWithPredicate_InScalarContext_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text-Multilingual['a' == 'a']:rawValue == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL, ex.getErrorCode());
  }

  // Matrix 3, Row 7 RV-SEQ: Multilingual[pred]:rawValue in sequence context
  @Test
  void testFieldRawValue_MultilingualWithPredicate_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = PathNode/TextMultilingualField['a' = 'a']/text()",
        "ND-Root",
        "'test' in BT-00-Text-Multilingual['a' == 'a']:rawValue");
  }

  // Matrix 3, Row 8 RV-SEQ: Multilingual[idx]:rawValue in sequence context
  @Test
  void testFieldRawValue_MultilingualWithIndexer_InSequenceContext() {
    testExpressionTranslationWithContext(
        "'test' = (PathNode/TextMultilingualField)[1]/text()",
        "ND-Root",
        "'test' in BT-00-Text-Multilingual[at 1]:rawValue");
  }

  // #endregion: rawValue cardinality

  // #region: Privacy property cardinality

  @Test
  void testComputedProperty_wasWithheld_FieldInRepeatableNode_FromRoot() {
    testExpressionTranslationWithContext(
        "RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']/FieldIdentifierCode/normalize-space(text()) = 'test-priv'",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:wasWithheld");
  }

  @Test
  void testComputedProperty_isWithheld_FieldInRepeatableNode_FromRoot() {
    final String privacyPath = "RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "(not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date())",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:isWithheld");
  }

  @Test
  void testComputedProperty_isDisclosed_FieldInRepeatableNode_FromRoot() {
    final String privacyPath = "RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "not((not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date()))"
            + " and "
            + "not(RepeatableNode/TextField/normalize-space(text()) = 'unpublished')",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:isDisclosed");
  }

  @Test
  void testComputedProperty_isMasked_FieldInRepeatableNode_FromRoot() {
    final String privacyPath = "RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "RepeatableNode/TextField/normalize-space(text()) = 'unpublished'",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:isMasked");
  }

  // Matrix 4, Row 1 PC: privacyCode on withholdable scalar field
  @Test
  void testComputedProperty_privacyCode_WithholdableField() {
    testExpressionTranslationWithContext(
        "'test-priv'",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:privacyCode");
  }

  // Matrix 4, Row 2 PC: privacyCode on non-withholdable field — should throw FIELD_NOT_WITHHOLDABLE
  @Test
  void testComputedProperty_privacyCode_NonWithholdableField_Throws() {
    InvalidUsageException ex = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:privacyCode"));
    assertEquals(InvalidUsageException.ErrorCode.FIELD_NOT_WITHHOLDABLE, ex.getErrorCode());
  }

  // Matrix 4, Row 3 IH: isWithholdable on repeatable field from root
  @Test
  void testComputedProperty_isWithholdable_RepeatableFromRoot() {
    testExpressionTranslationWithContext(
        "true()",
        "ND-Root",
        "BT-00-Repeatable-Text:isWithholdable");
  }

  // Matrix 4, Row 3 ID: isDisclosed on repeatable field from root
  @Test
  void testComputedProperty_isDisclosed_RepeatableFromRoot() {
    final String privacyPath = "PathNode/FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'"
            + " and "
            + "not((not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date()))"
            + " and "
            + "not(PathNode/RepeatableTextField/normalize-space(text()) = 'unpublished')",
        "ND-Root",
        "BT-00-Repeatable-Text:isDisclosed");
  }

  // Matrix 4, Row 4 ID: isDisclosed on repeatable field from own context
  @Test
  void testComputedProperty_isDisclosed_RepeatableFromOwnContext() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'"
            + " and "
            + "not((not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date()))"
            + " and "
            + "not(./normalize-space(text()) = 'unpublished')",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:isDisclosed");
  }

  // Matrix 4, Row 4 IM: isMasked on repeatable field from own context
  @Test
  void testComputedProperty_isMasked_RepeatableFromOwnContext() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='rep-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'rep-text-priv'"
            + " and "
            + "./normalize-space(text()) = 'unpublished'",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:isMasked");
  }

  // Matrix 4, Row 5 IH: isWithholdable on field in repeatable node from root
  @Test
  void testComputedProperty_isWithholdable_FieldInRepeatableNode_FromRoot() {
    testExpressionTranslationWithContext(
        "true()",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:isWithholdable");
  }

  // Matrix 4, Row 6 WW: wasWithheld on multilingual field
  @Test
  void testComputedProperty_wasWithheld_Multilingual() {
    testExpressionTranslationWithContext(
        "PathNode/FieldsPrivacy[FieldIdentifierCode/text()='ml-text-priv']/FieldIdentifierCode/normalize-space(text()) = 'ml-text-priv'",
        "ND-Root",
        "BT-00-Text-Multilingual:wasWithheld");
  }

  // Matrix 4, Row 6 IW: isWithheld on multilingual field
  @Test
  void testComputedProperty_isWithheld_Multilingual() {
    final String privacyPath = "PathNode/FieldsPrivacy[FieldIdentifierCode/text()='ml-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'ml-text-priv'"
            + " and "
            + "(not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date())",
        "ND-Root",
        "BT-00-Text-Multilingual:isWithheld");
  }

  // Matrix 4, Row 6 ID: isDisclosed on multilingual field
  @Test
  void testComputedProperty_isDisclosed_Multilingual() {
    final String privacyPath = "PathNode/FieldsPrivacy[FieldIdentifierCode/text()='ml-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'ml-text-priv'"
            + " and "
            + "not((not(exists(" + privacyPath + "/PublicationDate))"
            + " or "
            + privacyPath + "/PublicationDate/xs:date(text()) > current-date()))"
            + " and "
            + "not(PathNode/TextMultilingualField/normalize-space(text()) = 'unpublished')",
        "ND-Root",
        "BT-00-Text-Multilingual:isDisclosed");
  }

  // Matrix 4, Row 6 IM: isMasked on multilingual field
  @Test
  void testComputedProperty_isMasked_Multilingual() {
    final String privacyPath = "PathNode/FieldsPrivacy[FieldIdentifierCode/text()='ml-text-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'ml-text-priv'"
            + " and "
            + "PathNode/TextMultilingualField/normalize-space(text()) = 'unpublished'",
        "ND-Root",
        "BT-00-Text-Multilingual:isMasked");
  }

  // Matrix 4, Row 3 PC: privacyCode on repeatable field from root
  @Test
  void testComputedProperty_privacyCode_RepeatableFromRoot() {
    testExpressionTranslationWithContext(
        "'rep-text-priv'",
        "ND-Root",
        "BT-00-Repeatable-Text:privacyCode");
  }

  // Matrix 4, Row 4 IH: isWithholdable on repeatable field from own context
  @Test
  void testComputedProperty_isWithholdable_RepeatableFromOwnContext() {
    testExpressionTranslationWithContext(
        "true()",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:isWithholdable");
  }

  // Matrix 4, Row 4 PC: privacyCode on repeatable field from own context
  @Test
  void testComputedProperty_privacyCode_RepeatableFromOwnContext() {
    testExpressionTranslationWithContext(
        "'rep-text-priv'",
        "BT-00-Repeatable-Text",
        "BT-00-Repeatable-Text:privacyCode");
  }

  // Matrix 4, Row 5 PC: privacyCode on field in repeatable node from root
  @Test
  void testComputedProperty_privacyCode_FieldInRepeatableNode_FromRoot() {
    testExpressionTranslationWithContext(
        "'test-priv'",
        "ND-Root",
        "BT-00-Text-In-Repeatable-Node:privacyCode");
  }

  // Matrix 4, Row 6 IH: isWithholdable on multilingual field
  @Test
  void testComputedProperty_isWithholdable_Multilingual() {
    testExpressionTranslationWithContext(
        "true()",
        "ND-Root",
        "BT-00-Text-Multilingual:isWithholdable");
  }

  // Matrix 4, Row 6 PC: privacyCode on multilingual field — succeeds (compile-time constant)
  @Test
  void testComputedProperty_privacyCode_Multilingual() {
    testExpressionTranslationWithContext(
        "'ml-text-priv'",
        "ND-Root",
        "BT-00-Text-Multilingual:privacyCode");
  }

  // #endregion: Privacy property cardinality

  // #endregion: Cardinality x Consumer Gaps

  // #region: EFX-2 COMPUTE syntax ---------------------------------------------

  @Test
  void testCompute_BooleanExpression() {
    testComputeExpressionWithContext("(true() or true()) and false()", "BT-00-Text",
        "(ALWAYS or TRUE) and NEVER");
  }

  @Test
  void testCompute_PresenceCondition() {
    testComputeExpressionWithContext("exists(PathNode/TextField)", "ND-Root", "BT-00-Text is present");
  }

  @Test
  void testCompute_NumericExpression() {
    testComputeExpressionWithContext("1 + 2", "BT-00-Text", "1 + 2");
  }

  @Test
  void testCompute_StringComparison() {
    testComputeExpressionWithContext("'abc' = 'def'", "BT-00-Text", "'abc' == 'def'");
  }

  @Test
  void testCompute_FieldReference() {
    testComputeExpressionWithContext("PathNode/TextField/normalize-space(text())", "ND-Root",
        "BT-00-Text");
  }

  @Test
  void testCompute_WithNodeContext() {
    testComputeExpressionWithContext("exists(PathNode/TextField)", "ND-Root", "BT-00-Text is present");
  }

  @Test
  void testCompute_WithParameters() {
    testExpressionTranslation("'hello' = 'world'",
        "WITH ND-Root, text:$p1, text:$p2 COMPUTE $p1 == $p2", "'hello'", "'world'");
  }

  @Test
  void testCompute_WithNumericParameters() {
    testExpressionTranslation("1 = 2",
        "WITH ND-Root, number:$p1, number:$p2 COMPUTE $p1 == $p2", "1", "2");
  }

  @Test
  void testCompute_WithDateParameters() {
    testExpressionTranslation("xs:date('2018-01-01Z') = xs:date('2020-01-01Z')",
        "WITH ND-Root, date:$p1, date:$p2 COMPUTE $p1 == $p2", "2018-01-01Z", "2020-01-01Z");
  }

  @Test
  void testCompute_WithSequenceParameter() {
    testExpressionTranslation("count(('a','b','c'))",
        "WITH ND-Root, text*:$items COMPUTE count($items)", "['a', 'b', 'c']");
  }

  @Test
  void testCompute_ParameterValueFollowedByAComment() {
    testExpressionTranslation("1", "WITH ND-Root, number:$p COMPUTE $p", "1 // a comment");
  }

  @Test
  void testCompute_ParameterValueWithASyntaxError_IsEmptyBehindAListenerThatDoesNotThrow() {
    final List<String> syntaxErrors = new ArrayList<>();
    final String translation = translateExpressionBehindAListenerThatDoesNotThrow(syntaxErrors,
        "WITH ND-Root, text:$p COMPUTE $p", "}");
    assertEquals(List.of("Syntax error: unexpected '}' in parameter value."), syntaxErrors);
    assertEquals("", translation);
  }

  @Test
  void testCompute_ParameterValueWithAnUnknownCharacter_IsEmptyBehindAListenerThatDoesNotThrow() {
    final List<String> syntaxErrors = new ArrayList<>();
    final String translation = translateExpressionBehindAListenerThatDoesNotThrow(syntaxErrors,
        "WITH ND-Root, number:$p COMPUTE $p", "1¤");
    assertEquals(List.of("Syntax error: unexpected character '¤'."), syntaxErrors);
    assertEquals("", translation);
  }

  /**
   * Translates an expression behind an error listener that collects the messages of the syntax
   * errors and does not throw.
   */
  private String translateExpressionBehindAListenerThatDoesNotThrow(
      final List<String> syntaxErrors, final String expression, final String... parameterValues) {
    return new EfxExpressionTranslatorV2(
        DependencyFactoryMock.INSTANCE.createSymbolResolver(getSdkVersion(), ""),
        DependencyFactoryMock.INSTANCE.createScriptGenerator(getSdkVersion(), "", DEFAULT_OPTIONS),
        new BaseErrorListener() {
          @Override
          public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
              final int line, final int column, final String message,
              final RecognitionException exception) {
            syntaxErrors.add(message);
          }
        }).translateExpression(expression, parameterValues);
  }

  @Test
  void testCompute_KeywordsInUpperCaseOnly() {
    testComputeExpressionWithContext("(true() or true()) and false()", "BT-00-Text",
        "(ALWAYS or TRUE) and NEVER");
    assertSyntaxError(() -> translateExpression("with BT-00-Text compute TRUE"));
    assertSyntaxError(() -> translateExpression("WITH BT-00-Text compute TRUE"));
  }

  // #endregion: EFX-2 COMPUTE syntax

  // #region: Selectors -------------------------------------------------------

  /**
   * A selector yields the same path an expression would use, without the value step that an
   * expression appends. It is relative or absolute according to how the reference was written.
   */
  @Test
  void testSelector_YieldsTheSamePathAsTheEquivalentExpression() {
    assertEquals(translateExpression("WITH ND-Root COMPUTE BT-00-Text"),
        translateExpression("WITH ND-Root SELECT BT-00-Text") + "/normalize-space(text())");
    assertEquals(translateExpression("WITH ND-Root COMPUTE BT-00-Integer"),
        translateExpression("WITH ND-Root SELECT BT-00-Integer") + "/number()");
    assertEquals(translateExpression("WITH ND-SubNode COMPUTE BT-00-Text"),
        translateExpression("WITH ND-SubNode SELECT BT-00-Text") + "/normalize-space(text())");
  }

  /** Both spellings of the selector must produce the same result. */
  @Test
  void testSelector_BothSpellingsAgree() {
    assertEquals(translateExpression("WITH ND-Root SELECT /BT-00-Text"),
        translateExpression("{ND-Root} &{/BT-00-Text}"));
    assertEquals(translateExpression("WITH ND-Root SELECT /BT-00-Text[BT-00-Code == 'x']"),
        translateExpression("{ND-Root} &{/BT-00-Text[BT-00-Code == 'x']}"));
  }

  @Test
  void testSelector_FieldReference() {
    testExpressionTranslation("PathNode/TextField", "WITH ND-Root SELECT BT-00-Text");
  }

  @Test
  void testSelector_AbsoluteFieldReference() {
    testExpressionTranslation("/*/PathNode/TextField", "WITH ND-Root SELECT /BT-00-Text");
  }

  @Test
  void testSelector_WithPredicate() {
    testExpressionTranslation("/*/PathNode/TextField[../CodeField/normalize-space(text()) = 'x']",
        "WITH ND-Root SELECT /BT-00-Text[BT-00-Code == 'x']");
  }

  @Test
  void testSelector_NumericFieldHasNoValueStep() {
    testExpressionTranslation("PathNode/IntegerField", "WITH ND-Root SELECT BT-00-Integer");
  }

  @Test
  void testSelector_DurationFieldHasNoValueStep() {
    testExpressionTranslation("PathNode/DurationField", "WITH ND-Root SELECT BT-00-Duration");
  }

  @Test
  void testSelector_NodeReference() {
    testExpressionTranslation("SubNode", "WITH ND-Root SELECT ND-SubNode");
    testExpressionTranslation("/*/SubNode", "WITH ND-Root SELECT /ND-SubNode");
  }

  @Test
  void testSelector_AttributeReference() {
    testExpressionTranslation("/*/PathNode/TextField/@Attribute",
        "WITH ND-Root SELECT /BT-00-Text/@Attribute");
  }

  @Test
  void testSelector_IsRelativeToTheDeclaredContext() {
    testExpressionTranslation("../PathNode/TextField", "WITH ND-SubNode SELECT BT-00-Text");
    testExpressionTranslation("/*/PathNode/TextField", "WITH ND-SubNode SELECT /BT-00-Text");
  }

  @Test
  void testSelector_KeywordInLowerCase_IsRejected() {
    assertSyntaxError(() -> translateExpression("WITH ND-Root select BT-00-Text"));
  }

  @Test
  void testSelector_RejectsValueExpression() {
    assertSyntaxError(() -> translateExpression("WITH ND-Root SELECT BT-00-Text == 'x'"));
  }


  /**
   * A selector's indexer, like every indexer in the language, selects the nth item of the whole
   * sequence the path selects, not the nth occurrence within each parent. An indexed selector is
   * therefore the indexed expression without its value step.
   */
  @Test
  void testSelector_IndexerSelectsFromWholeSequence() {
    testExpressionTranslation("(PathNode/TextField)[1]", "WITH ND-Root SELECT BT-00-Text[at 1]");
    testExpressionTranslation("(PathNode/TextField)[1]/normalize-space(text())",
        "WITH ND-Root COMPUTE BT-00-Text[at 1]");
  }

  @Test
  void testSelector_IndexerAfterPredicate() {
    testExpressionTranslation(
        "(/*/PathNode/TextField[../CodeField/normalize-space(text()) = 'x'])[1]",
        "WITH ND-Root SELECT /BT-00-Text[BT-00-Code == 'x'][at 1]");
  }

  // #endregion: Selectors ----------------------------------------------------
}
