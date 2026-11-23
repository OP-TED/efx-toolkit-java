package eu.europa.ted.efx.model.templates;

import java.util.Objects;

/**
 * What a call puts above the lines of the fragment it invokes, as the markup generator receives it:
 * a nesting level offset, a heading level offset and a navigation id prefix. It holds information
 * only.
 *
 * <p>A call of a nested line puts nothing above it ({@link #NONE}): the levels and the outline
 * address of the line already hold those of the lines above it in the same section or template. A
 * call of a template puts above its lines what is above the calling line, as the template takes
 * the place of that line (see {@link #ofTemplateCall(int, int, String)}).
 */
public final class OutlineOffset {

  /** What the call of a nested line puts above it: nothing. */
  public static final OutlineOffset NONE = new OutlineOffset(0, 0, "");

  private final int nestingLevelOffset;
  private final int headingLevelOffset;
  private final String navIdPrefix;

  private OutlineOffset(final int nestingLevelOffset, final int headingLevelOffset,
      final String navIdPrefix) {
    this.nestingLevelOffset = nestingLevelOffset;
    this.headingLevelOffset = headingLevelOffset;
    this.navIdPrefix = navIdPrefix;
  }

  /**
   * @param nestingLevelOffset the nesting level of the calling line in its own section or template,
   *        minus 1
   * @param headingLevelOffset the number of headings that the calling line is nested in, in its own
   *        section or template; not more than the nesting level offset
   * @param navIdPrefix the navigation id body of the nearest line above the calling line that has
   *        one, in its own section or template, or "" when there is none; a line that has one is a
   *        heading, so the heading level offset is 1 or more when it is not empty
   * @return what the call of a template puts above its lines
   */
  public static OutlineOffset ofTemplateCall(final int nestingLevelOffset,
      final int headingLevelOffset, final String navIdPrefix) {
    Objects.requireNonNull(navIdPrefix, "navIdPrefix");
    if (nestingLevelOffset < 0) {
      throw new IllegalArgumentException(
          "The nesting level offset cannot be negative: " + nestingLevelOffset + ".");
    }
    if (headingLevelOffset < 0 || headingLevelOffset > nestingLevelOffset) {
      throw new IllegalArgumentException("The heading level offset must be between 0 and the "
          + "nesting level offset " + nestingLevelOffset + ", not " + headingLevelOffset + ".");
    }
    if (!navIdPrefix.isEmpty() && headingLevelOffset == 0) {
      throw new IllegalArgumentException(
          "A navigation id prefix needs a heading above the calling line: '" + navIdPrefix + "'.");
    }
    return new OutlineOffset(nestingLevelOffset, headingLevelOffset, navIdPrefix);
  }

  /**
   * @return the number of nesting levels that the call puts above the fragment
   */
  public int getNestingLevelOffset() {
    return this.nestingLevelOffset;
  }

  /**
   * @return the number of heading levels that the call puts above the fragment
   */
  public int getHeadingLevelOffset() {
    return this.headingLevelOffset;
  }

  /**
   * @return the plain text that the call puts in front of the navigation ids in the fragment, or ""
   *         when it puts none
   */
  public String getNavIdPrefix() {
    return this.navIdPrefix;
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof OutlineOffset)) {
      return false;
    }
    final OutlineOffset that = (OutlineOffset) other;
    return this.nestingLevelOffset == that.nestingLevelOffset
        && this.headingLevelOffset == that.headingLevelOffset
        && this.navIdPrefix.equals(that.navIdPrefix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.nestingLevelOffset, this.headingLevelOffset, this.navIdPrefix);
  }

  @Override
  public String toString() {
    return "nestingLevelOffset " + this.nestingLevelOffset + ", headingLevelOffset "
        + this.headingLevelOffset + " and navIdPrefix '" + this.navIdPrefix + "'";
  }
}
