package eu.europa.ted.efx.model.variables;

import java.util.LinkedList;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.types.EfxDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParameterList extends LinkedList<Parameter> implements ParsedEntity {

    public Map<String, Class<? extends EfxDataType>> toMap() {
        var map = new LinkedHashMap<String, Class<? extends EfxDataType>>();
        for (Parameter parameter : this) {
            map.put(parameter.name, parameter.dataType);
        }
        return map;
    }
}