package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;

public abstract class EfxTestsBase {

  protected static final TranslatorOptions DEFAULT_OPTIONS = new EfxTranslatorOptions("udf",
      DecimalFormat.EFX_DEFAULT);

  private static final String EFX_NAMESPACE = "http://ted.europa.eu/efx";
  private static final XPathCompiler XPATH_COMPILER;

  static {
    Processor processor = new Processor(false);

    XPATH_COMPILER = processor.newXPathCompiler();
    XPATH_COMPILER.setLanguageVersion("3.1");
    XPATH_COMPILER.declareNamespace("fn", "http://www.w3.org/2005/xpath-functions");
    XPATH_COMPILER.declareNamespace("xs", "http://www.w3.org/2001/XMLSchema");
    // The functions of the EFX namespace are provided by the XSL of the notice viewer and are
    // available to view templates only. None of them is registered here, so an expression that
    // calls one fails to compile: that is what keeps them out of validation rules.
    XPATH_COMPILER.declareNamespace("efx", EFX_NAMESPACE);
    XPATH_COMPILER.declareVariable(new QName("urlPrefix"));
  }

  protected abstract String getSdkVersion();

  protected void testExpressionTranslationWithContext(final String expectedTranslation,
      final String context, final String expression) {
    assertEquals(expectedTranslation, translateExpressionWithContext(context, expression));
  }

  protected void testExpressionTranslation(final String expectedTranslation,
      final String expression, final String... params) {
    assertEquals(expectedTranslation, translateExpression(expression, params));
  }

  protected String translateExpressionWithContext(final String context, final String expression) {
    return translateExpression(String.format("{%s} ${%s}", context, expression));
  }

  protected void testComputeExpressionWithContext(final String expectedTranslation,
      final String context, final String expression) {
    assertEquals(expectedTranslation, translateComputeExpressionWithContext(context, expression));
  }

  protected String translateComputeExpressionWithContext(final String context,
      final String expression) {
    return translateExpression(String.format("WITH %s COMPUTE %s", context, expression));
  }

  protected String translateExpression(final String expression, final String... params) {
    try {
      String result = EfxTranslator.translateExpression(DependencyFactoryMock.INSTANCE,
          getSdkVersion(), expression, DEFAULT_OPTIONS, params);
      assertValidXPath(result);
      return result;
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected String translateTemplate(final String template) {
    try {
      return EfxTranslator.translateTemplate(DependencyFactoryMock.INSTANCE, getSdkVersion(),
          template + "\n", DEFAULT_OPTIONS);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void assertValidXPath(final String xpath) {
    try {
      XPATH_COMPILER.compile(xpath);
    } catch (SaxonApiException e) {
      throw new AssertionError(
          "Generated XPath is not valid: " + xpath + "\n" + e.getMessage(), e);
    }
  }

  protected String lines(String... lines) {
    return String.join("\n", lines);
  }
}
