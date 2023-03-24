package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.StringExpression;

public class StringVariable extends Variable<StringExpression> {

    public StringVariable(String variableName, StringExpression initializationExpression, StringExpression referenceExpression) {
        super(variableName, initializationExpression, referenceExpression);
    }
}
