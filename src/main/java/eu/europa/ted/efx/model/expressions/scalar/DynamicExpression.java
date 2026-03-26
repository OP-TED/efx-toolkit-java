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
package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * A dynamic-typed scalar AST node, extending {@link BooleanExpression}.
 *
 * Represents the result of a dynamic function call (tri-state: 1 = true, 0 = false, -1 = error).
 * Can be used wherever a boolean expression is expected, similar to how
 * {@link MultilingualStringExpression} can be used wherever a string is expected.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.DynamicScalar.class)
public class DynamicExpression extends BooleanExpression {

  public DynamicExpression(final String script) {
    super(script, EfxDataType.DynamicScalar.class);
  }
}
