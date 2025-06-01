package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.DeclarationExpression;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class Variable extends Identifier {
  public final Expression declarationExpression;
  public final TypedExpression initializationExpression;
  public final TypedExpression referenceExpression;

  public Variable(String variableName, TypedExpression initializationExpression, TypedExpression referenceExpression) {
    this(variableName, DeclarationExpression.empty(), initializationExpression, referenceExpression);  
  }

  public Variable(String variableName, Expression declarationExpression, TypedExpression initializationExpression, TypedExpression referenceExpression) {
    super(variableName, initializationExpression.getDataType());
    this.declarationExpression = declarationExpression;
    this.initializationExpression = initializationExpression;
    this.referenceExpression = referenceExpression;
    assert referenceExpression.getDataType() == initializationExpression.getDataType();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Variable variable = (Variable) o;
    return java.util.Objects.equals(declarationExpression, variable.declarationExpression) &&
           java.util.Objects.equals(initializationExpression, variable.initializationExpression) &&
           java.util.Objects.equals(referenceExpression, variable.referenceExpression);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + java.util.Objects.hashCode(declarationExpression);
    result = 31 * result + java.util.Objects.hashCode(initializationExpression);
    result = 31 * result + java.util.Objects.hashCode(referenceExpression);
    return result;
  }
}