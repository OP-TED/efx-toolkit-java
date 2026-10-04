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

import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;

/**
 * A label in a template line, as described by the labelBlock rule of the grammar. The quantity
 * selects the plural form of the label; it is empty when the label is not pluralised.
 */
public abstract class LabelContentTemplateFragment extends ContentTemplateFragment {

  protected final NumericExpression quantity;

  protected LabelContentTemplateFragment(NumericExpression quantity) {
    this.quantity = quantity;
  }
}
