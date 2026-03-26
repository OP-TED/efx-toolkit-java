package eu.europa.ted.efx.model.templates;

import eu.europa.ted.efx.model.variables.ParsedArguments;
import eu.europa.ted.efx.model.variables.ParsedParameters;

/**
 * Represents a template definition line in the content block stack.
 * 
 * A template definition is defined using the syntax:
 * <pre>
 * LET template-name (param1, ...) DISPLAY content;
 * </pre>
 * 
 * This class extends {@link ContentBlock} and provides functionality specific
 * to template definitions. It overrides the {@link #getOutlineNumber()} method
 * to indicate that template definitions do not have an outline number.
 */
public class TemplateDefinition extends ContentBlock {

  /**
   * Constructs a new TemplateDefinition instance.
   *
   * @param parent       The parent ContentBlock to which this template is attached.
   * @param name         The name with which we will call the template.
   * @param conditionals The conditionals that determine when this template should be used.
   * @param content      The content to display for this template line.
   * @param parameters   The parameters that this template expects to be passed when invoked.
   */
  public TemplateDefinition(final ContentBlock parent, final String name,
      final Conditionals conditionals, final Markup content, ParsedParameters parameters) {
    super(parent, name, 0, conditionals, content, null, parameters, new ParsedArguments(parameters));
  }
  

  /***
   * Template definitions do not have an outline number.
   * When called, they get the outline number of the calling block.
   */
  @Override
  public String getOutlineNumber() {
    return "";
  }
}