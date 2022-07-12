package eu.europa.ted.efx.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface EfxTemplateTranslator extends EfxExpressionTranslator {
  /**
   * Opens the indicated EFX file and translates the EFX template it contains.
   */
  String renderTemplate(Path pathname) throws IOException;

  /**
   * Translates the template contained in the string passed as a parameter.
   */
  String renderTemplate(String template);

  String renderTemplate(InputStream stream) throws IOException;
}
