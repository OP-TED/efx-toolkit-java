package eu.europa.ted.efx.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import org.apache.commons.lang3.Validate;

/**
 * Utility methods for working with locales
 */
public class LocaleHelper {
  private LocaleHelper() {}

  /**
   * /** Converts a 2-characters or 3-characters ISO 639 language code into a Locale.
   *
   * @param iso639LanguageCode The languace code to be converted
   * @return A {@link Locale} instance
   */
  public static Locale getLocale(final String iso639LanguageCode) {
    Validate.notBlank(iso639LanguageCode, "Undefined language code");

    final String languageCode = iso639LanguageCode.toLowerCase();

    return Arrays.asList(Locale.getISOLanguages()).stream()
        .map(Locale::new)
        .filter(
            (Locale locale) -> locale.getISO3Language().equals(languageCode)
                || locale.getLanguage().equals(languageCode))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            MessageFormat.format("[{0}] is not a valid ISO 639  code", languageCode)));
  }
}
