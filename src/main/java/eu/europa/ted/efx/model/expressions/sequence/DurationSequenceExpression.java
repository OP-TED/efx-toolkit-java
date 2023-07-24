package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Used to represent a list of durations in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Duration.class)
public class DurationSequenceExpression extends SequenceExpression.Impl<EfxDataType.Duration> {

  public DurationSequenceExpression(final String script) {
    super(script, EfxDataType.Duration.class);
  }

  public DurationSequenceExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Duration.class);
  }
}