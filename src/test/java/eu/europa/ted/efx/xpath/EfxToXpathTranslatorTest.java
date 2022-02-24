package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class EfxToXpathTranslatorTest {

  @Test
  public void testTranslateConditionAlways() {
    assertEquals("true", EfxToXpathTranslator.translateCondition("ALWAYS"));
  }

  @Test
  public void testTemplateParsing() throws IOException {
    // TODO see this later.
    // String output =
    // EfxtToXsltTranslator.translateTemplateFile("src/test/resources/efxt-test1.efxt");
    // System.out.println(output);
  }

}
