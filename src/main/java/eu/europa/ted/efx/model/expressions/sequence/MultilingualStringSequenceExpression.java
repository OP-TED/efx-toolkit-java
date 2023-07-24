package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

@EfxDataTypeAssociation(dataType = EfxDataType.MultilingualString.class)
public class MultilingualStringSequenceExpression extends StringSequenceExpression {

  public MultilingualStringSequenceExpression(final String script) {
    super(script, false, EfxDataType.MultilingualString.class);
  }
}