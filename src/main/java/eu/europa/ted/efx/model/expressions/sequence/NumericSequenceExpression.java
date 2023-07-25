package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Used to represent a list of numbers in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Number.class)
public class NumericSequenceExpression extends SequenceExpression.Impl<EfxDataType.Number> {

  public NumericSequenceExpression(final String script) {
    super(script, EfxDataType.Number.class);
  }

  public NumericSequenceExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Number.class);
  }
}