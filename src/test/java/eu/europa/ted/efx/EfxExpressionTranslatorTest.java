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
    public void testFieldValueComparison() {
        assertEquals("PathNode/TextField/normalize-space(text()) = PathNode/TextMultilingualField/normalize-space(text())",
                test("ND-Root", "BT-00-Text == BT-00-Text-Multilingual"));
    }

    @Test
    public void testFieldValueComparison_WithNumericLiteral() {
        assertEquals("PathNode/IntegerField/number() > 0",
                test("ND-Root", "BT-00-Integer > 0"));
    }

    @Test
    public void testBooleanComparison() {
        assertEquals("false() != true()",
                test("BT-00-Text", "NEVER != ALWAYS"));
    }

    @Test
    public void testNumericComparison() {
        assertEquals("2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5",
                test("BT-00-Text", "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5"));
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
        assertEquals("(-3 * (2 * (if (lower-case(PathNode/MeasureField/@unit)='w') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (lower-case(PathNode/MeasureField/@unit)='d') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), upper-case(PathNode/MeasureField/@unit))) else xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(PathNode/MeasureField/@unit))))))",
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

    @Test
    public void testQuantifiedExpression() {
        assertEquals("every $x in ('a','b','c') satisfies $x <= 'a'", test("BT-00-Text", "every $x in ('a', 'b', 'c') satisfies $x <= 'a'"));
    }

    @Test
    public void testConditionalExpression() {
        assertEquals("(if 1 > 2 then 'a' else 'b')", test("BT-00-Text", "if 1 > 2 then 'a' else 'b'"));
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
        assertEquals("(if (lower-case(../MeasureField/@unit)='w') then xs:dayTimeDuration(concat('P', ../MeasureField/number() * 7, 'D')) else if (lower-case(../MeasureField/@unit)='d') then xs:dayTimeDuration(concat('P', ../MeasureField/number(), upper-case(../MeasureField/@unit))) else xs:yearMonthDuration(concat('P', ../MeasureField/number(), upper-case(../MeasureField/@unit)))) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))", test("BT-00-Text", "BT-00-Measure in (P1M, P3M, P6M)"));
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
        assertEquals("(if (lower-case(PathNode/MeasureField/@unit)='w') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number() * 7, 'D')) else if (lower-case(PathNode/MeasureField/@unit)='d') then xs:dayTimeDuration(concat('P', PathNode/MeasureField/number(), upper-case(PathNode/MeasureField/@unit))) else xs:yearMonthDuration(concat('P', PathNode/MeasureField/number(), upper-case(PathNode/MeasureField/@unit))))",
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
    public void testCountFunction() {
        assertEquals("count(PathNode/TextField)", test("ND-Root", "count(BT-00-Text)"));
    }

    @Test
    public void testNumberFunction() {
        assertEquals("number(PathNode/TextField/normalize-space(text()))",
                test("ND-Root", "number(BT-00-Text)"));
    }

    @Test
    public void testSumFunction() {
        assertEquals("sum(PathNode/NumberField)", test("ND-Root", "sum(BT-00-Number)"));
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
