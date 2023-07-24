package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.Date.class)
public class DatePathExpression extends PathExpression.Impl<EfxDataType.Date> {

  public DatePathExpression(final String script) {
    super(script, EfxDataType.Date.class);
  }
}