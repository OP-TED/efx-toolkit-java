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

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.ParsedEntity;

/**
 * A piece of a content template, as described by the templateFragment rule of the grammar: a text,
 * a label, a value, a line break or a hyperlink. Each fragment holds what it needs to render itself.
 */
public abstract class ContentTemplateFragment implements ParsedEntity {

  /**
   * Renders this fragment as markup. The translator context tells the markup generator which
   * section of the view template is being rendered, as some markup differs between sections: free
   * text, for example, is rendered differently in the navigation section.
   */
  public abstract Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext);
}
