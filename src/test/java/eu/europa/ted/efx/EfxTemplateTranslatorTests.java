package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.MockRenderer;
import eu.europa.ted.efx.mock.MockSymbolMap;
import eu.europa.ted.efx.xpath.XPathSyntaxMap;

public class EfxTemplateTranslatorTests {

    final private String SDK_VERSION = "latest";

    private String translate(final String template) {
        return EfxTemplateTranslator.renderTemplate(template + "\n",
                MockSymbolMap.getInstance(SDK_VERSION), 
                new XPathSyntaxMap(),
                new MockRenderer(), ThrowingErrorListener.INSTANCE);
    }

    @Test
    public void testStandardLabelReference() {
        assertEquals("block01 = 'field|name|BT-01-Text'; for-each('/*/PathNode/TextField') { block01(); }", translate("{BT-01-Text}::#{field|name|BT-01-Text}"));
    }

    @Test
    public void testShorthandBtLabelTypeReference() {
        assertEquals("block01 = 'business_term|name|BT-01'; for-each('/*/PathNode/TextField') { block01(); }", translate("{BT-01-Text}::#{name|BT-01}"));
    }

    @Test
    public void testShorthandFieldLabelTypeReference() {
        assertEquals("block01 = 'field|name|BT-01-Text'; for-each('/*/PathNode/TextField') { block01(); }", translate("{BT-01-Text}::#{name|BT-01-Text}"));
    }

    @Test
    public void testShorthandBtLabelReference() {
        assertEquals("block01 = 'business_term|name|BT-01'; for-each('/*/PathNode/TextField') { block01(); }", translate("{BT-01-Text}::#{BT-01}"));
    }

    @Test
    public void testShorthandFieldLabelReference() {
        assertEquals("block01 = 'field|name|BT-01-Text'; for-each('/*/PathNode/TextField') { block01(); }", translate("{BT-01-Text}::#{BT-01-Text}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForIndicators() {
        assertEquals("block01 = 'concat('code|value-', ../IndicatorField, '|BT-02-Indicator')'; for-each('/*/PathNode/TextField') { block01(); }", translate("{BT-01-Text}::#{[BT-02-Indicator]}"));
    }
}
