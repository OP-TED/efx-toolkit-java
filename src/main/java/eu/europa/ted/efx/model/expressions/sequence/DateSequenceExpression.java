package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Used to represent a list of dates in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Date.class)
public class DateSequenceExpression extends SequenceExpression.Impl<EfxDataType.Date> {

  public DateSequenceExpression(final String script) {
    super(script, EfxDataType.Date.class);
  }

  public DateSequenceExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Date.class);
  }
}