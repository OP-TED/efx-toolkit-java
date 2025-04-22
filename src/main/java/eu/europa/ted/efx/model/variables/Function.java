package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

public class Function extends Identifier {

    public ParameterList parameters;
    public TypedExpression expression;

    public Function(String name, Class<? extends EfxDataType> returnType, ParameterList parameters,
            TypedExpression expression) {
        super(name, returnType);
        this.parameters = parameters;
        this.expression = expression;
    }
}
