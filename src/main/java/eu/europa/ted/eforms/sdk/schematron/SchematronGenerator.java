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
package eu.europa.ted.eforms.sdk.schematron;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.model.rules.CompleteValidation;
import eu.europa.ted.efx.model.rules.ValidationStage;
import eu.europa.ted.efx.model.variables.Variable;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Generates Schematron XML markup using Freemarker templates.
 *
 * This class is responsible for transforming the intermediate validation model
 * (ValidationStage) into Schematron XML format. It implements ValidatorGenerator
 * to provide a clean separation between translation and output generation.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.VALIDATOR_GENERATOR)
public class SchematronGenerator implements ValidatorGenerator {

  private static final Logger logger = LoggerFactory.getLogger(SchematronGenerator.class);

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  private final Configuration freemarkerConfig;

  public SchematronGenerator() {
    this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
    this.freemarkerConfig.setClassForTemplateLoading(SchematronGenerator.class,
        "/freemarker/schematron");
    this.freemarkerConfig.setDefaultEncoding("UTF-8");
    this.freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    this.freemarkerConfig.setLogTemplateExceptions(false);
  }

  // #region ValidatorMarkupGenerator Implementation

  @Override
  public Map<String, String> generateOutput(
      CompleteValidation completeValidation
  ) throws IOException {
    logger.debug("Generating Schematron output from {} stages", completeValidation.getStages().size());

    // Create local state for this generation run
    SchematronSchema schema = new SchematronSchema("eForms schematron rules");
    List<SchematronPattern> patterns = new ArrayList<>();
    Map<String, SchematronDiagnostic> diagnosticsMap = new LinkedHashMap<>();

    // Add global variables to schema
    for (Variable variable : completeValidation.getGlobalVariables()) {
      String xpathValue = variable.initializationExpression.getScript();
      SchematronLet globalVar = new SchematronLet(variable.name, xpathValue);
      schema.addGlobalVariable(globalVar);
      logger.debug("Added global variable: {} = {}", variable.name, xpathValue);
    }

    // Transform intermediate model (ValidationStage) to Schematron model (SchematronPattern)
    transformStagesToPatterns(completeValidation.getStages(), patterns, diagnosticsMap);

    // Add collected diagnostics to schema
    addDiagnosticsToSchema(diagnosticsMap, schema);

    // Generate all output files
    return generateOutputFiles(completeValidation.getNoticeSubtypes(), patterns, schema);
  }

  // #endregion ValidatorMarkupGenerator Implementation

  // #region Freemarker Template Methods

  /**
   * Generates the complete-validation.sch file content.
   *
   * @param schema The SchematronSchema object containing global variables, phases, and includes.
   * @return The XML content of complete-validation.sch as a string.
   * @throws IOException If template loading fails.
   * @throws TemplateException If template processing fails.
   */
  public String generateCompleteValidation(SchematronSchema schema)
      throws IOException, TemplateException {
    Template template = freemarkerConfig.getTemplate("complete-validation.ftl");
    StringWriter writer = new StringWriter();
    template.process(schema, writer);
    return writer.toString();
  }

  /**
   * Generates a single pattern file content (e.g., validation-stage-1a.sch).
   *
   * @param pattern The SchematronPattern object containing rules and assertions.
   * @param config The output configuration specifying which rule natures to include.
   * @return The XML content of the pattern file as a string.
   * @throws IOException If template loading fails.
   * @throws TemplateException If template processing fails.
   */
  public String generatePattern(SchematronPattern pattern, SchematronOutputConfig config)
      throws IOException, TemplateException {
    Template template = freemarkerConfig.getTemplate("pattern.ftl");
    StringWriter writer = new StringWriter();

    Map<String, Object> model = new HashMap<>();
    model.put("id", pattern.getId());
    model.put("variables", pattern.getVariables());
    model.put("rules", pattern.getRules());
    model.put("tags", config.ruleNatures().stream()
        .map(Enum::name)
        .toList());

    template.process(model, writer);
    return writer.toString();
  }

  // #endregion Freemarker Template Methods

  // #region Transformation Methods

  /**
   * Transforms validation stages into Schematron patterns.
   * Creates one pattern per (stage, noticeType) combination, where each pattern
   * only contains the rules/assertions that apply to that specific notice type.
   */
  private void transformStagesToPatterns(List<ValidationStage> stages,
      List<SchematronPattern> patterns, Map<String, SchematronDiagnostic> diagnosticsMap) {
    for (ValidationStage stage : stages) {
      // Get all notice types referenced in this stage
      for (String noticeType : SchematronPattern.getNoticeTypesInStage(stage)) {
        SchematronPattern pattern = new SchematronPattern(stage, noticeType);
        if (pattern.hasRules()) {
          patterns.add(pattern);
          diagnosticsMap.putAll(pattern.getDiagnostics());
          logger.debug("Created pattern {} for stage {} / notice type {}",
              pattern.getId(), stage.getName(), noticeType);
        }
      }
    }
  }

  private void addDiagnosticsToSchema(Map<String, SchematronDiagnostic> diagnosticsMap,
      SchematronSchema schema) {
    for (SchematronDiagnostic diagnostic : diagnosticsMap.values()) {
      schema.addDiagnostic(diagnostic);
    }
  }

  // #endregion Transformation Methods

  // #region Output Generation Methods

  /**
   * Generates all Schematron output files from the processed patterns and schema.
   * This includes individual pattern files, the complete-validation.sch master file,
   * and the schematrons.json metadata file.
   *
   * Output is organized into subfolders based on rule nature:
   * - dynamic/: Contains all rules (static + dynamic) for full validation
   * - static/: Contains only static rules for validation without external services
   *
   * @return A map of filename to file content for all generated Schematron files
   * @throws IOException If an error occurs during file generation
   */
  private Map<String, String> generateOutputFiles(List<String> noticeTypeIds,
      List<SchematronPattern> patterns, SchematronSchema baseSchema) throws IOException {
    logger.debug("Generating Schematron output files");

    Map<String, String> outputFiles = new HashMap<>();
    List<Map<String, Object>> schematronsMetadata = new ArrayList<>();

    // Generate output for each configuration (DYNAMIC first, then STATIC)
    List<SchematronOutputConfig> configs = List.of(
        SchematronOutputConfig.DYNAMIC,
        SchematronOutputConfig.STATIC
    );

    try {
      for (SchematronOutputConfig config : configs) {
        generateOutputForConfig(config, noticeTypeIds, patterns, baseSchema, outputFiles, schematronsMetadata);
      }

      // Generate schematrons.json with entries from all configurations
      String schematronsJson = generateSchematronsJson(schematronsMetadata);
      outputFiles.put("schematrons.json", schematronsJson);

      logger.debug("Generated {} Schematron files", outputFiles.size());

      return outputFiles;

    } catch (TemplateException e) {
      throw new IOException("Failed to generate Schematron XML", e);
    }
  }

  /**
   * Generates output files for a specific configuration (e.g., DYNAMIC or STATIC).
   */
  private void generateOutputForConfig(
      SchematronOutputConfig config,
      List<String> noticeTypeIds,
      List<SchematronPattern> patterns,
      SchematronSchema baseSchema,
      Map<String, String> outputFiles,
      List<Map<String, Object>> schematronsMetadata) throws IOException, TemplateException {

    String folderPrefix = config.folderName() + "/";
    String configType = config.folderName();

    // Create a fresh schema for this configuration
    SchematronSchema schema = new SchematronSchema(baseSchema.getTitle());
    for (SchematronLet globalVar : baseSchema.getGlobalVariables()) {
      schema.addGlobalVariable(globalVar);
    }
    for (SchematronDiagnostic diagnostic : baseSchema.getDiagnostics()) {
      schema.addDiagnostic(diagnostic);
    }

    List<String> generatedPatternIds = new ArrayList<>();

    // Generate individual pattern files for this configuration
    for (SchematronPattern pattern : patterns) {
      // Skip patterns that have no rules matching this configuration
      if (!pattern.hasRulesFor(config.ruleNatures())) {
        logger.debug("Skipping pattern {} for {} (no matching rules)", pattern.getId(), configType);
        continue;
      }

      String patternXml = generatePattern(pattern, config);
      String patternId = pattern.getId();
      String filename = folderPrefix + patternId + ".sch";
      outputFiles.put(filename, patternXml);

      // Add include to schema (relative path within the folder)
      schema.addInclude(patternId + ".sch");
      generatedPatternIds.add(patternId);

      // Add to metadata
      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("name", patternId);
      metadata.put("type", configType);
      if (pattern.getStage() != null) {
        metadata.put("stage", pattern.getStage());
      }
      metadata.put("filename", filename);
      schematronsMetadata.add(metadata);

      logger.debug("Generated {} pattern file: {}", configType, filename);
    }

    // Build and add phases to schema
    Map<String, List<String>> phasesMap = buildPhasesMapForConfig(noticeTypeIds, patterns, config);
    for (Map.Entry<String, List<String>> entry : phasesMap.entrySet()) {
      String noticeTypeId = entry.getKey();
      List<String> patternIds = entry.getValue();

      if (!patternIds.isEmpty()) {
        SchematronPhase phase = new SchematronPhase(noticeTypeId);
        for (String patternId : patternIds) {
          phase.addActivePattern(patternId);
        }
        schema.addPhase(phase);
        logger.debug("Added phase {} with {} patterns for {}", phase.getId(), patternIds.size(), configType);
      }
    }

    // Generate complete-validation.sch for this configuration
    String completeValidation = generateCompleteValidation(schema);
    String completeFilename = folderPrefix + "complete-validation.sch";
    outputFiles.put(completeFilename, completeValidation);

    // Add complete-validation to metadata at the beginning of this config's entries
    Map<String, Object> completeMetadata = new LinkedHashMap<>();
    completeMetadata.put("name", "complete-validation");
    completeMetadata.put("type", configType);
    completeMetadata.put("filename", completeFilename);

    // Find the insertion point (before the pattern entries for this config)
    int insertIndex = schematronsMetadata.size() - generatedPatternIds.size();
    schematronsMetadata.add(insertIndex, completeMetadata);

    logger.debug("Generated {} complete-validation.sch", configType);
  }

  /**
   * Builds a map from notice type ID to list of pattern IDs that apply to it,
   * filtered by the given output configuration.
   * Each pattern now applies to exactly one notice type, so this is a simple grouping.
   * Pattern order is preserved from the EFX file order.
   *
   * @param config The output configuration specifying which rule natures to include
   * @return Map of notice type ID to ordered list of pattern IDs
   */
  private Map<String, List<String>> buildPhasesMapForConfig(List<String> noticeTypeIds,
      List<SchematronPattern> patterns, SchematronOutputConfig config) {
    Map<String, List<String>> phasesMap = new LinkedHashMap<>();

    // Initialize map with all valid notice types
    for (String noticeTypeId : noticeTypeIds) {
      phasesMap.put(noticeTypeId, new ArrayList<>());
    }

    // Each pattern applies to exactly one notice type
    for (SchematronPattern pattern : patterns) {
      // Skip patterns that don't have rules matching this configuration
      if (!pattern.hasRulesFor(config.ruleNatures())) {
        continue;
      }
      String noticeType = pattern.getNoticeType();
      List<String> patternList = phasesMap.get(noticeType);
      if (patternList != null) {
        patternList.add(pattern.getId());
      }
    }

    return phasesMap;
  }

  // #endregion Output Generation Methods

  // #region Helper Methods

  private String generateSchematronsJson(List<Map<String, Object>> schematronsMetadata) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("schematrons", schematronsMetadata);

    try {
      return JSON_MAPPER.writeValueAsString(root) + "\n";
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize schematrons.json", e);
    }
  }

  // #endregion Helper Methods
}
