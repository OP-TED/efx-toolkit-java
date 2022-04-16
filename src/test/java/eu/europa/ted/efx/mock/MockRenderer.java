package eu.europa.ted.efx.mock;

import java.util.List;
import eu.europa.ted.efx.interfaces.Renderer;

public class MockRenderer implements Renderer {

    @Override
    public String renderValueReference(String valueReference) {
        return String.format("eval(%s)", valueReference);
    }

    @Override
    public String renderLabelFromKey(String key) {
        return String.format("label(%s)", key);
    }

    @Override
    public String renderLabelFromExpression(String expression) {
        return String.format("label(%s)", expression);
    }

    @Override
    public String renderFreeText(String freeText) {
        return String.format("text('%s')", freeText);
    }

    @Override
    public String renderTemplate(String name, String number, String content) {
        return String.format("%s = %s;", name,  content);
    }

    @Override
    public String renderCallTemplate(String name, String context) {
        return String.format("for-each(%s) { %s(); }", context, name);
    }

    @Override
    public String renderFile(List<String> body, List<String> templates) {
        return String.format("%s %s", String.join("\n", templates), String.join("\n", body));
    }
}
