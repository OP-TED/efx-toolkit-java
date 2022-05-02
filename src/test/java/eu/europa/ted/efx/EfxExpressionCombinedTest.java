package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.SymbolResolverMock;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

/**
 * Test for EFX expressions that combine several aspects of the language.
 */
public class EfxExpressionCombinedTest {

    // TODO: Currently handling multiple SDK versions is not implemented.
    final private String SDK_VERSION = "latest";

    private String test(final String context, final String expression) {
        return EfxExpressionTranslator.translateExpression(context, expression,
                SymbolResolverMock.getInstance(SDK_VERSION), new XPathScriptGenerator(),
                ThrowingErrorListener.INSTANCE);
    }

    @Test
    public void testNotAnd() {
        assertEquals("not(1 = 2) and (2 = 2)",
                test("BT-00-Text", "not(1 == 2) and (2 == 2)"));
    }

    @Test
    public void testNotPresentAndNotPresent() {
        assertEquals("not(PathNode/TextField) and not(PathNode/IntegerField)",
                test("ND-0", "BT-00-Text is not present and BT-00-Integer is not present"));
    }

    @Test
    public void testCountWithNodeContextOverride() {
        assertEquals("count(PathNode/CodeField) = 1",
                test("BT-00-Text", "count(ND-0::BT-00-Code) == 1"));
    }

    @Test
    public void testCountWithAbsoluteFieldReference() {
        assertEquals("count(/*/PathNode/CodeField) = 1",
                test("BT-00-Text", "count(/BT-00-Code) == 1"));
    }

    @Test
    public void testCountWithAbsoluteFieldReferenceAndPredicate() {
        assertEquals("count(/*/PathNode/CodeField[../IndicatorField = true()]) = 1",
                test("BT-00-Text", "count(/BT-00-Code[BT-00-Indicator == TRUE]) == 1"));
    }

    @Test
    public void testCountWithAbsoluteFieldReferenceAndContextOverride() {
        assertEquals("count(/*/PathNode/CodeField[../IndicatorField = true()]) = 1",
                test("BT-00-Text", "count(ND-0::/BT-00-Code[BT-00-Indicator == TRUE]) == 1"));
    }
}
