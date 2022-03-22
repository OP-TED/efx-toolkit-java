package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.labels.LabelMapImpl;

@SuppressWarnings("static-method")
public class LabelMapTest {

  private static final String SDK_VERSION_DUMMY = "dummy";
  private static final String DECORATION_NAME_ADDITIONAL_ROLE = "decoration|name|additional-role";
  private static final String DECORATION_NAME_ACCESSIBILITY = "decoration|name|accessibility";

  @Test
  void testEnglish() throws IOException {
    final LabelMapImpl instance = LabelMapImpl.getInstance(SDK_VERSION_DUMMY);
    final Locale locale = Locale.ENGLISH;

    // <entry key="decoration|name|accessibility">Accessibility criteria</entry>
    final String labelText1 = instance.mapLabel(DECORATION_NAME_ACCESSIBILITY, locale);
    assertEquals("Accessibility criteria", labelText1);

    // <entry key="decoration|name|additional-role">Additional roles</entry>
    final String labelText2 = instance.mapLabel(DECORATION_NAME_ADDITIONAL_ROLE, locale);
    assertEquals("Additional roles", labelText2);
  }

  @Test
  void testGreek() throws IOException {
    final LabelMapImpl instance = LabelMapImpl.getInstance(SDK_VERSION_DUMMY);
    final Locale locale = new Locale("el", "GR");

    // <entry key="decoration|name|accessibility">Κριτήρια προσβασιμότητας</entry>
    final String labelText1 = instance.mapLabel(DECORATION_NAME_ACCESSIBILITY, locale);
    assertEquals("Κριτήρια προσβασιμότητας", labelText1);

    // <entry key="decoration|name|additional-role">Πρόσθετοι ρόλοι</entry>
    final String labelText2 = instance.mapLabel(DECORATION_NAME_ADDITIONAL_ROLE, locale);
    assertEquals("Πρόσθετοι ρόλοι", labelText2);
  }

  @Test
  void testEnglishFallback() throws IOException {
    final LabelMapImpl instance = LabelMapImpl.getInstance(SDK_VERSION_DUMMY);
    final Locale locale = Locale.FRENCH;
    // This test will work as long as this code has no french translation ...
    final String labelText1 = instance.mapLabel("code|value|notice-type.brin-eeig", locale);
    assertEquals("European Economic Interest Grouping notice", labelText1);
  }

  @Test
  void testInvalidKeyInSdk() throws IOException {
    final String sdkVersion = SDK_VERSION_DUMMY;
    final LabelMapImpl instance = LabelMapImpl.getInstance(sdkVersion);
    final Locale locale = Locale.ENGLISH;
    final String labelText1 = instance.mapLabel(DECORATION_NAME_ACCESSIBILITY + ".xyz", locale);
    assertEquals(null, labelText1);
  }

  @Test
  void testLanguageUnsupportedBySdk() {
    final String sdkVersion = SDK_VERSION_DUMMY;
    final LabelMapImpl instance = LabelMapImpl.getInstance(sdkVersion);
    final Locale locale = new Locale("xy");
    assertThrows(IllegalArgumentException.class,
        () -> instance.mapLabel(DECORATION_NAME_ADDITIONAL_ROLE, locale));
  }
}
