package eu.europa.ted.efx.model.identifiers;

import java.util.Objects;

import eu.europa.ted.efx.model.constructs.Parameter;
import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class ParsedParameter extends Identifier implements Parameter {

  public final TypedExpression referenceExpression;

  public ParsedParameter(String parameterName, TypedExpression referenceExpression) {
    super(parameterName, referenceExpression.getDataType());
    this.referenceExpression = referenceExpression;
  }

  public ParsedParameter(Variable variable) {
    this(variable.name, variable.referenceExpression);
  }

  @Override
  public String getName() {
    return this.name;
  }

  /**
   * @param name the name of a parameter
   * @return how an error names a parameter of that name: "Parameter '$x'"
   */
  public static String describe(final String name) {
    return describeDollarName("Parameter", name);
  }

  @Override
  public String describe() {
    return describe(this.name);
  }

  /**
   * A parameter declared in EFX takes what converts to the class of the expression that reads it.
   */
  @Override
  public ParameterType<?> getType() {
    return ParameterType.of(this.referenceExpression.getClass());
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
    ParsedParameter other = (ParsedParameter) obj;
    return Objects.equals(referenceExpression, other.referenceExpression);
  }
}
