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
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;
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
      assertEquals(expectedContent, actualContent, "Content mismatch in " + filename);
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

    assertEquals(19, outputFiles.size(), "Should generate 19 files");
    assertAllOutputs(testName, outputFiles);
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
    assertEquals(57, outputFiles.size(), "Should generate exactly 57 files");

    // Verify we have the expected files
    assertTrue(outputFiles.containsKey("dynamic/complete-validation.sch"));
    assertTrue(outputFiles.containsKey("static/complete-validation.sch"));
    assertTrue(outputFiles.containsKey("schematrons.json"));

    // Check that patterns exist for each stage
    assertTrue(outputFiles.keySet().stream().anyMatch(f -> f.startsWith("dynamic/validation-stage-1a-")));
    assertTrue(outputFiles.keySet().stream().anyMatch(f -> f.startsWith("dynamic/validation-stage-1b-")));
    assertTrue(outputFiles.keySet().stream().anyMatch(f -> f.startsWith("dynamic/validation-stage-2a-")));
    assertTrue(outputFiles.keySet().stream().anyMatch(f -> f.startsWith("dynamic/validation-stage-3a-")));
    assertTrue(outputFiles.keySet().stream().anyMatch(f -> f.startsWith("static/validation-stage-1a-")));

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
}
