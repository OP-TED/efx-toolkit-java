package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class Variable extends Identifier {
  final public TypedExpression initializationExpression;

  public Variable(String variableName, Expression declarationExpression, TypedExpression initializationExpression, TypedExpression referenceExpression) {
    super(variableName, declarationExpression, referenceExpression);
    this.initializationExpression = initializationExpression;
    assert referenceExpression.getDataType() == initializationExpression.getDataType();
  }

  public Variable(Identifier identifier, TypedExpression initializationExpression) {
    super(identifier.name, identifier.declarationExpression, identifier.referenceExpression);
    this.initializationExpression = initializationExpression;
    assert identifier.dataType == initializationExpression.getDataType();
  }
}