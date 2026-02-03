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
package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * A time-typed sequence path AST node referencing an eForms Field that resolves to multiple time values.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.TimeSequence.class)
public class TimeSequencePath extends SequencePath.Impl<EfxDataType.TimeSequence> {

  public TimeSequencePath(final String script) {
    super(script, EfxDataType.TimeSequence.class);
  }

  protected TimeSequencePath(final String script, Class<? extends EfxDataType.TimeSequence> type) {
    super(script, type);
  }

  public static TimeSequencePath empty() {
    return new TimeSequencePath("");
  }
}
