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
 * A sequence path AST node referencing multiple eForms Nodes in a structured document.
 *
 * Unlike eForms Fields, eForms Nodes have no value and are used to set evaluation context.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.NodeSequence.class)
public class NodeSequencePath extends SequencePath.Impl<EfxDataType.NodeSequence> {

  public NodeSequencePath(final String script) {
    super(script, EfxDataType.NodeSequence.class);
  }

  protected NodeSequencePath(final String script, Class<? extends EfxDataType.NodeSequence> type) {
    super(script, type);
  }

  public static NodeSequencePath empty() {
    return new NodeSequencePath("");
  }
}
