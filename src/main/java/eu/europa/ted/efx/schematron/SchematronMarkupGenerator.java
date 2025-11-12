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
 * the Lic
 */
package eu.europa.ted.efx.schematron;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import eu.europa.ted.efx.model.schematron.SchematronPattern;
import eu.europa.ted.efx.model.schematron.SchematronSchema;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Generates Schematron XML markup using Freemarker templates.
 *
 * This class is responsible for rendering Schematron data model objects
 * (SchematronSchema, SchematronPattern, etc.) into valid Schematron XML.
 */
public class SchematronMarkupGenerator {

  private final Configuration freemarkerConfig;

  public SchematronMarkupGenerator() {
    this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
    this.freemarkerConfig.setClassForTemplateLoading(SchematronMarkupGenerator.class,
        "/freemarker/schematron");
    this.freemarkerConfig.setDefaultEncoding("UTF-8");
    this.freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    this.freemarkerConfig.setLogTemplateExceptions(false);
  }

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
   * @return The XML content of the pattern file as a string.
   * @throws IOException If template loading fails.
   * @throws TemplateException If template processing fails.
   */
  public String generatePattern(SchematronPattern pattern) throws IOException, TemplateException {
    Template template = freemarkerConfig.getTemplate("pattern.ftl");
    StringWriter writer = new StringWriter();
    template.process(pattern, writer);
    return writer.toString();
  }

  /**
   * Generates the complete-validation.sch file content, writing to the provided Writer.
   *
   * @param schema The SchematronSchema object.
   * @param writer The Writer to output the XML content to.
   * @throws IOException If template loading or writing fails.
   * @throws TemplateException If template processing fails.
   */
  public void generateCompleteValidation(SchematronSchema schema, Writer writer)
      throws IOException, TemplateException {
    Template template = freemarkerConfig.getTemplate("complete-validation.ftl");
    template.process(schema, writer);
  }

  /**
   * Generates a pattern file content, writing to the provided Writer.
   *
   * @param pattern The SchematronPattern object.
   * @param writer The Writer to output the XML content to.
   * @throws IOException If template loading or writing fails.
   * @throws TemplateException If template processing fails.
   */
  public void generatePattern(SchematronPattern pattern, Writer writer)
      throws IOException, TemplateException {
    Template template = freemarkerConfig.getTemplate("pattern.ftl");
    template.process(pattern, writer);
  }
}
