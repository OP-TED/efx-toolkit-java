package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.TypedExpression;

public class Parameter extends Identifier {

  public final TypedExpression referenceExpression;

  public Parameter(String parameterName, TypedExpression referenceExpression) {
    super(parameterName, referenceExpression.getDataType());
    this.referenceExpression = referenceExpression;
  }

  public Class<? extends TypedExpression> getParameterType() {
    return referenceExpression.getClass();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((referenceExpression == null) ? 0 : referenceExpression.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Parameter other = (Parameter) obj;
    if (referenceExpression == null) {
      if (other.referenceExpression != null)
        return false;
    } else if (!referenceExpression.equals(other.referenceExpression))
      return false;
    return true;
  }
}
