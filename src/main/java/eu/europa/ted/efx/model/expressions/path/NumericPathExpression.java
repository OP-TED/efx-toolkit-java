package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.Number.class)
public class NumericPathExpression extends PathExpression.Impl<EfxDataType.Number> {

  public NumericPathExpression(final String script) {
    super(script, EfxDataType.Number.class);
  }
}