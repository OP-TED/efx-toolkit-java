package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

/**
 * Test for EFX expressions that combine several aspects of the language.
 */
class EfxExpressionCombinedTest {
  private static final String[] SDK_VERSIONS = new String[] {"eforms-sdk-1.0", "eforms-sdk-2.0"};

  private static Stream<Arguments> provideSdkVersions() {
    List<Arguments> arguments = new ArrayList<>();

    for (String sdkVersion : SDK_VERSIONS) {
      arguments.add(Arguments.of(sdkVersion));
    }

    return Stream.of(arguments.toArray(new Arguments[0]));
  }

  private void testExpressionTranslationWithContext(final String sdkVersion,
      final String expectedTranslation, final String context, final String expression) {
    assertEquals(expectedTranslation,
        translateExpressionWithContext(sdkVersion, context, expression));
  }

  private String translateExpressionWithContext(String sdkVersion, final String context,
      final String expression) {
    try {
      return EfxTranslator.translateExpression(DependencyFactoryMock.INSTANCE, sdkVersion,
          String.format("{%s} ${%s}", context, expression));
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNotAnd(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "not(1 = 2) and (2 = 2)", "BT-00-Text",
        "not(1 == 2) and (2 == 2)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNotPresentAndNotPresent(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "not(PathNode/TextField) and not(PathNode/IntegerField)", "ND-Root",
        "BT-00-Text is not present and BT-00-Integer is not present");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCountWithNodeContextOverride(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "count(../../PathNode/CodeField) = 1",
        "BT-00-Text", "count(ND-Root::BT-00-Code) == 1");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCountWithAbsoluteFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "count(/*/PathNode/CodeField) = 1",
        "BT-00-Text", "count(/BT-00-Code) == 1");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCountWithAbsoluteFieldReferenceAndPredicate(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "count(/*/PathNode/CodeField[../IndicatorField = true()]) = 1", "BT-00-Text",
        "count(/BT-00-Code[BT-00-Indicator == TRUE]) == 1");
  }
}
