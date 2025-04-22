package eu.europa.ted.efx.model.variables;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class ArgumentList extends LinkedList<Argument> implements ParsedEntity {

    private static final String TYPE_MISMATCH = "Type mismatch. Expected %s instead of %s.";

    public final String identifier;
    public final ParameterList parameters;

    public ArgumentList(Function function) {
        this.identifier = function.name;
        this.parameters = function.parameters;
    }

    public boolean addArgument(TypedExpression argument) {
        int position = this.size(); // Get the current position of the argument
        if (position >= parameters.size()) {
            throw new IllegalArgumentException("Too many arguments for the function.");
        }

        var actualType = argument.getClass();
        var expectedType = parameters.get(position).getParameterType();
        if (!TypedExpression.canConvert(actualType, expectedType)) {
            throw new ParseCancellationException(
                    String.format(TYPE_MISMATCH, TypedExpression.getEfxDataType(expectedType),
                            TypedExpression.getEfxDataType(actualType)));
        }
        return this.add(new Argument(parameters.get(position), argument));
    }

    public List<TypedExpression> getArguments() {
        return this.stream().map(argument -> argument.value).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ArgumentList)) {
            return false;
        }
        ArgumentList other = (ArgumentList) obj;
        return Objects.equals(identifier, other.identifier) &&
               Objects.equals(parameters, other.parameters);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(identifier, parameters);
        return result;
    }
}