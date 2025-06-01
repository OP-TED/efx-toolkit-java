package eu.europa.ted.efx.model.variables;

import java.util.Objects;

import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class StrictArguments extends ParsedArguments {

    public final transient Parametrised identifier;

    public StrictArguments(Parametrised identifier) {
        super();
        this.identifier = identifier;
    }

    public boolean addArgument(TypedExpression argument) {
        int position = this.size(); // Get the current position of the argument
        if (position >= identifier.parameters.size()) {
            throw InvalidArgumentException.argumentNumberMismatch(this.identifier, this.identifier.parameters.size());
        }

        var actualType = argument.getClass();
        var expectedType = this.identifier.parameters.get(position).getParameterType();
        if (!TypedExpression.canConvert(actualType, expectedType)) {
            throw InvalidArgumentException.argumentTypeMismatch(position, this.identifier, expectedType, actualType);
        }
        return this.add(new ParsedArgument(this.identifier.parameters.get(position), argument));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof StrictArguments)) {
            return false;
        }
        StrictArguments other = (StrictArguments) obj;
        return Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(this.identifier);
    }
}