package eu.europa.ted.efx.sdk2;

import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;

/**
 * Test for EFX expressions that combine several aspects of the language.
 */
class EfxExpressionCombinedV2Test extends EfxTestsBase {
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  @Test
  void testNotAnd() {
    testExpressionTranslationWithContext("not(1 = 2) and (2 = 2)", "BT-00-Text",
        "not(1 == 2) and (2 == 2)");
  }

  @Test
  void testNotPresentAndNotPresent() {
    testExpressionTranslationWithContext("not(PathNode/TextField) and not(PathNode/IntegerField)",
        "ND-Root", "BT-00-Text is not present and BT-00-Integer is not present");
  }

  @Test
  void testCountWithNodeContextOverride() {
    testExpressionTranslationWithContext("count(../../PathNode/CodeField) = 1", "BT-00-Text",
        "count(ND-Root::BT-00-Code) == 1");
  }

  @Test
  void testCountWithAbsoluteFieldReference() {
    testExpressionTranslationWithContext("count(/*/PathNode/CodeField) = 1", "BT-00-Text",
        "count(/BT-00-Code) == 1");
  }

  @Test
  void testCountWithAbsoluteFieldReferenceAndPredicate() {
    testExpressionTranslationWithContext(
        "count(/*/PathNode/CodeField[../IndicatorField = true()]) = 1", "BT-00-Text",
        "count(/BT-00-Code[BT-00-Indicator == TRUE]) == 1");
  }
}
