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
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
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

    assertEquals(9, outputFiles.size(), "Should generate 9 files");
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

  //#region SCOPE clause tests

  @Test
  void testScope_Flag() throws IOException {
    String testName = "testScope_Flag";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testScope_PreExcluded() throws IOException {
    String testName = "testScope_PreExcluded";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion SCOPE clause tests

  //#region Variable tests (output verification)

  @Test
  void testVariable_Global_AppearsBeforeIncludes() throws IOException {
    String testName = "testVariable_Global_AppearsBeforeIncludes";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testVariable_StageLevel() throws IOException {
    String testName = "testVariable_StageLevel";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
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
  }

  //#region Comprehensive/Integration tests

  @Test
  void testOutput_FromSampleRulesFile() throws IOException {
    String testName = "testOutput_FromSampleRulesFile";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(15, outputFiles.size(), "Should generate exactly 15 files");
    assertAllOutputs(testName, outputFiles);
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

  //#region API call tests

  @Test
  void testApiCall_SimpleEndpointAndFunction() throws IOException {
    String testName = "testApiCall_SimpleEndpointAndFunction";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_DefaultEndpointName() throws IOException {
    String testName = "testApiCall_DefaultEndpointName";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_NamedEndpoint() throws IOException {
    String testName = "testApiCall_NamedEndpoint";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_EndpointWithoutUrl() throws IOException {
    String testName = "testApiCall_EndpointWithoutUrl";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_MultipleArguments() throws IOException {
    String testName = "testApiCall_MultipleArguments";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_UndeclaredFunction_ThrowsError() throws IOException {
    String rules = readInput("testApiCall_UndeclaredFunction_ThrowsError");
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translator.translateRules(rules));
    assertEquals(InvalidIdentifierException.ErrorCode.UNDECLARED_IDENTIFIER, exception.getErrorCode());
  }

  @Test
  void testApiCall_UndeclaredEndpoint_ThrowsError() throws IOException {
    String rules = readInput("testApiCall_UndeclaredEndpoint_ThrowsError");
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translator.translateRules(rules));
    assertEquals(InvalidIdentifierException.ErrorCode.UNDECLARED_ENDPOINT, exception.getErrorCode());
  }

  @Test
  void testApiCall_CompositeVariableInitializer_ThrowsError() throws IOException {
    // The grammar enforces that dynamicVariableInitializer only accepts a single function
    // invocation, so a compound expression is rejected at parse time.
    String rules = readInput("testApiCall_CompositeVariableInitializer_ThrowsError");
    assertThrows(Exception.class, () -> translator.translateRules(rules));
  }

  @Test
  void testApiCall_WrongArgumentCount_ThrowsError() throws IOException {
    String rules = readInput("testApiCall_WrongArgumentCount_ThrowsError");
    InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
        () -> translator.translateRules(rules));
    assertEquals(InvalidArgumentException.ErrorCode.ARGUMENT_NUMBER_MISMATCH, exception.getErrorCode());
  }

  @Test
  void testApiCall_WrongArgumentType_ThrowsError() throws IOException {
    String rules = readInput("testApiCall_WrongArgumentType_ThrowsError");
    InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
        () -> translator.translateRules(rules));
    assertEquals(InvalidArgumentException.ErrorCode.ARGUMENT_TYPE_MISMATCH, exception.getErrorCode());
  }

  @Test
  void testApiCall_TranslatorReuse() throws IOException {
    // Verify that calling translateRules twice on the same translator instance works.
    String rules = readInput("testApiCall_Comprehensive");
    translator.translateRules(rules);
    // Second call should not fail with "already declared" errors.
    Map<String, String> outputFiles = translator.translateRules(rules);
    assertEquals(11, outputFiles.size());
  }

  @Test
  void testApiCall_ViaInclude() throws IOException {
    String testName = "testApiCall_ViaInclude";
    IncludedFileResolver resolver = path -> readExpected(testName, path);
    TranslatorOptions options = TranslatorOptions.withResolver(EfxTranslatorOptions.DEFAULT, resolver);

    Map<String, String> outputFiles = translator.translateRules(readInput(testName), options);

    assertEquals(4, outputFiles.size(), "Should generate exactly 4 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_InBooleanExpression() throws IOException {
    String testName = "testApiCall_InBooleanExpression";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size(), "Should generate exactly 5 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_TypeMismatch_InArithmeticExpression() throws IOException {
    String rules = readInput("testApiCall_TypeMismatch_InArithmeticExpression");
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translator.translateRules(rules));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, exception.getErrorCode());
  }

  @Test
  void testApiCall_TwoApiCallsInExpression() throws IOException {
    String testName = "testApiCall_TwoApiCallsInExpression";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_OnErrorReject() throws IOException {
    String testName = "testApiCall_OnErrorReject";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_CustomErrorLabels() throws IOException {
    String testName = "testApiCall_CustomErrorLabels";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_ReportRule() throws IOException {
    String testName = "testApiCall_ReportRule";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(7, outputFiles.size(), "Should generate exactly 7 files");
    assertAllOutputs(testName, outputFiles);
  }

  /**
   * Comprehensive test exercising all API call features together:
   * - 3 endpoints (with URL, with URL, without URL)
   * - 4 API functions across multiple endpoints
   * - 2 stages with multiple rules per stage
   * - Mix of static and dynamic rules
   * - Mix of ASSERT and REPORT
   * - Mix of ON ERROR WARN / REJECT
   * - Mix of default and custom error labels
   * - Single and multiple API calls per rule
   * - Multiple function arguments
   */
  @Test
  void testApiCall_Comprehensive() throws IOException {
    String testName = "testApiCall_Comprehensive";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(11, outputFiles.size(), "Should generate exactly 11 files");
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_ResultInVariable() throws IOException {
    String testName = "testApiCall_ResultInVariable";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(9, outputFiles.size());
    assertAllOutputs(testName, outputFiles);
  }

  @Test
  void testApiCall_IndirectDynamicDependency() throws IOException {
    String testName = "testApiCall_IndirectDynamicDependency";
    Map<String, String> outputFiles = translator.translateRules(readInput(testName));

    assertEquals(5, outputFiles.size());
    assertAllOutputs(testName, outputFiles);
  }

  //#endregion API call tests
}
