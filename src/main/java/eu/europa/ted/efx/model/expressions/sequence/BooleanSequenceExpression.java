/*
 * Copyright 2023 European Union
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
package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * A boolean-typed sequence AST node in the expression tree built during EFX translation.
 *
 * Wraps a target-language script fragment that evaluates to a sequence of boolean values.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.BooleanSequence.class)
public class BooleanSequenceExpression extends SequenceExpression.Impl<EfxDataType.BooleanSequence> {

  public BooleanSequenceExpression(final String script) {
    super(script, EfxDataType.BooleanSequence.class);
  }

  protected BooleanSequenceExpression(final String script, Class<? extends EfxDataType.BooleanSequence> type) {
    super(script, type);
  }

  public static BooleanSequenceExpression empty() {
    return new BooleanSequenceExpression("");
  }
}