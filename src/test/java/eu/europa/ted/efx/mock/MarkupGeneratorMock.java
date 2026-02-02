/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.Parameter;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.templates.Conditional;
import eu.europa.ted.efx.model.templates.Markup;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

public class MarkupGeneratorMock implements MarkupGenerator {

  static final Map<Class<? extends EfxDataType.Primitive>, Markup> typeFromEfxDataType = Map
      .ofEntries(
          Map.entry(EfxDataType.String.class, new Markup("string")), //
          Map.entry(EfxDataType.MultilingualString.class, new Markup("string")), //
          Map.entry(EfxDataType.Boolean.class, new Markup("boolean")), //
          Map.entry(EfxDataType.Number.class, new Markup("decimal")), //
          Map.entry(EfxDataType.Date.class, new Markup("date")), //
          Map.entry(EfxDataType.Time.class, new Markup("time")), //
          Map.entry(EfxDataType.Duration.class, new Markup("duration")), //
          Map.entry(EfxDataType.Node.class, new Markup("context")) //
      );


  @Override
  public Markup renderVariableDeclaration(String variableName, TypedExpression initialiser) {
      Class<? extends EfxDataType> dataType = initialiser.getDataType();
      String typeName = this.getEfxDataTypeEquivalent(EfxTypeLattice.toPrimitive(dataType)).script;
      if (EfxTypeLattice.isSequence(dataType)) {
          typeName += "*";
      }
      return new Markup(String.format("%s:%s=%s", typeName, variableName, initialiser.getScript()));
  }

  @Override
  public Markup renderFunctionDeclaration(String name, Map<String, Class<? extends EfxDataType>> parameters, TypedExpression expression) {
    Class<? extends EfxDataType> type = expression.getDataType();
    String typeName = this.getEfxDataTypeEquivalent(EfxTypeLattice.toPrimitive(type)).script;
    if (EfxTypeLattice.isSequence(type)) {
        typeName += "*";
    }
    return new Markup(
        String.format("%s:%s(%s) -> { %s }", typeName, name,
            parameters.entrySet().stream()
                .map(entry -> {
                    String paramType = this.getEfxDataTypeEquivalent(EfxTypeLattice.toPrimitive(entry.getValue())).script;
                    if (EfxTypeLattice.isSequence(entry.getValue())) {
                        paramType += "*";
                    }
                    return paramType + ":" + entry.getKey();
                })
                .collect(Collectors.joining(", ")),
            expression.getScript()));
  }

  @Override
  public Markup renderVariableExpression(Expression valueReference, TranslatorContext translatorContext) {
    return new Markup(String.format("eval(%s)", valueReference.getScript()));
  }

  @Override
  public Markup renderLabelFromKey(StringExpression key, TranslatorContext translatorContext) {
    return this.renderLabelFromKey(key, NumericExpression.empty(), translatorContext);
  }

  @Override
  public Markup renderLabelFromKey(StringExpression key, NumericExpression quantity, TranslatorContext translatorContext) {
    if (quantity.isEmpty()) {
      return new Markup(String.format("label(%s)", key.getScript()));
    }
    return new Markup(String.format("label(%s, %s)", key.getScript(), quantity.getScript()));
  }

  @Override
  public Markup renderLabelFromExpression(Expression expression, TranslatorContext translatorContext) {
    return this.renderLabelFromExpression(expression, NumericExpression.empty(), TranslatorContext.DEFAULT); 
  }

  @Override
  public Markup renderLabelFromExpression(Expression expression, NumericExpression quantity, TranslatorContext translatorContext) {
    if (quantity.isEmpty()) {
      return new Markup(String.format("label(%s)", expression.getScript()));
    }
    return new Markup(String.format("label(%s, %s)", expression.getScript(), quantity.getScript()));
  }

  @Override
  public Markup renderFreeText(String freeText, TranslatorContext translatorContext) {
    return new Markup(String.format("text('%s')", freeText));
  }

  @Override
  public Markup renderHyperlink(Markup label, StringExpression url, TranslatorContext translatorContext) {
    return new Markup(String.format("hyperlink(%s, %s)", label.script, url.getScript()));
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
  public Markup renderLineBreak(TranslatorContext translatorContext) {
    return new Markup("line-break()");
  }

  @Override
  public Markup composeFragmentDefinition(String name, String number, Set<Conditional> conditionals,
      Markup content, Markup children, Set<Parameter> parameters, TranslatorContext translatorContext) {
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
  public Markup renderFragmentInvocation(String name, Set<Argument> arguments, TranslatorContext translatorContext) {
    return new Markup(String.format("call(%s(%s))", name,
        arguments.stream()
            .map(arg -> String.format("%s:%s=%s", arg.getType(), arg.getName(), arg.getValue()))
            .collect(Collectors.joining(", "))));
  }

  @Override
  public Markup composeOutputFile(List<Markup> globals, List<Markup> body, List<Markup> summary, List<Markup> navigation, final List<Markup> fragments) {
    var renderedGlobals = renderSection("GLOBALS", globals);
    var renderedTemplates = renderSection("TEMPLATES", fragments);
    var renderedMain = renderSection("MAIN", body);
    var renderedSummary = renderSection("SUMMARY", summary);
    var renderedNavigation = renderSection("NAV", navigation);

    return new Markup(String.format("%1$s%6$s%2$s%6$s%3$s%6$s%4$s%6$s%5$s",
        renderedGlobals,
        renderedTemplates,
        renderedMain,
        renderedSummary,
        renderedNavigation, "\n").trim());
  }

  private String renderSection(String heading, List<Markup> markupList) {
    return markupList.size() > 0
        ? String.format("%1$s:\n%2$s", heading, markupList.stream().map(t -> t.script).collect(Collectors.joining("\n")))
        : "";
  }

  @Override
  public Markup getEfxDataTypeEquivalent(Class<? extends EfxDataType> type) {
    // Use toPrimitive() to handle both scalar and sequence types
    return typeFromEfxDataType.getOrDefault(EfxTypeLattice.toPrimitive(type), Markup.empty());
  }

  @Override
  public Markup renderDictionaryDeclaration(final String name, final PathExpression match,
      final StringExpression key) {
        return new Markup(String.format("let %s index %s by %s;", name, match.getScript(), key.getScript()));
  }
}
