package eu.europa.ted.efx.model.variables;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class ParsedArguments extends LinkedList<ParsedArgument> implements ParsedEntity {

    protected ParsedArguments() {
        super();      
    }

    public ParsedArguments(Variables variables) {
        super(variables.stream()
                .map(ParsedArgument::new)
                .collect(Collectors.toList()));
    }

    public ParsedArguments(ParsedParameters parameters) {
        super(parameters.stream()
                .map(p -> new ParsedArgument(p, p.referenceExpression))
                .collect(Collectors.toList()));
    }

    public List<TypedExpression> getArgumentValues() {
        return this.stream().map(argument -> argument.value).collect(Collectors.toList());
    }

    /**
     * Converts the argument list to a set of arguments.
     * Uses a LinkedHashSet to maintain argument order.
     * 
     * @return a Set of Argument objects
     */
    public Set<ParsedArgument> toSet() {
        return this.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ParsedArguments other = (ParsedArguments) obj;
        return Objects.equals(this.getArgumentValues(), other.getArgumentValues());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(this.getArgumentValues());
    }
}