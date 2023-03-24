package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.TimeExpression;

public class TimeVariable extends Variable<TimeExpression> {

    public TimeVariable(String variableName, TimeExpression initializationExpression, TimeExpression referenceExpression) {
        super(variableName, initializationExpression, referenceExpression);
    }
}