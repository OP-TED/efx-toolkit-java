package eu.europa.ted.efx.model.variables;

import java.util.Objects;

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
    int result = super.hashCode();
    result = prime * result + Objects.hash(referenceExpression);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Parameter other = (Parameter) obj;
    return Objects.equals(referenceExpression, other.referenceExpression);
  }
}
