package eu.europa.ted.efx.model.templates;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.variables.Variables;

/***
 * Represents a template invocation line.
 */
public class TemplateInvocation extends ContentBlock {

  public TemplateInvocation(final ContentBlock parent, final TemplateDefinition template, final int number,
      Context context,
      Variables variables) {
    super(parent, template.id, number, template.conditionals, template.content, context, variables);
  }

  /***
   * Template invocations do not have own content or child content to render.
   */
  @Override
  public List<Markup> renderDefinition(MarkupGenerator markupGenerator) {
    return new ArrayList<Markup>() {
    };
  }

  /***
   * When calling a template, we only pass the arguments that it expects as it
   * should net have access to local variables.
   */
  @Override
  public Markup renderInvocation(MarkupGenerator markupGenerator) {
    Set<Argument> arguments = this.getOwnArguments().stream().map(a -> new Argument.Impl(a.name, markupGenerator.getEfxDataTypeEquivalent(a.dataType), a.value))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    var content = markupGenerator.renderFragmentInvocation(this.id, arguments);
    return markupGenerator.renderContextLoop(this.id, this.context.relativePath(), content, arguments);
  }
}