package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

@EfxDataTypeAssociation(dataType = EfxDataType.MultilingualString.class)
public class MultilingualStringExpression extends StringExpression {

  public MultilingualStringExpression(final String script) {
    super(script, false, EfxDataType.MultilingualString.class);
  }

  public MultilingualStringExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.MultilingualString.class);
  }
}