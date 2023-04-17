package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.DurationExpression;

public class DurationVariable extends Variable<DurationExpression> {

  public DurationVariable(String variableName, DurationExpression initializationExpression,
      DurationExpression referenceExpression) {
    super(variableName, initializationExpression, referenceExpression);
  }

}
