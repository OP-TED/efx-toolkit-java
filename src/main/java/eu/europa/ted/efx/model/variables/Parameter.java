package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class Parameter extends Identifier {

  public final TypedExpression parameterValue;

  public Parameter(String parameterName, Expression declarationExpression, TypedExpression referenceExpression, TypedExpression parameterValue) {
    super(parameterName, declarationExpression, referenceExpression);
    this.parameterValue = parameterValue;
  }
}