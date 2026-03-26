package eu.europa.ted.efx.model.variables;

import java.util.Objects;

import eu.europa.ted.efx.model.expressions.TypedExpression;

public class ParsedArgument extends ParsedParameter {

    public final TypedExpression value;

    public ParsedArgument(ParsedParameter parameter, TypedExpression value) {
        super(parameter.name, parameter.referenceExpression);
        this.value = value;
    }

    public ParsedArgument(Variable variable) {
        super(variable);
        this.value = variable.initializationExpression;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(value);
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
        ParsedArgument other = (ParsedArgument) obj;
        return Objects.equals(value, other.value);
    }
}
