package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.SymbolResolverMock;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

public class EfxExpressionTranslatorTest {

    // TODO: Currently handling multiple SDK versions is not implemented.
    final private String SDK_VERSION = "latest";

    private String test(final String context, final String expression) {
        return EfxExpressionTranslator.transpileExpression(context, expression,
                SymbolResolverMock.getInstance(SDK_VERSION), new XPathScriptGenerator(),
                ThrowingErrorListener.INSTANCE);
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
                test("ND-0", "BT-00-Text is empty"));
    }

    @Test
    public void testEmptinessCondition_WithNot() {
        assertEquals("PathNode/TextField/normalize-space(text()) != ''",
                test("ND-0", "BT-00-Text is not empty"));
    }

    @Test
    public void testPresenceCondition() {
        assertEquals("PathNode/TextField",
                test("ND-0", "BT-00-Text is present"));
    }

    @Test
    public void testPresenceCondition_WithNot() {
        assertEquals("not(PathNode/TextField)",
                test("ND-0", "BT-00-Text is not present"));
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
                test("ND-0", "BT-00-Text == BT-00-Text-Multilingual"));
    }

    @Test
    public void testFieldValueComparison_WithNumericLiteral() {
        assertEquals("PathNode/IntegerField > 0",
                test("ND-0", "BT-00-Integer > 0"));
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
        assertEquals("PathNode/DateField/xs:date(text()) = PathNode/DateField/xs:date(text())",
                test("ND-0", "BT-00-Date == BT-00-Date"));
    }

    @Test
    public void testDateComparison_OfDateReferenceAndDateFunction() {
        assertEquals("PathNode/DateField/xs:date(text()) = xs:date(PathNode/TextField/normalize-space(text()))",
                test("ND-0", "BT-00-Date == date(BT-00-Text)"));
    }
    
    @Test
    public void testTimeComparison_OfTwoTimeLiterals() {
        assertEquals("xs:time('13:00:10') > xs:time('21:20:30')",
                test("BT-00-Text", "13:00:10 > 21:20:30"));
    }

    @Test
    public void testTimeComparison_OfTwoTimeReferences() {
        assertEquals("PathNode/TimeField/xs:time(text()) = PathNode/TimeField/xs:time(text())",
                test("ND-0", "BT-00-Time == BT-00-Time"));
    }

    @Test
    public void testTimeComparison_OfTimeReferenceAndTimeFunction() {
        assertEquals("PathNode/TimeField/xs:time(text()) = xs:time(PathNode/TextField/normalize-space(text()))",
                test("ND-0", "BT-00-Time == time(BT-00-Text)"));
    }

    @Test
    public void testDurationComparison_OfTwoDurationLiterals() {
        assertEquals("xs:duration('P1Y') = xs:duration('P12M')",
                test("BT-00-Text", "P1Y == P12M"));
    }

    @Test
    public void testLeftCalculatedDurationComparison() {
        assertEquals("PathNode/DateField/xs:date(text()) < (PathNode/DateField/xs:date(text()) + xs:duration('P3M'))",
                test("ND-0", "BT-00-Date - BT-00-Date < P3M"));
    }

    @Test
    public void testRightCalculatedDurationComparison() {
        assertEquals("PathNode/DateField/xs:date(text()) < (PathNode/DateField/xs:date(text()) + xs:duration('P3M'))",
                test("ND-0", "P3M > BT-00-Date - BT-00-Date"));
    }

    @Test
    public void testBooleanLiteralExpression_Always() {
        assertEquals("true()", test("BT-00-Text", "ALWAYS"));
    }

    @Test
    public void testBooleanLiteralExpression_Never() {
        assertEquals("false()", test("BT-00-Text", "NEVER"));
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
    public void testExplicitList() {
        assertEquals("'a' = ('a','b','c')", test("BT-00-Text", "'a' in ('a', 'b', 'c')"));
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
                test("ND-0", "BT-00-Attribute == 'text'"));
    }

    @Test
    public void testFieldReferenceWithPredicate() {
        assertEquals("PathNode/IndicatorField['a' = 'a']",
                test("ND-0", "BT-00-Indicator['a' == 'a']"));
    }

    @Test
    public void testFieldReferenceWithPredicate_WithFieldReferenceInPredicate() {
        assertEquals("PathNode/IndicatorField[../CodeField/normalize-space(text()) = 'a']",
                test("ND-0", "BT-00-Indicator[BT-00-Code == 'a']"));
    }

    @Test
    public void testFieldReferenceInOtherNotice() {
        // FIXME: Test causes exception
        assertEquals("",
                test("ND-0", "notice('da4d46e9-490b-41ff-a2ae-8166d356a619')/BT-00-Text"));
    }

    @Test
    public void testFieldReferenceWithFieldContextOverride() {
        assertEquals("../../TextField/normalize-space(text())",
                test("BT-00-Code", "BT-01-SubLevel-Text::BT-00-Text"));
    }

    @Test
    public void testFieldReferenceWithFieldContextOverride_WithIntegerField() {
        assertEquals("../../IntegerField/number()",
                test("BT-00-Code", "BT-01-SubLevel-Text::BT-00-Integer"));
    }

    @Test
    public void testFieldReferenceWithNodeContextOverride() {
        assertEquals("PathNode/IntegerField/number()",
                test("BT-00-Text", "ND-0::BT-00-Integer"));
    }

    @Test
    public void testFieldReferenceWithNodeContextOverride_WithPredicate() {
        assertEquals("PathNode/IntegerField/number()",
                test("BT-00-Text", "ND-0[BT-00-Indicator == TRUE]::BT-00-Integer"));
    }

    @Test
    public void testSimpleFieldReference() {
        assertEquals("../IndicatorField",
                test("BT-00-Text", "BT-00-Indicator"));
    }

    @Test
    public void testFieldReference_ForDurationFields() {
        assertEquals("xs:duration(if (lower-case(PathNode/MeasureField/@unit)='w') then concat('P', PathNode/MeasureField/number() * 7, 'D') else concat('P', PathNode/MeasureField/number(), upper-case(/@unit)))",
                test("ND-0", "BT-00-Measure"));
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
                test("ND-0", "contains(BT-00-Text, 'xyz')"));
    }

    @Test
    public void testStartsWithFunction() {
        assertEquals("starts-with(PathNode/TextField/normalize-space(text()), 'abc')",
                test("ND-0", "starts-with(BT-00-Text, 'abc')"));
    }

    @Test
    public void testEndsWithFunction() {
        assertEquals("ends-with(PathNode/TextField/normalize-space(text()), 'abc')",
                test("ND-0", "ends-with(BT-00-Text, 'abc')"));
    }

    /*** Numeric functions ***/

    @Test
    public void testCountFunction() {
        assertEquals("count(PathNode/TextField)", test("ND-0", "count(BT-00-Text)"));
    }

    @Test
    public void testNumberFunction() {
        assertEquals("number(PathNode/TextField/normalize-space(text()))",
                test("ND-0", "number(BT-00-Text)"));
    }

    @Test
    public void testSumFunction() {
        assertEquals("sum(PathNode/NumberField)", test("ND-0", "sum(BT-00-Number)"));
    }

    @Test
    public void testStringLengthFunction() {
        assertEquals("string-length(PathNode/TextField/normalize-space(text()))",
                test("ND-0", "string-length(BT-00-Text)"));
    }

    /*** String functions ***/

    @Test
    public void testSubstringFunction() {
        assertEquals("substring(PathNode/TextField/normalize-space(text()), 1, 3)",
                test("ND-0", "substring(BT-00-Text, 1, 3)"));
        assertEquals("substring(PathNode/TextField/normalize-space(text()), 4)",
                test("ND-0", "substring(BT-00-Text, 4)"));
    }

    @Test
    public void testToStringFunction() {
        assertEquals("string(123)", test("ND-0", "string(123)"));
    }

    @Test
    public void testConcatFunction() {
        assertEquals("concat('abc', 'def')", test("ND-0", "concat('abc', 'def')"));
    };

    @Test
    public void testFormatNumberFunction() {
        assertEquals("format-number(PathNode/NumberField/number(), '#,##0.00')",
                test("ND-0", "format-number(BT-00-Number, '#,##0.00')"));
    }


    /*** Date functions ***/

    @Test
    public void testDateFromStringFunction() {
        assertEquals("xs:date(PathNode/TextField/normalize-space(text()))",
                test("ND-0", "date(BT-00-Text)"));
    }

    /*** Time functions ***/

    @Test
    public void testTimeFromStringFunction() {
        assertEquals("xs:time(PathNode/TextField/normalize-space(text()))",
                test("ND-0", "time(BT-00-Text)"));
    }

    /*** Duration functions ***/

    @Test
    public void testDurationFromDatesFunction_UsingTwoDateFields() {
        assertEquals("PathNode/DateField/xs:date(text()) - PathNode/DateField/xs:date(text())",
                test("ND-0", "duration(BT-00-Date, BT-00-Date)"));
    }

    @Test
    public void testDurationFromDatesFunction_UsingOneNonDateField() {
        assertThrows(ParseCancellationException.class,
                () -> test("ND-0", "duration(BT-00-Date, BT-00-Text)"));
    }

    @Test
    public void testDurationFromDatesFunction_UsingTwoDateLiterals() {
        assertEquals("xs:date('2019-12-01') - xs:date('2021-01-12')",
                test("ND-0", "duration(2019-12-01, 2021-01-12)"));
    }


    /*** Other ***/

    @Test
    public void testCountWithNodeContextOverride() {
        assertEquals("count(PathNode/CodeField) = 1",
                test("BT-00-Text", "count(ND-0::BT-00-Code) == 1"));
    }
}
