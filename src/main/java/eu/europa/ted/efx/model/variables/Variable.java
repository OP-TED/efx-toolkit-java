package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class Variable extends Identifier {
  public final Expression declarationExpression;
  public final TypedExpression initializationExpression;
  public final TypedExpression referenceExpression;

  public Variable(String variableName, Expression declarationExpression, TypedExpression initializationExpression, TypedExpression referenceExpression) {
    super(variableName, initializationExpression.getDataType());
    this.declarationExpression = declarationExpression;
    this.initializationExpression = initializationExpression;
    this.referenceExpression = referenceExpression;
    assert referenceExpression.getDataType() == initializationExpression.getDataType();
  }
}