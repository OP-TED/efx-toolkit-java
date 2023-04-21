package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

public abstract class EfxTestsBase {
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

  protected String translateExpression(final String expression, final String... params) {
    try {
      return EfxTranslator.translateExpression(DependencyFactoryMock.INSTANCE, getSdkVersion(),
          expression, params);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected String translateTemplate(final String template) {
    try {
      return EfxTranslator.translateTemplate(DependencyFactoryMock.INSTANCE, getSdkVersion(),
          template + "\n");
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected String lines(String... lines) {
    return String.join("\n", lines);
  }
}
