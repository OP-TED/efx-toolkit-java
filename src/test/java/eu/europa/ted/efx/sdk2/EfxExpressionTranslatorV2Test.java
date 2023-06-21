package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;

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
        "'x' not in ('a', 'b', 'c')");
  }

  @Test
  void testEmptinessCondition() {
    testExpressionTranslationWithContext("PathNode/TextField/normalize-space(text()) = ''",
        "ND-Root", "BT-00-Text is empty");
  }

  @Test
  void testEmptinessCondition_WithNot() {
    testExpressionTranslationWithContext("PathNode/TextField/normalize-space(text()) != ''",
        "ND-Root", "BT-00-Text is not empty");
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
        "count(for $x in PathNode/TextField, $y in /*/PathNode/TextField[. = $x] return $y) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Text");
  }

  @Test
  void testUniqueValueCondition_WithNot() {
    testExpressionTranslationWithContext(
        "not(count(for $x in PathNode/TextField, $y in /*/PathNode/TextField[. = $x] return $y) = 1)",
        "ND-Root", "BT-00-Text is not unique in /BT-00-Text");
  }


  @Test
  void testLikePatternCondition() {
    testExpressionTranslationWithContext("fn:matches(normalize-space('123'), '[0-9]*')",
        "BT-00-Text", "'123' like '[0-9]*'");
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
  void testPreferredLanguageFunction() {
    testExpressionTranslation("PathNode/TextMultilingualField[./@languageID = efx:preferred-language(.)]/normalize-space(text())", 
    "{ND-Root} ${BT-00-Text-Multilingual[BT-00-Text-Multilingual/@languageID == preferred-language(BT-00-Text-Multilingual)]}");
  }

    @Test 
  void testPreferredLanguageFunction_InPredicate() {
    testExpressionTranslation("efx:preferred-language(PathNode/TextMultilingualField)", 
    "{ND-Root} ${preferred-language(BT-00-Text-Multilingual)}");
  }

  @Test 
  void testPreferredLanguageTextFunction() {
    testExpressionTranslation("efx:preferred-language-text(PathNode/TextMultilingualField)", 
    "{ND-Root} ${preferred-language-text(BT-00-Text-Multilingual)}");
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
        "every text:$x in ('a', 'b', 'c') satisfies $x <= 'a'");
  }

  @Test
  void testStringQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext("every $x in PathNode/TextField/normalize-space(text()) satisfies $x <= 'a'",
        "ND-Root", "every text:$x in BT-00-Text satisfies $x <= 'a'");
  }

  @Test
  void testBooleanQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext("every $x in (true(),false(),true()) satisfies $x",
        "ND-Root", "every indicator:$x in (TRUE, FALSE, ALWAYS) satisfies $x");
  }

  @Test
  void testBooleanQuantifiedExpression_UsingFieldReference() {
    testExpressionTranslationWithContext("every $x in PathNode/IndicatorField satisfies $x",
        "ND-Root", "every indicator:$x in BT-00-Indicator satisfies $x");
  }

  @Test
  void testNumericQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext("every $x in (1,2,3) satisfies $x <= 1", "ND-Root",
        "every number:$x in (1, 2, 3) satisfies $x <= 1");
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
        "every date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) satisfies $x <= 2012-01-01Z");
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
        "every date:$x in BT-00-StartDate, date:$y in ($x, 2022-02-02Z), indicator:$i in (ALWAYS, TRUE) satisfies $x <= 2012-01-01Z");
  }

  @Test
  void testTimeQuantifiedExpression_UsingLiterals() {
    testExpressionTranslationWithContext(
        "every $x in (xs:time('00:00:00Z'),xs:time('00:00:01Z'),xs:time('00:00:02Z')) satisfies $x <= xs:time('00:00:00Z')",
        "ND-Root", "every time:$x in (00:00:00Z, 00:00:01Z, 00:00:02Z) satisfies $x <= 00:00:00Z");
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
        "ND-Root", "every measure:$x in (P1D, P2D, P3D) satisfies $x <= P1D");
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
        "'a' in (for text:$x in ('a', 'b', 'c') return concat($x, 'text'))");
  }

  @Test
  void testStringsSequenceFromIteration_UsingMultipleIterators() {
    testExpressionTranslationWithContext(
        "'a' = (for $x in ('a','b','c'), $y in (1,2), $z in PathNode/IndicatorField return concat($x, format-number($y, '0,##########'), 'text'))",
        "ND-Root",
        "'a' in (for text:$x in ('a', 'b', 'c'), number:$y in (1, 2), indicator:$z in BT-00-Indicator return concat($x, string($y), 'text'))");
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
  void testStringsFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext("'a' = (for $x in (true(),false()) return 'y')", "ND-Root",
        "'a' in (for indicator:$x in (TRUE, FALSE) return 'y')");
  }

  @Test
  void testStringsFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("'a' = (for $x in PathNode/IndicatorField return 'y')",
        "ND-Root", "'a' in (for indicator:$x in BT-00-Indicator return 'y')");
  }


  @Test
  void testStringsFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext("'a' = (for $x in (1,2,3) return 'y')", "ND-Root",
        "'a' in (for number:$x in (1, 2, 3) return 'y')");
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
        "ND-Root", "'a' in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 'y')");
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
        "ND-Root", "'a' in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 'y')");
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
        "ND-Root", "'a' in (for measure:$x in (P1D, P1Y, P2M) return 'y')");
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
        "ND-Root", "123 in (for text:$x in ('a', 'b', 'c') return number($x))");
  }

  @Test
  void testNumbersFromStringIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/TextField/normalize-space(text()) return number($x))",
        "ND-Root", "123 in (for text:$x in BT-00-Text return number($x))");
  }


  @Test
  void testNumbersFromBooleanIteration_UsingLiterals() {
    testExpressionTranslationWithContext("123 = (for $x in (true(),false()) return 0)", "ND-Root",
        "123 in (for indicator:$x in (TRUE, FALSE) return 0)");
  }

  @Test
  void testNumbersFromBooleanIteration_UsingFieldReference() {
    testExpressionTranslationWithContext("123 = (for $x in PathNode/IndicatorField return 0)",
        "ND-Root", "123 in (for indicator:$x in BT-00-Indicator return 0)");
  }


  @Test
  void testNumbersFromNumericIteration_UsingLiterals() {
    testExpressionTranslationWithContext("123 = (for $x in (1,2,3) return 0)", "ND-Root",
        "123 in (for number:$x in (1, 2, 3) return 0)");
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
        "ND-Root", "123 in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 0)");
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
        "ND-Root", "123 in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 0)");
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
        "ND-Root", "123 in (for measure:$x in (P1D, P1Y, P2M) return 0)");
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
        "2022-01-01Z in (for text:$x in ('a', 'b', 'c') return date($x))");
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
        "ND-Root", "2022-01-01Z in (for indicator:$x in (TRUE, FALSE) return 2022-01-01Z)");
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
        "2022-01-01Z in (for number:$x in (1, 2, 3) return 2022-01-01Z)");
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
        "2022-01-01Z in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 2022-01-01Z)");
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
        "2022-01-01Z in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 2022-01-01Z)");
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
        "ND-Root", "2022-01-01Z in (for measure:$x in (P1D, P1Y, P2M) return 2022-01-01Z)");
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
        "12:00:00Z in (for text:$x in ('a', 'b', 'c') return time($x))");
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
        "ND-Root", "12:00:00Z in (for indicator:$x in (TRUE, FALSE) return 12:00:00Z)");
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
        "12:00:00Z in (for number:$x in (1, 2, 3) return 12:00:00Z)");
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
        "12:00:00Z in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 12:00:00Z)");
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
        "12:00:00Z in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 12:00:00Z)");
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
        "ND-Root", "12:00:00Z in (for measure:$x in (P1D, P1Y, P2M) return 12:00:00Z)");
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
        "ND-Root", "P1D in (for measure:$x in (P1D, P2D, P1W) return $x)");
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
        "ND-Root", "P1D in (for indicator:$x in (TRUE, FALSE) return P1D)");
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
        "ND-Root", "P1D in (for number:$x in (1, 2, 3) return P1D)");
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
        "ND-Root", "P1D in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return P1D)");
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
        "ND-Root", "P1D in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return P1D)");
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
        "ND-Root", "P1D in (for measure:$x in (P1D, P1Y, P2M) return P1D)");
  }

  @Test
  void testDurationsFromDurationIteration_UsingFieldReference() {
    testExpressionTranslationWithContext(
      "xs:dayTimeDuration('P1D') = (for $x in (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for measure:$x in BT-00-Measure return P1D)");
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
        "'a' in ('a', 'b', 'c')");
  }

  @Test
  void testNumericList_UsingNumericLiterals() {
    testExpressionTranslationWithContext("4 = (1,2,3)", "BT-00-Text", "4 in (1, 2, 3)");
  }

  @Test
  void testNumericList_UsingNumericField() {
    testExpressionTranslationWithContext("4 = (1,../NumberField/number(),3)", "BT-00-Text",
        "4 in (1, BT-00-Number, 3)");
  }

  @Test
  void testNumericList_UsingTextField() {
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext("BT-00-Text", "4 in (1, BT-00-Text, 3)"));
  }

  @Test
  void testBooleanList() {
    testExpressionTranslationWithContext("false() = (true(),PathNode/IndicatorField,true())",
        "ND-Root", "NEVER in (TRUE, BT-00-Indicator, ALWAYS)");
  }

  @Test
  void testDateList() {
    testExpressionTranslationWithContext(
        "xs:date('2022-01-01Z') = (xs:date('2022-01-02Z'),PathNode/StartDateField/xs:date(text()),xs:date('2022-02-02Z'))",
        "ND-Root", "2022-01-01Z in (2022-01-02Z, BT-00-StartDate, 2022-02-02Z)");
  }

  @Test
  void testTimeList() {
    testExpressionTranslationWithContext(
        "xs:time('12:20:21Z') = (xs:time('12:30:00Z'),PathNode/StartTimeField/xs:time(text()),xs:time('13:40:00Z'))",
        "ND-Root", "12:20:21Z in (12:30:00Z, BT-00-StartTime, 13:40:00Z)");
  }

  @Test
  void testDurationList_UsingDurationLiterals() {
    testExpressionTranslationWithContext(
        "xs:yearMonthDuration('P3M') = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        "BT-00-Text", "P3M in (P1M, P3M, P6M)");
  }



  @Test
  void testDurationList_UsingDurationField() {
    assertEquals(
        "(for $F in ../MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        translateExpressionWithContext("BT-00-Text", "BT-00-Measure in (P1M, P3M, P6M)"));
  }

  @Test
  void testCodeList() {
    testExpressionTranslationWithContext("'a' = ('code1','code2','code3')", "BT-00-Text",
        "'a' in #accessibility");
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
    testExpressionTranslationWithContext("@Attribute = 'text'", "BT-00-Text",
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

  @Test
  void testFieldReference_WithAxis() {
    testExpressionTranslationWithContext("./preceding::PathNode/IntegerField/number()", "ND-Root",
        "ND-Root::preceding::integerField");
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
  void testStringLengthFunction() {
    testExpressionTranslationWithContext(
        "string-length(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "string-length(BT-00-Text)");
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
  void testToStringFunction() {
    testExpressionTranslationWithContext("format-number(123, '0,##########')", "ND-Root",
        "string(123)");
  }

  @Test
  void testConcatFunction() {
    testExpressionTranslationWithContext("concat('abc', 'def')", "ND-Root", "concat('abc', 'def')");
  };

  @Test
  void testStringJoinFunction_withLiterals() {
    testExpressionTranslationWithContext("string-join(('abc','def'), ',')", "ND-Root",
        "string-join(('abc', 'def'), ',')");
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
        "distinct-values(('one', 'two', 'one'))");
  }

  @Test
  void testDistinctValuesFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values((1,2,3,2,3,4))", "ND-Root",
        "distinct-values((1, 2, 3, 2, 3, 4))");
  }

  @Test
  void testDistinctValuesFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'),xs:date('2022-01-02Z')))",
        "ND-Root", "distinct-values((2018-01-01Z, 2020-01-01Z, 2018-01-01Z, 2022-01-02Z))");
  }

  @Test
  void testDistinctValuesFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'),xs:time('14:00:00Z')))",
        "ND-Root", "distinct-values((12:00:00Z, 13:00:00Z, 12:00:00Z, 14:00:00Z))");
  }

  @Test
  void testDistinctValuesFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')))",
        "ND-Root", "distinct-values((P1W, P2D, P2D, P5D))");
  }

  @Test
  void testDistinctValuesFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values((true(),false(),false(),false()))",
        "ND-Root", "distinct-values((TRUE, FALSE, FALSE, NEVER))");
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
        "ND-Root", "value-union(('one', 'two'), ('two', 'three', 'four'))");
  }

  @Test
  void testUnionFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(((1,2,3), (2,3,4)))", "ND-Root",
        "value-union((1, 2, 3), (2, 3, 4))");
  }

  @Test
  void testUnionFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:date('2018-01-01Z'),xs:date('2020-01-01Z')), (xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))))",
        "ND-Root", "value-union((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @Test
  void testUnionFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(((xs:time('12:00:00Z'),xs:time('13:00:00Z')), (xs:time('12:00:00Z'),xs:time('14:00:00Z'))))",
        "ND-Root", "value-union((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @Test
  void testUnionFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values(((xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')), (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D'))))",
        "ND-Root", "value-union((P1W, P2D), (P2D, P5D))");
  }

  @Test
  void testUnionFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values(((true(),false()), (false(),false())))",
        "ND-Root", "value-union((TRUE, FALSE), (FALSE, NEVER))");
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
        "value-intersect(('one', 'two'), ('two', 'three', 'four'))");
  }

  @Test
  void testIntersectFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (1,2,3) return if (some $L2 in (2,3,4) satisfies $L1 = $L2) then $L1 else ())", "ND-Root",
        "value-intersect((1, 2, 3), (2, 3, 4))");
  }

  @Test
  void testIntersectFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:date('2018-01-01Z'),xs:date('2020-01-01Z')) return if (some $L2 in (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @Test
  void testIntersectFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:time('12:00:00Z'),xs:time('13:00:00Z')) return if (some $L2 in (xs:time('12:00:00Z'),xs:time('14:00:00Z')) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @Test
  void testIntersectFunction_WithDurationSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')) return if (some $L2 in (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect((P1W, P2D), (P2D, P5D))");
  }

  @Test
  void testIntersectFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (true(),false()) return if (some $L2 in (false(),false()) satisfies $L1 = $L2) then $L1 else ())",
        "ND-Root", "value-intersect((TRUE, FALSE), (FALSE, NEVER))");
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
        "value-except(('one', 'two'), ('two', 'three', 'four'))");
  }

  @Test
  void testExceptFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("distinct-values(for $L1 in (1,2,3) return if (every $L2 in (2,3,4) satisfies $L1 != $L2) then $L1 else ())", "ND-Root",
        "value-except((1, 2, 3), (2, 3, 4))");
  }

  @Test
  void testExceptFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:date('2018-01-01Z'),xs:date('2020-01-01Z')) return if (every $L2 in (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @Test
  void testExceptFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (xs:time('12:00:00Z'),xs:time('13:00:00Z')) return if (every $L2 in (xs:time('12:00:00Z'),xs:time('14:00:00Z')) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @Test
  void testExceptFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "distinct-values(for $L1 in (xs:dayTimeDuration('P7D'),xs:dayTimeDuration('P2D')) return if (every $L2 in (xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P5D')) satisfies $L1 != $L2) then $L1 else ())",
        "ND-Root", "value-except((P1W, P2D), (P2D, P5D))");
  }

  @Test
  void testExceptFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
      "distinct-values(for $L1 in (true(),false()) return if (every $L2 in (false(),false()) satisfies $L1 != $L2) then $L1 else ())", "ND-Root",
        "value-except((TRUE, FALSE), (FALSE, NEVER))");
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

  // #region: Compare sequences

  @Test
  void testSequenceEqualFunction_WithStringSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(('one','two')), sort(('two','three','four')))", "ND-Root",
        "sequence-equal(('one', 'two'), ('two', 'three', 'four'))");
  }

  @Test
  void testSequenceEqualFunction_WithNumberSequences() {
    testExpressionTranslationWithContext("deep-equal(sort((1,2,3)), sort((2,3,4)))", "ND-Root",
        "sequence-equal((1, 2, 3), (2, 3, 4))");
  }

  @Test
  void testSequenceEqualFunction_WithDateSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'))), sort((xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))))",
        "ND-Root", "sequence-equal((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @Test
  void testSequenceEqualFunction_WithTimeSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:time('12:00:00Z'),xs:time('13:00:00Z'))), sort((xs:time('12:00:00Z'),xs:time('14:00:00Z'))))",
        "ND-Root", "sequence-equal((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @Test
  void testSequenceEqualFunction_WithBooleanSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((true(),false())), sort((false(),false())))", "ND-Root",
        "sequence-equal((TRUE, FALSE), (FALSE, NEVER))");
  }

  @Test
  void testSequenceEqualFunction_WithDurationSequences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2Y'))), sort((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P3Y'))))",
        "ND-Root", "sequence-equal((P1Y, P2Y), (P1Y, P3Y))");
  }

  @Test
  void testSequenceEqualFunction_WithFieldReferences() {
    testExpressionTranslationWithContext(
        "deep-equal(sort(PathNode/TextField/normalize-space(text())), sort(PathNode/TextField/normalize-space(text())))", "ND-Root",
        "sequence-equal(BT-00-Text, BT-00-Text)");
  }

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

  // #endregion: Compare sequences

  // #endregion Sequence Functions

  // #region: Indexers --------------------------------------------------------

  @Test
  void testIndexer_WithFieldReference() {
    testExpressionTranslationWithContext("PathNode/TextField/normalize-space(text())[1]", "ND-Root", "(text)BT-00-Text[1]");
  }

  @Test
  void testIndexer_WithFieldReferenceAndPredicate() {
    testExpressionTranslationWithContext(
        "PathNode/TextField[./normalize-space(text()) = 'hello']/normalize-space(text())[1]", "ND-Root",
        "(text)BT-00-Text[BT-00-Text == 'hello'][1]");
  }

  @Test
  void testIndexer_WithTextSequence() {
    testExpressionTranslationWithContext("('a','b','c')[1]", "ND-Root", "('a', 'b','c')[1]");
  }

  // #endregion: Indexers
}
