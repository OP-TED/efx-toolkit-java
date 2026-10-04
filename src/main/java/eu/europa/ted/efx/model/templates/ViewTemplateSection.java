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
import java.util.List;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.TemplateSection;
import eu.europa.ted.efx.interfaces.TranslatorContext;

/**
 * A section of a view template: the body, the summary or the navigation section.
 */
public class ViewTemplateSection {

  private final TemplateSection templateSection;

  // The root block holds the top-level template lines of the section as its children. It displays
  // nothing itself, but its name prefixes the names of the blocks under it: body01, body0102...
  private final ContentBlock root;

  /**
   * @param templateSection Which section this is.
   * @param name The name of the section's root block: body, summary or nav.
   */
  public ViewTemplateSection(final TemplateSection templateSection, final String name) {
    this.templateSection = templateSection;
    this.root = ContentBlock.newRootBlock(name);
  }

  public TemplateSection getTemplateSection() {
    return this.templateSection;
  }

  public ContentBlock getRoot() {
    return this.root;
  }

  /**
   * Renders the section: returns the calls to its top-level blocks, and adds the definitions of all
   * its blocks to the given list, which the view template shares between its sections.
   */
  public List<Markup> render(final MarkupGenerator markupGenerator, final TranslatorContext translatorContext,
      final List<Markup> definitions) {
    List<Markup> calls = new ArrayList<>();
    translatorContext.setCurrentSection(this.templateSection);
    for (ContentBlock block : this.root.getChildren()) {
      calls.add(block.renderInvocation(markupGenerator, translatorContext));
      definitions.addAll(block.renderDefinition(markupGenerator, translatorContext));
    }
    return calls;
  }
}
