package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.Node.class)
public class NodePathExpression extends PathExpression.Impl<EfxDataType.Node> {

  public NodePathExpression(final String script) {
    super(script, EfxDataType.Node.class);
  }
}