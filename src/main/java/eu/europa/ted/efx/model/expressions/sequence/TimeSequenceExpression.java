package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;
/**
 * Used to represent a list of times in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Time.class)
public class TimeSequenceExpression extends SequenceExpression.Impl<EfxDataType.Time> {

  public TimeSequenceExpression(final String script) {
    super(script, EfxDataType.Time.class);
  }

  public TimeSequenceExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Time.class);
  }
}