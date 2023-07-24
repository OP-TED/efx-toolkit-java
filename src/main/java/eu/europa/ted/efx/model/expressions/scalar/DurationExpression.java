package eu.europa.ted.efx.model.expressions.scalar;

import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Represents a duration expression, value or literal in the target language.
 */
@EfxDataTypeAssociation(dataType = EfxDataType.Duration.class)
public class DurationExpression extends ScalarExpression.Impl<EfxDataType.Duration> {

  public DurationExpression(final String script) {
    super(script, EfxDataType.Duration.class);
  }

  public DurationExpression(final String script, final Boolean isLiteral) {
    super(script, isLiteral, EfxDataType.Duration.class);
  }

  public static DurationExpression empty() {
    return new DurationExpression("");
  }
}