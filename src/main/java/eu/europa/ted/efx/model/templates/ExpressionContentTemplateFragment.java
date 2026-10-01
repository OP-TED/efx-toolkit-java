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
import eu.europa.ted.efx.model.expressions.Expression;

/**
 * The value of an expression in a template line, as described by the expressionBlock rule of
 * the grammar.
 */
public class ExpressionContentTemplateFragment extends ContentTemplateFragment {

  private final Expression expression;

  public ExpressionContentTemplateFragment(Expression expression) {
    this.expression = expression;
  }

  @Override
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    return markupGenerator.renderVariableExpression(this.expression, translatorContext);
  }
}
