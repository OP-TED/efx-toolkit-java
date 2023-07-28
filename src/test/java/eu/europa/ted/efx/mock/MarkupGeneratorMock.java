package eu.europa.ted.efx.mock;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.templates.Markup;

public class MarkupGeneratorMock implements MarkupGenerator {

  @Override
  public Markup renderVariableExpression(Expression valueReference) {
    return new Markup(String.format("eval(%s)", valueReference.getScript()));
  }

  @Override
  public Markup renderLabelFromKey(StringExpression key) {
    return this.renderLabelFromKey(key, NumericExpression.empty());
  }

  @Override
  public Markup renderLabelFromKey(StringExpression key, NumericExpression quantity) {
    if (quantity.isEmpty()) {
      return new Markup(String.format("label(%s)", key.getScript()));
    }
    return new Markup(String.format("label(%s, %s)", key.getScript(), quantity.getScript()));
  }

  @Override
  public Markup renderLabelFromExpression(Expression expression) {
    return this.renderLabelFromExpression(expression, NumericExpression.empty()); 
  }

  @Override
  public Markup renderLabelFromExpression(Expression expression, NumericExpression quantity) {
    if (quantity.isEmpty()) {
      return new Markup(String.format("label(%s)", expression.getScript()));
    }
    return new Markup(String.format("label(%s, %s)", expression.getScript(), quantity.getScript()));
  }

  @Override
  public Markup renderFreeText(String freeText) {
    return new Markup(String.format("text('%s')", freeText));
  }

  @Override
  public Markup renderLineBreak() {
    return new Markup("<line-break>");
  }

  @Override
  public Markup composeFragmentDefinition(String name, String number, Markup content,
      Set<String> parameters) {
    if (StringUtils.isBlank(number)) {
      return new Markup(String.format("let %s(%s) -> { %s }", name,
          parameters.stream().collect(Collectors.joining(", ")), content.script));
    }
    return new Markup(String.format("let %s(%s) -> { #%s: %s }", name,
        parameters.stream().collect(Collectors.joining(", ")), number, content.script));
  }

  @Override
  public Markup renderFragmentInvocation(String name, PathExpression context,
      Set<Pair<String, String>> variables) {
    return new Markup(String.format("for-each(%s).call(%s(%s))", context.getScript(), name,
        variables.stream()
            .map(v -> String.format("%s:%s", v.getLeft(), v.getRight()))
            .collect(Collectors.joining(", "))));
  }

  @Override
  public Markup composeOutputFile(List<Markup> body, List<Markup> templates) {
    return new Markup(String.format("%s\n%s",
        templates.stream().map(t -> t.script).collect(Collectors.joining("\n")),
        body.stream().map(t -> t.script).collect(Collectors.joining("\n"))));
  }
}
