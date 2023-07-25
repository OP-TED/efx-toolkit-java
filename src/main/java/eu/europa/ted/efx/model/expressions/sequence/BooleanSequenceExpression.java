package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * Used to represent a list of booleans in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Boolean.class)
public class BooleanSequenceExpression extends SequenceExpression.Impl<EfxDataType.Boolean> {

  public BooleanSequenceExpression(final String script) {
    super(script, EfxDataType.Boolean.class);
  }

  public BooleanSequenceExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral,EfxDataType.Boolean.class);
  }
}