package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

public class Function extends Parametrised {

    public final TypedExpression expression;

    public Function(String name, Class<? extends EfxDataType> returnType, ParsedParameters parameters,
            TypedExpression expression) {
        super(name, returnType, parameters);
        this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Function function = (Function) o;
        if (parameters != null ? !parameters.equals(function.parameters) : function.parameters != null)
            return false;
        return expression != null ? expression.equals(function.expression) : function.expression == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        return result;
    }
}