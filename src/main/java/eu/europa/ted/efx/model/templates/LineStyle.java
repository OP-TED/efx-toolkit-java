package eu.europa.ted.efx.model.templates;

/**
 * How a template line is displayed: as a heading or as a normal line.
 *
 * <p>In EFX-2, a line that starts with an outline marker (a number, {@code ?} or {@code #}) is a
 * heading, and a line without one is a normal line. EFX-1 lines are always normal lines.
 */
public enum LineStyle {
  HEADING, NORMAL
}
