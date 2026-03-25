package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.dependencies.DependencyGraph;

class EfxValidationDependencyExtractorTest extends EfxTestsBase {

  private static final String SDK_VERSION = "eforms-sdk-2.0";
  private EfxValidationDependencyExtractor extractor;

  @Override
  protected String getSdkVersion() {
    return SDK_VERSION;
  }

  @BeforeEach
  void setUp() {
    this.extractor = new EfxValidationDependencyExtractor(
        DependencyFactoryMock.INSTANCE.createSymbolResolver(SDK_VERSION, ""),
        ThrowingErrorListener.INSTANCE);
  }

  private String readResource(final String testMethodName, final String filename)
      throws IOException {
    String resourcePath = "/eu/europa/ted/efx/sdk2/EfxValidationDependencyExtractorTest/"
        + testMethodName + "/" + filename;
    try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private void assertDependencyGraph(final String testName) throws IOException {
    String input = this.readResource(testName, "input.efx");
    DependencyGraph graph = this.extractor.extractDependencyGraph(input);
    String actual = graph.toJson();
    String expected = this.readResource(testName, "expected.json");
    assertEquals(expected.stripTrailing(), actual.stripTrailing(),
        "Dependency graph mismatch for " + testName);
  }

  // #region: Basic rules ------------------------------------------------------

  @Test
  void testSimpleRule() throws IOException {
    assertDependencyGraph("testSimpleRule");
  }

  @Test
  void testMultipleRules_SameTarget() throws IOException {
    assertDependencyGraph("testMultipleRules_SameTarget");
  }

  @Test
  void testMultipleRules_DifferentTargets() throws IOException {
    assertDependencyGraph("testMultipleRules_DifferentTargets");
  }

  @Test
  void testConditionalRule() throws IOException {
    assertDependencyGraph("testConditionalRule");
  }

  @Test
  void testNodeTarget() throws IOException {
    assertDependencyGraph("testNodeTarget");
  }

  // #endregion: Basic rules

  // #region: Context variations -----------------------------------------------

  @Test
  void testRootContextShortcut() throws IOException {
    assertDependencyGraph("testRootContextShortcut");
  }

  @Test
  void testContextOverride() throws IOException {
    assertDependencyGraph("testContextOverride");
  }

  @Test
  void testContextVariable() throws IOException {
    assertDependencyGraph("testContextVariable");
  }

  // #endregion: Context variations

  // #region: Field reference variations ---------------------------------------

  @Test
  void testAbsoluteFieldReference() throws IOException {
    assertDependencyGraph("testAbsoluteFieldReference");
  }

  @Test
  void testFieldWithPredicate() throws IOException {
    assertDependencyGraph("testFieldWithPredicate");
  }

  // #endregion: Field reference variations

  // #region: Structure --------------------------------------------------------

  @Test
  void testMultipleStages() throws IOException {
    assertDependencyGraph("testMultipleStages");
  }

  // #endregion: Structure

  // #region: Comprehensive ----------------------------------------------------

  @Test
  void testComprehensive() throws IOException {
    assertDependencyGraph("testComprehensive");
  }

  // #endregion: Comprehensive
}
