/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;
import eu.europa.ted.efx.model.rules.NoticeSubtypeRange;
import eu.europa.ted.eforms.sdk.schematron.SchematronGenerator;

/**
 * Unit tests for EfxRulesTranslatorV2.
 * Tests the transpilation of EFX Rules files to Schematron XML.
 */
class EfxRulesTranslatorV2Test extends EfxTestsBase {

  private static final String SDK_VERSION = "eforms-sdk-2.0";
  private EfxRulesTranslatorV2 translator;

  @Override
  protected String getSdkVersion() {
    return SDK_VERSION;
  }

  @BeforeEach
  void setUp() throws InstantiationException {
    translator = new EfxRulesTranslatorV2(
        new SchematronGenerator(),
        DependencyFactoryMock.INSTANCE.createSymbolResolver(SDK_VERSION, ""),
        DependencyFactoryMock.INSTANCE.createScriptGenerator(SDK_VERSION, "",
            new EfxTranslatorOptions("udf", DecimalFormat.EFX_DEFAULT)),
        ThrowingErrorListener.INSTANCE);
  }

  //#region Private helper methods

  private String readExpected(String testMethodName, String filename) throws IOException {
    String resourcePath = "/eu/europa/ted/efx/sdk2/EfxRulesTranslatorV2Test/"
        + testMethodName + "/" + filename;
    try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String readInput(String testMethodName) throws IOException {
    return readExpected(testMethodName, "input.efx");
  }

  private void assertValidXml(String xmlContent, String description) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.parse(new InputSource(new StringReader(xmlContent)));
    } catch (Exception e) {
      throw new AssertionError(description + " is not valid XML: " + e.getMessage(), e);
    }
  }

  /**
   * Verifies all generated output files match expected content.
   * Reads expected files from test resources folder structure that mirrors actual output.
   */
  private void assertAllOutputs(String testName, Map<String, String> outputFiles) throws IOException {
    for (Map.Entry<String, String> entry : outputFiles.entrySet()) {
      String filename = entry.getKey();
      String actualContent = entry.getValue();

      String expectedContent = readExpected(testName, filename);

      if (filename.endsWith(".sch")) {
        assertValidXml(actualContent, filename);
      }
      assertEquals(expectedContent.stripTrailing(), actualContent.stripTrailing(),
          "Content mismatch in " + filename);
    }
  }

  //#endregion Private helper methods

  //#region STAGE tests

  @Test
  void testStage_Multiple_GeneratesSeparatePatterns() throws IOException {
    String testName = "testStage_Multiple_GeneratesSeparatePatterns";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(11, outputFiles.size(), "Should generate exactly 11 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion STAGE tests

  //#region WITH clause tests

  @Test
  void testWithClause_RootContextShortcut() throws IOException {
    String testName = "testWithClause_RootContextShortcut";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testWithClause_ContextVariable() throws IOException {
    String testName = "testWithClause_ContextVariable";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testWithClause_VariablePositioning() throws IOException {
    String testName = "testWithClause_VariablePositioning";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(11, outputFiles.size(), "Should generate 11 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testWithClause_ContextVariableOverride() throws IOException {
    String testName = "testWithClause_ContextVariableOverride";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion WITH clause tests

  //#region ASSERT and REPORT tests

  @Test
  void testAssertAndReport_Simple() throws IOException {
    String testName = "testAssertAndReport_Simple";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion ASSERT and REPORT tests

  //#region WHEN/OTHERWISE tests

  @Test
  void testWhen_SimpleCondition() throws IOException {
    String testName = "testWhen_SimpleCondition";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testWhen_WithOtherwise() throws IOException {
    String testName = "testWhen_WithOtherwise";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion WHEN/OTHERWISE tests

  //#region FOR clause tests

  @Test
  void testForClause_NodeReference() throws IOException {
    String testName = "testForClause_NodeReference";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion FOR clause tests

  //#region IN clause tests

  @Test
  void testInClause_SpecificNoticeTypes() throws IOException {
    String testName = "testInClause_SpecificNoticeTypes";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(9, outputFiles.size(), "Should generate exactly 9 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testInClause_NoticeTypeRange() throws IOException {
    String testName = "testInClause_NoticeTypeRange";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(15, outputFiles.size(), "Should generate exactly 15 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testInClause_AllNoticeTypes() throws IOException {
    String testName = "testInClause_AllNoticeTypes";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testInClause_MixedAllAndSpecific() throws IOException {
    String testName = "testInClause_MixedAllAndSpecific";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    // 1 shared + 2 specific per config (dynamic + static) + 2 complete-validation + schematrons.json
    assertEquals(9, outputFiles.size(), "Should generate 9 files");

    // Shared pattern exists (no subtype suffix)
    assertTrue(outputFiles.containsKey("dynamic/validation-stage-1a.sch"));
    // Subtype-specific patterns exist
    assertTrue(outputFiles.containsKey("dynamic/validation-stage-1a-1.sch"));
    assertTrue(outputFiles.containsKey("dynamic/validation-stage-1a-2.sch"));

    // Shared pattern should contain R-K7P-M2Q (the IN * rule)
    String sharedPattern = outputFiles.get("dynamic/validation-stage-1a.sch");
    assertTrue(sharedPattern.contains("R-K7P-M2Q"));
    assertFalse(sharedPattern.contains("R-X3F-N8W"), "Specific rule should not be in shared pattern");

    // Specific patterns should contain R-X3F-N8W but not R-K7P-M2Q
    String specific1 = outputFiles.get("dynamic/validation-stage-1a-1.sch");
    assertTrue(specific1.contains("R-X3F-N8W"));
    assertFalse(specific1.contains("R-K7P-M2Q"), "Shared rule should not be in specific pattern");

    // Complete validation should reference both shared and specific patterns in phases
    String completeValidation = outputFiles.get("dynamic/complete-validation.sch");
    String phase1 = extractPhase(completeValidation, "eforms-1");
    assertTrue(phase1.contains("EFORMS-validation-stage-1a\""), "Phase 1 should include shared pattern");
    assertTrue(phase1.contains("EFORMS-validation-stage-1a-1"), "Phase 1 should include specific pattern");

    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testInClause_ReversedRange_ThrowsInvalidRangeOrder() {
    String rules = lines(
        "---- STAGE 1a ----",
        "",
        "WITH ND-SubNode",
        "    ASSERT BT-00-Text is present",
        "    AS ERROR R-K7P-M2Q",
        "    FOR BT-00-Text IN 11-9"
    );
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translator.translateRules(rules));
    assertEquals(InvalidUsageException.ErrorCode.INVALID_NOTICE_SUBTYPE_RANGE_ORDER,
        exception.getErrorCode());
  }

  @Test
  void testInClause_MalformedToken_ThrowsInvalidToken() {
    List<String> subtypes = Arrays.asList("1", "2", "3");
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> new NoticeSubtypeRange("1-2-3", subtypes));
    assertEquals(InvalidUsageException.ErrorCode.INVALID_NOTICE_SUBTYPE_TOKEN,
        exception.getErrorCode());
  }

  //#endregion IN clause tests

  //#region Variable tests (output verification)

  @Test
  void testVariable_Global_AppearsBeforeIncludes() throws IOException {
    String testName = "testVariable_Global_AppearsBeforeIncludes";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);

    // Verify global variables appear before <include> elements in complete-validation.sch
    String completeValidation = outputFiles.get("dynamic/complete-validation.sch");
    int letIndex = completeValidation.indexOf("<let name=");
    int includeIndex = completeValidation.indexOf("<include href=");
    assertTrue(letIndex < includeIndex,
        "Schema-level variables should appear before <include> elements");
  }

  @Test
  void testVariable_StageLevel() throws IOException {
    String testName = "testVariable_StageLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);

    // Verify each pattern has its own variable with the correct value
    String stage1a = outputFiles.get("dynamic/validation-stage-1a-1.sch");
    String stage1b = outputFiles.get("dynamic/validation-stage-1b-1.sch");

    assertTrue(stage1a.contains("name=\"stageVar\""), "Stage 1a should have stageVar variable");
    assertTrue(stage1a.contains("&quot;first&quot;"), "Stage 1a stageVar should have value 'first'");

    assertTrue(stage1b.contains("name=\"stageVar\""), "Stage 1b should have stageVar variable");
    assertTrue(stage1b.contains("&quot;second&quot;"), "Stage 1b stageVar should have value 'second'");
  }

  //#endregion Variable tests (output verification)

  //#region Sequence variable tests

  @Test
  void testVariable_TextSequence_GlobalLevel() throws IOException {
    String testName = "testVariable_TextSequence_GlobalLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testVariable_NumericSequence_GlobalLevel() throws IOException {
    String testName = "testVariable_NumericSequence_GlobalLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testVariable_BooleanSequence_GlobalLevel() throws IOException {
    String testName = "testVariable_BooleanSequence_GlobalLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testVariable_DateSequence_GlobalLevel() throws IOException {
    String testName = "testVariable_DateSequence_GlobalLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testVariable_TimeSequence_GlobalLevel() throws IOException {
    String testName = "testVariable_TimeSequence_GlobalLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testVariable_DurationSequence_GlobalLevel() throws IOException {
    String testName = "testVariable_DurationSequence_GlobalLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion Sequence variable tests

  //#region Variable tests (error verification)

  @Test
  void testVariable_GlobalOnly_FailsWithoutStage() {
    String simpleRules = "LET text : $testVar = \"test-value\";";
    assertThrows(Exception.class, () -> translator.translateRules(simpleRules));
  }

  @Test
  void testVariable_StageScopeIsolation() {
    String rulesWithStageScoping = lines(
        "---- STAGE 1a ----",
        "LET text : $stageVar = \"stage1\";",
        "",
        "WITH BT-00-Text",
        "ASSERT $stageVar is not empty",
        "AS ERROR BR-BT-00001-0001",
        "FOR BT-00-Text IN 1, 2;",
        "",
        "---- STAGE 1b ----",
        "WITH BT-01-Text",
        "ASSERT $stageVar is not empty",
        "AS ERROR BR-BT-00001-0002",
        "FOR BT-01-Text IN 1, 2;"
    );

    assertThrows(Exception.class, () -> translator.translateRules(rulesWithStageScoping),
        "Pattern-level variables should not be visible across stages");
  }

  @Test
  void testVariable_RuleScopeIsolation() {
    String rulesWithRuleScoping = lines(
        "---- STAGE 1a ----",
        "WITH text : $ruleVar = \"rule1\", BT-00-Text",
        "ASSERT $ruleVar is not empty",
        "AS ERROR BR-BT-00001-0001",
        "FOR BT-00-Text IN 1, 2;",
        "",
        "WITH BT-01-Text",
        "ASSERT $ruleVar is not empty",
        "AS ERROR BR-BT-00001-0002",
        "FOR BT-01-Text IN 1, 2;"
    );

    assertThrows(Exception.class, () -> translator.translateRules(rulesWithRuleScoping),
        "WITH clause variables should not be visible across rules");
  }

  //#endregion Variable tests (error verification)

  //#region Diagnostics tests

  @Test
  void testDiagnostics_MultipleFields() throws IOException {
    String testName = "testDiagnostics_MultipleFields";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testDiagnostics_NoDuplicateEntries() throws IOException {
    String testName = "testDiagnostics_NoDuplicateEntries";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testDiagnostics_SanitizesParentheses() throws IOException {
    String testName = "testDiagnostics_SanitizesParentheses";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion Diagnostics tests

  @Test
  void testInClause_PhaseGeneration() throws IOException {
    String testName = "testInClause_PhaseGeneration";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(19, outputFiles.size(), "Should generate exactly 19 files");
    assertAllOutputs(testName, outputFiles);

    // Verify phase generation logic
    String completeValidation = outputFiles.get("dynamic/complete-validation.sch");

    assertTrue(completeValidation.contains("<phase id=\"eforms-1\">"));
    assertTrue(completeValidation.contains("<phase id=\"eforms-2\">"));
    assertTrue(completeValidation.contains("<phase id=\"eforms-3\">"));
    assertFalse(completeValidation.contains("<phase id=\"eforms-4\">"));

    String phase1 = extractPhase(completeValidation, "eforms-1");
    assertTrue(phase1.contains("validation-stage-1"));
    assertTrue(phase1.contains("validation-stage-2"));
    assertTrue(phase1.contains("validation-stage-3"));

    String phase2 = extractPhase(completeValidation, "eforms-2");
    assertTrue(phase2.contains("validation-stage-1"));
    assertTrue(phase2.contains("validation-stage-2"));
    assertFalse(phase2.contains("validation-stage-3"));

    String phase3 = extractPhase(completeValidation, "eforms-3");
    assertTrue(phase3.contains("validation-stage-1"));
    assertTrue(phase3.contains("validation-stage-2"));
    assertTrue(phase3.contains("validation-stage-3"));
  }

  private String extractPhase(String content, String phaseId) {
    int start = content.indexOf("<phase id=\"" + phaseId + "\">");
    int end = content.indexOf("</phase>", start);
    return content.substring(start, end);
  }

  //#region Comprehensive/Integration tests

  @Test
  void testOutput_FromSampleRulesFile() throws IOException {
    String testName = "testOutput_FromSampleRulesFile";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertFalse(outputFiles.isEmpty(), "Output files should not be empty");
    assertEquals(15, outputFiles.size(), "Should generate exactly 15 files");

    // Verify we have the expected files
    assertTrue(outputFiles.containsKey("dynamic/complete-validation.sch"));
    assertTrue(outputFiles.containsKey("static/complete-validation.sch"));
    assertTrue(outputFiles.containsKey("schematrons.json"));

    // Stages with only IN * rules produce shared patterns (no subtype suffix)
    assertTrue(outputFiles.containsKey("dynamic/validation-stage-1a.sch"));
    assertTrue(outputFiles.containsKey("dynamic/validation-stage-2a.sch"));
    assertTrue(outputFiles.containsKey("dynamic/validation-stage-3a.sch"));
    assertTrue(outputFiles.containsKey("static/validation-stage-1a.sch"));

    // Stage 1b has subtype-specific rules
    assertTrue(outputFiles.keySet().stream().anyMatch(f -> f.startsWith("dynamic/validation-stage-1b-")));

    // Verify XML well-formedness for all .sch files
    for (Map.Entry<String, String> entry : outputFiles.entrySet()) {
      if (entry.getKey().endsWith(".sch")) {
        assertValidXml(entry.getValue(), entry.getKey());
      }
    }

    // Verify schematrons.json structure
    String schematronsJson = outputFiles.get("schematrons.json");
    assertTrue(schematronsJson.contains("\"schematrons\""));
    assertTrue(schematronsJson.contains("\"type\" : \"dynamic\""));
    assertTrue(schematronsJson.contains("\"type\" : \"static\""));
  }

  @Test
  void testOutput_ComprehensiveMixedRules() throws IOException {
    String testName = "testOutput_ComprehensiveMixedRules";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(11, outputFiles.size(), "Should generate 11 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion Comprehensive/Integration tests

  //#region Context variable type tests

  /**
   * Context variables should always be scalar, even when pointing to a repeatable field.
   * This is because the context iterates, so the variable holds the current iteration value.
   * Using $ctx in scalar arithmetic ($ctx + 1) should work.
   */
  @Test
  void testContextVariable_RepeatableField_UsedAsScalar() throws IOException {
    String testName = "testContextVariable_RepeatableField_UsedAsScalar";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion Context variable type tests

  //#region Include directive tests

  /**
   * Verifies that a rules file with #include produces the same output
   * as the equivalent rules file with all content inlined.
   */
  @Test
  void testInclude_SingleFile_SameOutputAsInlined() throws IOException {
    String testName = "testInclude_SingleFile_SameOutputAsInlined";

    IncludedFileResolver resolver = path -> readExpected(testName, path);
    EfxTranslatorOptions options = new EfxTranslatorOptions(
        false, null, EfxTranslatorOptions.DEFAULT_UDF_NAMESPACE,
        DecimalFormat.XSL_DEFAULT, resolver, java.util.Locale.ENGLISH);

    Map<String, String> outputFiles = translator.translateRules(readInput(testName), options);

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  /**
   * Verifies that translateRules(Path) auto-wires a filesystem resolver
   * when no resolver is provided in options.
   */
  @Test
  void testInclude_FromPath_DefaultResolverWorks(@TempDir Path tempDir) throws IOException {
    String mainContent =
        "---- STAGE 1a ----\n\n"
        + "WITH BT-00-Text\n"
        + "    REPORT empty(BT-00-Text)\n"
        + "    AS WARNING R-K7P-M2Q\n"
        + "    FOR BT-00-Text IN 1\n\n"
        + "#include \"extra.efx\"\n";

    String extraContent =
        "---- STAGE 1b ----\n\n"
        + "WITH BT-00-Text\n"
        + "    ASSERT BT-00-Text is present\n"
        + "    AS ERROR R-X3F-N8W\n"
        + "    FOR BT-00-Text IN 1\n";

    Files.writeString(tempDir.resolve("rules.efx"), mainContent);
    Files.writeString(tempDir.resolve("extra.efx"), extraContent);

    Map<String, String> outputFiles =
        translator.translateRules(tempDir.resolve("rules.efx"), EfxTranslatorOptions.DEFAULT);

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    for (Map.Entry<String, String> entry : outputFiles.entrySet()) {
      if (entry.getKey().endsWith(".sch")) {
        assertValidXml(entry.getValue(), entry.getKey());
      }
    }
  }

  /**
   * Verifies that translateRules(String, options) propagates include IO failures
   * as UncheckedIOException with meaningful context.
   */
  @Test
  void testInclude_FromString_IOFailurePropagates() {
    String input = "#include \"missing.efx\"\n---- STAGE 1a ----\n";

    IncludedFileResolver resolver = path -> {
      throw new IOException("File not found: " + path);
    };

    EfxTranslatorOptions options = new EfxTranslatorOptions(
        false, null, EfxTranslatorOptions.DEFAULT_UDF_NAMESPACE,
        DecimalFormat.XSL_DEFAULT, resolver, java.util.Locale.ENGLISH);

    UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
        () -> translator.translateRules(input, options));
    assertTrue(thrown.getMessage().contains("Include resolution failed"));
  }

  //#endregion Include directive tests
}
