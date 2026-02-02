package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.types.EfxDataType;

public class Template extends Parametrised {

    public Template(String name, ParsedParameters parameters) {
        super(name, EfxDataType.VOID, parameters);
    }
}