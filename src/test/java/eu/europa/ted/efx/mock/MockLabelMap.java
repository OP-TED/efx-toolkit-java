package eu.europa.ted.efx.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Map;
import eu.europa.ted.efx.interfaces.LabelMap;

public class MockLabelMap implements LabelMap {

    private static final Map<String, MockLabelMap> instances = new HashMap<>();

    public static MockLabelMap getInstance(final String sdkVersion) {
        return instances.computeIfAbsent(sdkVersion, k -> new MockLabelMap());
    }

    private Map<String, Map<String, String>> labels = new HashMap<>();

    private MockLabelMap() {}

    @Override
    public String mapLabel(String labelId, Locale locale) throws IOException {
        return mapLabel(labelId, locale.getLanguage());
    }

    @Override
    public String mapLabel(String labelId, String language) throws IOException {
        Map<String, String> languageLabels = labels.get(language);
        if (languageLabels == null) {
            throw new InputMismatchException(
                    "Translations for language " + language + " are not available");
        }
        String label = languageLabels.get(labelId);
        if (label == null) {
            throw new InputMismatchException(
                    "Label " + labelId + " is not available for language " + language);
        }
        return label;
    }


    @Override
    public String mapLabel(String assetType, String labelType, String assetId, String language)
            throws IOException {
        return mapLabel(String.format("%s|%s|%s", assetType, labelType, assetId), language);
    }

    public void mockLabel(final String language, final String assetType, final String labelType,
            final String assetId, final String label) {
        Map<String, String> languageLabels =
                this.labels.computeIfAbsent(language, k -> new HashMap<>());
        languageLabels.put(String.format("%s|%s|%s", assetType, labelType, assetId), label);
    }

}
