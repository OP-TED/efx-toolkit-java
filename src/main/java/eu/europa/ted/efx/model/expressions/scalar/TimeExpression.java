package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Represents a time expression, value or literal in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Time.class)
public class TimeExpression extends ScalarExpression.Impl<EfxDataType.Time> {

  public TimeExpression(final String script) {
    super(script, EfxDataType.Time.class);
  }

  public TimeExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Time.class);
  }

  public static TimeExpression empty() {
    return new TimeExpression("");
  }
}