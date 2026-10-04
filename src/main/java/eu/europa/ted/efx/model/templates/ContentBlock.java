package eu.europa.ted.efx.model.templates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.Parameter;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.variables.ParsedArgument;
import eu.europa.ted.efx.model.variables.ParsedArguments;
import eu.europa.ted.efx.model.variables.ParsedParameter;
import eu.europa.ted.efx.model.variables.ParsedParameters;
import eu.europa.ted.efx.model.variables.Variables;

public class ContentBlock {
  private final int number;

  protected final ContentBlock parent;
  protected final String id;
  protected final Integer indentationLevel;

  // What the block displays, in order: one content template for each WHEN alternative, then the one
  // displayed otherwise. A block without WHEN alternatives has a single content template.
  protected final List<ContentTemplate> contentTemplates;

  protected final Context context;
  protected final Queue<ContentBlock> children = new LinkedList<>();
  protected final ParsedParameters parameters;
  protected final ParsedArguments arguments;

  private ContentBlock(String id) {
    this.parent = null;
    this.id = id;
    this.indentationLevel = -1;
    this.contentTemplates = List.of();
    this.context = null;
    this.number = 0;
    this.parameters = new ParsedParameters();
    this.arguments = new ParsedArguments(this.parameters);
  }

  public ContentBlock(final ContentBlock parent, final String id, final int number,
      final List<ContentTemplate> contentTemplates, Context contextPath, ParsedParameters parameters,
      ParsedArguments arguments) {
    this.parent = parent;
    this.id = id;
    this.indentationLevel = parent.indentationLevel + 1;
    this.contentTemplates = contentTemplates;
    this.context = contextPath;
    this.number = number;
    this.parameters = parameters;
    this.arguments = arguments;
  }

  public ContentBlock(final ContentBlock parent, final String id, final int number,
      final List<ContentTemplate> contentTemplates, Context contextPath, Variables variables) {
    this(parent, id, number, contentTemplates, contextPath, new ParsedParameters(variables),
        new ParsedArguments(variables));
  }

  public static ContentBlock newRootBlock(String id) {
    return new ContentBlock(id);
  }

  // #region Add Children

  public TemplateDefinition addChild(final String blockId, final List<ContentTemplate> contentTemplates,
      final ParsedParameters parameters) {
    TemplateDefinition newBlock = new TemplateDefinition(this, blockId, contentTemplates, parameters);
    this.children.add(newBlock);
    return newBlock;
  }

  public ContentBlock addChild(final String blockId, final int number, final Context context,
      final Variables variables, final List<ContentTemplate> contentTemplates) {
    // number < 0 means "autogenerate", number == 0 means "no number", number > 0
    // means "use this number"
    final int actualNumber = number >= 0 ? number
        : this.children.stream()
            .filter(ContentBlock.class::isInstance)
            .map(ContentBlock::getNumber)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

    ContentBlock newBlock = new ContentBlock(this, blockId, actualNumber, contentTemplates, context, variables);
    this.children.add(newBlock);
    return newBlock;
  }

  public ContentBlock addChild(final int number, final Context context, final Variables variables,
      final TemplateDefinition template) {
    // number < 0 means "autogenerate", number == 0 means "no number", number > 0
    // means "use this number"
    final int actualNumber = number >= 0 ? number
        : this.children.stream()
            .filter(ContentBlock.class::isInstance)
            .map(ContentBlock::getNumber)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

    ContentBlock newBlock = new TemplateInvocation(this, template, actualNumber, context, variables);
    this.children.add(newBlock);
    return newBlock;
  }

  public ContentBlock addChild(final int number, final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    String newBlockId = String.format("%s%02d", this.id, this.children.size() + 1);
    return this.addChild(newBlockId, number, context, variables, contentTemplates);
  }

  // #endregion Add Children

  // #region Add Siblings

  public ContentBlock addSibling(final int number, final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    return this.parent.addChild(number, context, variables, contentTemplates);
  }

  public ContentBlock addSibling(final int number, final Context context, final Variables variables,
      final TemplateDefinition template) {
    return this.parent.addChild(number, context, variables, template);
  }

  // #endregion Add Siblings

  public int getNumber() {
    return this.number;
  }

  public ContentBlock findParentByLevel(final int parentIndentationLevel) {

    assert this.indentationLevel >= parentIndentationLevel : "Unexpected indentation tracker state.";

    ContentBlock targetBlock = this;
    while (targetBlock.indentationLevel > parentIndentationLevel) {
      targetBlock = targetBlock.parent;
    }
    return targetBlock;
  }

  public TemplateDefinition get(final String blockId) {
    for (ContentBlock child : this.children) {
      if (child instanceof TemplateDefinition && child.id.equals(blockId)) {
        return (TemplateDefinition) child;
      }
    }
    return null;
  }

  public Queue<ContentBlock> getChildren() {
    return this.children;
  }

  public String getOutlineNumber() {
    if (this.number == 0 || this.children.isEmpty()) {
      return "";
    }

    if (this.parent == null || (this.parent.number == 0)) {
      return String.format("%d", this.number);
    }

    final String parentNumber = this.parent.getOutlineNumber();
    if (parentNumber.isEmpty()) {
      return String.format("%d", this.number);
    }

    return String.format("%s.%d", parentNumber, this.number);
  }

  public Integer getIndentationLevel() {
    return this.indentationLevel;
  }

  public Context getContext() {
    return this.context;
  }

  public Context getParentContext() {
    if (this.parent == null) {
      return null;
    }
    return this.parent.getContext();
  }

  protected Set<ParsedArgument> getOwnArguments() {
    return this.arguments.toSet();
  }

  protected Set<ParsedParameter> getOwnParameters() {
    return this.parameters.toSet();
  }

  protected Set<ParsedParameter> getAllParameters() {
    if (this.parent == null) {
      return new LinkedHashSet<>(this.getOwnArguments());
    }
    final Set<ParsedParameter> merged = new LinkedHashSet<>();
    merged.addAll(this.parent.getAllArguments());
    merged.addAll(this.getOwnArguments());
    return merged;
  }

  protected Set<ParsedArgument> getAllArguments() {
    if (this.parent == null) {
      return new LinkedHashSet<>(this.getOwnArguments());
    }
    final Set<ParsedArgument> merged = new LinkedHashSet<>();
    merged.addAll(this.parent.getAllArguments());
    merged.addAll(this.getOwnArguments());
    return merged;
  }

  // #region Render -----------------------------------------------------------

  public Markup renderChildren(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    StringBuilder stringBuilder = new StringBuilder();
    for (ContentBlock child : this.children) {
      stringBuilder.append('\n').append(child.renderInvocation(markupGenerator, translatorContext).script);
    }
    return new Markup(stringBuilder.toString());
  }

  /**
   * Renders the definition of this block, then those of its children. The markup generator receives
   * the WHEN alternatives as conditionals, and the content displayed otherwise on its own.
   */
  public List<Markup> renderDefinition(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    Set<Parameter> params = this.getAllParameters().stream()
        .map(param -> new Parameter.Impl(param.name, markupGenerator.getEfxDataTypeEquivalent(param.dataType)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<Conditional> conditionals = new LinkedHashSet<>();
    Markup content = Markup.empty();
    for (ContentTemplate contentTemplate : this.contentTemplates) {
      if (contentTemplate.getCondition().isEmpty()) {
        content = contentTemplate.render(markupGenerator, translatorContext);
      } else {
        conditionals.add(new Conditional(contentTemplate.getCondition(),
            contentTemplate.render(markupGenerator, translatorContext)));
      }
    }
    List<Markup> templates = new ArrayList<>();
    templates.add(markupGenerator.composeFragmentDefinition(this.id, this.getOutlineNumber(), conditionals, content,
        this.renderChildren(markupGenerator, translatorContext), params, translatorContext));
    for (ContentBlock child : this.children) {
      templates.addAll(child.renderDefinition(markupGenerator, translatorContext));
    }
    return templates;
  }

  public Markup renderInvocation(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    Set<Argument> args = new LinkedHashSet<>();
    if (this.parent != null) {
      args.addAll(this.parent.getAllArguments().stream()
          .map(a -> new Argument.Impl(a.name, markupGenerator.getEfxDataTypeEquivalent(a.dataType), a.referenceExpression))
          .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
    args.addAll(this.getOwnArguments().stream().map(a -> new Argument.Impl(a.name, markupGenerator.getEfxDataTypeEquivalent(a.dataType), a.value))
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    var invocation = markupGenerator.renderFragmentInvocation(this.id, args, translatorContext);
    return markupGenerator.renderContextLoop(this.context.relativePath(), invocation, args);
  }

  // #endregion Render --------------------------------------------------------
}