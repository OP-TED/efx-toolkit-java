package eu.europa.ted.efx.interfaces;

public interface Renderer {
    
    /**
     * Given a body (main content) and a set of templates (all in one prerendered string), this
     * method should return the full content of the target template.
     */
    String renderFile(final String body, final String templates);

    /**
     * Given a reference (which will eventually, at runtime, evaluate to the value of a field), this
     * method should return the template code that encapsulates this reference in the target
     * template.
     */
    String renderValueReference(final String valueReference);

    /**
     * Given a label key (which will eventually, at runtime, be evaluated to a label text), this method
     * should return the template code that renders this label in the target template.
     */
    String renderLabelFromKey(final String key);

    /**
     * Given an expression (which will eventually, at runtime, be evaluated to a label key and subsequently to a label text), this method
     * should return the template code that renders this label in the target template.
     */    
    String renderLabelFromExpression(final String expression);

    /**
     * Given a string of free text, this method should return the template code that encapsulates
     * this text in the target template.
     */
    String renderFreeText(final String freeText);

    /**
     * Given a template name (identifier) and some pre-rendered content, this method should return
     * the template code that encapsulates it in the target template.
     */
    String renderTemplate(final String name, String number, String content);

    /**
     * Given a template name (identifier), and an evaluation context, this method should return the
     * code that invokes (calls) the tempalte in the target template.
     */
    String renderCallTemplate(final String name, final String context);
}
