package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * Represents a date expression or value in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Date.class)
public class DateExpression extends ScalarExpression.Impl<EfxDataType.Date> {

  public DateExpression(final String script) {
    super(script, EfxDataType.Date.class);
  }

  public DateExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Date.class);
  }

  public static DateExpression empty() {
    return new DateExpression("");
  }
}