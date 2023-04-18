package eu.europa.ted.efx;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for EFX expressions that combine several aspects of the language.
 */
class EfxExpressionCombinedTest extends EfxTestsBase {
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
