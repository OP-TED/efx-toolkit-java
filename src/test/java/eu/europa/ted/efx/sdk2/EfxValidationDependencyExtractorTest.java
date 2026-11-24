package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
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

  // #region: Behaviours -------------------------------------------------------

  // A rule written with a behaviour has the dependencies of the rule written with the expression
  // that the behaviour stands for. The test extracts the graph of the same rules twice: written
  // with behaviours (input.efx), and written with these expressions (expressions.efx).

  @Test
  void testBehaviours() throws IOException {
    assertDependencyGraph("testBehaviours");
    assertEquals(
        this.extractor.extractDependencyGraph(this.readResource("testBehaviours", "expressions.efx"))
            .toJson(),
        this.extractor.extractDependencyGraph(this.readResource("testBehaviours", "input.efx"))
            .toJson(),
        "The rules written with behaviours and with expressions should have the same dependencies");
  }

  // #endregion: Behaviours

  // #region: Structure --------------------------------------------------------

  @Test
  void testMultipleStages() throws IOException {
    assertDependencyGraph("testMultipleStages");
  }

  @Test
  void testGlobalAndStageVariables() throws IOException {
    assertDependencyGraph("testGlobalAndStageVariables");
  }

  // #endregion: Structure

  // #region: Rules file without a stage ---------------------------------------

  @Test
  void testEmptyRulesFile_IsRejected() {
    assertEmptyRulesFile("");
  }

  @Test
  void testRulesFileWithoutAStage_IsRejected() {
    assertEmptyRulesFile("LET text : $testVar = \"test-value\";");
  }

  private void assertEmptyRulesFile(final String rules) {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> this.extractor.extractDependencyGraph(rules));
    assertEquals(InvalidUsageException.ErrorCode.EMPTY_RULES_FILE, exception.getErrorCode());
    assertEquals("Rules file must contain at least one validation stage.",
        exception.getMessage());
  }

  // #endregion: Rules file without a stage

  // #region: Comprehensive ----------------------------------------------------

  @Test
  void testComprehensive() throws IOException {
    assertDependencyGraph("testComprehensive");
  }

  // #endregion: Comprehensive
}
