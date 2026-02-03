package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * An identifier that accepts parameters.
 *
 * Base class for parameterized identifiers like functions and templates. Extends {@link Identifier}
 * to add parsed parameter information.
 */
public class Parametrised extends Identifier {

    public final ParsedParameters parameters;

    public Parametrised(String name, Class<? extends EfxDataType> returnType, ParsedParameters parameters) {
        super(name, returnType);
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Parametrised other = (Parametrised) o;
        return parameters != null ? parameters.equals(other.parameters) : other.parameters == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}