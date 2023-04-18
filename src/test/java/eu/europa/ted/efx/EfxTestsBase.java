package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

public class EfxTestsBase {
  private static final String[] SDK_VERSIONS = new String[] {"eforms-sdk-1.0", "eforms-sdk-2.0"};

  protected static Stream<Arguments> provideSdkVersions() {
    List<Arguments> arguments = new ArrayList<>();

    for (String sdkVersion : SDK_VERSIONS) {
      arguments.add(Arguments.of(sdkVersion));
    }

    return Stream.of(arguments.toArray(new Arguments[0]));
  }

  protected void testExpressionTranslationWithContext(final String sdkVersion,
      final String expectedTranslation, final String context, final String expression) {
    assertEquals(expectedTranslation,
        translateExpressionWithContext(sdkVersion, context, expression));
  }

  protected void testExpressionTranslation(final String sdkVersion,
      final String expectedTranslation, final String expression, final String... params) {
    assertEquals(expectedTranslation, translateExpression(sdkVersion, expression, params));
  }

  protected String translateExpressionWithContext(final String sdkVersion, final String context,
      final String expression) {
    return translateExpression(sdkVersion, String.format("{%s} ${%s}", context, expression));
  }

  protected String translateExpression(final String sdkVersion, final String expression,
      final String... params) {
    try {
      return EfxTranslator.translateExpression(DependencyFactoryMock.INSTANCE, sdkVersion,
          expression, params);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }
}
