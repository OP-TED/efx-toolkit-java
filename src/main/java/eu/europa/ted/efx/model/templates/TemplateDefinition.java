package eu.europa.ted.efx.model.templates;

import java.util.List;

import eu.europa.ted.efx.model.identifiers.ParsedParameters;

/**
 * Represents the declaration of a callable template in the content block stack.
 * 
 * A template is declared by a template section:
 * <pre>
 * --- TEMPLATE template-name ---
 * PARAMETERS text:$param1, ...;
 * template-lines
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
   * @param contentTemplates What the template displays.
   * @param parameters   The parameters that this template expects to be passed when invoked.
   */
  public TemplateDefinition(final ContentBlock parent, final String name,
      final List<ContentTemplate> contentTemplates, ParsedParameters parameters) {
    super(parent, name, 0, contentTemplates, null, parameters, parameters.toArguments());
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