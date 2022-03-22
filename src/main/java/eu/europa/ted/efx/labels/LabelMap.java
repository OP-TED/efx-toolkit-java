package eu.europa.ted.efx.labels;

import java.io.IOException;
import java.util.Locale;

public interface LabelMap {
  public String mapLabel(final String labelId, final Locale language) throws IOException;

  public String mapLabel(final String labelId, final String language) throws IOException;
}
