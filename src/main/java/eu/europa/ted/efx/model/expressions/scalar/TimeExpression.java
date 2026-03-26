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

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * A time-typed scalar AST node in the expression tree built during EFX translation.
 *
 * Wraps a target-language script fragment that evaluates to a time value.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.TimeScalar.class)
public class TimeExpression extends ScalarExpression.Impl<EfxDataType.TimeScalar> {

  public TimeExpression(final String script) {
    super(script, EfxDataType.TimeScalar.class);
  }

  protected TimeExpression(final String script, Class<? extends EfxDataType.TimeScalar> type) {
    super(script, type);
  }

  public static TimeExpression empty() {
    return new TimeExpression("");
  }
}