package eu.europa.ted.efx.model.templates;

import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

public class Conditional {
    private final BooleanExpression condition;
    private final Markup markup;

    public Conditional(BooleanExpression condition, Markup markup) {
        this.condition = condition;
        this.markup = markup;
    }


    public BooleanExpression getCondition() {
        return condition;
    }

    public Markup getMarkup() {
        return this.markup;
    }
}