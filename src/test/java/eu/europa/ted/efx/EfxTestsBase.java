package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.transform.stream.StreamSource;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.function.Executable;

import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Predicates;
import net.sf.saxon.s9api.streams.Steps;

public abstract class EfxTestsBase {

  protected static final TranslatorOptions DEFAULT_OPTIONS = new EfxTranslatorOptions("udf",
      DecimalFormat.EFX_DEFAULT);

  private static final String EFX_NAMESPACE = "http://ted.europa.eu/efx";
  private static final Processor PROCESSOR = new Processor(false);
  private static final XPathCompiler XPATH_COMPILER;

  static {
    XPATH_COMPILER = PROCESSOR.newXPathCompiler();
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

  /**
   * Evaluates an XPath with Saxon, with the root element of a document as the context item, as the
   * XPath of an expression read in the context of {@code ND-Root} expects.
   *
   * @param xpath the XPath
   * @param xml the document
   * @return the string values of the items of the result, separated by spaces
   */
  protected static String evaluate(final String xpath, final String xml) {
    try {
      final XdmNode document =
          PROCESSOR.newDocumentBuilder().build(new StreamSource(new StringReader(xml)));
      final XPathSelector selector = XPATH_COMPILER.compile(xpath).load();
      selector.setVariable(new QName("urlPrefix"), new XdmAtomicValue(""));
      selector.setContextItem(XPATH_COMPILER.evaluateSingle("/*", document));
      return selector.evaluate().stream().map(XdmItem::getStringValue)
          .collect(Collectors.joining(" "));
    } catch (SaxonApiException e) {
      throw new AssertionError("Generated XPath cannot be evaluated: " + xpath + "\n"
          + e.getMessage(), e);
    }
  }

  /**
   * Gives the elements of a document that a rule context matches. The rule context is compiled as
   * an XSLT 3 pattern, as a Schematron whose query binding is {@code xslt3} needs it to be.
   *
   * @param ruleContext the rule context
   * @param xml the document
   * @return the string values of the elements that match, separated by spaces
   */
  protected static String elementsMatching(final String ruleContext, final String xml) {
    try {
      final XdmNode document =
          PROCESSOR.newDocumentBuilder().build(new StreamSource(new StringReader(xml)));
      final XPathSelector pattern = XPATH_COMPILER.compilePattern(ruleContext).load();
      final List<String> matching = new ArrayList<>();
      for (final XdmNode element : document.select(Steps.descendant(Predicates.isElement()))
          .asListOfNodes()) {
        pattern.setContextItem(element);
        if (pattern.effectiveBooleanValue()) {
          matching.add(element.getStringValue());
        }
      }
      return String.join(" ", matching);
    } catch (SaxonApiException e) {
      throw new AssertionError("Rule context cannot be matched as an XSLT 3 pattern: "
          + ruleContext + "\n" + e.getMessage(), e);
    }
  }

  /**
   * Asserts that a translation fails with a syntax error: the parser's own
   * {@link ParseCancellationException}, as the {@link ThrowingErrorListener} raises it, and not one
   * of the compilation errors that extend it.
   *
   * @param translation the translation
   */
  protected static void assertSyntaxError(final Executable translation) {
    final ParseCancellationException exception =
        assertThrows(ParseCancellationException.class, translation);
    assertEquals(ParseCancellationException.class, exception.getClass(), exception::toString);
  }

  protected String lines(String... lines) {
    return String.join("\n", lines);
  }
}
