/*
 * Copyright 2026 European Union
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
package eu.europa.ted.efx.model.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.TemplateSection;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.identifiers.Dictionary;
import eu.europa.ted.efx.model.identifiers.Function;
import eu.europa.ted.efx.model.identifiers.Identifier;
import eu.europa.ted.efx.model.identifiers.Variable;
import eu.europa.ted.efx.model.identifiers.Variables;

/**
 * A view template, as described by the templateFile rule of the grammar: its global declarations and
 * its sections. It is built while the template file is parsed, and rendered once parsing is complete.
 */
public class ViewTemplate {

  // The global declarations of the template file, in the order they are written: variables,
  // dictionaries and functions.
  private final List<Identifier> globalDeclarations = new ArrayList<>();

  // The declared templates, in the order they are written. They belong to no section. Their root block
  // only holds them: each declared template is named after itself, not after the root.
  private final ContentBlock templateDeclarations = ContentBlock.newRootBlock("templates");

  // The body holds the template lines that come first in the template file. The summary,
  // navigation and header sections follow, each after its own section header. They are optional:
  // when a template file leaves them out, they are empty.
  private final ViewTemplateSection body = new ViewTemplateSection(TemplateSection.DEFAULT, "body");
  private final ViewTemplateSection summary = new ViewTemplateSection(TemplateSection.SUMMARY, "summary");
  private final ViewTemplateSection navigation = new ViewTemplateSection(TemplateSection.NAVIGATION, "nav");
  private final ViewTemplateSection header = new ViewTemplateSection(TemplateSection.HEADER, "header");

  // The lines at the top of the header section that display a part of the header, by part.
  private final Map<HeaderPart, ContentBlock> headerParts = new EnumMap<>(HeaderPart.class);

  public void addVariable(final Variable variable) {
    this.globalDeclarations.add(variable);
  }

  public void addDictionary(final Dictionary dictionary) {
    this.globalDeclarations.add(dictionary);
  }

  public void addFunction(final Function function) {
    this.globalDeclarations.add(function);
  }

  public List<Identifier> getGlobalDeclarations() {
    return Collections.unmodifiableList(this.globalDeclarations);
  }

  public ContentBlock getTemplateDeclarations() {
    return this.templateDeclarations;
  }

  public ViewTemplateSection getSection(final TemplateSection templateSection) {
    switch (templateSection) {
      case SUMMARY:
        return this.summary;
      case NAVIGATION:
        return this.navigation;
      case HEADER:
        return this.header;
      default:
        return this.body;
    }
  }

  public ViewTemplateSection getBody() {
    return this.body;
  }

  public ViewTemplateSection getSummary() {
    return this.summary;
  }

  public ViewTemplateSection getNavigation() {
    return this.navigation;
  }

  public ViewTemplateSection getHeader() {
    return this.header;
  }

  /**
   * Adds a line at the top of the header section that displays a part of the header.
   *
   * @return the block of the line
   */
  public ContentBlock addHeaderPart(final HeaderPart part, final Context context,
      final Variables variables, final List<ContentTemplate> contentTemplates) {
    final ContentBlock line = this.header.getRoot().addChild(0, context, variables, contentTemplates);
    this.headerParts.put(part, line);
    return line;
  }

  /**
   * @return whether the block is a line that displays a part of the header
   */
  public boolean displaysHeaderPart(final ContentBlock block) {
    return this.headerParts.containsValue(block);
  }

  /**
   * Renders the view template: its global declarations, its declared templates, then its sections.
   * The header is composed from the calls of the lines at the top of the header section: those that
   * display a part of the header, by part, and the others in their order.
   */
  public Markup render(final MarkupGenerator markupGenerator, final TranslatorContext translatorContext) {
    List<Markup> globals = new ArrayList<>();
    for (Identifier identifier : this.globalDeclarations) {
      if (identifier instanceof Variable) {
        Variable variable = (Variable) identifier;
        globals.add(markupGenerator.renderVariableDeclaration(variable.name, variable.initializationExpression));
      } else if (identifier instanceof Function) {
        Function function = (Function) identifier;
        globals.add(markupGenerator.renderFunctionDeclaration(function.name, function.parameters.toMap(), function.expression));
      } else if (identifier instanceof Dictionary) {
        Dictionary dictionary = (Dictionary) identifier;
        globals.add(markupGenerator.renderDictionaryDeclaration(dictionary.name, dictionary.pathExpression, dictionary.keyExpression));
      }
    }

    // Declared templates belong to no section, so they are rendered with the default one.
    List<Markup> definitions = new ArrayList<>();
    translatorContext.setCurrentSection(TemplateSection.DEFAULT);
    for (ContentBlock templateDeclaration : this.templateDeclarations.getChildren()) {
      definitions.addAll(templateDeclaration.renderDefinition(markupGenerator, translatorContext));
    }

    List<Markup> body = this.body.render(markupGenerator, translatorContext, definitions);
    List<Markup> summary = this.summary.render(markupGenerator, translatorContext, definitions);
    List<Markup> navigation = this.navigation.render(markupGenerator, translatorContext, definitions);
    // The call of each line at the top of the header is at the index of the line among them.
    List<Markup> headerCalls = this.header.render(markupGenerator, translatorContext, definitions);
    List<ContentBlock> headerLines = new ArrayList<>(this.header.getRoot().getChildren());
    Map<HeaderPart, Markup> parts = new EnumMap<>(HeaderPart.class);
    this.headerParts.forEach((part, line) -> parts.put(part, headerCalls.get(headerLines.indexOf(line))));
    List<Markup> lines = new ArrayList<>();
    for (int i = 0; i < headerLines.size(); i++) {
      if (!this.displaysHeaderPart(headerLines.get(i))) {
        lines.add(headerCalls.get(i));
      }
    }
    Markup header = markupGenerator.composeNoticeHeader(parts, lines, translatorContext);
    return markupGenerator.composeOutputFile(globals, body, summary, navigation, header, definitions);
  }
}
