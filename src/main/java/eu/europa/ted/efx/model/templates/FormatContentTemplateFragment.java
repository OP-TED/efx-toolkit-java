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
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;

/**
 * A value displayed by an expression block, as described by the formattedExpressionBlock rule of the
 * grammar: the formatted value, followed by its unit when it has one.
 */
public class FormatContentTemplateFragment extends ContentTemplateFragment {

  private final Expression value;

  // The space that separates the unit from the value, the label key of the unit, and the quantity that
  // selects its plural form. They are empty when the value has no unit, or its unit is hidden. An absent
  // value has neither a space nor a unit.
  private final StringSequenceExpression unitSeparator;
  private final StringSequenceExpression unitKey;
  private final NumericExpression quantity;

  public FormatContentTemplateFragment(final Expression value) {
    this(value, StringSequenceExpression.empty(), StringSequenceExpression.empty(), NumericExpression.empty(), true);
  }

  public FormatContentTemplateFragment(final Expression value, final StringSequenceExpression unitSeparator,
      final StringSequenceExpression unitKey, final NumericExpression quantity, final boolean hideUnit) {
    this.value = value;
    this.unitSeparator = hideUnit ? StringSequenceExpression.empty() : unitSeparator;
    this.unitKey = hideUnit ? StringSequenceExpression.empty() : unitKey;
    this.quantity = hideUnit ? NumericExpression.empty() : quantity;
  }

  @Override
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    Markup markup = markupGenerator.renderVariableExpression(this.value, translatorContext);
    if (this.unitKey.isEmpty()) {
      return markup;
    }
    return markup
        .join(markupGenerator.renderVariableExpression(this.unitSeparator, translatorContext))
        .join(markupGenerator.renderLabelFromExpression(this.unitKey, this.quantity, translatorContext));
  }
}
