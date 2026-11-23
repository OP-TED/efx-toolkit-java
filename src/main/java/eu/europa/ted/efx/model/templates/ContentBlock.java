package eu.europa.ted.efx.model.templates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.Parameter;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.constructs.Arguments;
import eu.europa.ted.efx.model.identifiers.ParsedParameter;
import eu.europa.ted.efx.model.identifiers.ParsedParameters;
import eu.europa.ted.efx.model.identifiers.Variables;

public class ContentBlock {
  private final int number;

  protected final ContentBlock parent;
  protected final String id;
  protected final Integer indentationLevel;

  // The marker the line starts with, which makes it a heading, and its nesting level: 1 for a line
  // at the top of its section, 2 for a line nested in it, and so on. The root block, and a template
  // section that only holds lines, are at nesting level 0.
  protected final OutlineMarker marker;
  protected final int nestingLevel;

  // What the block displays, in order: one content template for each WHEN alternative, then the one
  // displayed otherwise. A block without WHEN alternatives has a single content template.
  protected final List<ContentTemplate> contentTemplates;

  protected final Context context;
  protected final Queue<ContentBlock> children = new LinkedList<>();
  protected final ParsedParameters parameters;
  protected final Arguments<ParsedParameter> arguments;

  private ContentBlock(String id) {
    this.parent = null;
    this.id = id;
    this.indentationLevel = -1;
    this.marker = OutlineMarker.NONE;
    this.nestingLevel = 0;
    this.contentTemplates = List.of();
    this.context = null;
    this.number = 0;
    this.parameters = new ParsedParameters();
    this.arguments = this.parameters.toArguments();
  }

  public ContentBlock(final ContentBlock parent, final String id, final int number,
      final List<ContentTemplate> contentTemplates, Context contextPath, ParsedParameters parameters,
      Arguments<ParsedParameter> arguments) {
    this(parent, id, number, OutlineMarker.NONE, parent.nestingLevel + 1, contentTemplates, contextPath,
        parameters, arguments);
  }

  /**
   * @param marker the marker the line starts with
   * @param nestingLevel the nesting level of the line: 1 at the top of its section, one more for
   *        each line it is nested in
   */
  protected ContentBlock(final ContentBlock parent, final String id, final int number,
      final OutlineMarker marker, final int nestingLevel,
      final List<ContentTemplate> contentTemplates, Context contextPath,
      ParsedParameters parameters, Arguments<ParsedParameter> arguments) {
    this.parent = parent;
    this.id = id;
    this.indentationLevel = parent.indentationLevel + 1;
    this.marker = marker;
    this.nestingLevel = nestingLevel;
    this.contentTemplates = contentTemplates;
    this.context = contextPath;
    this.number = number;
    this.parameters = parameters;
    this.arguments = arguments;
  }

  public ContentBlock(final ContentBlock parent, final String id, final int number,
      final List<ContentTemplate> contentTemplates, Context contextPath, Variables variables) {
    this(parent, id, number, OutlineMarker.NONE, contentTemplates, contextPath, variables);
  }

  public ContentBlock(final ContentBlock parent, final String id, final int number,
      final OutlineMarker marker, final List<ContentTemplate> contentTemplates,
      Context contextPath, Variables variables) {
    this(parent, id, number, marker, parent.nestingLevel + 1, contentTemplates, contextPath,
        new ParsedParameters(variables), variables.toArguments());
  }

  public static ContentBlock newRootBlock(String id) {
    return new ContentBlock(id);
  }

  // #region Add Children

  /**
   * Adds the definition of a template that holds lines.
   */
  public TemplateDefinition addChild(final String blockId, final List<ContentTemplate> contentTemplates,
      final ParsedParameters parameters) {
    TemplateDefinition newBlock = new TemplateDefinition(this, blockId, contentTemplates, parameters);
    this.children.add(newBlock);
    return newBlock;
  }

  /**
   * Adds the definition of a template that displays a single line itself.
   *
   * @param nestingLevel the nesting level of the line it displays
   */
  public TemplateDefinition addChild(final String blockId, final List<ContentTemplate> contentTemplates,
      final ParsedParameters parameters, final int nestingLevel) {
    TemplateDefinition newBlock =
        new TemplateDefinition(this, blockId, contentTemplates, parameters, nestingLevel);
    this.children.add(newBlock);
    return newBlock;
  }

  public ContentBlock addChild(final String blockId, final int number, final Context context,
      final Variables variables, final List<ContentTemplate> contentTemplates) {
    return this.addChild(blockId, number, OutlineMarker.NONE, context, variables,
        contentTemplates);
  }

  public ContentBlock addChild(final String blockId, final int number,
      final OutlineMarker marker, final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    // number < 0 means "autogenerate", number == 0 means "no number", number > 0
    // means "use this number"
    final int actualNumber = number >= 0 ? number
        : this.children.stream()
            .filter(ContentBlock.class::isInstance)
            .map(ContentBlock::getNumber)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

    ContentBlock newBlock = new ContentBlock(this, blockId, actualNumber, marker, contentTemplates, context, variables);
    this.children.add(newBlock);
    return newBlock;
  }

  public ContentBlock addChild(final int number, final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    return this.addChild(number, OutlineMarker.NONE, context, variables, contentTemplates);
  }

  public ContentBlock addChild(final int number, final OutlineMarker marker,
      final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    String newBlockId = String.format(Locale.ROOT, "%s%02d", this.id, this.children.size() + 1);
    return this.addChild(newBlockId, number, marker, context, variables, contentTemplates);
  }

  /**
   * Adds copies of the children of another block, and of their own children, under this block,
   * named and numbered as they would be if they had been added to this block.
   *
   * @param other the block whose children are copied
   */
  public void adoptChildrenOf(final ContentBlock other) {
    for (ContentBlock child : other.children) {
      final String childId = String.format(Locale.ROOT, "%s%02d", this.id, this.children.size() + 1);
      final ContentBlock copy = new ContentBlock(this, childId, child.number, child.marker,
          this.nestingLevel + 1, child.contentTemplates, child.context, child.parameters, child.arguments);
      this.children.add(copy);
      copy.adoptChildrenOf(child);
    }
  }

  /**
   * Removes a child of this block.
   *
   * @param child the child to remove
   */
  public void removeChild(final ContentBlock child) {
    this.children.remove(child);
  }

  // #endregion Add Children

  // #region Add Siblings

  public ContentBlock addSibling(final int number, final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    return this.addSibling(number, OutlineMarker.NONE, context, variables, contentTemplates);
  }

  public ContentBlock addSibling(final int number, final OutlineMarker marker,
      final Context context, final Variables variables,
      final List<ContentTemplate> contentTemplates) {
    return this.parent.addChild(number, marker, context, variables, contentTemplates);
  }

  // #endregion Add Siblings

  public String getId() {
    return this.id;
  }

  /**
   * Adds the ids of the children of this block, and of their own children, to a set.
   *
   * @param ids the set to add them to
   */
  public void collectDescendantIds(final Set<String> ids) {
    for (ContentBlock child : this.children) {
      ids.add(child.id);
      child.collectDescendantIds(ids);
    }
  }

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
      return String.format(Locale.ROOT, "%d", this.number);
    }

    final String parentNumber = this.parent.getOutlineNumber();
    if (parentNumber.isEmpty()) {
      return String.format(Locale.ROOT, "%d", this.number);
    }

    return String.format(Locale.ROOT, "%s.%d", parentNumber, this.number);
  }

  public Integer getIndentationLevel() {
    return this.indentationLevel;
  }

  /**
   * @return what the line is: a heading, a normal line, or a line that invokes a template in each
   *         of its alternatives
   */
  public FragmentKind getKind() {
    return this.isInvocation() ? FragmentKind.INVOCATION : this.marker.getKind();
  }

  /**
   * @return whether the line invokes a template in each of its alternatives, so that it is not
   *         displayed itself: the templates it invokes take its place
   */
  public boolean isInvocation() {
    return !this.contentTemplates.isEmpty()
        && this.contentTemplates.stream().allMatch(InvokeContentTemplate.class::isInstance);
  }

  /**
   * @return the nesting level of the line: 1 at the top of its section, one more for each line it is
   *         nested in; 0 for the root block and for a template section that only holds lines
   */
  public int getNestingLevel() {
    return this.nestingLevel;
  }

  /**
   * @return the heading level of the line: for a heading, the number of headings it is nested in, in
   *         its own section or template, plus 1; for a normal line, and for a line that invokes a
   *         template, 0
   */
  public int getHeadingLevel() {
    return this.getKind() == FragmentKind.HEADING ? this.getNumberOfHeadingsAbove() + 1 : 0;
  }

  /**
   * @return the number of headings that this line is nested in, in its own section or template
   */
  public int getNumberOfHeadingsAbove() {
    int count = 0;
    for (ContentBlock block = this.parent; block != null; block = block.parent) {
      if (block.getKind() == FragmentKind.HEADING) {
        count++;
      }
    }
    return count;
  }

  /**
   * @return the tag written after the '#' of the line, or "" when it has none
   */
  public String getTag() {
    return this.marker.getTag();
  }

  /**
   * @return the marker the line starts with
   */
  public OutlineMarker getMarker() {
    return this.marker;
  }

  /**
   * @return the number or the tag that this line adds to the addresses of the headings nested in it:
   *         its tag, or its number if it is a numbered heading; "" for a line that adds none, such
   *         as a normal line or a heading written with a bare '#'
   */
  private String getAddressSegment() {
    if (!this.getTag().isEmpty()) {
      return this.getTag();
    }
    return this.getKind() == FragmentKind.HEADING && this.number > 0 ? String.valueOf(this.number)
        : "";
  }

  /**
   * The outline address of a heading is that of the line it is nested in, followed by its own
   * outline number or tag, also when the number is not displayed because the heading has no lines
   * nested in it. A line that adds no number or tag has no address of its own: the lines nested in it
   * continue the address of the line above it. The address is local to the section or to the
   * callable template that holds the line.
   *
   * @return the outline address of this line, if it is a heading with an outline number or a tag
   */
  public Optional<OutlineAddress> getOutlineAddress() {
    final String segment = this.getAddressSegment();
    if (segment.isEmpty() || this.parent == null) {
      return Optional.empty();
    }
    return Optional.of(this.parent.getAddressOfNestedLines().append(segment));
  }

  /**
   * @return the outline address that the lines nested in this line, and the templates it invokes,
   *         continue
   */
  public OutlineAddress getAddressOfNestedLines() {
    return this.getOutlineAddress().orElseGet(() -> this.parent == null
        ? OutlineAddress.TOP
        : this.parent.getAddressOfNestedLines());
  }

  /**
   * @return where this block is in the outline of its own section or template, as the markup
   *         generator receives it
   */
  public FragmentOutline getOutline() {
    switch (this.getKind()) {
      case HEADING:
        return FragmentOutline.heading(this.getOutlineNumber(), this.nestingLevel,
            this.getHeadingLevel(),
            this.getOutlineAddress().map(OutlineAddress::toString).orElse(""));
      case INVOCATION:
        return FragmentOutline.invocation(this.nestingLevel);
      case TEMPLATE:
        return FragmentOutline.template();
      default:
        return FragmentOutline.normalLine(this.getOutlineNumber(), this.nestingLevel);
    }
  }

  /**
   * @return what a call of a template from this line puts above the lines of the template, which
   *         take the place of this line: the nesting level of the line above this one, the number
   *         of headings this line is nested in, and the outline address of the nearest line above it
   *         that has one, all in its own section or template
   */
  public OutlineOffset getOffsetOfTemplateCall() {
    return OutlineOffset.ofTemplateCall(this.nestingLevel - 1, this.getNumberOfHeadingsAbove(),
        this.getAddressOfNestedLines().toString());
  }

  /**
   * @return what the block displays: a content template for each WHEN alternative, then the one
   *         displayed otherwise
   */
  public List<ContentTemplate> getContentTemplates() {
    return this.contentTemplates;
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

  protected Arguments<ParsedParameter> getOwnArguments() {
    return this.arguments;
  }

  /**
   * @return the variables that the line declares, each with its expression; for a template, its
   *         parameters
   */
  public Arguments<ParsedParameter> getDeclaredVariables() {
    return this.arguments;
  }

  protected Set<ParsedParameter> getOwnParameters() {
    return this.parameters.toSet();
  }

  /**
   * @return the parameters of the arguments of this block and of the blocks that hold it, those of
   *         the outermost block first
   */
  protected Set<ParsedParameter> getAllParameters() {
    final Set<ParsedParameter> all =
        this.parent == null ? new LinkedHashSet<>() : this.parent.getAllParameters();
    this.arguments.forEach(argument -> all.add(argument.parameter));
    return all;
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
   * the WHEN alternatives as conditionals, and the content displayed otherwise on its own, and the
   * body of the navigation id of the line: its outline address in its own section or template.
   */
  public List<Markup> renderDefinition(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    Set<Parameter> params = this.getAllParameters().stream()
        .map(param -> new Parameter.Impl(param.name, markupGenerator.getEfxDataTypeEquivalent(param.dataType)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    List<Conditional> conditionals = new ArrayList<>();
    Markup content = Markup.empty();
    for (ContentTemplate contentTemplate : this.contentTemplates) {
      if (contentTemplate.getCondition().isEmpty()) {
        content = this.render(contentTemplate, markupGenerator, translatorContext);
      } else {
        conditionals.add(new Conditional(contentTemplate.getCondition(),
            this.render(contentTemplate, markupGenerator, translatorContext)));
      }
    }
    List<Markup> templates = new ArrayList<>();
    templates.add(markupGenerator.composeFragmentDefinition(this.id, this.getOutline(),
        conditionals, content, this.renderChildren(markupGenerator, translatorContext), params,
        translatorContext));
    for (ContentBlock child : this.children) {
      templates.addAll(child.renderDefinition(markupGenerator, translatorContext));
    }
    return templates;
  }

  /**
   * Renders what the line displays. A call of a template takes the place of this line: it puts
   * above the lines of the template the nesting level of the line above this one, the number of
   * headings this line is nested in, and the outline address of the nearest line above it that has
   * one, all in its own section or template.
   */
  private Markup render(final ContentTemplate contentTemplate,
      final MarkupGenerator markupGenerator, final TranslatorContext translatorContext) {
    if (contentTemplate instanceof InvokeContentTemplate) {
      return ((InvokeContentTemplate) contentTemplate).render(markupGenerator, translatorContext,
          this.getOffsetOfTemplateCall());
    }
    return contentTemplate.render(markupGenerator, translatorContext);
  }

  /**
   * Renders the call of this block. The call of a line puts nothing above it, offsets 0 and an empty
   * navigation id prefix: the levels and the outline address of the line already hold those of the
   * lines above it in the same section or template.
   */
  public Markup renderInvocation(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    Set<Argument> args = new LinkedHashSet<>();
    if (this.parent != null) {
      args.addAll(this.parent.getAllParameters().stream()
          .map(parameter -> new Argument.Impl(parameter.name,
              markupGenerator.getEfxDataTypeEquivalent(parameter.dataType),
              parameter.referenceExpression))
          .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
    args.addAll(this.getOwnArguments().stream()
        .map(a -> new Argument.Impl(a.parameter.name,
            markupGenerator.getEfxDataTypeEquivalent(a.parameter.dataType), a.value))
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    var invocation =
        markupGenerator.renderFragmentInvocation(this.id, OutlineOffset.NONE, args, translatorContext);
    if (this.context == null) {
      // A line that declares no context, at the top of a template section, is displayed in the
      // context the template is invoked in.
      return invocation;
    }
    return markupGenerator.renderContextLoop(this.context.relativePath(), invocation, args);
  }

  // #endregion Render --------------------------------------------------------
}