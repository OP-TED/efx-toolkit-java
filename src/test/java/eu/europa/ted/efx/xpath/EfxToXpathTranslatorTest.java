package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class EfxToXpathTranslatorTest {

  // TODO: Currently handling multiple SDK versions is not implemented.
  final private String testSdkVersion = "latest";
  final private Boolean testNewContextualizer = true;
  
  @Test
  public void testTranslateConditionAlways() {
    assertEquals("true", EfxToXpathTranslator.translateCondition("ALWAYS", testSdkVersion, testNewContextualizer));
  }

  @Test
  public void testTransalteFile() throws IOException {
    System.out.print(EfxToXpathTranslator.translateTestFile("src/test/resources/examples.efx", testSdkVersion, testNewContextualizer));
  }

  @Test
  public void testTemplateParsing() throws IOException {
    // TODO see this later.
    // String output =
    // EfxtToXsltTranslator.translateTemplateFile("src/test/resources/efxt-test1.efxt");
    // System.out.println(output);
  }

}
