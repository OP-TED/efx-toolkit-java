package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class EfxToXpathTranslatorTest {

  @Test
  @SuppressWarnings("static-method")
  public void testTranslateConditionAlways() {
    assertEquals("true", EfxToXpathTranslator.translateCondition("ALWAYS"));
  }
}
