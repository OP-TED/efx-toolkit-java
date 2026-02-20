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
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;

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
  void testPresenceCondition() {
    testExpressionTranslationWithContext("PathNode/TextField", "ND-Root", "BT-00-Text is present");
  }

  @Test
  void testPresenceCondition_WithNot() {
    testExpressionTranslationWithContext("not(PathNode/TextField)", "ND-Root",
        "BT-00-Text is not present");
  }

  @Test
  void testUniqueValueCondition() {
    testExpressionTranslationWithContext(
        "count(for $n in PathNode/TextField/normalize-space(text()), $x in /*/PathNode/TextField/normalize-space(text())[. = $n] return $x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Text");
  }

  @Test
  void testUniqueValueCondition_WithNot() {
    testExpressionTranslationWithContext(
        "not(count(for $n in PathNode/TextField/normalize-space(text()), $x in /*/PathNode/TextField/normalize-space(text())[. = $n] return $x) = 1)",
        "ND-Root", "BT-00-Text is not unique in /BT-00-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $n in 'b', $x in ('a','b','c','b')[. = $n] return $x) = 1",
        "BT-00-Text", "'b' is unique in ['a', 'b', 'c', 'b']");
  }

  @Test
  void testNumericUniqueValueCondition_WithLiteralSequence() {
    testExpressionTranslationWithContext(
        "count(for $n in 2, $x in (1,2,3,2)[. = $n] return $x) = 1",
        "BT-00-Number", "2 is unique in [1, 2, 3, 2]");
  }

  @Test
  void testStringUniqueValueCondition_WithRepeatableField() {
    testExpressionTranslationWithContext(
        "count(for $n in PathNode/TextField/normalize-space(text()), $x in /*/PathNode/RepeatableTextField/normalize-space(text())[. = $n] return $x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Repeatable-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithNot() {
    testExpressionTranslationWithContext(
        "not(count(for $n in 'x', $x in ('a','b','c')[. = $n] return $x) = 1)",
        "BT-00-Text", "'x' is not unique in ['a', 'b', 'c']");
  }

  @Test
  void testStringUniqueValueCondition_WithRelativeFieldReference() {
    testExpressionTranslationWithContext(
        "count(for $n in PathNode/TextField/normalize-space(text()), $x in PathNode/RepeatableTextField/normalize-space(text())[. = $n] return $x) = 1",
        "ND-Root", "BT-00-Text is unique in BT-00-Repeatable-Text");
  }

  @Test
  void testStringUniqueValueCondition_WithFieldReferencePredicate() {
    testExpressionTranslationWithContext(
        "count(for $n in PathNode/TextField/normalize-space(text()), $x in /*/PathNode/RepeatableTextField[./normalize-space(text()) != '']/normalize-space(text())[. = $n] return $x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Repeatable-Text[BT-00-Repeatable-Text != '']");
  }

  @Test
  void testStringUniqueValueCondition_WithFieldInRepeatableNodePredicate() {
    testExpressionTranslationWithContext(
        "count(for $n in PathNode/TextField/normalize-space(text()), $x in /*/RepeatableNode/TextField[./normalize-space(text()) != '']/normalize-space(text())[. = $n] return $x) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Text-In-Repeatable-Node[BT-00-Text-In-Repeatable-Node != '']");
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
  void testLikePatternCondition_WithTextMultilingualField() {
    testExpressionTranslation("every $lang in PathNode/TextMultilingualField/@languageID satisfies fn:matches(normalize-space(PathNode/TextMultilingualField[./@languageID = $lang]/normalize-space(text())), '[0-9]*')",
        "{ND-Root} ${every text:$lang in BT-00-Text-Multilingual/@languageID satisfies BT-00-Text-Multilingual[BT-00-Text-Multilingual/@languageID == $lang]  like '[0-9]*'}");
  }

  @Test
  void testPreferredLanguage_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "preferred-language(BT-00-Text-Multilingual)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testPreferredLanguageText_ThrowsInExpressionContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateExpressionWithContext("ND-Root", "preferred-language-text(BT-00-Text-Multilingual)"));
    assertEquals(InvalidUsageException.ErrorCode.TEMPLATE_ONLY_FUNCTION, exception.getErrorCode());
  }

  @Test
  void testFieldValueComparison_UsingTextFields() {
    testExpressionTranslationWithContext(
        "PathNode/TextField/normalize-space(text()) = PathNode/TextMultilingualField/normalize-space(text())",
        "Root", "textField == textMultilingualField");
  }

  @Test
  void testFieldValueComparison_UsingNumericFields() {
    testExpressionTranslationWithContext(
        "PathNode/NumberField/number() <= PathNode/IntegerField/number()", "ND-Root",
        "BT-00-Number <= integerField");
  }

  @Test
  void testFieldValueComparison_UsingIndicatorFields() {
    testExpressionTranslationWithContext("PathNode/IndicatorField != PathNode/IndicatorField",
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
  void testFieldValueComparison_UsingMeasureFields() {
    assertEquals(
        "boolean(for $T in (current-date()) return ($T + (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) <= $T + (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ()))))",
        translateExpressionWithContext("ND-Root", "BT-00-Measure <= BT-00-Measure"));
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
        "integerField - BT-00-Number > 0");
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
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("ND-Root", "00:01:00 > BT-00-StartDate"));
  }


  @Test
  void testBooleanComparison_UsingLiterals() {
    testExpressionTranslationWithContext("false() != true()", "BT-00-Text", "NEVER != ALWAYS");
  }

  @Test
  void testBooleanComparison_UsingFieldReference() {
    testExpressionTranslationWithContext("../IndicatorField != true()", "BT-00-Text",
        "BT-00-Indicator != ALWAYS");
  }

  @Test
  void testNumericComparison() {
    testExpressionTranslationWithContext(
        "2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5 and ../NumberField/number() > ../IntegerField/number()",
        "BT-00-Text", "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5 and BT-00-Number > BT-00-Integer");
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
  void testDurationComparison_UsingYearMOnthDurationLiterals() {
    testExpressionTranslationWithContext(
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P1Y') = $T + xs:yearMonthDuration('P12M')))",
        "BT-00-Text", "P1Y == P12M");
  }

  @Test
  void testDurationComparison_UsingDayTimeDurationLiterals() {
    testExpressionTranslationWithContext(
        "boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P21D') > $T + xs:dayTimeDuration('P7D')))",
        "BT-00-Text", "P3W > P7D");
  }

  @Test
  void testCalculatedDurationComparison() {
    testExpressionTranslationWithContext(
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P3M') > $T + xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text()))))",
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
        "(-3 * (2 * (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ()))))",
        translateExpressionWithContext("ND-Root", "2 * (measure)BT-00-Measure * -3"));
  }

  @Test
  void testDurationAddition() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P3D') + xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
        "ND-Root", "P3D + (BT-00-StartDate - BT-00-EndDate)");
  }

  @Test
  void testDurationSubtraction() {
    testExpressionTranslationWithContext(
        "(xs:dayTimeDuration('P3D') - xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
        "ND-Root", "P3D - (BT-00-StartDate - BT-00-EndDate)");
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
    testExpressionTranslationWithContext("every $x in PathNode/IndicatorField satisfies $x",
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
        "every $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')) satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "every measure:$x in [P1D, P2D, P3D] satisfies $x <= P1D");
  }

  @Test
  void testDurationQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "every $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "every measure:$x in BT-00-Measure satisfies $x <= P1D");
  }

  // #endregion: Quantified expressions

  // #region: Conditional expressions -----------------------------------------

  @Test
  void testConditionalExpression() {
    testExpressionTranslationWithContext("(if 1 > 2 then 'a' else 'b')", "ND-Root",
        "if 1 > 2 then 'a' else 'b'");
  }

  @Test
  void testConditionalStringExpression_UsingLiterals() {
    testExpressionTranslationWithContext("(if 'a' > 'b' then 'a' else 'b')", "ND-Root",
        "if 'a' > 'b' then 'a' else 'b'");
  }

  @Test
  void testConditionalStringExpression_UsingFieldReferenceInCondition() {
    testExpressionTranslationWithContext(
        "(if 'a' > PathNode/TextField/normalize-space(text()) then 'a' else 'b')", "ND-Root",
        "if 'a' > BT-00-Text then 'a' else 'b'");
    testExpressionTranslationWithContext(
        "(if PathNode/TextField/normalize-space(text()) >= 'a' then 'a' else 'b')", "ND-Root",
        "if BT-00-Text >= 'a' then 'a' else 'b'");
    testExpressionTranslationWithContext(
        "(if PathNode/TextField/normalize-space(text()) >= PathNode/TextField/normalize-space(text()) then 'a' else 'b')",
        "ND-Root", "if BT-00-Text >= BT-00-Text then 'a' else 'b'");
    testExpressionTranslationWithContext(
        "(if PathNode/StartDateField/xs:date(text()) >= PathNode/EndDateField/xs:date(text()) then 'a' else 'b')",
        "ND-Root", "if BT-00-StartDate >= BT-00-EndDate then 'a' else 'b'");
  }

  @Test
  void testConditionalStringExpression_UsingFieldReference() {
    testExpressionTranslationWithContext(
        "(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else 'b')", "ND-Root",
        "if 'a' > 'b' then BT-00-Text else 'b'");
    testExpressionTranslationWithContext(
        "(if 'a' > 'b' then 'a' else PathNode/TextField/normalize-space(text()))", "ND-Root",
        "if 'a' > 'b' then 'a' else BT-00-Text");
    testExpressionTranslationWithContext(
        "(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else PathNode/TextField/normalize-space(text()))",
        "ND-Root", "if 'a' > 'b' then BT-00-Text else BT-00-Text");
  }

  @Test
  void testConditionalStringExpression_UsingFieldReferences_TypeMismatch() {
    assertThrows(ParseCancellationException.class, () -> translateExpressionWithContext("ND-Root",
        "if 'a' > 'b' then BT-00-StartDate else BT-00-Text"));
  }

  @Test
  void testConditionalBooleanExpression() {
    testExpressionTranslationWithContext("(if PathNode/IndicatorField then true() else false())",
        "ND-Root", "if BT-00-Indicator then TRUE else FALSE");
  }

  @Test
  void testConditionalNumericExpression() {
    testExpressionTranslationWithContext("(if 1 > 2 then 1 else PathNode/NumberField/number())",
        "ND-Root", "if 1 > 2 then 1 else BT-00-Number");
  }

  @Test
  void testConditionalDateExpression() {
    testExpressionTranslationWithContext(
        "(if xs:date('2012-01-01Z') > PathNode/EndDateField/xs:date(text()) then PathNode/StartDateField/xs:date(text()) else xs:date('2012-01-02Z'))",
        "ND-Root", "if 2012-01-01Z > BT-00-EndDate then BT-00-StartDate else 2012-01-02Z");
  }

  @Test
  void testConditionalTimeExpression() {
    testExpressionTranslationWithContext(
        "(if PathNode/EndTimeField/xs:time(text()) > xs:time('00:00:01Z') then PathNode/StartTimeField/xs:time(text()) else xs:time('00:00:01Z'))",
        "ND-Root", "if BT-00-EndTime > 00:00:01Z then BT-00-StartTime else 00:00:01Z");
  }

  @Test
  void testConditionalDurationExpression() {
    assertEquals(
        "(if boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P1D') > $T + (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())))) then xs:dayTimeDuration('P1D') else xs:dayTimeDuration('P2D'))",
        translateExpressionWithContext("ND-Root", "if P1D > BT-00-Measure then P1D else P2D"));
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
        "'a' = (for $x in ('a','b','c'), $y in (1,2), $z in PathNode/IndicatorField return concat($x, string($y), 'text'))",
        "ND-Root",
        "'a' in (for text:$x in ['a', 'b', 'c'], number:$y in [1, 2], indicator:$z in BT-00-Indicator return concat($x, string($y), 'text'))");
  }

  @Test
  void testStringsSequenceFromIteration_UsingObjectVariable() {
    testExpressionTranslationWithContext(
        "for $n in PathNode/TextField[../NumberField], $d in $n/../StartDateField/xs:date(text()) return 'text'",
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
    testExpressionTranslationWithContext("'a' = (for $x in PathNode/IndicatorField return 'y')",
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
        "ND-Root", "'a' in (for measure:$x in [P1D, P1Y, P2M] return 'y')");
  }


  @Test
  void testStringsFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return 'y')",
        "ND-Root", "'a' in (for measure:$x in BT-00-Measure return 'y')");
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
    testExpressionTranslationWithContext("123 = (for $x in PathNode/IndicatorField return 0)",
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
        "ND-Root", "123 in (for measure:$x in [P1D, P1Y, P2M] return 0)");
  }


  @Test
  void testNumbersFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return 0)",
        "ND-Root", "123 in (for measure:$x in BT-00-Measure return 0)");
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
        "xs:date('2022-01-01Z') = (for $x in PathNode/IndicatorField return xs:date('2022-01-01Z'))",
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
        "ND-Root", "2022-01-01Z in (for measure:$x in [P1D, P1Y, P2M] return 2022-01-01Z)");
  }


  @Test
  void testDatesFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:date('2022-01-01Z') = (for $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for measure:$x in BT-00-Measure return 2022-01-01Z)");
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
        "xs:time('12:00:00Z') = (for $x in PathNode/IndicatorField return xs:time('12:00:00Z'))",
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
        "ND-Root", "12:00:00Z in (for measure:$x in [P1D, P1Y, P2M] return 12:00:00Z)");
  }


  @Test
  void testTimesFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:time('12:00:00Z') = (for $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for measure:$x in BT-00-Measure return 12:00:00Z)");
  }

  // Durations from iteration ---------------------------------------------------

  @Test
  void testDurationsFromStringIteration_UsingLiterals() {
    testExpressionTranslationWithContext(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')) return $x)",
        "ND-Root", "P1D in (for measure:$x in [P1D, P2D, P1W] return $x)");
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
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/IndicatorField return xs:dayTimeDuration('P1D'))",
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
        "ND-Root", "P1D in (for measure:$x in [P1D, P1Y, P2M] return P1D)");
  }

  @Test
  void testDurationsFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:dayTimeDuration('P1D') = (for $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for measure:$x in BT-00-Measure return P1D)");
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
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("BT-00-Text", "4 in [1, BT-00-Text, 3]"));
  }

  @Test
  void testBooleanList() {
    testExpressionTranslationWithContext("false() = (true(),PathNode/IndicatorField,true())",
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
        "(for $F in ../MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        translateExpressionWithContext("BT-00-Text", "BT-00-Measure in [P1M, P3M, P6M]"));
  }

  @Test
  void testCodeList() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in #accessibility");
  }

  @Test
  void testCodeList_WithNumericSuffix() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in #legal-basis-1");
  }

  @Test
  void testCodeList_WithKeywordName() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in #indicator");
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
  void testFieldReferenceWithPredicate() {
    testExpressionTranslationWithContext("PathNode/IndicatorField['a' = 'a']", "ND-Root",
        "BT-00-Indicator['a' == 'a']");
  }

  @Test
  void testFieldReferenceWithPredicate_WithFieldReferenceInPredicate() {
    testExpressionTranslationWithContext(
        "PathNode/IndicatorField[../CodeField/normalize-space(text()) = 'a']", "ND-Root",
        "BT-00-Indicator[BT-00-Code == 'a']");
  }

  @Test
  void testFieldReferenceInOtherNotice() {
    testExpressionTranslationWithContext(
        "fn:doc(concat($urlPrefix, 'da4d46e9-490b-41ff-a2ae-8166d356a619'))/*/PathNode/TextField/normalize-space(text())",
        "ND-Root", "notice('da4d46e9-490b-41ff-a2ae-8166d356a619')/BT-00-Text");
  }

  @Test
  void testFieldReferenceInOtherNotice_UsingAReference() {
    testExpressionTranslationWithContext(
        "fn:doc(concat($urlPrefix, /*/PathNode/IdField/normalize-space(text())))/*/PathNode/TextField/normalize-space(text())",
        "ND-Root", "notice(BT-00-Identifier)/BT-00-Text");
  }

  @Test
  void testFieldReferenceWithFieldContextOverride() {
    testExpressionTranslationWithContext("../TextField/normalize-space(text())", "BT-00-Code",
        "BT-01-SubLevel-Text::BT-00-Text");
  }

  @Test
  void testFieldReferenceWithFieldContextOverride_WithIntegerField() {
    testExpressionTranslationWithContext("../IntegerField/number()", "BT-00-Code",
        "BT-01-SubLevel-Text::integerField");
  }

  @Test
  void testFieldReferenceWithNodeContextOverride() {
    testExpressionTranslationWithContext("../../PathNode/IntegerField/number()", "BT-00-Text",
        "ND-Root::integerField");
  }

  @Test
  void testFieldReferenceWithNodeContextOverride_WithPredicate() {
    testExpressionTranslationWithContext("../../PathNode/IntegerField/number()", "BT-00-Text",
        "ND-Root[BT-00-Indicator == TRUE]::integerField");
  }

  @Test
  void testAbsoluteFieldReference() {
    testExpressionTranslationWithContext("/*/PathNode/IndicatorField", "BT-00-Text",
        "/BT-00-Indicator");
  }

  @Test
  void testSimpleFieldReference() {
    testExpressionTranslationWithContext("../IndicatorField", "BT-00-Text", "BT-00-Indicator");
  }

  @Test
  void testFieldReference_ForDurationFields() {
    testExpressionTranslationWithContext(
        "(for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ()))",
        "ND-Root", "BT-00-Measure");
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


  // #endregion: References

  // #region: Boolean functions -----------------------------------------------

  @Test
  void testNotFunction() {
    testExpressionTranslationWithContext("not(true())", "BT-00-Text", "not(ALWAYS)");
    testExpressionTranslationWithContext("not(1 + 1 = 2)", "BT-00-Text", "not(1 + 1 == 2)");
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("BT-00-Text", "not('text')"));
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
        privacyPath + "/PublicationDate",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:publicationDate is present");
  }

  @Test
  void testLinkedFieldProperty_justificationCode() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/ReasonCode",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:justificationCode is present");
  }

  @Test
  void testLinkedFieldProperty_justificationDescription() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/ReasonDescription",
        "BT-00-Text-In-Repeatable-Node",
        "BT-00-Text-In-Repeatable-Node:justificationDescription is present");
  }

  @Test
  void testLinkedFieldProperty_onNonWithholdableField() {
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("BT-00-Text",
            "BT-00-Text:publicationDate is present"));
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
  }

  @Test
  void testComputedProperty_isWithheld() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='test-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'test-priv'"
            + " and "
            + "(not(" + privacyPath + "/PublicationDate)"
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
                + "(not(" + privacyPath + "/PublicationDate)"
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
    assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text-In-Repeatable-Node:isMasked"));
  }

  @Test
  void testComputedProperty_isDisclosed_numericField() {
    final String privacyPath = "../FieldsPrivacy[FieldIdentifierCode/text()='num-priv']";
    testExpressionTranslationWithContext(
        privacyPath + "/FieldIdentifierCode/normalize-space(text()) = 'num-priv'"
            + " and "
            + "not("
                + "(not(" + privacyPath + "/PublicationDate)"
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
                + "(not(" + privacyPath + "/PublicationDate)"
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

  // #endregion: Boolean functions

  // #region: Numeric functions -----------------------------------------------

  @Test
  void testCountFunction_UsingFieldReference() {
    testExpressionTranslationWithContext("count(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "count(BT-00-Text)");
  }

  @Test
  void testCountFunction_UsingSequenceFromIteration() {
    testExpressionTranslationWithContext(
        "count(for $x in PathNode/TextField/normalize-space(text()) return concat($x, '-xyz'))", "ND-Root",
        "count(for text:$x in BT-00-Text return concat($x, '-xyz'))");
  }

  @Test
  void testNumberFunction() {
    testExpressionTranslationWithContext("number(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "number(BT-00-Text)");
  }

  @Test
  void testNumberFromBooleanFunction() {
    testExpressionTranslationWithContext("number(true())", "ND-Root", "number(TRUE)");
  }

  @Test
  void testNumberFromBooleanFunction_WithFieldReference() {
    testExpressionTranslationWithContext("number(PathNode/IndicatorField)", "ND-Root",
        "number(BT-00-Indicator)");
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
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("ND-Root", "trim(BT-00-Repeatable-Text)"));
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
    assertThrows(ParseCancellationException.class,
        () -> translateExpression("{ND-Root} ${url-encode(ND-Root)}"));
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
    assertThrows(ParseCancellationException.class,
        () -> translateExpression("{ND-Root} ${capitalize-first(ND-Root)}"));
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
    testExpressionTranslationWithContext("string(PathNode/IndicatorField)", "ND-Root",
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
        "string((for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())))",
        "ND-Root", "string(BT-00-Measure)");
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
  void testFormatNumberFunction() {
    testExpressionTranslationWithContext("format-number(PathNode/NumberField/number(), '# ##0,00')",
        "ND-Root", "format-number(BT-00-Number, '#,##0.00')");
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

  // #endregion: String functions

  // #region: Date functions --------------------------------------------------

  @Test
  void testDateFromStringFunction() {
    testExpressionTranslationWithContext("xs:date(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "date(BT-00-Text)");
  }

  // #endregion: Date functions

  // #region: Time functions --------------------------------------------------

  @Test
  void testTimeFromStringFunction() {
    testExpressionTranslationWithContext("xs:time(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "time(BT-00-Text)");
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
  void testUnionFunction_WithTypeMismatch() {
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("ND-Root", "value-union(BT-00-Text, BT-00-Number)"));
  }

  // #endregion: Union

  // #region: Intersect

  @Test
  void testIntersectFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in ('one','two') return if (some $L2 in ('two','three','four') satisfies $L1 = $L2) then $L1 else ())", "ND-Root",
        "value-intersect(['one', 'two'], ['two', 'three', 'four'])");
  }

  @Test
  void testIntersectFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (1,2,3) return if (some $L2 in (2,3,4) satisfies $L1 = $L2) then $L1 else ())", "ND-Root",
        "value-intersect([1, 2, 3], [2, 3, 4])");
  }

  @Test
  void testIntersectFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:date('2018-01-01Z'),xs:date('2020-01-01Z')) return if (some $L2 in (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect([2018-01-01Z, 2020-01-01Z], [2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testIntersectFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:time('12:00:00Z'),xs:time('13:00:00Z')) return if (some $L2 in (xs:time('12:00:00Z'),xs:time('14:00:00Z')) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect([12:00:00Z, 13:00:00Z], [12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testIntersectFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')) return if (some $L2 in (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect([P1W, P2D], [P2D, P5D])");
  }

  @Test
  void testIntersectFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (true(),false()) return if (some $L2 in (false(),false()) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect([TRUE, FALSE], [FALSE, NEVER])");
  }

  @Test
  void testIntersectFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in PathNode/TextField/normalize-space(text()) return if (some $L2 in PathNode/TextField/normalize-space(text()) satisfies $L1 = $L2) then $L1 else ())", "ND-Root",
        "value-intersect(BT-00-Text, BT-00-Text)");
  }

  @Test
  void testIntersectFunction_WithTypeMismatch() {
    assertThrows(ParseCancellationException.class, () -> translateExpressionWithContext("ND-Root",
        "value-intersect(BT-00-Text, BT-00-Number)"));
  }

  // #endregion: Intersect

  // #region: Except

  @Test
  void testExceptFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in ('one','two') return if (every $L2 in ('two','three','four') satisfies $L1 != $L2) then $L1 else ())", "ND-Root",
        "value-except(['one', 'two'], ['two', 'three', 'four'])");
  }

  @Test
  void testExceptFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (1,2,3) return if (every $L2 in (2,3,4) satisfies $L1 != $L2) then $L1 else ())", "ND-Root",
        "value-except([1, 2, 3], [2, 3, 4])");
  }

  @Test
  void testExceptFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:date('2018-01-01Z'),xs:date('2020-01-01Z')) return if (every $L2 in (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except([2018-01-01Z, 2020-01-01Z], [2018-01-01Z, 2022-01-02Z])");
  }

  @Test
  void testExceptFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:time('12:00:00Z'),xs:time('13:00:00Z')) return if (every $L2 in (xs:time('12:00:00Z'),xs:time('14:00:00Z')) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except([12:00:00Z, 13:00:00Z], [12:00:00Z, 14:00:00Z])");
  }

  @Test
  void testExceptFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(for $L1 in (xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')) return if (every $L2 in (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except([P1W, P2D], [P2D, P5D])");
  }

  @Test
  void testExceptFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (true(),false()) return if (every $L2 in (false(),false()) satisfies $L1 != $L2) then $L1 else ())", "ND-Root",
        "value-except([TRUE, FALSE], [FALSE, NEVER])");
  }

  @Test
  void testExceptFunction_WithTextFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in PathNode/TextField/normalize-space(text()) return if (every $L2 in PathNode/TextField/normalize-space(text()) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except(BT-00-Text, BT-00-Text)");
  }

  @Test
  void testExceptFunction_WithNumberFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in PathNode/IntegerField/number() return if (every $L2 in PathNode/IntegerField/number() satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except(BT-00-Integer, BT-00-Integer)");
  }

  @Test
  void testExceptFunction_WithBooleanFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in PathNode/IndicatorField return if (every $L2 in PathNode/IndicatorField satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except(BT-00-Indicator, BT-00-Indicator)");
  }

    @Test
  void testExceptFunction_WithDateFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in PathNode/StartDateField/xs:date(text()) return if (every $L2 in PathNode/StartDateField/xs:date(text()) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except(BT-00-StartDate, BT-00-StartDate)");
  }

  @Test
  void testExceptFunction_WithTimeFieldReferences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in PathNode/StartTimeField/xs:time(text()) return if (every $L2 in PathNode/StartTimeField/xs:time(text()) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except(BT-00-StartTime, BT-00-StartTime)");
  }

  @Test
  void testExceptFunction_WithDurationFieldReferences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return if (every $L2 in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) satisfies $L1 != $L2) then $L1 else ())", "ND-Root", "value-except(BT-00-Measure, BT-00-Measure)");
  }

  @Test
  void testExceptFunction_WithTypeMismatch() {
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("ND-Root", "value-except(BT-00-Text, BT-00-Number)"));
  }

  // #endregion: Except

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
  void testParameterizedExpression_WithUnquotedStringParameter() {
    assertThrows(ParseCancellationException.class,
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
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P1Y') = $T + xs:yearMonthDuration('P2Y')))",
        "{ND-Root, measure:$p1, measure:$p2} ${$p1 == $p2}", "P1Y", "P2Y");
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
        "{ND-Root, measure*:$items} ${count($items)}", "[P1Y, P2M]");
  }

  // #endregion: Compare sequences

  // #endregion Sequence Functions

  // #region: Indexers --------------------------------------------------------

  @Test
  void testIndexer_WithNonRepeatableField() {
    // Indexing a non-repeatable field - currently allowed (semantically odd but syntactically valid)
    testExpressionTranslationWithContext("(PathNode/TextField/normalize-space(text()))[1]", "ND-Root",
        "BT-00-Text[1]");
  }

  @Test
  void testIndexer_WithNonRepeatableFieldAndPredicate() {
    // Indexing a non-repeatable field with predicate
    testExpressionTranslationWithContext(
        "(PathNode/TextField[./normalize-space(text()) = 'hello']/normalize-space(text()))[1]", "ND-Root",
        "BT-00-Text[BT-00-Text == 'hello'][1]");
  }

  @Test
  void testIndexer_WithRepeatableField() {
    // Indexing a repeatable field should work - BT-00-Repeatable-Text is repeatable
    // No explicit cast - preprocessor inserts (text*) making it stringSequence[indexer]
    testExpressionTranslationWithContext("(PathNode/RepeatableTextField/normalize-space(text()))[1]", "ND-Root",
        "BT-00-Repeatable-Text[1]");
  }

  @Test
  void testIndexer_WithRepeatableFieldAndPredicate() {
    // Indexing a repeatable field with predicate should work
    // No explicit cast - preprocessor handles typing
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField[(./normalize-space(text()))[1] = 'hello']/normalize-space(text()))[1]", "ND-Root",
        "BT-00-Repeatable-Text[BT-00-Repeatable-Text[1] == 'hello'][1]");
  }

  @Test
  void testIndexer_WithTextSequence() {
    testExpressionTranslationWithContext("('a','b','c')[1]", "ND-Root", "['a', 'b','c'][1]");
  }

  // #endregion: Indexers

  // #region: Scalar/Sequence Validation --------------------------------------

  @Test
  void testScalarFromRepeatableField_ThrowsError() {
    // A repeatable field used as scalar should throw TypeMismatchException.fieldMayRepeat()
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInRepeatableNode_ThrowsErrorFromRootContext() {
    // Field in ND-RepeatableNode (repeatable) used as scalar from ND-Root should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text-In-Repeatable-Node == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
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
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInNestedRepeatableNode_ThrowsErrorFromRepeatableNodeContext() {
    // Field in ND-RepeatableSubSubNode used from ND-RepeatableNode should still throw (ND-RepeatableSubSubNode is also repeatable)
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-RepeatableNode", "BT-00-Text-In-RepeatableSubSubNode == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
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
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
  }

  @Test
  void testScalarFromFieldInNonRepeatableNestedInRepeatable_OkFromRepeatableNodeContext() {
    // Field in ND-NonRepeatableSubNode used from ND-RepeatableNode context should NOT throw
    testExpressionTranslationWithContext("NonRepeatableSubNode/TextField/normalize-space(text()) = 'test'",
        "ND-RepeatableNode", "BT-00-Text-In-NonRepeatableSubNode == 'test'");
  }

  @Test
  void testRepeatableFieldInUniqueCondition_ThrowsError() {
    // A repeatable field used as needle (left side) in uniqueness condition should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text is unique in /BT-00-Text"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
  }

  // #endregion: Scalar/Sequence Validation

  // #region: InvalidIdentifierException Tests --------------------------------

  @Test
  void testContextSpecifier_WithRegularVariable_ThrowsNotAContextVariable() {
    // A regular variable (not declared with context:) used as context specifier should throw
    InvalidIdentifierException ex = assertThrows(InvalidIdentifierException.class,
        () -> translateExpressionWithContext("ND-Root", "for text:$x in BT-00-Text return $x::BT-00-Number"));
    assertEquals(InvalidIdentifierException.ErrorCode.NOT_A_CONTEXT_VARIABLE, ex.getErrorCode());
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

  // #region: TypeMismatchException - nodeCannotBeValue -------------------------

  @Test
  void testScalarFromNodeContextVariable_ThrowsNodeCannotBeValue() {
    // A node context variable used as scalar value should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "for context:$n in ND-SubNode return $n == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_FIELD_CONTEXT, ex.getErrorCode());
  }

  @Test
  void testSequenceFromNodeContextVariable_ThrowsNodeCannotBeValue() {
    // A node context variable used in count() should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "for context:$n in ND-SubNode return count($n)"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_FIELD_CONTEXT, ex.getErrorCode());
  }

  // #endregion: TypeMismatchException - nodeCannotBeValue

  // #region: TypeMismatchException - fieldMayRepeat (Context Variables) --------

  @Test
  void testScalarFromFieldContextVariable_NonRepeatable_Works() {
    // A non-repeatable field context variable used as scalar should work
    testExpressionTranslationWithContext(
        "for $f in PathNode/TextField return $f = 'test'",
        "ND-Root",
        "for context:$f in BT-00-Text return $f == 'test'");
  }

  @Test
  void testScalarFromFieldContextVariable_Repeatable_ThrowsFieldMayRepeat() {
    // A repeatable field context variable used as scalar should throw
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "for context:$f in BT-00-Repeatable-Text return $f == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
  }

  // #endregion: TypeMismatchException - fieldMayRepeat (Context Variables)

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
    assertEquals(TypeMismatchException.ErrorCode.EXPECTED_SCALAR, ex.getErrorCode());
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
}
