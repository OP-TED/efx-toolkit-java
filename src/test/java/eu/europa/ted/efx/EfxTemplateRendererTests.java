package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.MockLabelMap;
import eu.europa.ted.efx.mock.MockNoticeReader;
import eu.europa.ted.efx.mock.MockNoticeRenderer;
import eu.europa.ted.efx.mock.MockSymbolMap;

public class EfxTemplateRendererTests {

    final private String SDK_VERSION = "latest";
    final private String LANGUAGE = "en";

    final MockNoticeReader mockNoticeReader = new MockNoticeReader();

    private void mockValue(final String fieldId, final String contextId, final String value) {
        mockNoticeReader.mockFieldValue(MockSymbolMap.getInstance(SDK_VERSION), fieldId, contextId,
                value);
    }

    private void mockLabel(final String assetType, final String labelType, final String assetId,
            final String label) {
        MockLabelMap.getInstance(SDK_VERSION).mockLabel(LANGUAGE, assetType, labelType, assetId,
                label);
    }

    private String translate(final String template) {
        return EfxTemplateRenderer.renderTemplate(template + "\n",
                MockSymbolMap.getInstance(SDK_VERSION), MockLabelMap.getInstance(SDK_VERSION),
                mockNoticeReader, new MockNoticeRenderer(), LANGUAGE,
                ThrowingErrorListener.INSTANCE);
    }

    @Test
    public void testStandardLabelReference() {
        mockLabel("field", "name", "BT-01-text", "the label");
        assertEquals("the label", translate("BT-01-text::#{field|name|BT-01-text}"));
    }

    @Test
    public void testShorthandBtLabelTypeReference() {
        mockLabel("business_term", "name", "BT-01", "the label");
        assertEquals("the label", translate("BT-01-text::#{name|BT-01}"));
    }


    @Test
    public void testShorthandFieldLabelTypeReference() {
        mockLabel("field", "name", "BT-01-text", "the label");
        assertEquals("the label", translate("BT-01-text::#{name|BT-01-text}"));
    }


    @Test
    public void testShorthandBtLabelReference() {
        mockLabel("business_term", "name", "BT-01", "the label");
        assertEquals("the label", translate("BT-01-text::#{BT-01}"));
    }

    @Test
    public void testShorthandFieldLabelReference() {
        mockLabel("field", "name", "BT-01-text", "the label");
        assertEquals("the label", translate("BT-01-text::#{BT-01-text}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForIndicators() {
        final String fieldId = "BT-02-indicator";
        final String contextId = "BT-01-text";
        final String value = "true";
        final String label = "the label";
        final String labelType = String.format("value-%s", value);

        mockValue(fieldId, contextId, value);
        mockLabel("code", labelType, fieldId, label);

        assertEquals(label, translate("BT-01-text::#{[BT-02-indicator]}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForCodeLists() {
        final String fieldId = "BT-03-code";
        final String contextId = "BT-01-text";
        final String value = "some-code";
        final String rootCodelistId =
                MockSymbolMap.getInstance(SDK_VERSION).getFieldById(fieldId).getRootCodelistId();
        final String assetId = String.format("%s.%s", rootCodelistId, value);
        final String label = "the label";

        mockValue(fieldId, contextId, value);
        mockLabel("code", "value", assetId, label);

        assertEquals(label, translate("BT-01-text::#{[BT-03-code]}"));
    }

    // @Test
    // public void testSelfLabeleReference() {
    // assertEquals("BT-01-text", translate("BT-01-text::#label"));
    // }
}
