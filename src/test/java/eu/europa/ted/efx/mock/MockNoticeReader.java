package eu.europa.ted.efx.mock;

import java.util.HashMap;
import java.util.Map;
import eu.europa.ted.efx.NoticeReader;
import eu.europa.ted.efx.SymbolMap;

public class MockNoticeReader implements NoticeReader {

    private Map<String, String> values = new HashMap<>();

    public MockNoticeReader() {}

    @Override
    public void load(final String pathname) {
        // no-op
    }

    @Override
    public String getRequiredSdkVersion() {
        return "latest";
    }

    @Override
    public String valueOf(final String xpath) {
        return values.get(this.mockXpath(xpath, "/*"));
    }

    @Override
    public String valueOf(String xpath, String contextPath) {
        return values.get(this.mockXpath(xpath, contextPath));
    }

    public void mockFieldValue(final SymbolMap symbols, final String fieldId,
            final String contextId, final String value) {
        String contextPath = symbols.contextPathOfField(contextId);
        String xpath = symbols.relativeXpathOfField(fieldId, contextPath);
        values.put(this.mockXpath(xpath, contextPath), value);
    }

    private String mockXpath(final String xpath, final String contextPath) {
        return String.format("{{%s::%s}}", contextPath, xpath);
    }
}

