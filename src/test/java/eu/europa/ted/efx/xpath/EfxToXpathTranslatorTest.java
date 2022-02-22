package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class EfxToXpathTranslatorTest {

  @Test
  @SuppressWarnings("static-method")
  public void testTranslateConditionAlways() {
    assertEquals("true", EfxToXpathTranslator.translateCondition("ALWAYS"));
  }

  @Test
  @SuppressWarnings("static-method")
  public void testTemplateParsing() throws IOException {
    String output = EfxtToXsltTranslator.translateTemplateFile("src/test/resources/efxt-test1.efxt");
    System.out.println(output);
  }

}
