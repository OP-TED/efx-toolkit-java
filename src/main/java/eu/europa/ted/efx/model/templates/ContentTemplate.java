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
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

/**
 * What a content block displays, as described by the displayTemplate and invokeTemplate rules of the
 * grammar. A block has one content template, or one for each of its WHEN and OTHERWISE alternatives.
 */
public abstract class ContentTemplate implements ParsedEntity {

  // The condition of a WHEN alternative. It is empty for the content displayed otherwise, and when
  // the block has no WHEN alternatives.
  private BooleanExpression condition = BooleanExpression.empty();

  public BooleanExpression getCondition() {
    return this.condition;
  }

  public void setCondition(final BooleanExpression condition) {
    this.condition = condition;
  }

  /**
   * Renders this content template as markup. The translator context tells the markup generator
   * which section of the view template is being rendered (see ContentTemplateFragment).
   */
  public abstract Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext);
}
