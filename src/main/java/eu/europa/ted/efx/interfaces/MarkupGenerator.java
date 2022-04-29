package eu.europa.ted.efx.interfaces;

import java.util.List;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Markup;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;

public interface MarkupGenerator {
    
    /**
     * Given a body (main content) and a set of templates (all in one pre-rendered string), this
     * method should return the full content of the target template.
     */
    Markup renderFile(final List<Markup> instructions, final List<Markup> templates);

    /**
     * Given a reference (which will eventually, at runtime, evaluate to the value of a field), this
     * method should return the template code that encapsulates this reference in the target
     * template.
     */
    Markup renderValueReference(final Expression valueReference);

    /**
     * Given a label key (which will eventually, at runtime, be evaluated to a label text), this method
     * should return the template code that renders this label in the target template.
     */
    Markup renderLabelFromKey(final StringExpression key);

    /**
     * Given an expression (which will eventually, at runtime, be evaluated to a label key and subsequently to a label text), this method
     * should return the template code that renders this label in the target template.
     */    
    Markup renderLabelFromExpression(final Expression expression);


    // Markup renderLabel(final StringExpression assetType, final StringExpression labelType, final StringExpression assetId);

    /**
     * Given a string of free text, this method should return the template code that encapsulates
     * this text in the target template.
     */
    Markup renderFreeText(final String freeText);

    /**
     * Given a template name (identifier) and some pre-rendered content, this method should return
     * the template code that encapsulates it in the target template.
     */
    Markup renderTemplate(final String name, String number, Markup content);

    /**
     * Given a template name (identifier), and an evaluation context, this method should return the
     * code that invokes (calls) the template in the target template.
     */
    Markup renderCallTemplate(final String name, final PathExpression context);
}
