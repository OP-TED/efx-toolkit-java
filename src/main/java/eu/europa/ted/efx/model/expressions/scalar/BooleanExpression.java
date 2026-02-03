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
package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * A boolean-typed scalar AST node in the expression tree built during EFX translation.
 *
 * Wraps a target-language script fragment that evaluates to a boolean value.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.BooleanScalar.class)
public class BooleanExpression extends ScalarExpression.Impl<EfxDataType.BooleanScalar> {

  public BooleanExpression(final String script) {
    super(script, EfxDataType.BooleanScalar.class);
  }

  protected BooleanExpression(final String script, Class<? extends EfxDataType.BooleanScalar> type) {
    super(script, type);
  }

  public static BooleanExpression empty() {
    return new BooleanExpression("");
  }
}