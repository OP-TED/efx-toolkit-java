package eu.europa.ted.efx.model.expressions.sequence;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Used to represent a list of strings in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.String.class)
public class StringSequenceExpression extends SequenceExpression.Impl<EfxDataType.String> {

  public StringSequenceExpression(final String script) {
    this(script, false);
  }

  public StringSequenceExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.String.class);
  }

  protected StringSequenceExpression(final String script, final Boolean isLiteral, Class<? extends EfxDataType.String> type) {
    super(script, isLiteral, type);
  }
}