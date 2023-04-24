package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.NumericExpression;

public class NumericVariable extends Variable<NumericExpression> {

  public NumericVariable(String variableName, NumericExpression initializationExpression,
      NumericExpression referenceExpression) {
    super(variableName, initializationExpression, referenceExpression);
  }
}
