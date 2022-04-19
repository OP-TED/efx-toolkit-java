package eu.europa.ted.efx.mock;

import java.util.List;
import java.util.stream.Collectors;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Markup;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;

public class MarkupGeneratorMock implements MarkupGenerator {

    @Override
    public Markup renderValueReference(Expression valueReference) {
        return new Markup(String.format("eval(%s)", valueReference.script));
    }

    @Override
    public Markup renderLabelFromKey(StringExpression key) {
        return new Markup(String.format("label(%s)", key.script));
    }

    @Override
    public Markup renderLabelFromExpression(Expression expression) {
        return new Markup(String.format("label(%s)", expression.script));
    }

    @Override
    public Markup renderFreeText(String freeText) {
        return new Markup(String.format("text('%s')", freeText));
    }

    @Override
    public Markup renderTemplate(String name, String number, Markup content) {
        return new Markup(String.format("%s = %s;", name,  content.script));
    }

    @Override
    public Markup renderCallTemplate(String name, PathExpression context) {
        return new Markup(String.format("for-each(%s) { %s(); }", context.script, name));
    }

    @Override
    public Markup renderFile(List<Markup> body, List<Markup> templates) {
        return new Markup(String.format("%s %s", templates.stream().map(t -> t.script).collect(Collectors.joining("\n")), body.stream().map(t -> t.script).collect(Collectors.joining("\n"))));
    }
}
