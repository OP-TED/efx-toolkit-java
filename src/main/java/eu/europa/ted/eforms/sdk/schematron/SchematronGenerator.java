/*
 * Copyright 2025 European Union
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.model.rules.ValidationPlan;
import eu.europa.ted.efx.model.rules.RuleNature;
import eu.europa.ted.efx.model.rules.ValidationStage;
import eu.europa.ted.efx.model.variables.Variable;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
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

  private static final String CLASSPATH_TEMPLATES = "/freemarker/schematron";

  private final Configuration freemarkerConfig;

  public SchematronGenerator() {
    this(null);
  }

  public SchematronGenerator(final TranslatorOptions options) {
    this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_34);
    this.freemarkerConfig.setTemplateLoader(this.resolveTemplateLoader(options));
    this.freemarkerConfig.setDefaultEncoding("UTF-8");
    this.freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    this.freemarkerConfig.setLogTemplateExceptions(false);
  }

  private TemplateLoader resolveTemplateLoader(final TranslatorOptions options) {
    final ClassTemplateLoader classpathLoader =
            new ClassTemplateLoader(SchematronGenerator.class, CLASSPATH_TEMPLATES);

    if (options == null) {
      return classpathLoader;
    }

    final Path templatesRoot = options.getTemplatesRoot();
    if (templatesRoot == null || !Files.isDirectory(templatesRoot)) {
      return classpathLoader;
    }

    try {
      return new MultiTemplateLoader(new TemplateLoader[] {
              new FileTemplateLoader(templatesRoot.toFile()), classpathLoader
      });
    } catch (final IOException e) {
      logger.warn("Failed to load templates from {}, using classpath defaults", templatesRoot);
      return classpathLoader;
    }
  }

  // #region ValidatorMarkupGenerator Implementation

  @Override
  public Map<String, String> generateOutput(ValidationPlan validationPlan) {
    logger.debug("Generating Schematron output from {} stages", validationPlan.getStages().size());

    // Create local state for this generation run
    SchematronSchema schema = new SchematronSchema("eForms schematron rules");
    List<SchematronPattern> patterns = new ArrayList<>();
    Map<String, SchematronDiagnostic> diagnosticsMap = new LinkedHashMap<>();

    // Add endpoint params to schema
    for (Map.Entry<String, String> endpoint : validationPlan.getEndpoints().entrySet()) {
      String url = endpoint.getValue() != null ? endpoint.getValue() : "";
      SchematronParam param = new SchematronParam("apiUrl-" + endpoint.getKey(), "'" + url + "'");
      schema.addParam(param);
      logger.debug("Added endpoint param: {} = {}", param.getName(), param.getValue());
    }

    // Add global variables to schema
    for (Variable variable : validationPlan.getVariables()) {
      SchematronLet letElement = new SchematronLet(variable);
      schema.addLetElement(letElement);
      logger.debug("Added global variable: {} = {}", variable.name, variable.initializationExpression.getScript());
    }

    // Transform intermediate model (ValidationStage) to Schematron model (SchematronPattern)
    for (ValidationStage stage : validationPlan.getStages()) {
      if (stage.containsUniversalRules()) {
        SchematronPattern sharedPattern = new SchematronPattern(stage);
        if (sharedPattern.hasRules()) {
          patterns.add(sharedPattern);
          diagnosticsMap.putAll(sharedPattern.getDiagnostics());
          logger.debug("Created shared pattern {} for stage {}",
              sharedPattern.getId(), stage.getName());
        }
      }
      for (String noticeSubtype : stage.getNoticeSubtypes()) {
        SchematronPattern pattern = new SchematronPattern(stage, noticeSubtype);
        if (pattern.hasRules()) {
          patterns.add(pattern);
          diagnosticsMap.putAll(pattern.getDiagnostics());
          logger.debug("Created pattern {} for stage {} / notice subtype {}",
              pattern.getId(), stage.getName(), noticeSubtype);
        }
      }
    }

    // Add collected diagnostics to schema
    for (SchematronDiagnostic diagnostic : diagnosticsMap.values()) {
      schema.addDiagnostic(diagnostic);
    }

    // Generate all output files
    try {
      return this.generateOutputFiles(validationPlan.getNoticeSubtypes(), patterns, schema);
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate Schematron output", e);
    }
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
    List<String> tags = config.ruleNatures().stream()
        .map(Enum::name).collect(Collectors.toList());
    List<SchematronLet> letElements = pattern.getLetElements().stream()
        .filter(v -> tags.contains(v.getTag())).collect(Collectors.toList());
    model.put("letElements", letElements);
    model.put("rules", pattern.getRules());
    model.put("tags", tags);

    template.process(model, writer);
    return writer.toString();
  }

  // #endregion Freemarker Template Methods

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
  private Map<String, String> generateOutputFiles(List<String> noticeSubtypeIds,
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
        this.generateOutputForConfig(config, noticeSubtypeIds, patterns, baseSchema, outputFiles, schematronsMetadata);
      }

      // Generate schematrons.json with entries from all configurations
      String schematronsJson = this.generateSchematronsJson(schematronsMetadata);
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
      List<String> noticeSubtypeIds,
      List<SchematronPattern> patterns,
      SchematronSchema baseSchema,
      Map<String, String> outputFiles,
      List<Map<String, Object>> schematronsMetadata) throws IOException, TemplateException {

    String folderPrefix = config.folderName() + "/";
    String configType = config.folderName();

    // Create a fresh schema for this configuration
    SchematronSchema schema = new SchematronSchema(baseSchema.getTitle());
    // API endpoint params are only relevant for configurations that include dynamic rules
    if (config.ruleNatures().contains(RuleNature.DYNAMIC)) {
      for (SchematronParam param : baseSchema.getParams()) {
        schema.addParam(param);
      }
    }
    Set<String> configTags = config.ruleNatures().stream()
        .map(Enum::name).collect(Collectors.toSet());
    for (SchematronLet letElement : baseSchema.getLetElements()) {
      if (configTags.contains(letElement.getTag())) {
        schema.addLetElement(letElement);
      }
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
    Map<String, List<String>> phasesMap = buildPhasesMapForConfig(noticeSubtypeIds, patterns, config);
    for (Map.Entry<String, List<String>> entry : phasesMap.entrySet()) {
      String noticeSubtypeId = entry.getKey();
      List<String> patternIds = entry.getValue();

      if (!patternIds.isEmpty()) {
        SchematronPhase phase = new SchematronPhase(noticeSubtypeId);
        for (String patternId : patternIds) {
          phase.addActivePattern(patternId);
        }
        schema.addPhase(phase);
        logger.debug("Added phase {} with {} patterns for {}", phase.getId(), patternIds.size(), configType);
      }
    }

    // Generate complete-validation.sch for this configuration
    String completeValidationContent = generateCompleteValidation(schema);
    String completeFilename = folderPrefix + "complete-validation.sch";
    outputFiles.put(completeFilename, completeValidationContent);

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
   * Builds a map from notice subtype ID to list of pattern IDs that apply to it,
   * filtered by the given output configuration.
   * Shared patterns (applying to all subtypes) are added to every phase.
   * Subtype-specific patterns are added only to their notice subtype's phase.
   *
   * @param config The output configuration specifying which rule natures to include
   * @return Map of notice subtype ID to ordered list of pattern IDs
   */
  private Map<String, List<String>> buildPhasesMapForConfig(List<String> noticeSubtypeIds,
      List<SchematronPattern> patterns, SchematronOutputConfig config) {
    Map<String, List<String>> phasesMap = new LinkedHashMap<>();

    // Initialize map with all valid notice subtypes
    for (String noticeSubtypeId : noticeSubtypeIds) {
      phasesMap.put(noticeSubtypeId, new ArrayList<>());
    }

    for (SchematronPattern pattern : patterns) {
      // Skip patterns that don't have rules matching this configuration
      if (!pattern.hasRulesFor(config.ruleNatures())) {
        continue;
      }

      if (pattern.isShared()) {
        // Shared patterns go into all phases
        for (List<String> patternList : phasesMap.values()) {
          patternList.add(pattern.getId());
        }
      } else {
        // Subtype-specific patterns go only into their notice subtype's phase
        String noticeSubtype = pattern.getNoticeSubtype();
        List<String> patternList = phasesMap.get(noticeSubtype);
        if (patternList != null) {
          patternList.add(pattern.getId());
        }
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
