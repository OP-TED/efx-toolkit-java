package eu.europa.ted.efx.model.variables;

import java.util.LinkedList;

import eu.europa.ted.efx.model.ParsedEntity;
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
 */
public class ParsedParameters extends LinkedList<ParsedParameter> implements ParsedEntity {

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



    public Map<String, Class<? extends EfxDataType>> toMap() {
        var map = new LinkedHashMap<String, Class<? extends EfxDataType>>();
        for (ParsedParameter parameter : this) {
            map.put(parameter.name, parameter.dataType);
        }
        return map;
    }
}