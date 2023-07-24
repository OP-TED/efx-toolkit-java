package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.Duration.class)
public class DurationPathExpression extends PathExpression.Impl<EfxDataType.Duration> {

  public DurationPathExpression(final String script) {
    super(script, EfxDataType.Duration.class);
  }
}