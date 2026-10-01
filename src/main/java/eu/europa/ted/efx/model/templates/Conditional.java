package eu.europa.ted.efx.model.templates;

import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

/**
 * A WHEN alternative as the markup generator receives it: a condition, and the markup displayed when
 * the condition is true.
 */
public class Conditional {
    private final BooleanExpression condition;
    private final Markup markup;

    public Conditional(BooleanExpression condition, Markup markup) {
        this.condition = condition;
        this.markup = markup;
    }


    public BooleanExpression getCondition() {
        return this.condition;
    }

    public Markup getMarkup() {
        return this.markup;
    }
}
