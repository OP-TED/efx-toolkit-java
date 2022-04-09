package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.MockSymbolMap;
import eu.europa.ted.efx.xpath.XPathSyntaxMap;

public class EfxExpressionTranslatorTests {

    // TODO: Currently handling multiple SDK versions is not implemented.
    final private String SDK_VERSION = "latest";

    private String test(final String context, final String expression) {
        return EfxExpressionTranslator.transpileExpression(context, expression,
                MockSymbolMap.getInstance(SDK_VERSION), new XPathSyntaxMap(), ThrowingErrorListener.INSTANCE);
    }

    @Test
    public void testLogicalOrCondition() {
        assertEquals("true() or false()", test("BT-01-text", "ALWAYS or NEVER"));
    }

    @Test
    public void testLogicalAndCondition() {
        assertEquals("true() and 1 + 1 = 2", test("BT-01-text", "ALWAYS and 1 + 1 == 2"));
    }

    @Test
    @Disabled ("until logical not is properly implemented as a function") 
    public void testLogicalNotCondition() {
        assertEquals("not (true)", test("BT-01-text", "not(ALWAYS)"));
    }

    @Test
    public void testParenthesizedCondition() {
        assertEquals("(true() or true()) and false()",
                test("BT-01-text", "(ALWAYS or true()) and NEVER"));
    }

    @Test
    public void testAlwaysCondition() {
        assertEquals("true()", test("BT-01-text", "ALWAYS"));
    }

    @Test
    public void testNeverCondition() {
        assertEquals("false()", test("BT-01-text", "NEVER"));
    }


    @Test
    public void testcomparisonCondition() {
        assertEquals("2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5",
                test("BT-01-text", "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5"));
    }

    @Test
    public void testInListCondition() {
        assertEquals("not('x' = ('a','b','c'))",
                test("BT-01-text", "'x' not in ('a', 'b', 'c')"));
    }

    // @Test
    // public void testEmptinessCondition() {
    //     assertEquals("../text/normalize-space(text()) != ''",
    //             test("BT-01-text", "BT-01-text is not empty"));
    // }

    // @Test
    // public void testPresenceCondition() {
    //     assertEquals("../text/normalize-space(text()) != ''",
    //             test("BT-01-text", "BT-01-text is present"));
    // }

    @Test
    public void testLikePatternCondition() {
        assertEquals("fn:matches(normalize-space('123'), '[0-9]*')", test("BT-01-text", "'123' like '[0-9]*'"));
    }

    @Test
    public void testMultiplicationExpression() {
        assertEquals("3 * 4", test("BT-01-text", "3 * 4"));
    }

    @Test
    public void testAdditionExpression() {
        assertEquals("4 + 4", test("BT-01-text", "4 + 4"));
    }

    @Test
    public void testParenthesizedExpression() {
        assertEquals("(2 + 2) * 4", test("BT-01-text", "(2 + 2)*4"));
    }

    @Test
    public void testExplicitList() {
        assertEquals("'a' = ('a','b','c')", test("BT-01-text", "'a' in ('a', 'b', 'c')"));
    }

    @Test
    public void testCodeList() {
        assertEquals("'a' = ('code1','code2','code3')",
                test("BT-01-text", "'a' in (accessibility)"));
    }
}
