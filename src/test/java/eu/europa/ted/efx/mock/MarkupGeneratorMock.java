package eu.europa.ted.efx.mock;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Markup;

public class MarkupGeneratorMock implements MarkupGenerator {

  @Override
  public Markup renderVariableExpression(Expression valueReference) {
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
  public Markup composeFragmentDefinition(String name, String number, Markup content) {
    return this.composeFragmentDefinition(name, number, content, new LinkedHashSet<>());
  }

  public Markup composeFragmentDefinition(String name, String number, Markup content, Set<String> parameters) {
    if (StringUtils.isBlank(number)) {
      return new Markup(String.format("let %s(%s) -> { %s }", name,
          parameters.stream().collect(Collectors.joining(", ")), content.script));
    }
    return new Markup(String.format("let %s(%s) -> { #%s: %s }", name,
        parameters.stream().collect(Collectors.joining(", ")), number, content.script));
  }

  @Override
  public Markup renderFragmentInvocation(String name, PathExpression context) {
    return this.renderFragmentInvocation(name, context, new LinkedHashSet<>());
  }

  public Markup renderFragmentInvocation(String name, PathExpression context,
      Set<Pair<String, String>> variables) {
    return new Markup(String.format("for-each(%s).call(%s(%s))", context.script, name, variables.stream()
        .map(v -> String.format("%s:%s", v.getLeft(), v.getRight())).collect(Collectors.joining(", "))));
  }

  @Override
  public Markup composeOutputFile(List<Markup> body, List<Markup> templates) {
    return new Markup(String.format("%s\n%s",
        templates.stream().map(t -> t.script).collect(Collectors.joining("\n")),
        body.stream().map(t -> t.script).collect(Collectors.joining("\n"))));
  }
}
