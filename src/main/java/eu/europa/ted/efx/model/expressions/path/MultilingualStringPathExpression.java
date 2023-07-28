package eu.europa.ted.efx.model.expressions.path;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

@EfxDataTypeAssociation(dataType = EfxDataType.MultilingualString.class)
public class MultilingualStringPathExpression extends StringPathExpression {

  public MultilingualStringPathExpression(final String script) {
    super(script, EfxDataType.MultilingualString.class);
  }
}