package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

@EfxDataTypeAssociation(dataType = EfxDataType.Boolean.class)
public class BooleanPathExpression extends PathExpression.Impl<EfxDataType.Boolean> {

  public BooleanPathExpression(final String script) {
    super(script, EfxDataType.Boolean.class);
  }
}