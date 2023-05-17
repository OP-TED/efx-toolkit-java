package eu.europa.ted.efx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocaleHelperTest {
  @Test
  void testLocaleParsing() {
    assertEquals(Locale.ENGLISH, LocaleHelper.getLocale("en"));
    assertEquals(Locale.ENGLISH, LocaleHelper.getLocale("ENG"));
    assertEquals(Locale.FRENCH, LocaleHelper.getLocale("FR"));
    assertEquals(Locale.FRENCH, LocaleHelper.getLocale("FRA"));
  }
}
