package eu.europa.ted.efx.labels;

import java.io.IOException;
import java.util.Locale;

public interface LabelMap {

  /**
   * @param labelId A unique label identifier
   * @param locale A locale which contains a two letter language code.
   * @return The text which was found for the given input, null otherwise
   * @throws IOException In case something went wrong reading the SDK files
   */
  public String mapLabel(final String labelId, final Locale locale) throws IOException;

  /**
   * @param labelId A unique label identifier
   * @param language A language like "en" for English, "fr" for French, ...
   * @return The text which was found for the given input, null otherwise
   * @throws IOException In case something went wrong reading the SDK files
   */
  public String mapLabel(final String labelId, final String language) throws IOException;
}
