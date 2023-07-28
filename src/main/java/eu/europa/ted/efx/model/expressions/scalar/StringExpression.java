package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Represents a string expression, value or literal in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.String.class)
public class StringExpression extends ScalarExpression.Impl<EfxDataType.String> {

  public StringExpression(final String script) {
    this(script, false);
  }

  public StringExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.String.class);
  }

  public StringExpression(final String script, final Boolean isLiteral, Class<? extends EfxDataType.String> type) {
    super(script, isLiteral, type);
  }

  public static StringExpression empty() {
    return new StringExpression("");
  }
}