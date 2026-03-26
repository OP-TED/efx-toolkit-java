package eu.europa.ted.efx.autocomplete;

/**
 * A parameter in an EFX function signature.
 */
public class EfxFunctionParameter {
    private final String name;
    private final EfxDataType dataType;
    private final boolean optional;
    private final boolean varargs;

    public EfxFunctionParameter(String name, EfxDataType dataType) {
        this(name, dataType, false, false);
    }

    public EfxFunctionParameter(String name, EfxDataType dataType, boolean optional, boolean varargs) {
        this.name = name;
        this.dataType = dataType;
        this.optional = optional;
        this.varargs = varargs;
    }

    public String getName() { return name; }
    public EfxDataType getDataType() { return dataType; }
    public boolean isOptional() { return optional; }
    public boolean isVarargs() { return varargs; }
}
