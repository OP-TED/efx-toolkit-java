package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.DateExpression;

public class DateVariable extends Variable<DateExpression> {

    public DateVariable(String variableName, DateExpression initializationExpression, DateExpression referenceExpression) {
        super(variableName, initializationExpression, referenceExpression);
    }

}