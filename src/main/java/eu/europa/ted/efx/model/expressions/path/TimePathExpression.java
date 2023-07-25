package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.Time.class)
public class TimePathExpression extends PathExpression.Impl<EfxDataType.Time> {

  public TimePathExpression(final String script) {
    super(script, EfxDataType.Time.class);
  }
}