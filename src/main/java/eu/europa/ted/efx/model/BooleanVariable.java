package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.BooleanExpression;

public class BooleanVariable extends Variable<BooleanExpression> {

    public BooleanVariable(String variableName, BooleanExpression initializationExpression, BooleanExpression referenceExpression) {
        super(variableName, initializationExpression, referenceExpression);
    }
}