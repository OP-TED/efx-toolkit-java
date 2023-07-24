package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * Represents a boolean expression, value or literal in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Boolean.class)
public class BooleanExpression extends ScalarExpression.Impl<EfxDataType.Boolean> {

  public BooleanExpression(final String script) {
    super(script, EfxDataType.Boolean.class);
  }

  public BooleanExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Boolean.class);
  }

  public static BooleanExpression empty() {
    return new BooleanExpression("");
  }
}