package eu.europa.ted.efx.model.templates;

import org.antlr.v4.runtime.Token;

/**
 * The marker that a template line starts with: a number, '?', '#', or '#' followed by a tag. A line
 * with a marker is a heading, and a line without one is a normal line. The marker keeps its token,
 * so that an error about the heading is placed where it is written.
 */
public final class OutlineMarker {

  /** No marker: the line is a normal line. */
  public static final OutlineMarker NONE = new OutlineMarker(null, "");

  private final Token token;
  private final String tag;

  /**
   * @param token the token of the marker
   * @param tag the tag written after the '#', without it, or "" when the marker has none
   */
  public OutlineMarker(final Token token, final String tag) {
    this.token = token;
    this.tag = tag;
  }

  /**
   * @return what the line is: a heading when it has a marker, a normal line otherwise
   */
  public FragmentKind getKind() {
    return this.token == null ? FragmentKind.NORMAL : FragmentKind.HEADING;
  }

  /**
   * @return the tag written after the '#', or "" when the marker has none
   */
  public String getTag() {
    return this.tag;
  }

  /**
   * @return the token of the marker, or {@code null} for a normal line
   */
  public Token getToken() {
    return this.token;
  }
}
