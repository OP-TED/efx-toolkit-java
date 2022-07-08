package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.translator.EfxTranslator;

class EfxExpressionTranslatorTest {
  final private String SDK_VERSION = "0.7";

  private String test(final String context, final String expression) throws InstantiationException {
    return EfxTranslator.translateExpression(context, expression, DependencyFactoryMock.INSTANCE,
        SDK_VERSION);
  }

  /*** Boolean expressions ***/

  @Test
  void testParenthesizedBooleanExpression() throws InstantiationException {
    assertEquals("(true() or true()) and false()",
        test("BT-00-Text", "(ALWAYS or TRUE) and NEVER"));
  }

  @Test
  void testLogicalOrCondition() throws InstantiationException {
    assertEquals("true() or false()", test("BT-00-Text", "ALWAYS or NEVER"));
  }

  @Test
  void testLogicalAndCondition() throws InstantiationException {
    assertEquals("true() and 1 + 1 = 2", test("BT-00-Text", "ALWAYS and 1 + 1 == 2"));
  }

  @Test
  void testInListCondition() throws InstantiationException {
    assertEquals("not('x' = ('a','b','c'))", test("BT-00-Text", "'x' not in ('a', 'b', 'c')"));
  }

  @Test
  void testEmptinessCondition() throws InstantiationException {
    assertEquals("PathNode/TextField/normalize-space(text()) = ''",
        test("ND-Root", "BT-00-Text is empty"));
  }

  @Test
  void testEmptinessCondition_WithNot() throws InstantiationException {
    assertEquals("PathNode/TextField/normalize-space(text()) != ''",
        test("ND-Root", "BT-00-Text is not empty"));
  }

  @Test
  void testPresenceCondition() throws InstantiationException {
    assertEquals("PathNode/TextField", test("ND-Root", "BT-00-Text is present"));
  }

  @Test
  void testPresenceCondition_WithNot() throws InstantiationException {
    assertEquals("not(PathNode/TextField)", test("ND-Root", "BT-00-Text is not present"));
  }

  @Test
  void testLikePatternCondition() throws InstantiationException {
    assertEquals("fn:matches(normalize-space('123'), '[0-9]*')",
        test("BT-00-Text", "'123' like '[0-9]*'"));
  }

  @Test
  void testLikePatternCondition_WithNot() throws InstantiationException {
    assertEquals("not(fn:matches(normalize-space('123'), '[0-9]*'))",
        test("BT-00-Text", "'123' not like '[0-9]*'"));
  }

  @Test
  void testFieldValueComparison_UsingTextFields() throws InstantiationException {
    assertEquals(
        "PathNode/TextField/normalize-space(text()) = PathNode/TextMultilingualField/normalize-space(text())",
        test("ND-Root", "BT-00-Text == BT-00-Text-Multilingual"));
  }

  @Test
  void testFieldValueComparison_UsingNumericFields() throws InstantiationException {
    assertEquals("PathNode/NumberField/number() <= PathNode/IntegerField/number()",
        test("ND-Root", "BT-00-Number <= BT-00-Integer"));
  }

  @Test
  void testFieldValueComparison_UsingIndicatorFields() throws InstantiationException {
    assertEquals("PathNode/IndicatorField != PathNode/IndicatorField",
        test("ND-Root", "BT-00-Indicator != BT-00-Indicator"));
  }

  @Test
  void testFieldValueComparison_UsingDateFields() throws InstantiationException {
    assertEquals("PathNode/StartDateField/xs:date(text()) <= PathNode/EndDateField/xs:date(text())",
        test("ND-Root", "BT-00-StartDate <= BT-00-EndDate"));
  }

  @Test
  void testFieldValueComparison_UsingTimeFields() throws InstantiationException {
    assertEquals("PathNode/StartTimeField/xs:time(text()) <= PathNode/EndTimeField/xs:time(text())",
        test("ND-Root", "BT-00-StartTime <= BT-00-EndTime"));
  }

  @Test
  void testFieldValueComparison_UsingMeasureFields() throws InstantiationException {
    assertEquals(
        "boolean(for $T in (current-date()) return ($T + (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ()) <= $T + (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ())))",
        test("ND-Root", "BT-00-Measure <= BT-00-Measure"));
  }

  @Test
  void testFieldValueComparison_WithStringLiteral() throws InstantiationException {
    assertEquals("PathNode/TextField/normalize-space(text()) = 'abc'",
        test("ND-Root", "BT-00-Text == 'abc'"));
  }

  @Test
  void testFieldValueComparison_WithNumericLiteral() throws InstantiationException {
    assertEquals("PathNode/IntegerField/number() - PathNode/NumberField/number() > 0",
        test("ND-Root", "BT-00-Integer - BT-00-Number > 0"));
  }

  @Test
  void testFieldValueComparison_WithDateLiteral() throws InstantiationException {
    assertEquals("xs:date('2022-01-01') > PathNode/StartDateField/xs:date(text())",
        test("ND-Root", "2022-01-01 > BT-00-StartDate"));
  }

  @Test
  void testFieldValueComparison_WithTimeLiteral() throws InstantiationException {
    assertEquals("xs:time('00:01:00') > PathNode/EndTimeField/xs:time(text())",
        test("ND-Root", "00:01:00 > BT-00-EndTime"));
  }

  @Test
  void testFieldValueComparison_TypeMismatch() throws InstantiationException {
    assertThrows(ParseCancellationException.class,
        () -> test("ND-Root", "00:01:00 > BT-00-StartDate"));
  }


  @Test
  void testBooleanComparison_UsingLiterals() throws InstantiationException {
    assertEquals("false() != true()", test("BT-00-Text", "NEVER != ALWAYS"));
  }

  @Test
  void testBooleanComparison_UsingFieldReference() throws InstantiationException {
    assertEquals("../IndicatorField != true()", test("BT-00-Text", "BT-00-Indicator != ALWAYS"));
  }

  @Test
  void testNumericComparison() throws InstantiationException {
    assertEquals(
        "2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5 and ../NumberField/number() > ../IntegerField/number()",
        test("BT-00-Text",
            "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5 and BT-00-Number > BT-00-Integer"));
  }

  @Test
  void testStringComparison() throws InstantiationException {
    assertEquals("'aaa' < 'bbb'", test("BT-00-Text", "'aaa' < 'bbb'"));
  }

  @Test
  void testDateComparison_OfTwoDateLiterals() throws InstantiationException {
    assertEquals("xs:date('2018-01-01') > xs:date('2018-01-01')",
        test("BT-00-Text", "2018-01-01 > 2018-01-01"));
  }

  @Test
  void testDateComparison_OfTwoDateReferences() throws InstantiationException {
    assertEquals("PathNode/StartDateField/xs:date(text()) = PathNode/EndDateField/xs:date(text())",
        test("ND-Root", "BT-00-StartDate == BT-00-EndDate"));
  }

  @Test
  void testDateComparison_OfDateReferenceAndDateFunction() throws InstantiationException {
    assertEquals(
        "PathNode/StartDateField/xs:date(text()) = xs:date(PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "BT-00-StartDate == date(BT-00-Text)"));
  }

  @Test
  void testTimeComparison_OfTwoTimeLiterals() throws InstantiationException {
    assertEquals("xs:time('13:00:10') > xs:time('21:20:30')",
        test("BT-00-Text", "13:00:10 > 21:20:30"));
  }

  @Test
  void testZonedTimeComparison_OfTwoTimeLiterals() throws InstantiationException {
    assertEquals("xs:time('13:00:10+01:00') > xs:time('21:20:30+02:00')",
        test("BT-00-Text", "13:00:10+01:00 > 21:20:30+02:00"));
  }

  @Test
  void testTimeComparison_OfTwoTimeReferences() throws InstantiationException {
    assertEquals("PathNode/StartTimeField/xs:time(text()) = PathNode/EndTimeField/xs:time(text())",
        test("ND-Root", "BT-00-StartTime == BT-00-EndTime"));
  }

  @Test
  void testTimeComparison_OfTimeReferenceAndTimeFunction() throws InstantiationException {
    assertEquals(
        "PathNode/StartTimeField/xs:time(text()) = xs:time(PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "BT-00-StartTime == time(BT-00-Text)"));
  }

  @Test
  void testDurationComparison_UsingYearMOnthDurationLiterals() throws InstantiationException {
    assertEquals(
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P1Y') = $T + xs:yearMonthDuration('P12M')))",
        test("BT-00-Text", "P1Y == P12M"));
  }

  @Test
  void testDurationComparison_UsingDayTimeDurationLiterals() throws InstantiationException {
    assertEquals(
        "boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P21D') > $T + xs:dayTimeDuration('P7D')))",
        test("BT-00-Text", "P3W > P7D"));
  }

  @Test
  void testCalculatedDurationComparison() throws InstantiationException {
    assertEquals(
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P3M') > $T + xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text()))))",
        test("ND-Root", "P3M > (BT-00-EndDate - BT-00-StartDate)"));
  }


  @Test
  void testNegativeDuration_Literal() throws InstantiationException {
    assertEquals("xs:yearMonthDuration('-P3M')", test("ND-Root", "-P3M"));
  }

  @Test
  void testNegativeDuration_ViaMultiplication() throws InstantiationException {
    assertEquals("(-3 * (2 * xs:yearMonthDuration('-P3M')))", test("ND-Root", "2 * -P3M * -3"));
  }

  @Test
  void testNegativeDuration_ViaMultiplicationWithField() throws InstantiationException {
    assertEquals(
        "(-3 * (2 * (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ())))",
        test("ND-Root", "2 * measure:BT-00-Measure * -3"));
  }

  @Test
  void testDurationAddition() throws InstantiationException {
    assertEquals(
        "(xs:dayTimeDuration('P3D') + xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
        test("ND-Root", "P3D + (BT-00-StartDate - BT-00-EndDate)"));
  }

  @Test
  void testDurationSubtraction() throws InstantiationException {
    assertEquals(
        "(xs:dayTimeDuration('P3D') - xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
        test("ND-Root", "P3D - (BT-00-StartDate - BT-00-EndDate)"));
  }

  @Test
  void testBooleanLiteralExpression_Always() throws InstantiationException {
    assertEquals("true()", test("BT-00-Text", "ALWAYS"));
  }

  @Test
  void testBooleanLiteralExpression_Never() throws InstantiationException {
    assertEquals("false()", test("BT-00-Text", "NEVER"));
  }

  /*** Quantified expressions ***/

  @Test
  void testStringQuantifiedExpression_UsingLiterals() throws InstantiationException {
    assertEquals("every $x in ('a','b','c') satisfies $x <= 'a'",
        test("ND-Root", "every text:$x in ('a', 'b', 'c') satisfies $x <= 'a'"));
  }

  @Test
  void testStringQuantifiedExpression_UsingFieldReference() throws InstantiationException {
    assertEquals("every $x in PathNode/TextField satisfies $x <= 'a'",
        test("ND-Root", "every text:$x in BT-00-Text satisfies $x <= 'a'"));
  }

  @Test
  void testBooleanQuantifiedExpression_UsingLiterals() throws InstantiationException {
    assertEquals("every $x in (true(),false(),true()) satisfies $x",
        test("ND-Root", "every indicator:$x in (TRUE, FALSE, ALWAYS) satisfies $x"));
  }

  @Test
  void testBooleanQuantifiedExpression_UsingFieldReference() throws InstantiationException {
    assertEquals("every $x in PathNode/IndicatorField satisfies $x",
        test("ND-Root", "every indicator:$x in BT-00-Indicator satisfies $x"));
  }

  @Test
  void testNumericQuantifiedExpression_UsingLiterals() throws InstantiationException {
    assertEquals("every $x in (1,2,3) satisfies $x <= 1",
        test("ND-Root", "every number:$x in (1, 2, 3) satisfies $x <= 1"));
  }

  @Test
  void testNumericQuantifiedExpression_UsingFieldReference() throws InstantiationException {
    assertEquals("every $x in PathNode/NumberField satisfies $x <= 1",
        test("ND-Root", "every number:$x in BT-00-Number satisfies $x <= 1"));
  }

  @Test
  void testDateQuantifiedExpression_UsingLiterals() throws InstantiationException {
    assertEquals(
        "every $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) satisfies $x <= xs:date('2012-01-01')",
        test("ND-Root",
            "every date:$x in (2012-01-01, 2012-01-02, 2012-01-03) satisfies $x <= 2012-01-01"));
  }

  @Test
  void testDateQuantifiedExpression_UsingFieldReference() throws InstantiationException {
    assertEquals("every $x in PathNode/StartDateField satisfies $x <= xs:date('2012-01-01')",
        test("ND-Root", "every date:$x in BT-00-StartDate satisfies $x <= 2012-01-01"));
  }

  @Test
  void testTimeQuantifiedExpression_UsingLiterals() throws InstantiationException {
    assertEquals(
        "every $x in (xs:time('00:00:00'),xs:time('00:00:01'),xs:time('00:00:02')) satisfies $x <= xs:time('00:00:00')",
        test("ND-Root",
            "every time:$x in (00:00:00, 00:00:01, 00:00:02) satisfies $x <= 00:00:00"));
  }

  @Test
  void testTimeQuantifiedExpression_UsingFieldReference() throws InstantiationException {
    assertEquals("every $x in PathNode/StartTimeField satisfies $x <= xs:time('00:00:00')",
        test("ND-Root", "every time:$x in BT-00-StartTime satisfies $x <= 00:00:00"));
  }

  @Test
  void testDurationQuantifiedExpression_UsingLiterals() throws InstantiationException {
    assertEquals(
        "every $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')) satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
        test("ND-Root", "every measure:$x in (P1D, P2D, P3D) satisfies $x <= P1D"));
  }

  @Test
  void testDurationQuantifiedExpression_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "every $x in PathNode/MeasureField satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
        test("ND-Root", "every measure:$x in BT-00-Measure satisfies $x <= P1D"));
  }

  /*** Conditional expressions ***/

  @Test
  void testConditionalExpression() throws InstantiationException {
    assertEquals("(if 1 > 2 then 'a' else 'b')", test("ND-Root", "if 1 > 2 then 'a' else 'b'"));
  }

  @Test
  void testConditionalStringExpression_UsingLiterals() throws InstantiationException {
    assertEquals("(if 'a' > 'b' then 'a' else 'b')",
        test("ND-Root", "if 'a' > 'b' then 'a' else 'b'"));
  }

  @Test
  void testConditionalStringExpression_UsingFieldReferenceInCondition() throws InstantiationException {
    assertEquals("(if 'a' > PathNode/TextField/normalize-space(text()) then 'a' else 'b')",
        test("ND-Root", "if 'a' > BT-00-Text then 'a' else 'b'"));
    assertEquals("(if PathNode/TextField/normalize-space(text()) >= 'a' then 'a' else 'b')",
        test("ND-Root", "if BT-00-Text >= 'a' then 'a' else 'b'"));
    assertEquals(
        "(if PathNode/TextField/normalize-space(text()) >= PathNode/TextField/normalize-space(text()) then 'a' else 'b')",
        test("ND-Root", "if BT-00-Text >= BT-00-Text then 'a' else 'b'"));
    assertEquals(
        "(if PathNode/StartDateField/xs:date(text()) >= PathNode/EndDateField/xs:date(text()) then 'a' else 'b')",
        test("ND-Root", "if BT-00-StartDate >= BT-00-EndDate then 'a' else 'b'"));
  }

  @Test
  void testConditionalStringExpression_UsingFieldReference() throws InstantiationException {
    assertEquals("(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else 'b')",
        test("ND-Root", "if 'a' > 'b' then BT-00-Text else 'b'"));
    assertEquals("(if 'a' > 'b' then 'a' else PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "if 'a' > 'b' then 'a' else BT-00-Text"));
    assertEquals(
        "(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "if 'a' > 'b' then BT-00-Text else BT-00-Text"));
  }

  @Test
  void testConditionalStringExpression_UsingFieldReferences_TypeMismatch() throws InstantiationException {
    assertThrows(ParseCancellationException.class,
        () -> test("ND-Root", "if 'a' > 'b' then BT-00-StartDate else BT-00-Text"));
  }

  @Test
  void testConditionalBooleanExpression() throws InstantiationException {
    assertEquals("(if PathNode/IndicatorField then true() else false())",
        test("ND-Root", "if BT-00-Indicator then TRUE else FALSE"));
  }

  @Test
  void testConditionalNumericExpression() throws InstantiationException {
    assertEquals("(if 1 > 2 then 1 else PathNode/NumberField/number())",
        test("ND-Root", "if 1 > 2 then 1 else BT-00-Number"));
  }

  @Test
  void testConditionalDateExpression() throws InstantiationException {
    assertEquals(
        "(if xs:date('2012-01-01') > PathNode/EndDateField/xs:date(text()) then PathNode/StartDateField/xs:date(text()) else xs:date('2012-01-02'))",
        test("ND-Root", "if 2012-01-01 > BT-00-EndDate then BT-00-StartDate else 2012-01-02"));
  }

  @Test
  void testConditionalTimeExpression() throws InstantiationException {
    assertEquals(
        "(if PathNode/EndTimeField/xs:time(text()) > xs:time('00:00:01') then PathNode/StartTimeField/xs:time(text()) else xs:time('00:00:01'))",
        test("ND-Root", "if BT-00-EndTime > 00:00:01 then BT-00-StartTime else 00:00:01"));
  }

  @Test
  void testConditionalDurationExpression() throws InstantiationException {
    assertEquals(
        "(if boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P1D') > $T + (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ()))) then xs:dayTimeDuration('P1D') else xs:dayTimeDuration('P2D'))",
        test("ND-Root", "if P1D > BT-00-Measure then P1D else P2D"));
  }

  /*** Iteration expressions ***/

  // Strings from iteration ---------------------------------------------------

  @Test
  void testStringsFromStringIteration_UsingLiterals() throws InstantiationException {
    assertEquals("'a' = (for $x in ('a','b','c') return concat($x, 'text'))",
        test("ND-Root", "'a' in (for text:$x in ('a', 'b', 'c') return concat($x, 'text'))"));
  }

  @Test
  void testStringsFromStringIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("'a' = (for $x in PathNode/TextField return concat($x, 'text'))",
        test("ND-Root", "'a' in (for text:$x in BT-00-Text return concat($x, 'text'))"));
  }


  @Test
  void testStringsFromBooleanIteration_UsingLiterals() throws InstantiationException {
    assertEquals("'a' = (for $x in (true(),false()) return 'y')",
        test("ND-Root", "'a' in (for indicator:$x in (TRUE, FALSE) return 'y')"));
  }

  @Test
  void testStringsFromBooleanIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("'a' = (for $x in PathNode/IndicatorField return 'y')",
        test("ND-Root", "'a' in (for indicator:$x in BT-00-Indicator return 'y')"));
  }


  @Test
  void testStringsFromNumericIteration_UsingLiterals() throws InstantiationException {
    assertEquals("'a' = (for $x in (1,2,3) return 'y')",
        test("ND-Root", "'a' in (for number:$x in (1, 2, 3) return 'y')"));
  }

  @Test
  void testStringsFromNumericIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("'a' = (for $x in PathNode/NumberField return 'y')",
        test("ND-Root", "'a' in (for number:$x in BT-00-Number return 'y')"));
  }

  @Test
  void testStringsFromDateIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "'a' = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return 'y')",
        test("ND-Root", "'a' in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 'y')"));
  }

  @Test
  void testStringsFromDateIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("'a' = (for $x in PathNode/StartDateField return 'y')",
        test("ND-Root", "'a' in (for date:$x in BT-00-StartDate return 'y')"));
  }

  @Test
  void testStringsFromTimeIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "'a' = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return 'y')",
        test("ND-Root", "'a' in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 'y')"));
  }

  @Test
  void testStringsFromTimeIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("'a' = (for $x in PathNode/StartTimeField return 'y')",
        test("ND-Root", "'a' in (for time:$x in BT-00-StartTime return 'y')"));
  }

  @Test
  void testStringsFromDurationIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "'a' = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 'y')",
        test("ND-Root", "'a' in (for measure:$x in (P1D, P1Y, P2M) return 'y')"));
  }


  @Test
  void testStringsFromDurationIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("'a' = (for $x in PathNode/MeasureField return 'y')",
        test("ND-Root", "'a' in (for measure:$x in BT-00-Measure return 'y')"));
  }

  // Numbers from iteration ---------------------------------------------------

  @Test
  void testNumbersFromStringIteration_UsingLiterals() throws InstantiationException {
    assertEquals("123 = (for $x in ('a','b','c') return number($x))",
        test("ND-Root", "123 in (for text:$x in ('a', 'b', 'c') return number($x))"));
  }

  @Test
  void testNumbersFromStringIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("123 = (for $x in PathNode/TextField return number($x))",
        test("ND-Root", "123 in (for text:$x in BT-00-Text return number($x))"));
  }


  @Test
  void testNumbersFromBooleanIteration_UsingLiterals() throws InstantiationException {
    assertEquals("123 = (for $x in (true(),false()) return 0)",
        test("ND-Root", "123 in (for indicator:$x in (TRUE, FALSE) return 0)"));
  }

  @Test
  void testNumbersFromBooleanIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("123 = (for $x in PathNode/IndicatorField return 0)",
        test("ND-Root", "123 in (for indicator:$x in BT-00-Indicator return 0)"));
  }


  @Test
  void testNumbersFromNumericIteration_UsingLiterals() throws InstantiationException {
    assertEquals("123 = (for $x in (1,2,3) return 0)",
        test("ND-Root", "123 in (for number:$x in (1, 2, 3) return 0)"));
  }

  @Test
  void testNumbersFromNumericIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("123 = (for $x in PathNode/NumberField return 0)",
        test("ND-Root", "123 in (for number:$x in BT-00-Number return 0)"));
  }

  @Test
  void testNumbersFromDateIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "123 = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return 0)",
        test("ND-Root", "123 in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 0)"));
  }

  @Test
  void testNumbersFromDateIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("123 = (for $x in PathNode/StartDateField return 0)",
        test("ND-Root", "123 in (for date:$x in BT-00-StartDate return 0)"));
  }

  @Test
  void testNumbersFromTimeIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "123 = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return 0)",
        test("ND-Root", "123 in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 0)"));
  }

  @Test
  void testNumbersFromTimeIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("123 = (for $x in PathNode/StartTimeField return 0)",
        test("ND-Root", "123 in (for time:$x in BT-00-StartTime return 0)"));
  }

  @Test
  void testNumbersFromDurationIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "123 = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 0)",
        test("ND-Root", "123 in (for measure:$x in (P1D, P1Y, P2M) return 0)"));
  }


  @Test
  void testNumbersFromDurationIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("123 = (for $x in PathNode/MeasureField return 0)",
        test("ND-Root", "123 in (for measure:$x in BT-00-Measure return 0)"));
  }

  // Dates from iteration ---------------------------------------------------

  @Test
  void testDatesFromStringIteration_UsingLiterals() throws InstantiationException {
    assertEquals("xs:date('2022-01-01') = (for $x in ('a','b','c') return xs:date($x))",
        test("ND-Root", "2022-01-01 in (for text:$x in ('a', 'b', 'c') return date($x))"));
  }

  @Test
  void testDatesFromStringIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("xs:date('2022-01-01') = (for $x in PathNode/TextField return xs:date($x))",
        test("ND-Root", "2022-01-01 in (for text:$x in BT-00-Text return date($x))"));
  }


  @Test
  void testDatesFromBooleanIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in (true(),false()) return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for indicator:$x in (TRUE, FALSE) return 2022-01-01)"));
  }

  @Test
  void testDatesFromBooleanIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in PathNode/IndicatorField return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for indicator:$x in BT-00-Indicator return 2022-01-01)"));
  }


  @Test
  void testDatesFromNumericIteration_UsingLiterals() throws InstantiationException {
    assertEquals("xs:date('2022-01-01') = (for $x in (1,2,3) return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for number:$x in (1, 2, 3) return 2022-01-01)"));
  }

  @Test
  void testDatesFromNumericIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in PathNode/NumberField return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for number:$x in BT-00-Number return 2022-01-01)"));
  }

  @Test
  void testDatesFromDateIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return xs:date('2022-01-01'))",
        test("ND-Root",
            "2022-01-01 in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 2022-01-01)"));
  }

  @Test
  void testDatesFromDateIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in PathNode/StartDateField return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for date:$x in BT-00-StartDate return 2022-01-01)"));
  }

  @Test
  void testDatesFromTimeIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return xs:date('2022-01-01'))",
        test("ND-Root",
            "2022-01-01 in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 2022-01-01)"));
  }

  @Test
  void testDatesFromTimeIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in PathNode/StartTimeField return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for time:$x in BT-00-StartTime return 2022-01-01)"));
  }

  @Test
  void testDatesFromDurationIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for measure:$x in (P1D, P1Y, P2M) return 2022-01-01)"));
  }


  @Test
  void testDatesFromDurationIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (for $x in PathNode/MeasureField return xs:date('2022-01-01'))",
        test("ND-Root", "2022-01-01 in (for measure:$x in BT-00-Measure return 2022-01-01)"));
  }

  // Times from iteration ---------------------------------------------------

  @Test
  void testTimesFromStringIteration_UsingLiterals() throws InstantiationException {
    assertEquals("xs:time('12:00:00') = (for $x in ('a','b','c') return xs:time($x))",
        test("ND-Root", "12:00:00 in (for text:$x in ('a', 'b', 'c') return time($x))"));
  }

  @Test
  void testTimesFromStringIteration_UsingFieldReference() throws InstantiationException {
    assertEquals("xs:time('12:00:00') = (for $x in PathNode/TextField return xs:time($x))",
        test("ND-Root", "12:00:00 in (for text:$x in BT-00-Text return time($x))"));
  }


  @Test
  void testTimesFromBooleanIteration_UsingLiterals() throws InstantiationException {
    assertEquals("xs:time('12:00:00') = (for $x in (true(),false()) return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for indicator:$x in (TRUE, FALSE) return 12:00:00)"));
  }

  @Test
  void testTimesFromBooleanIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in PathNode/IndicatorField return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for indicator:$x in BT-00-Indicator return 12:00:00)"));
  }


  @Test
  void testTimesFromNumericIteration_UsingLiterals() throws InstantiationException {
    assertEquals("xs:time('12:00:00') = (for $x in (1,2,3) return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for number:$x in (1, 2, 3) return 12:00:00)"));
  }

  @Test
  void testTimesFromNumericIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in PathNode/NumberField return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for number:$x in BT-00-Number return 12:00:00)"));
  }

  @Test
  void testTimesFromDateIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return xs:time('12:00:00'))",
        test("ND-Root",
            "12:00:00 in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 12:00:00)"));
  }

  @Test
  void testTimesFromDateIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in PathNode/StartDateField return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for date:$x in BT-00-StartDate return 12:00:00)"));
  }

  @Test
  void testTimesFromTimeIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return xs:time('12:00:00'))",
        test("ND-Root",
            "12:00:00 in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 12:00:00)"));
  }

  @Test
  void testTimesFromTimeIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in PathNode/StartTimeField return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for time:$x in BT-00-StartTime return 12:00:00)"));
  }

  @Test
  void testTimesFromDurationIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for measure:$x in (P1D, P1Y, P2M) return 12:00:00)"));
  }


  @Test
  void testTimesFromDurationIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:time('12:00:00') = (for $x in PathNode/MeasureField return xs:time('12:00:00'))",
        test("ND-Root", "12:00:00 in (for measure:$x in BT-00-Measure return 12:00:00)"));
  }

  // Durations from iteration ---------------------------------------------------

  @Test
  void testDurationsFromStringIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')) return $x)",
        test("ND-Root", "P1D in (for measure:$x in (P1D, P2D, P1W) return $x)"));
  }

  @Test
  void testDurationsFromStringIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/TextField return xs:dayTimeDuration($x))",
        test("ND-Root", "P1D in (for text:$x in BT-00-Text return day-time-duration($x))"));
  }


  @Test
  void testDurationsFromBooleanIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in (true(),false()) return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for indicator:$x in (TRUE, FALSE) return P1D)"));
  }

  @Test
  void testDurationsFromBooleanIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/IndicatorField return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for indicator:$x in BT-00-Indicator return P1D)"));
  }


  @Test
  void testDurationsFromNumericIteration_UsingLiterals() throws InstantiationException {
    assertEquals("xs:dayTimeDuration('P1D') = (for $x in (1,2,3) return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for number:$x in (1, 2, 3) return P1D)"));
  }

  @Test
  void testDurationsFromNumericIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/NumberField return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for number:$x in BT-00-Number return P1D)"));
  }

  @Test
  void testDurationsFromDateIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return P1D)"));
  }

  @Test
  void testDurationsFromDateIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartDateField return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for date:$x in BT-00-StartDate return P1D)"));
  }

  @Test
  void testDurationsFromTimeIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return P1D)"));
  }

  @Test
  void testDurationsFromTimeIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartTimeField return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for time:$x in BT-00-StartTime return P1D)"));
  }

  @Test
  void testDurationsFromDurationIteration_UsingLiterals() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for measure:$x in (P1D, P1Y, P2M) return P1D)"));
  }

  @Test
  void testDurationsFromDurationIteration_UsingFieldReference() throws InstantiationException {
    assertEquals(
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/MeasureField return xs:dayTimeDuration('P1D'))",
        test("ND-Root", "P1D in (for measure:$x in BT-00-Measure return P1D)"));
  }

  /*** Numeric expressions ***/

  @Test
  void testMultiplicationExpression() throws InstantiationException {
    assertEquals("3 * 4", test("BT-00-Text", "3 * 4"));
  }

  @Test
  void testAdditionExpression() throws InstantiationException {
    assertEquals("4 + 4", test("BT-00-Text", "4 + 4"));
  }

  @Test
  void testParenthesizedNumericExpression() throws InstantiationException {
    assertEquals("(2 + 2) * 4", test("BT-00-Text", "(2 + 2)*4"));
  }

  @Test
  void testNumericLiteralExpression() throws InstantiationException {
    assertEquals("3.1415", test("BT-00-Text", "3.1415"));
  }

  /*** List ***/

  @Test
  void testStringList() throws InstantiationException {
    assertEquals("'a' = ('a','b','c')", test("BT-00-Text", "'a' in ('a', 'b', 'c')"));
  }

  @Test
  void testNumericList_UsingNumericLiterals() throws InstantiationException {
    assertEquals("4 = (1,2,3)", test("BT-00-Text", "4 in (1, 2, 3)"));
  }

  @Test
  void testNumericList_UsingNumericField() throws InstantiationException {
    assertEquals("4 = (1,../NumberField/number(),3)",
        test("BT-00-Text", "4 in (1, BT-00-Number, 3)"));
  }

  @Test
  void testNumericList_UsingTextField() throws InstantiationException {
    assertThrows(ParseCancellationException.class,
        () -> test("BT-00-Text", "4 in (1, BT-00-Text, 3)"));
  }

  @Test
  void testBooleanList() throws InstantiationException {
    assertEquals("false() = (true(),PathNode/IndicatorField,true())",
        test("ND-Root", "NEVER in (TRUE, BT-00-Indicator, ALWAYS)"));
  }

  @Test
  void testDateList() throws InstantiationException {
    assertEquals(
        "xs:date('2022-01-01') = (xs:date('2022-01-02'),PathNode/StartDateField/xs:date(text()),xs:date('2022-02-02'))",
        test("ND-Root", "2022-01-01 in (2022-01-02, BT-00-StartDate, 2022-02-02)"));
  }

  @Test
  void testTimeList() throws InstantiationException {
    assertEquals(
        "xs:time('12:20:21') = (xs:time('12:30:00'),PathNode/StartTimeField/xs:time(text()),xs:time('13:40:00'))",
        test("ND-Root", "12:20:21 in (12:30:00, BT-00-StartTime, 13:40:00)"));
  }

  @Test
  void testDurationList_UsingDurationLiterals() throws InstantiationException {
    assertEquals(
        "xs:yearMonthDuration('P3M') = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        test("BT-00-Text", "P3M in (P1M, P3M, P6M)"));
  }



  @Test
  void testDurationList_UsingDurationField() throws InstantiationException {
    assertEquals(
        "(if (../MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', ../MeasureField/number() * 7, 'D')) else if (../MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', ../MeasureField/number(), 'D')) else if (../MeasureField) then xs:yearMonthDuration(concat('P', ../MeasureField/number(), upper-case(substring(../MeasureField/@unitCode, 1, 1)))) else ()) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        test("BT-00-Text", "BT-00-Measure in (P1M, P3M, P6M)"));
  }

  @Test
  void testCodeList() throws InstantiationException {
    assertEquals("'a' = ('code1','code2','code3')", test("BT-00-Text", "'a' in (accessibility)"));
  }


  /*** References ***/

  @Test
  void testFieldAttributeValueReference() throws InstantiationException {
    assertEquals("PathNode/TextField/@Attribute = 'text'",
        test("ND-Root", "BT-00-Attribute == 'text'"));
  }

  @Test
  void testUntypedAttributeValueReference() throws InstantiationException {
    assertEquals("PathNode/CodeField/@listName", test("ND-Root", "BT-00-Code/@listName"));
  }

  @Test
  void testFieldReferenceWithPredicate() throws InstantiationException {
    assertEquals("PathNode/IndicatorField['a' = 'a']",
        test("ND-Root", "BT-00-Indicator['a' == 'a']"));
  }

  @Test
  void testFieldReferenceWithPredicate_WithFieldReferenceInPredicate() throws InstantiationException {
    assertEquals("PathNode/IndicatorField[../CodeField/normalize-space(text()) = 'a']",
        test("ND-Root", "BT-00-Indicator[BT-00-Code == 'a']"));
  }

  @Test
  void testFieldReferenceInOtherNotice() throws InstantiationException {
    assertEquals(
        "fn:doc(concat('http://notice.service/', 'da4d46e9-490b-41ff-a2ae-8166d356a619')')/PathNode/TextField/normalize-space(text())",
        test("ND-Root", "notice('da4d46e9-490b-41ff-a2ae-8166d356a619')/BT-00-Text"));
  }

  @Test
  void testFieldReferenceWithFieldContextOverride() throws InstantiationException {
    assertEquals("../TextField/normalize-space(text())",
        test("BT-00-Code", "BT-01-SubLevel-Text::BT-00-Text"));
  }

  @Test
  void testFieldReferenceWithFieldContextOverride_WithIntegerField() throws InstantiationException {
    assertEquals("../IntegerField/number()",
        test("BT-00-Code", "BT-01-SubLevel-Text::BT-00-Integer"));
  }

  @Test
  void testFieldReferenceWithNodeContextOverride() throws InstantiationException {
    assertEquals("../../PathNode/IntegerField/number()",
        test("BT-00-Text", "ND-Root::BT-00-Integer"));
  }

  @Test
  void testFieldReferenceWithNodeContextOverride_WithPredicate() throws InstantiationException {
    assertEquals("../../PathNode/IntegerField/number()",
        test("BT-00-Text", "ND-Root[BT-00-Indicator == TRUE]::BT-00-Integer"));
  }

  @Test
  void testAbsoluteFieldReference() throws InstantiationException {
    assertEquals("/*/PathNode/IndicatorField", test("BT-00-Text", "/BT-00-Indicator"));
  }

  @Test
  void testSimpleFieldReference() throws InstantiationException {
    assertEquals("../IndicatorField", test("BT-00-Text", "BT-00-Indicator"));
  }

  @Test
  void testFieldReference_ForDurationFields() throws InstantiationException {
    assertEquals(
        "(if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ())",
        test("ND-Root", "BT-00-Measure"));
  }

  /*** Boolean functions ***/

  @Test
  void testNotFunction() throws InstantiationException {
    assertEquals("not(true())", test("BT-00-Text", "not(ALWAYS)"));
    assertEquals("not(1 + 1 = 2)", test("BT-00-Text", "not(1 + 1 == 2)"));
    assertThrows(ParseCancellationException.class, () -> test("BT-00-Text", "not('text')"));
  }

  @Test
  void testContainsFunction() throws InstantiationException {
    assertEquals("contains(PathNode/TextField/normalize-space(text()), 'xyz')",
        test("ND-Root", "contains(BT-00-Text, 'xyz')"));
  }

  @Test
  void testStartsWithFunction() throws InstantiationException {
    assertEquals("starts-with(PathNode/TextField/normalize-space(text()), 'abc')",
        test("ND-Root", "starts-with(BT-00-Text, 'abc')"));
  }

  @Test
  void testEndsWithFunction() throws InstantiationException {
    assertEquals("ends-with(PathNode/TextField/normalize-space(text()), 'abc')",
        test("ND-Root", "ends-with(BT-00-Text, 'abc')"));
  }

  /*** Numeric functions ***/

  @Test
  void testCountFunction_UsingFieldReference() throws InstantiationException {
    assertEquals("count(PathNode/TextField)", test("ND-Root", "count(BT-00-Text)"));
  }

  @Test
  void testCountFunction_UsingSequenceFromIteration() throws InstantiationException {
    assertEquals("count(for $x in PathNode/TextField return concat($x, '-xyz'))",
        test("ND-Root", "count(for text:$x in BT-00-Text return concat($x, '-xyz'))"));
  }

  @Test
  void testNumberFunction() throws InstantiationException {
    assertEquals("number(PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "number(BT-00-Text)"));
  }

  @Test
  void testSumFunction_UsingFieldReference() throws InstantiationException {
    assertEquals("sum(PathNode/NumberField)", test("ND-Root", "sum(BT-00-Number)"));
  }

  @Test
  void testSumFunction_UsingNumericSequenceFromIteration() throws InstantiationException {
    assertEquals("sum(for $v in PathNode/NumberField return $v + 1)",
        test("ND-Root", "sum(for number:$v in BT-00-Number return $v +1)"));
  }

  @Test
  void testStringLengthFunction() throws InstantiationException {
    assertEquals("string-length(PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "string-length(BT-00-Text)"));
  }

  /*** String functions ***/

  @Test
  void testSubstringFunction() throws InstantiationException {
    assertEquals("substring(PathNode/TextField/normalize-space(text()), 1, 3)",
        test("ND-Root", "substring(BT-00-Text, 1, 3)"));
    assertEquals("substring(PathNode/TextField/normalize-space(text()), 4)",
        test("ND-Root", "substring(BT-00-Text, 4)"));
  }

  @Test
  void testToStringFunction() throws InstantiationException {
    assertEquals("string(123)", test("ND-Root", "string(123)"));
  }

  @Test
  void testConcatFunction() throws InstantiationException {
    assertEquals("concat('abc', 'def')", test("ND-Root", "concat('abc', 'def')"));
  };

  @Test
  void testFormatNumberFunction() throws InstantiationException {
    assertEquals("format-number(PathNode/NumberField/number(), '#,##0.00')",
        test("ND-Root", "format-number(BT-00-Number, '#,##0.00')"));
  }


  /*** Date functions ***/

  @Test
  void testDateFromStringFunction() throws InstantiationException {
    assertEquals("xs:date(PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "date(BT-00-Text)"));
  }

  /*** Time functions ***/

  @Test
  void testTimeFromStringFunction() throws InstantiationException {
    assertEquals("xs:time(PathNode/TextField/normalize-space(text()))",
        test("ND-Root", "time(BT-00-Text)"));
  }
}
