package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.String.class)
public class StringPathExpression extends PathExpression.Impl<EfxDataType.String> {

  public StringPathExpression(final String script) {
    super(script, EfxDataType.String.class);
  }

  protected StringPathExpression(final String script, Class<? extends EfxDataType.String> type) {
    super(script, type);
  }
}