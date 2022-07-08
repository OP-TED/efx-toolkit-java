package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.translator.EfxTranslator;

/**
 * Test for EFX expressions that combine several aspects of the language.
 */
class EfxExpressionCombinedTest {
  final private String SDK_VERSION = "0.7";

  private String test(final String context, final String expression) throws InstantiationException {
    return EfxTranslator.translateExpression(context, expression, DependencyFactoryMock.INSTANCE,
        SDK_VERSION);
  }

  @Test
  void testNotAnd() throws InstantiationException {
    assertEquals("not(1 = 2) and (2 = 2)", test("BT-00-Text", "not(1 == 2) and (2 == 2)"));
  }

  @Test
  void testNotPresentAndNotPresent() throws InstantiationException {
    assertEquals("not(PathNode/TextField) and not(PathNode/IntegerField)",
        test("ND-Root", "BT-00-Text is not present and BT-00-Integer is not present"));
  }

  @Test
  void testCountWithNodeContextOverride() throws InstantiationException {
    assertEquals("count(../../PathNode/CodeField) = 1",
        test("BT-00-Text", "count(ND-Root::BT-00-Code) == 1"));
  }

  @Test
  void testCountWithAbsoluteFieldReference() throws InstantiationException {
    assertEquals("count(/*/PathNode/CodeField) = 1", test("BT-00-Text", "count(/BT-00-Code) == 1"));
  }

  @Test
  void testCountWithAbsoluteFieldReferenceAndPredicate() throws InstantiationException {
    assertEquals("count(/*/PathNode/CodeField[../IndicatorField = true()]) = 1",
        test("BT-00-Text", "count(/BT-00-Code[BT-00-Indicator == TRUE]) == 1"));
  }
}
