package eu.europa.ted.efx.model.templates;

/**
 * What a fragment is: a template line displayed as a heading or as a normal line, a line that is not
 * displayed itself because the templates it invokes take its place, or a callable template.
 *
 * <p>In EFX-2, a line that starts with an outline marker (a number, {@code ?}, {@code #} or a tag)
 * is a heading, and a line without one is a normal line, unless it invokes a template in each of
 * its alternatives. EFX-1 lines are always normal lines.
 */
public enum FragmentKind {
  /** A heading: a line that starts with an outline marker. */
  HEADING,
  /** A normal line: a line without an outline marker that displays content. */
  NORMAL,
  /** A line that is not displayed itself: the templates it invokes take its place. */
  INVOCATION,
  /**
   * A callable template that holds lines. It is not displayed itself: its top lines are displayed
   * in the place of the line that invokes the template.
   */
  TEMPLATE
}
