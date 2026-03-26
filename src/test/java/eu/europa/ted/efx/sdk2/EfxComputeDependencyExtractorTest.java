package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

class EfxComputeDependencyExtractorTest {

  private static final String SDK_VERSION = "eforms-sdk-2.0";

  private static Set<String> extract(final String expression) {
    try {
      return EfxTranslator.extractComputeDependencies(DependencyFactoryMock.INSTANCE, SDK_VERSION,
          expression);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  // #region: Context dependencies ---------------------------------------------

  @Test
  void testFieldContext() {
    Set<String> deps = extract("{BT-00-Text} ${ALWAYS}");
    assertTrue(deps.contains("BT-00-Text"));
  }

  @Test
  void testNodeContext() {
    Set<String> deps = extract("{ND-Root} ${ALWAYS}");
    assertTrue(deps.contains("ND-Root"));
  }

  @Test
  void testFieldContext_ComputeSyntax() {
    Set<String> deps = extract("WITH BT-00-Text COMPUTE ALWAYS");
    assertTrue(deps.contains("BT-00-Text"));
  }

  @Test
  void testNodeContext_ComputeSyntax() {
    Set<String> deps = extract("WITH ND-Root COMPUTE ALWAYS");
    assertTrue(deps.contains("ND-Root"));
  }

  // #endregion: Context dependencies

  // #region: Expression field references --------------------------------------

  @Test
  void testFieldReferenceInExpression() {
    Set<String> deps = extract("{ND-Root} ${BT-00-Text is present}");
    assertEquals(Set.of("ND-Root", "BT-00-Text"), deps);
  }

  @Test
  void testMultipleFieldReferences() {
    Set<String> deps = extract("{ND-Root} ${BT-00-Text == BT-00-Number}");
    assertEquals(Set.of("ND-Root", "BT-00-Text", "BT-00-Number"), deps);
  }

  @Test
  void testAbsoluteFieldReference() {
    Set<String> deps = extract("{ND-Root} ${/BT-00-Text is present}");
    assertEquals(Set.of("ND-Root", "BT-00-Text"), deps);
  }

  @Test
  void testFieldReferenceWithContextOverride() {
    Set<String> deps = extract("{ND-Root} ${ND-Root::BT-00-Text is present}");
    assertTrue(deps.contains("ND-Root"));
    assertTrue(deps.contains("BT-00-Text"));
  }

  // #endregion: Expression field references

  // #region: Node references --------------------------------------------------

  @Test
  void testNodeReferenceInExpression() {
    Set<String> deps = extract("{ND-Root} ${ND-Root::BT-00-Text is present}");
    assertTrue(deps.contains("ND-Root"));
    assertTrue(deps.contains("BT-00-Text"));
  }

  // #endregion: Node references

  // #region: No spurious dependencies -----------------------------------------

  @Test
  void testLiteralExpression_NoDependencies() {
    Set<String> deps = extract("{BT-00-Text} ${1 + 2}");
    assertEquals(Set.of("BT-00-Text"), deps);
  }

  @Test
  void testBooleanLiteral_NoDependencies() {
    Set<String> deps = extract("{BT-00-Text} ${ALWAYS and TRUE}");
    assertEquals(Set.of("BT-00-Text"), deps);
  }

  // #endregion: No spurious dependencies

  // #region: COMPUTE syntax ---------------------------------------------------

  @Test
  void testComputeSyntax_WithFieldReferences() {
    Set<String> deps = extract("WITH ND-Root COMPUTE BT-00-Text == BT-00-Number");
    assertEquals(Set.of("ND-Root", "BT-00-Text", "BT-00-Number"), deps);
  }

  @Test
  void testComputeSyntax_WithAbsoluteReference() {
    Set<String> deps = extract("WITH ND-Root COMPUTE /BT-00-Text is present");
    assertEquals(Set.of("ND-Root", "BT-00-Text"), deps);
  }

  // #endregion: COMPUTE syntax

  // #region: Deduplication ----------------------------------------------------

  @Test
  void testDuplicateFieldReferences() {
    Set<String> deps = extract("{ND-Root} ${BT-00-Text == BT-00-Text}");
    assertEquals(Set.of("ND-Root", "BT-00-Text"), deps);
  }

  // #endregion: Deduplication
}
