package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.CallStackObject;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

public class Identifier implements CallStackObject {
  final public String name;
  final public Class<? extends EfxDataType> dataType;
  final public Expression declarationExpression;
  final public TypedExpression referenceExpression;

  public Identifier(String name, Expression declarationExpression, TypedExpression referenceExpression) {
    this.name = name;
    this.dataType = referenceExpression.getDataType();
    this.declarationExpression = declarationExpression;
    this.referenceExpression = referenceExpression;
  }
}