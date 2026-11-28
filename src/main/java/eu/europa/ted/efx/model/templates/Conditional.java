package eu.europa.ted.efx.model.templates;

import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

/**
 * A WHEN alternative as the markup generator receives it: a condition, and the markup displayed when
 * the condition is true.
 */
public class Conditional {
    private final BooleanExpression condition;
    private final Markup caption;
    private final Markup markup;

    public Conditional(BooleanExpression condition, Markup markup) {
        this(condition, Markup.empty(), markup);
    }

    /**
     * @param caption the caption of the alternative: what its template displays before the colon
     *                that separates it from the rest; empty when its template has no caption
     * @param markup  what the alternative displays: after that colon, when it has a caption
     */
    public Conditional(BooleanExpression condition, Markup caption, Markup markup) {
        this.condition = condition;
        this.caption = caption;
        this.markup = markup;
    }

    /**
     * @return the caption of the alternative, or empty markup when its template has no caption
     */
    public Markup getCaption() {
        return this.caption;
    }


    public BooleanExpression getCondition() {
        return this.condition;
    }

    public Markup getMarkup() {
        return this.markup;
    }
}
