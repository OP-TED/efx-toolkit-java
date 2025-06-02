package eu.europa.ted.efx.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.Parameter;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.templates.Conditional;
import eu.europa.ted.efx.model.templates.Markup;
import eu.europa.ted.efx.model.types.EfxDataType;

public class MarkupGeneratorMock implements MarkupGenerator {

  static final Map<Class<? extends EfxDataType>, Markup> typeFromEfxDataType = Map
      .ofEntries(
          Map.entry(EfxDataType.String.class, new Markup("string")), //
          Map.entry(EfxDataType.MultilingualString.class, new Markup("string")), //
          Map.entry(EfxDataType.Boolean.class, new Markup("boolean")), //
          Map.entry(EfxDataType.Number.class, new Markup("decimal")), //
          Map.entry(EfxDataType.Date.class, new Markup("date")), //
          Map.entry(EfxDataType.Time.class, new Markup("time")), //
          Map.entry(EfxDataType.Duration.class, new Markup("duration")) //
      );

  @Override
  public Markup renderVariableDeclaration(Class<? extends EfxDataType> dataType, String variableName,
          Expression initialiser) {
      return new Markup(String.format("%s:%s=%s", this.getEfxDataTypeEquivalent(dataType).script, variableName, initialiser.getScript()));
  }

  @Override
  public Markup renderFunctionDeclaration(Class<? extends EfxDataType> type, String name, Map<String, Class<? extends EfxDataType>> parameters,
      Expression expression) {
    return new Markup(
        String.format("%s:%s(%s) -> { %s }", this.getEfxDataTypeEquivalent(type).script, name, 
            parameters.entrySet().stream()
                .map(entry -> this.getEfxDataTypeEquivalent(entry.getValue()).script + ":" + entry.getKey())
                .collect(Collectors.joining(", ")), 
            expression.getScript()));
  }

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
  public String escapeSpecialCharacters(String text) {
    if (text == null) {
      return null;
    }

    return text
        // Replace & that are NOT part of existing HTML entities (&#...; or &name;)
        .replaceAll("&(?![#a-zA-Z0-9]+;)", "&#38;")
        .replace("<", "&#60;")
        .replace(">", "&#62;")
        .replace("\"", "&#34;")
        .replace("'", "&#39;");
  }

  @Override
  public Markup renderLineBreak() {
    return new Markup("line-break()");
  }

  @Override
  public Markup composeFragmentDefinition(String name, String number, Set<Conditional> conditionals,
      Markup content, Markup children, Set<Parameter> parameters) {
    String contents = "";

    if (conditionals != null && !conditionals.isEmpty()) {
      contents = conditionals.stream()
          .map(c -> String.format("when %s: %s", c.getCondition().getScript(), c.getMarkup().script))
          .collect(Collectors.joining(", ", contents, ""));
      if (content.isEmpty()) {
        contents += ", otherwise nothing";
      } else {
        contents += ", otherwise: " + content.script;
      }
      contents = "choose { " + contents + " }";

    } else {
      contents = content.script;
    }

    if (children != null && !children.isEmpty()) {
      contents += /* "\n" +*/ children.script;
    }

    if (StringUtils.isBlank(number)) {
      return new Markup(String.format("let %s(%s) -> { %s }", name,
          parameters.stream().map(p -> String.format("%s:%s", p.getType(), p.getName())).collect(Collectors.joining(", ")), contents));
    }

    return new Markup(String.format("let %s(%s) -> { #%s: %s }", name,
        parameters.stream().map(p -> String.format("%s:%s", p.getType(), p.getName())).collect(Collectors.joining(", ")), number, contents));
  }

  @Override
  public Markup renderContextLoop(PathExpression context, final Markup content,
      Set<Argument> arguments) {
    return new Markup(String.format("for-each(%s).%s", context.getScript(), content.script));
  }

  @Override
  public Markup renderFragmentInvocation(String name, Set<Argument> arguments) {
    return new Markup(String.format("call(%s(%s))", name,
        arguments.stream()
            .map(arg -> String.format("%s:%s=%s", arg.getType(), arg.getName(), arg.getValue()))
            .collect(Collectors.joining(", "))));
  }

  @Override
  public Markup composeOutputFile(List<Markup> body, List<Markup> templates) {
    return this.composeOutputFile(new ArrayList<Markup>(), body, templates);
  }

  @Override
  public Markup composeOutputFile(List<Markup> globals, List<Markup> body, List<Markup> templates) {
    return new Markup(String.format("%1$s%4$s%2$s%4$s%3$s",
        globals.stream().map(t -> t.script).collect(Collectors.joining("\n")),
        templates.stream().map(t -> t.script).collect(Collectors.joining("\n")),
        body.stream().map(t -> t.script).collect(Collectors.joining("\n")), "\n").trim());
  }

  @Override
  public Markup getEfxDataTypeEquivalent(Class<? extends EfxDataType> type) {
    return typeFromEfxDataType.getOrDefault(type, Markup.empty());
  }
}
