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

import java.util.ArrayDeque;
import java.util.Deque;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.TranslatorContext;

/**
 * A content template displayed in place, made of fragments, as described by the displayTemplate rule
 * of the grammar.
 */
public class DisplayContentTemplate extends ContentTemplate {

  // The fragments in display order. The grammar nests the fragments of a template from the last to
  // the first, so the translator builds the template from its last fragment, putting each one in
  // front of those already there.
  private final Deque<ContentTemplateFragment> fragments = new ArrayDeque<>();

  /**
   * Puts a fragment in front of those already in the template.
   */
  public void prepend(final ContentTemplateFragment fragment) {
    this.fragments.addFirst(fragment);
  }

  @Override
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    StringBuilder markup = new StringBuilder();
    for (ContentTemplateFragment fragment : this.fragments) {
      markup.append(fragment.render(markupGenerator, translatorContext).script);
    }
    return new Markup(markup.toString());
  }
}
