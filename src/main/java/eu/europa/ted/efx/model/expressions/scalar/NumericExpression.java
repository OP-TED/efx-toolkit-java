package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Represents a numeric expression, value or literal in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Number.class)
public class NumericExpression extends ScalarExpression.Impl<EfxDataType.Number> {

  public NumericExpression(final String script) {
    super(script, EfxDataType.Number.class);
  }

  public NumericExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Number.class);
  }

  public static NumericExpression empty() {
    return new NumericExpression("");
  }
}