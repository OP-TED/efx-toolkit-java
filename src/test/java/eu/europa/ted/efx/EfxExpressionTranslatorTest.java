package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

public class EfxExpressionTranslatorTest {

    final private String SDK_VERSION = "eforms-sdk-0.7";

    private String test(final String context, final String expression) {
        return EfxTranslator.translateExpression(context, expression, DependencyFactoryMock.INSTANCE, SDK_VERSION);
    }

    /*** Boolean expressions ***/

    @Test
    public void testParenthesizedBooleanExpression() {
        assertEquals("(true() or true()) and false()",
                test("BT-00-Text", "(ALWAYS or TRUE) and NEVER"));
    }

    @Test
    public void testLogicalOrCondition() {
        assertEquals("true() or false()", test("BT-00-Text", "ALWAYS or NEVER"));
    }

    @Test
    public void testLogicalAndCondition() {
        assertEquals("true() and 1 + 1 = 2", test("BT-00-Text", "ALWAYS and 1 + 1 == 2"));
    }

    @Test
    public void testInListCondition() {
        assertEquals("not('x' = ('a','b','c'))", test("BT-00-Text", "'x' not in ('a', 'b', 'c')"));
    }

    @Test
    public void testEmptinessCondition() {
        assertEquals("PathNode/TextField/normalize-space(text()) = ''",
                test("ND-Root", "BT-00-Text is empty"));
    }

    @Test
    public void testEmptinessCondition_WithNot() {
        assertEquals("PathNode/TextField/normalize-space(text()) != ''",
                test("ND-Root", "BT-00-Text is not empty"));
    }

    @Test
    public void testPresenceCondition() {
        assertEquals("PathNode/TextField",
                test("ND-Root", "BT-00-Text is present"));
    }

    @Test
    public void testPresenceCondition_WithNot() {
        assertEquals("not(PathNode/TextField)",
                test("ND-Root", "BT-00-Text is not present"));
    }

    @Test
    public void testLikePatternCondition() {
        assertEquals("fn:matches(normalize-space('123'), '[0-9]*')",
                test("BT-00-Text", "'123' like '[0-9]*'"));
    }

    @Test
    public void testLikePatternCondition_WithNot() {
        assertEquals("not(fn:matches(normalize-space('123'), '[0-9]*'))",
                test("BT-00-Text", "'123' not like '[0-9]*'"));
    }

    @Test
    public void testFieldValueComparison_UsingTextFields() {
        assertEquals("PathNode/TextField/normalize-space(text()) = PathNode/TextMultilingualField/normalize-space(text())",
                test("ND-Root", "BT-00-Text == BT-00-Text-Multilingual"));
    }

    @Test
    public void testFieldValueComparison_UsingNumericFields() {
        assertEquals("PathNode/NumberField/number() <= PathNode/IntegerField/number()",
                test("ND-Root", "BT-00-Number <= BT-00-Integer"));
    }

    @Test
    public void testFieldValueComparison_UsingIndicatorFields() {
        assertEquals("PathNode/IndicatorField != PathNode/IndicatorField",
                test("ND-Root", "BT-00-Indicator != BT-00-Indicator"));
    }

    @Test
    public void testFieldValueComparison_UsingDateFields() {
        assertEquals("PathNode/StartDateField/xs:date(text()) <= PathNode/EndDateField/xs:date(text())",
                test("ND-Root", "BT-00-StartDate <= BT-00-EndDate"));
    }
    
    @Test
    public void testFieldValueComparison_UsingTimeFields() {
        assertEquals("PathNode/StartTimeField/xs:time(text()) <= PathNode/EndTimeField/xs:time(text())",
                test("ND-Root", "BT-00-StartTime <= BT-00-EndTime"));
    }
    
    @Test
    public void testFieldValueComparison_UsingMeasureFields() {
        assertEquals("boolean(for $T in (current-date()) return ($T + (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ()) <= $T + (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ())))",
                test("ND-Root", "BT-00-Measure <= BT-00-Measure"));
    }

    @Test
    public void testFieldValueComparison_WithStringLiteral() {
        assertEquals("PathNode/TextField/normalize-space(text()) = 'abc'",
                test("ND-Root", "BT-00-Text == 'abc'"));
    }
    
    @Test
    public void testFieldValueComparison_WithNumericLiteral() {
        assertEquals("PathNode/IntegerField/number() - PathNode/NumberField/number() > 0",
                test("ND-Root", "BT-00-Integer - BT-00-Number > 0"));
    }

    @Test
    public void testFieldValueComparison_WithDateLiteral() {
        assertEquals("xs:date('2022-01-01') > PathNode/StartDateField/xs:date(text())",
                test("ND-Root", "2022-01-01 > BT-00-StartDate"));
    }

    @Test
    public void testFieldValueComparison_WithTimeLiteral() {
        assertEquals("xs:time('00:01:00') > PathNode/EndTimeField/xs:time(text())",
                test("ND-Root", "00:01:00 > BT-00-EndTime"));
    }

    @Test
    public void testFieldValueComparison_TypeMismatch() {
        assertThrows(ParseCancellationException.class,
                () -> test("ND-Root", "00:01:00 > BT-00-StartDate"));
    }

    
    @Test
    public void testBooleanComparison_UsingLiterals() {
        assertEquals("false() != true()",
                test("BT-00-Text", "NEVER != ALWAYS"));
    }

    @Test
    public void testBooleanComparison_UsingFieldReference() {
        assertEquals("../IndicatorField != true()",
                test("BT-00-Text", "BT-00-Indicator != ALWAYS"));
    }

    @Test
    public void testNumericComparison() {
        assertEquals("2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5 and ../NumberField/number() > ../IntegerField/number()",
                test("BT-00-Text", "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5 and BT-00-Number > BT-00-Integer"));
    }

    @Test
    public void testStringComparison() {
        assertEquals("'aaa' < 'bbb'",
                test("BT-00-Text", "'aaa' < 'bbb'"));
    }

    @Test
    public void testDateComparison_OfTwoDateLiterals() {
        assertEquals("xs:date('2018-01-01') > xs:date('2018-01-01')",
                test("BT-00-Text", "2018-01-01 > 2018-01-01"));
    }

    @Test
    public void testDateComparison_OfTwoDateReferences() {
        assertEquals("PathNode/StartDateField/xs:date(text()) = PathNode/EndDateField/xs:date(text())",
                test("ND-Root", "BT-00-StartDate == BT-00-EndDate"));
    }

    @Test
    public void testDateComparison_OfDateReferenceAndDateFunction() {
        assertEquals("PathNode/StartDateField/xs:date(text()) = xs:date(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "BT-00-StartDate == date(BT-00-Text)"));
    }
    
    @Test
    public void testTimeComparison_OfTwoTimeLiterals() {
        assertEquals("xs:time('13:00:10') > xs:time('21:20:30')",
                test("BT-00-Text", "13:00:10 > 21:20:30"));
    }

    @Test
    public void testZonedTimeComparison_OfTwoTimeLiterals() {
        assertEquals("xs:time('13:00:10+01:00') > xs:time('21:20:30+02:00')",
                test("BT-00-Text", "13:00:10+01:00 > 21:20:30+02:00"));
    }

    @Test
    public void testTimeComparison_OfTwoTimeReferences() {
        assertEquals("PathNode/StartTimeField/xs:time(text()) = PathNode/EndTimeField/xs:time(text())",
                test("ND-Root", "BT-00-StartTime == BT-00-EndTime"));
    }

    @Test
    public void testTimeComparison_OfTimeReferenceAndTimeFunction() {
        assertEquals("PathNode/StartTimeField/xs:time(text()) = xs:time(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "BT-00-StartTime == time(BT-00-Text)"));
    }

    @Test
    public void testDurationComparison_UsingYearMOnthDurationLiterals() {
        assertEquals("boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P1Y') = $T + xs:yearMonthDuration('P12M')))",
                test("BT-00-Text", "P1Y == P12M"));
    }

    @Test
    public void testDurationComparison_UsingDayTimeDurationLiterals() {
        assertEquals("boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P21D') > $T + xs:dayTimeDuration('P7D')))",
                test("BT-00-Text", "P3W > P7D"));
    }

    @Test
    public void testCalculatedDurationComparison() {
        assertEquals("boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P3M') > $T + xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text()))))",
                test("ND-Root", "P3M > (BT-00-EndDate - BT-00-StartDate)"));
    }

    
    @Test
    public void testNegativeDuration_Literal() {
        assertEquals("xs:yearMonthDuration('-P3M')",
                test("ND-Root", "-P3M"));
    }

    @Test
    public void testNegativeDuration_ViaMultiplication() {
        assertEquals("(-3 * (2 * xs:yearMonthDuration('-P3M')))",
                test("ND-Root", "2 * -P3M * -3"));
    }

    @Test
    public void testNegativeDuration_ViaMultiplicationWithField() {
        assertEquals("(-3 * (2 * (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ())))",
                test("ND-Root", "2 * measure:BT-00-Measure * -3"));
    }

    @Test
    public void testDurationAddition() {
        assertEquals("(xs:dayTimeDuration('P3D') + xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
                test("ND-Root", "P3D + (BT-00-StartDate - BT-00-EndDate)"));
    }

    @Test
    public void testDurationSubtraction() {
        assertEquals("(xs:dayTimeDuration('P3D') - xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
                test("ND-Root", "P3D - (BT-00-StartDate - BT-00-EndDate)"));
    }

    @Test
    public void testBooleanLiteralExpression_Always() {
        assertEquals("true()", test("BT-00-Text", "ALWAYS"));
    }

    @Test
    public void testBooleanLiteralExpression_Never() {
        assertEquals("false()", test("BT-00-Text", "NEVER"));
    }

    /*** Quantified expressions ***/

    @Test
    public void testStringQuantifiedExpression_UsingLiterals() {
        assertEquals("every $x in ('a','b','c') satisfies $x <= 'a'",
                test("ND-Root", "every text:$x in ('a', 'b', 'c') satisfies $x <= 'a'"));
    }

    @Test
    public void testStringQuantifiedExpression_UsingFieldReference() {
        assertEquals("every $x in PathNode/TextField satisfies $x <= 'a'",
                test("ND-Root", "every text:$x in BT-00-Text satisfies $x <= 'a'"));
    }

    @Test
    public void testBooleanQuantifiedExpression_UsingLiterals() {
        assertEquals("every $x in (true(),false(),true()) satisfies $x",
                test("ND-Root", "every indicator:$x in (TRUE, FALSE, ALWAYS) satisfies $x"));
    }

    @Test
    public void testBooleanQuantifiedExpression_UsingFieldReference() {
        assertEquals("every $x in PathNode/IndicatorField satisfies $x",
                test("ND-Root", "every indicator:$x in BT-00-Indicator satisfies $x"));
    }
    
    @Test
    public void testNumericQuantifiedExpression_UsingLiterals() {
        assertEquals("every $x in (1,2,3) satisfies $x <= 1",
                test("ND-Root", "every number:$x in (1, 2, 3) satisfies $x <= 1"));
    }

    @Test
    public void testNumericQuantifiedExpression_UsingFieldReference() {
        assertEquals("every $x in PathNode/NumberField satisfies $x <= 1",
                test("ND-Root", "every number:$x in BT-00-Number satisfies $x <= 1"));
    }

    @Test
    public void testDateQuantifiedExpression_UsingLiterals() {
        assertEquals(
                "every $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) satisfies $x <= xs:date('2012-01-01')",
                test("ND-Root",
                        "every date:$x in (2012-01-01, 2012-01-02, 2012-01-03) satisfies $x <= 2012-01-01"));
    }

    @Test
    public void testDateQuantifiedExpression_UsingFieldReference() {
        assertEquals(
                "every $x in PathNode/StartDateField satisfies $x <= xs:date('2012-01-01')",
                test("ND-Root",
                        "every date:$x in BT-00-StartDate satisfies $x <= 2012-01-01"));
    }

    @Test
    public void testTimeQuantifiedExpression_UsingLiterals() {
        assertEquals(
                "every $x in (xs:time('00:00:00'),xs:time('00:00:01'),xs:time('00:00:02')) satisfies $x <= xs:time('00:00:00')",
                test("ND-Root",
                        "every time:$x in (00:00:00, 00:00:01, 00:00:02) satisfies $x <= 00:00:00"));
    }

    @Test
    public void testTimeQuantifiedExpression_UsingFieldReference() {
        assertEquals(
                "every $x in PathNode/StartTimeField satisfies $x <= xs:time('00:00:00')",
                test("ND-Root",
                        "every time:$x in BT-00-StartTime satisfies $x <= 00:00:00"));
    }

    @Test
    public void testDurationQuantifiedExpression_UsingLiterals() {
        assertEquals(
                "every $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')) satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
                test("ND-Root",
                        "every measure:$x in (P1D, P2D, P3D) satisfies $x <= P1D"));
    }

    @Test
    public void testDurationQuantifiedExpression_UsingFieldReference() {
        assertEquals(
                "every $x in PathNode/MeasureField satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
                test("ND-Root",
                        "every measure:$x in BT-00-Measure satisfies $x <= P1D"));
    }

    /*** Conditional expressions ***/

    @Test
    public void testConditionalExpression() {
        assertEquals("(if 1 > 2 then 'a' else 'b')",
                test("ND-Root", "if 1 > 2 then 'a' else 'b'"));
    }

    @Test
    public void testConditionalStringExpression_UsingLiterals() {
        assertEquals("(if 'a' > 'b' then 'a' else 'b')",
                test("ND-Root", "if 'a' > 'b' then 'a' else 'b'"));
    }

    @Test
    public void testConditionalStringExpression_UsingFieldReferenceInCondition() {
        assertEquals("(if 'a' > PathNode/TextField/normalize-space(text()) then 'a' else 'b')",
                test("ND-Root", "if 'a' > BT-00-Text then 'a' else 'b'"));
        assertEquals("(if PathNode/TextField/normalize-space(text()) >= 'a' then 'a' else 'b')",
                test("ND-Root", "if BT-00-Text >= 'a' then 'a' else 'b'"));
        assertEquals("(if PathNode/TextField/normalize-space(text()) >= PathNode/TextField/normalize-space(text()) then 'a' else 'b')",
                test("ND-Root", "if BT-00-Text >= BT-00-Text then 'a' else 'b'"));
        assertEquals("(if PathNode/StartDateField/xs:date(text()) >= PathNode/EndDateField/xs:date(text()) then 'a' else 'b')",
                test("ND-Root", "if BT-00-StartDate >= BT-00-EndDate then 'a' else 'b'"));
}

    @Test
    public void testConditionalStringExpression_UsingFieldReference() {
        assertEquals("(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else 'b')",
                test("ND-Root", "if 'a' > 'b' then BT-00-Text else 'b'"));
        assertEquals("(if 'a' > 'b' then 'a' else PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "if 'a' > 'b' then 'a' else BT-00-Text"));
        assertEquals("(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "if 'a' > 'b' then BT-00-Text else BT-00-Text"));
    }

    @Test
    public void testConditionalStringExpression_UsingFieldReferences_TypeMismatch() {
        assertThrows(ParseCancellationException.class,
                () -> test("ND-Root", "if 'a' > 'b' then BT-00-StartDate else BT-00-Text"));
    }

    @Test
    public void testConditionalBooleanExpression() {
        assertEquals("(if PathNode/IndicatorField then true() else false())",
                test("ND-Root", "if BT-00-Indicator then TRUE else FALSE"));
    }

    @Test
    public void testConditionalNumericExpression() {
        assertEquals("(if 1 > 2 then 1 else PathNode/NumberField/number())", test("ND-Root", "if 1 > 2 then 1 else BT-00-Number"));
    }

    @Test
    public void testConditionalDateExpression() {
        assertEquals(
                "(if xs:date('2012-01-01') > PathNode/EndDateField/xs:date(text()) then PathNode/StartDateField/xs:date(text()) else xs:date('2012-01-02'))",
                test("ND-Root", "if 2012-01-01 > BT-00-EndDate then BT-00-StartDate else 2012-01-02"));
    }

    @Test
    public void testConditionalTimeExpression() {
        assertEquals(
                "(if PathNode/EndTimeField/xs:time(text()) > xs:time('00:00:01') then PathNode/StartTimeField/xs:time(text()) else xs:time('00:00:01'))",
                test("ND-Root", "if BT-00-EndTime > 00:00:01 then BT-00-StartTime else 00:00:01"));
    }

    @Test
    public void testConditionalDurationExpression() {
        assertEquals(
                "(if boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P1D') > $T + (if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ()))) then xs:dayTimeDuration('P1D') else xs:dayTimeDuration('P2D'))",
                test("ND-Root", "if P1D > BT-00-Measure then P1D else P2D"));
    }

    /*** Iteration expressions ***/

    // Strings from iteration ---------------------------------------------------

    @Test
    public void testStringsFromStringIteration_UsingLiterals() {
        assertEquals("'a' = (for $x in ('a','b','c') return concat($x, 'text'))",
                test("ND-Root", "'a' in (for text:$x in ('a', 'b', 'c') return concat($x, 'text'))"));
    }

    @Test
    public void testStringsFromStringIteration_UsingFieldReference() {
        assertEquals("'a' = (for $x in PathNode/TextField return concat($x, 'text'))",
                test("ND-Root", "'a' in (for text:$x in BT-00-Text return concat($x, 'text'))"));
    }


    @Test
    public void testStringsFromBooleanIteration_UsingLiterals() {
        assertEquals("'a' = (for $x in (true(),false()) return 'y')",
                test("ND-Root", "'a' in (for indicator:$x in (TRUE, FALSE) return 'y')"));
    }

    @Test
    public void testStringsFromBooleanIteration_UsingFieldReference() {
        assertEquals("'a' = (for $x in PathNode/IndicatorField return 'y')",
                test("ND-Root", "'a' in (for indicator:$x in BT-00-Indicator return 'y')"));
    }


    @Test
    public void testStringsFromNumericIteration_UsingLiterals() {
        assertEquals("'a' = (for $x in (1,2,3) return 'y')",
                test("ND-Root", "'a' in (for number:$x in (1, 2, 3) return 'y')"));
    }

    @Test
    public void testStringsFromNumericIteration_UsingFieldReference() {
        assertEquals("'a' = (for $x in PathNode/NumberField return 'y')",
                test("ND-Root", "'a' in (for number:$x in BT-00-Number return 'y')"));
    }

    @Test
    public void testStringsFromDateIteration_UsingLiterals() {
        assertEquals(
                "'a' = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return 'y')",
                test("ND-Root",
                        "'a' in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 'y')"));
    }

    @Test
    public void testStringsFromDateIteration_UsingFieldReference() {
        assertEquals("'a' = (for $x in PathNode/StartDateField return 'y')",
                test("ND-Root", "'a' in (for date:$x in BT-00-StartDate return 'y')"));
    }

    @Test
    public void testStringsFromTimeIteration_UsingLiterals() {
        assertEquals(
                "'a' = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return 'y')",
                test("ND-Root", "'a' in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 'y')"));
    }

    @Test
    public void testStringsFromTimeIteration_UsingFieldReference() {
        assertEquals("'a' = (for $x in PathNode/StartTimeField return 'y')",
                test("ND-Root", "'a' in (for time:$x in BT-00-StartTime return 'y')"));
    }

    @Test
    public void testStringsFromDurationIteration_UsingLiterals() {
        assertEquals(
                "'a' = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 'y')",
                test("ND-Root", "'a' in (for measure:$x in (P1D, P1Y, P2M) return 'y')"));
    }


    @Test
    public void testStringsFromDurationIteration_UsingFieldReference() {
        assertEquals("'a' = (for $x in PathNode/MeasureField return 'y')",
                test("ND-Root", "'a' in (for measure:$x in BT-00-Measure return 'y')"));
    }

    // Numbers from iteration ---------------------------------------------------

    @Test
    public void testNumbersFromStringIteration_UsingLiterals() {
        assertEquals("123 = (for $x in ('a','b','c') return number($x))",
                test("ND-Root", "123 in (for text:$x in ('a', 'b', 'c') return number($x))"));
    }

    @Test
    public void testNumbersFromStringIteration_UsingFieldReference() {
        assertEquals("123 = (for $x in PathNode/TextField return number($x))",
                test("ND-Root", "123 in (for text:$x in BT-00-Text return number($x))"));
    }


    @Test
    public void testNumbersFromBooleanIteration_UsingLiterals() {
        assertEquals("123 = (for $x in (true(),false()) return 0)",
                test("ND-Root", "123 in (for indicator:$x in (TRUE, FALSE) return 0)"));
    }

    @Test
    public void testNumbersFromBooleanIteration_UsingFieldReference() {
        assertEquals("123 = (for $x in PathNode/IndicatorField return 0)",
                test("ND-Root", "123 in (for indicator:$x in BT-00-Indicator return 0)"));
    }


    @Test
    public void testNumbersFromNumericIteration_UsingLiterals() {
        assertEquals("123 = (for $x in (1,2,3) return 0)",
                test("ND-Root", "123 in (for number:$x in (1, 2, 3) return 0)"));
    }

    @Test
    public void testNumbersFromNumericIteration_UsingFieldReference() {
        assertEquals("123 = (for $x in PathNode/NumberField return 0)",
                test("ND-Root", "123 in (for number:$x in BT-00-Number return 0)"));
    }

    @Test
    public void testNumbersFromDateIteration_UsingLiterals() {
        assertEquals(
                "123 = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return 0)",
                test("ND-Root",
                        "123 in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 0)"));
    }

    @Test
    public void testNumbersFromDateIteration_UsingFieldReference() {
        assertEquals("123 = (for $x in PathNode/StartDateField return 0)",
                test("ND-Root", "123 in (for date:$x in BT-00-StartDate return 0)"));
    }

    @Test
    public void testNumbersFromTimeIteration_UsingLiterals() {
        assertEquals(
                "123 = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return 0)",
                test("ND-Root", "123 in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 0)"));
    }

    @Test
    public void testNumbersFromTimeIteration_UsingFieldReference() {
        assertEquals("123 = (for $x in PathNode/StartTimeField return 0)",
                test("ND-Root", "123 in (for time:$x in BT-00-StartTime return 0)"));
    }

    @Test
    public void testNumbersFromDurationIteration_UsingLiterals() {
        assertEquals(
                "123 = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 0)",
                test("ND-Root", "123 in (for measure:$x in (P1D, P1Y, P2M) return 0)"));
    }


    @Test
    public void testNumbersFromDurationIteration_UsingFieldReference() {
        assertEquals("123 = (for $x in PathNode/MeasureField return 0)",
                test("ND-Root", "123 in (for measure:$x in BT-00-Measure return 0)"));
    }

    // Dates from iteration ---------------------------------------------------

    @Test
    public void testDatesFromStringIteration_UsingLiterals() {
        assertEquals("xs:date('2022-01-01') = (for $x in ('a','b','c') return xs:date($x))",
                test("ND-Root", "2022-01-01 in (for text:$x in ('a', 'b', 'c') return date($x))"));
    }

    @Test
    public void testDatesFromStringIteration_UsingFieldReference() {
        assertEquals("xs:date('2022-01-01') = (for $x in PathNode/TextField return xs:date($x))",
                test("ND-Root", "2022-01-01 in (for text:$x in BT-00-Text return date($x))"));
    }


    @Test
    public void testDatesFromBooleanIteration_UsingLiterals() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in (true(),false()) return xs:date('2022-01-01'))",
                test("ND-Root", "2022-01-01 in (for indicator:$x in (TRUE, FALSE) return 2022-01-01)"));
    }

    @Test
    public void testDatesFromBooleanIteration_UsingFieldReference() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in PathNode/IndicatorField return xs:date('2022-01-01'))",
                test("ND-Root",
                        "2022-01-01 in (for indicator:$x in BT-00-Indicator return 2022-01-01)"));
    }


    @Test
    public void testDatesFromNumericIteration_UsingLiterals() {
        assertEquals("xs:date('2022-01-01') = (for $x in (1,2,3) return xs:date('2022-01-01'))",
                test("ND-Root", "2022-01-01 in (for number:$x in (1, 2, 3) return 2022-01-01)"));
    }

    @Test
    public void testDatesFromNumericIteration_UsingFieldReference() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in PathNode/NumberField return xs:date('2022-01-01'))",
                test("ND-Root", "2022-01-01 in (for number:$x in BT-00-Number return 2022-01-01)"));
    }

    @Test
    public void testDatesFromDateIteration_UsingLiterals() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return xs:date('2022-01-01'))",
                test("ND-Root",
                        "2022-01-01 in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 2022-01-01)"));
    }

    @Test
    public void testDatesFromDateIteration_UsingFieldReference() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in PathNode/StartDateField return xs:date('2022-01-01'))",
                test("ND-Root",
                        "2022-01-01 in (for date:$x in BT-00-StartDate return 2022-01-01)"));
    }

    @Test
    public void testDatesFromTimeIteration_UsingLiterals() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return xs:date('2022-01-01'))",
                test("ND-Root",
                        "2022-01-01 in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 2022-01-01)"));
    }

    @Test
    public void testDatesFromTimeIteration_UsingFieldReference() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in PathNode/StartTimeField return xs:date('2022-01-01'))",
                test("ND-Root",
                        "2022-01-01 in (for time:$x in BT-00-StartTime return 2022-01-01)"));
    }

    @Test
    public void testDatesFromDurationIteration_UsingLiterals() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:date('2022-01-01'))",
                test("ND-Root", "2022-01-01 in (for measure:$x in (P1D, P1Y, P2M) return 2022-01-01)"));
    }


    @Test
    public void testDatesFromDurationIteration_UsingFieldReference() {
        assertEquals(
                "xs:date('2022-01-01') = (for $x in PathNode/MeasureField return xs:date('2022-01-01'))",
                test("ND-Root",
                        "2022-01-01 in (for measure:$x in BT-00-Measure return 2022-01-01)"));
    }

    // Times from iteration ---------------------------------------------------

    @Test
    public void testTimesFromStringIteration_UsingLiterals() {
        assertEquals("xs:time('12:00:00') = (for $x in ('a','b','c') return xs:time($x))",
                test("ND-Root", "12:00:00 in (for text:$x in ('a', 'b', 'c') return time($x))"));
    }

    @Test
    public void testTimesFromStringIteration_UsingFieldReference() {
        assertEquals("xs:time('12:00:00') = (for $x in PathNode/TextField return xs:time($x))",
                test("ND-Root", "12:00:00 in (for text:$x in BT-00-Text return time($x))"));
    }


    @Test
    public void testTimesFromBooleanIteration_UsingLiterals() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in (true(),false()) return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for indicator:$x in (TRUE, FALSE) return 12:00:00)"));
    }

    @Test
    public void testTimesFromBooleanIteration_UsingFieldReference() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in PathNode/IndicatorField return xs:time('12:00:00'))",
                test("ND-Root",
                        "12:00:00 in (for indicator:$x in BT-00-Indicator return 12:00:00)"));
    }


    @Test
    public void testTimesFromNumericIteration_UsingLiterals() {
        assertEquals("xs:time('12:00:00') = (for $x in (1,2,3) return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for number:$x in (1, 2, 3) return 12:00:00)"));
    }

    @Test
    public void testTimesFromNumericIteration_UsingFieldReference() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in PathNode/NumberField return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for number:$x in BT-00-Number return 12:00:00)"));
    }

    @Test
    public void testTimesFromDateIteration_UsingLiterals() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return xs:time('12:00:00'))",
                test("ND-Root",
                        "12:00:00 in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return 12:00:00)"));
    }

    @Test
    public void testTimesFromDateIteration_UsingFieldReference() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in PathNode/StartDateField return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for date:$x in BT-00-StartDate return 12:00:00)"));
    }

    @Test
    public void testTimesFromTimeIteration_UsingLiterals() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return xs:time('12:00:00'))",
                test("ND-Root",
                        "12:00:00 in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return 12:00:00)"));
    }

    @Test
    public void testTimesFromTimeIteration_UsingFieldReference() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in PathNode/StartTimeField return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for time:$x in BT-00-StartTime return 12:00:00)"));
    }

    @Test
    public void testTimesFromDurationIteration_UsingLiterals() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for measure:$x in (P1D, P1Y, P2M) return 12:00:00)"));
    }


    @Test
    public void testTimesFromDurationIteration_UsingFieldReference() {
        assertEquals(
                "xs:time('12:00:00') = (for $x in PathNode/MeasureField return xs:time('12:00:00'))",
                test("ND-Root", "12:00:00 in (for measure:$x in BT-00-Measure return 12:00:00)"));
    }

    // Durations from iteration ---------------------------------------------------

    @Test
    public void testDurationsFromStringIteration_UsingLiterals() {
        assertEquals("xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')) return $x)",
                test("ND-Root", "P1D in (for measure:$x in (P1D, P2D, P1W) return $x)"));
    } 

    @Test
    public void testDurationsFromStringIteration_UsingFieldReference() {
        assertEquals("xs:dayTimeDuration('P1D') = (for $x in PathNode/TextField return xs:dayTimeDuration($x))",
                test("ND-Root", "P1D in (for text:$x in BT-00-Text return day-time-duration($x))"));
    }


    @Test
    public void testDurationsFromBooleanIteration_UsingLiterals() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in (true(),false()) return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for indicator:$x in (TRUE, FALSE) return P1D)"));
    }

    @Test
    public void testDurationsFromBooleanIteration_UsingFieldReference() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in PathNode/IndicatorField return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for indicator:$x in BT-00-Indicator return P1D)"));
    }


    @Test
    public void testDurationsFromNumericIteration_UsingLiterals() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in (1,2,3) return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for number:$x in (1, 2, 3) return P1D)"));
    }

    @Test
    public void testDurationsFromNumericIteration_UsingFieldReference() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in PathNode/NumberField return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for number:$x in BT-00-Number return P1D)"));
    }

    @Test
    public void testDurationsFromDateIteration_UsingLiterals() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in (xs:date('2012-01-01'),xs:date('2012-01-02'),xs:date('2012-01-03')) return xs:dayTimeDuration('P1D'))",
                test("ND-Root",
                        "P1D in (for date:$x in (2012-01-01, 2012-01-02, 2012-01-03) return P1D)"));
    }

    @Test
    public void testDurationsFromDateIteration_UsingFieldReference() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartDateField return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for date:$x in BT-00-StartDate return P1D)"));
    }

    @Test
    public void testDurationsFromTimeIteration_UsingLiterals() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in (xs:time('12:00:00'),xs:time('12:00:01'),xs:time('12:00:02')) return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for time:$x in (12:00:00, 12:00:01, 12:00:02) return P1D)"));
    }

    @Test
    public void testDurationsFromTimeIteration_UsingFieldReference() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartTimeField return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for time:$x in BT-00-StartTime return P1D)"));
    }

    @Test
    public void testDurationsFromDurationIteration_UsingLiterals() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for measure:$x in (P1D, P1Y, P2M) return P1D)"));
    }

    @Test
    public void testDurationsFromDurationIteration_UsingFieldReference() {
        assertEquals(
                "xs:dayTimeDuration('P1D') = (for $x in PathNode/MeasureField return xs:dayTimeDuration('P1D'))",
                test("ND-Root", "P1D in (for measure:$x in BT-00-Measure return P1D)"));
    }

    /*** Numeric expressions ***/

    @Test
    public void testMultiplicationExpression() {
        assertEquals("3 * 4", test("BT-00-Text", "3 * 4"));
    }

    @Test
    public void testAdditionExpression() {
        assertEquals("4 + 4", test("BT-00-Text", "4 + 4"));
    }

    @Test
    public void testParenthesizedNumericExpression() {
        assertEquals("(2 + 2) * 4", test("BT-00-Text", "(2 + 2)*4"));
    }

    @Test
    public void testNumericLiteralExpression() {
        assertEquals("3.1415", test("BT-00-Text", "3.1415"));
    }

   /*** List ***/

    @Test
    public void testStringList() {
        assertEquals("'a' = ('a','b','c')", test("BT-00-Text", "'a' in ('a', 'b', 'c')"));
    }

    @Test
    public void testNumericList_UsingNumericLiterals() {
        assertEquals("4 = (1,2,3)", test("BT-00-Text", "4 in (1, 2, 3)"));
    }

    @Test
    public void testNumericList_UsingNumericField() {
        assertEquals("4 = (1,../NumberField/number(),3)", test("BT-00-Text", "4 in (1, BT-00-Number, 3)"));
    }

    @Test
    public void testNumericList_UsingTextField() {
        assertThrows(ParseCancellationException.class, () -> test("BT-00-Text", "4 in (1, BT-00-Text, 3)"));
    }

    @Test
    public void testBooleanList() {
        assertEquals("false() = (true(),PathNode/IndicatorField,true())", test("ND-Root", "NEVER in (TRUE, BT-00-Indicator, ALWAYS)"));
    }

    @Test
    public void testDateList() {
        assertEquals("xs:date('2022-01-01') = (xs:date('2022-01-02'),PathNode/StartDateField/xs:date(text()),xs:date('2022-02-02'))", test("ND-Root", "2022-01-01 in (2022-01-02, BT-00-StartDate, 2022-02-02)"));
    }

    @Test
    public void testTimeList() {
        assertEquals("xs:time('12:20:21') = (xs:time('12:30:00'),PathNode/StartTimeField/xs:time(text()),xs:time('13:40:00'))", test("ND-Root", "12:20:21 in (12:30:00, BT-00-StartTime, 13:40:00)"));
    }

    @Test
    public void testDurationList_UsingDurationLiterals() {
        assertEquals("xs:yearMonthDuration('P3M') = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))", test("BT-00-Text", "P3M in (P1M, P3M, P6M)"));
    }



    @Test
    public void testDurationList_UsingDurationField() {
        assertEquals("(if (../MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', ../MeasureField/number() * 7, 'D')) else if (../MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', ../MeasureField/number(), 'D')) else if (../MeasureField) then xs:yearMonthDuration(concat('P', ../MeasureField/number(), upper-case(substring(../MeasureField/@unitCode, 1, 1)))) else ()) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))", test("BT-00-Text", "BT-00-Measure in (P1M, P3M, P6M)"));
    }

    @Test
    public void testCodeList() {
        assertEquals("'a' = ('code1','code2','code3')",
                test("BT-00-Text", "'a' in (accessibility)"));
    }


    /*** References ***/
    
    @Test
    public void testFieldAttributeValueReference() {
        assertEquals("PathNode/TextField/@Attribute = 'text'",
                test("ND-Root", "BT-00-Attribute == 'text'"));
    }

    @Test
    public void testUntypedAttributeValueReference() {
        assertEquals("PathNode/CodeField/@listName",
                test("ND-Root", "BT-00-Code/@listName"));
    }

    @Test
    public void testFieldReferenceWithPredicate() {
        assertEquals("PathNode/IndicatorField['a' = 'a']",
                test("ND-Root", "BT-00-Indicator['a' == 'a']"));
    }

    @Test
    public void testFieldReferenceWithPredicate_WithFieldReferenceInPredicate() {
        assertEquals("PathNode/IndicatorField[../CodeField/normalize-space(text()) = 'a']",
                test("ND-Root", "BT-00-Indicator[BT-00-Code == 'a']"));
    }

    @Test
    public void testFieldReferenceInOtherNotice() {
        assertEquals("fn:doc(concat('http://notice.service/', 'da4d46e9-490b-41ff-a2ae-8166d356a619')')/PathNode/TextField/normalize-space(text())",
                test("ND-Root", "notice('da4d46e9-490b-41ff-a2ae-8166d356a619')/BT-00-Text"));
    }

    @Test
    public void testFieldReferenceWithFieldContextOverride() {
        assertEquals("../TextField/normalize-space(text())",
                test("BT-00-Code", "BT-01-SubLevel-Text::BT-00-Text"));
    }

    @Test
    public void testFieldReferenceWithFieldContextOverride_WithIntegerField() {
        assertEquals("../IntegerField/number()",
                test("BT-00-Code", "BT-01-SubLevel-Text::BT-00-Integer"));
    }

    @Test
    public void testFieldReferenceWithNodeContextOverride() {
        assertEquals("../../PathNode/IntegerField/number()",
                test("BT-00-Text", "ND-Root::BT-00-Integer"));
    }

    @Test
    public void testFieldReferenceWithNodeContextOverride_WithPredicate() {
        assertEquals("../../PathNode/IntegerField/number()",
                test("BT-00-Text", "ND-Root[BT-00-Indicator == TRUE]::BT-00-Integer"));
    }

    @Test
    public void testAbsoluteFieldReference() {
        assertEquals("/*/PathNode/IndicatorField",
                test("BT-00-Text", "/BT-00-Indicator"));
    }

    @Test
    public void testSimpleFieldReference() {
        assertEquals("../IndicatorField",
                test("BT-00-Text", "BT-00-Indicator"));
    }

    @Test
    public void testFieldReference_ForDurationFields() {
        assertEquals("(if (PathNode/MeasureField/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (PathNode/MeasureField/@unitCode='DAY') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), 'D')) else if (PathNode/MeasureField) then xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(substring(PathNode/MeasureField/@unitCode, 1, 1)))) else ())",
                test("ND-Root", "BT-00-Measure"));
    } 

    /*** Boolean functions ***/

    @Test
    public void testNotFunction() {
        assertEquals("not(true())", test("BT-00-Text", "not(ALWAYS)"));
        assertEquals("not(1 + 1 = 2)", test("BT-00-Text", "not(1 + 1 == 2)"));
        assertThrows(ParseCancellationException.class, () -> test("BT-00-Text", "not('text')"));
    }

    @Test
    public void testContainsFunction() {
        assertEquals("contains(PathNode/TextField/normalize-space(text()), 'xyz')",
                test("ND-Root", "contains(BT-00-Text, 'xyz')"));
    }

    @Test
    public void testStartsWithFunction() {
        assertEquals("starts-with(PathNode/TextField/normalize-space(text()), 'abc')",
                test("ND-Root", "starts-with(BT-00-Text, 'abc')"));
    }

    @Test
    public void testEndsWithFunction() {
        assertEquals("ends-with(PathNode/TextField/normalize-space(text()), 'abc')",
                test("ND-Root", "ends-with(BT-00-Text, 'abc')"));
    }

    /*** Numeric functions ***/

    @Test
    public void testCountFunction_UsingFieldReference() {
        assertEquals("count(PathNode/TextField)", test("ND-Root", "count(BT-00-Text)"));
    }

    @Test
    public void testCountFunction_UsingSequenceFromIteration() {
        assertEquals("count(for $x in PathNode/TextField return concat($x, '-xyz'))",
                test("ND-Root", "count(for text:$x in BT-00-Text return concat($x, '-xyz'))"));
    }

    @Test
    public void testNumberFunction() {
        assertEquals("number(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "number(BT-00-Text)"));
    }

    @Test
    public void testSumFunction_UsingFieldReference() {
        assertEquals("sum(PathNode/NumberField)", test("ND-Root", "sum(BT-00-Number)"));
    }

    @Test
    public void testSumFunction_UsingNumericSequenceFromIteration() {
        assertEquals("sum(for $v in PathNode/NumberField return $v + 1)",
                test("ND-Root", "sum(for number:$v in BT-00-Number return $v +1)"));
    }

    @Test
    public void testStringLengthFunction() {
        assertEquals("string-length(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "string-length(BT-00-Text)"));
    }

    /*** String functions ***/

    @Test
    public void testSubstringFunction() {
        assertEquals("substring(PathNode/TextField/normalize-space(text()), 1, 3)",
                test("ND-Root", "substring(BT-00-Text, 1, 3)"));
        assertEquals("substring(PathNode/TextField/normalize-space(text()), 4)",
                test("ND-Root", "substring(BT-00-Text, 4)"));
    }

    @Test
    public void testToStringFunction() {
        assertEquals("string(123)", test("ND-Root", "string(123)"));
    }

    @Test
    public void testConcatFunction() {
        assertEquals("concat('abc', 'def')", test("ND-Root", "concat('abc', 'def')"));
    };

    @Test
    public void testFormatNumberFunction() {
        assertEquals("format-number(PathNode/NumberField/number(), '#,##0.00')",
                test("ND-Root", "format-number(BT-00-Number, '#,##0.00')"));
    }


    /*** Date functions ***/

    @Test
    public void testDateFromStringFunction() {
        assertEquals("xs:date(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "date(BT-00-Text)"));
    }

    /*** Time functions ***/

    @Test
    public void testTimeFromStringFunction() {
        assertEquals("xs:time(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "time(BT-00-Text)"));
    }
}
