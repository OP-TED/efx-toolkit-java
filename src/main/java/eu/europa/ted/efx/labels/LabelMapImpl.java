package eu.europa.ted.efx.labels;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eu.europa.ted.efx.util.JavaTools;

public class LabelMapImpl implements LabelMap {

  /**
   * Path relative to maven src/main/resources/
   */
  private static final String EFORMS_SDK_TRANSLATIONS = "eforms-sdk/translations/";

  /**
   * Extension used for SDK translation files.
   */
  private static final String EFORMS_SDK_TRANSLATIONS_EXT = ".xml";

  /**
   * It is implemented as a "kind-of" singleton. One instance per version of the eForms SDK.
   */
  private static final Map<String, LabelMapImpl> instances = new HashMap<>();

  /**
   * Gets the single instance containing the labels defined in the given version of the eForms SDK.
   *
   * @param sdkVersion Version of the SDK
   */
  public static LabelMapImpl getInstance(final String sdkVersion) {
    return instances.computeIfAbsent(sdkVersion, k -> new LabelMapImpl(sdkVersion));
  }

  private final Map<String, Map<String, String>> labelsByLanguage = new HashMap<>();

  /**
   * Private, use getInstance method instead.
   *
   * @param sdkVersion The version of the SDK.
   */
  private LabelMapImpl(final String sdkVersion) {
    //
  }

  @Override
  public String mapLabel(final String labelCode, final Locale language) throws IOException {
    final String languageTwoLetterCode = language.getLanguage();
    if (languageTwoLetterCode.length() != 2) {
      throw new IllegalArgumentException(String.format(
          "Expected two letter code but found: %s, maybe try passing language and country separately",
          languageTwoLetterCode));
    }
    // TODO load all labels for this language from the translations folder.
    // EFORMS_SDK_TRANSLATIONS load resource as stream ? walk folder ?
    final String ext = EFORMS_SDK_TRANSLATIONS_EXT;

    final Map<String, String> labelById =
        labelsByLanguage.computeIfAbsent(languageTwoLetterCode, k -> new HashMap<>());

    if (labelById.isEmpty()) {
      // Lazily load all labels associated to given language.
      JavaTools.listFilesUsingFileWalk(JavaTools.getResourceAsPath(EFORMS_SDK_TRANSLATIONS), 2, ext)
          .forEach(path -> {
            final String filenameStr = path.getFileName().toString();
            final int lastIndexOfLangSeparator = filenameStr.lastIndexOf("_");
            assert lastIndexOfLangSeparator > 0 : "lastIndexOfLangSeparator must be > 0";

            final int lastIndexOfExt = filenameStr.lastIndexOf(ext);
            assert lastIndexOfExt > lastIndexOfLangSeparator : "lastIndexOfExt come after lastIndexOfLangSeparator";

            final String langFromFilename =
                filenameStr.substring(lastIndexOfLangSeparator + 1, lastIndexOfExt);

            if (languageTwoLetterCode.equals(langFromFilename)) {
              final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
              dbf.setValidating(false);
              dbf.setNamespaceAware(true);
              try {
                dbf.setFeature("http://xml.org/sax/features/namespaces", false);
                dbf.setFeature("http://xml.org/sax/features/validation", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
                final DocumentBuilder db = dbf.newDocumentBuilder();
                final Document doc = db.parse(path.toFile());
                final NodeList elements = doc.getElementsByTagName("entry");
                for (int i = 0; i < elements.getLength(); i++) {
                  final Node item = elements.item(i);
                  final String labelId = item.getAttributes().getNamedItem("key").getNodeValue();
                  final String labelText = item.getTextContent();
                  assert StringUtils.isNotBlank(labelId) : "labelId is blank";
                  labelById.put(labelId, labelText);
                }
              } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
              } catch (SAXException e) {
                throw new RuntimeException(e);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          });
      if (labelById.isEmpty()) {
        // It should not be empty anymore at this point!
        throw new IllegalArgumentException(String.format(
            "Found no texts for language: %s, please use a language provided by in the SDK translations",
            languageTwoLetterCode));
      }
    }
    final String labelText = labelById.get(labelCode);
    final String fallbackLanguage = "en";
    if (labelText != null) {
      return labelText;
    }
    if (languageTwoLetterCode != fallbackLanguage) {
      return mapLabel(labelCode, "en"); // Fallback to english.
    }
    return null;
  }

  @Override
  public String mapLabel(final String labelId, final String language) throws IOException {
    return mapLabel(labelId, new Locale(language));
  }


}
