package eu.europa.ted.efx.model.identifiers;

import java.util.LinkedList;
import java.util.stream.Collectors;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.constructs.Arguments;

public class Variables extends LinkedList<Variable> implements ParsedEntity {

    /**
     * Gives each variable as the argument a template block passes down to the blocks it holds:
     * the parameter it is read by, and the value it is initialized with.
     *
     * @return the variables as arguments, in order
     */
    public Arguments<ParsedParameter> toArguments() {
        return new Arguments<>(
                this.stream().map(ParsedParameter::new).collect(Collectors.toList()),
                this.stream().map(variable -> variable.initializationExpression)
                        .collect(Collectors.toList()));
    }
}
