package eu.europa.ted.efx.model.identifiers;


import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.constructs.Arguments;
import eu.europa.ted.efx.model.constructs.Parameters;
import eu.europa.ted.efx.model.types.EfxDataType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An ordered collection of parsed parameters for a function or template.
 *
 * Preserves parameter order (important for positional arguments) and provides conversions
 * to Set and Map representations.
 *
 * It carries what the generic {@link Parameters} cannot carry at run time: a class of its own, by
 * which the call stack and the declarations find it, and the {@link ParsedEntity} role.
 */
public class ParsedParameters extends Parameters<ParsedParameter> implements ParsedEntity {

    public ParsedParameters() {
        super();
    }

    public ParsedParameters(Variables variables) {
        for (Variable variable : variables) {
            add(new ParsedParameter(variable.name, variable.referenceExpression));
        }
    }

    /**
     * Converts the parameter list to a set of parameters.
     * Uses a LinkedHashSet to maintain parameter order.
     * 
     * @return a Set of Parameter objects
     */
    public Set<ParsedParameter> toSet() {
        return this.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }  



    /**
     * Gives each parameter as the argument a template passes down to its blocks: the value of each
     * is the parameter's own reference.
     *
     * @return the parameters, each with its reference expression
     */
    public Arguments<ParsedParameter> toArguments() {
        return new Arguments<>(this,
                this.stream().map(parameter -> parameter.referenceExpression)
                        .collect(Collectors.toList()));
    }

    public Map<String, Class<? extends EfxDataType>> toMap() {
        var map = new LinkedHashMap<String, Class<? extends EfxDataType>>();
        for (ParsedParameter parameter : this) {
            map.put(parameter.name, parameter.dataType);
        }
        return map;
    }
}