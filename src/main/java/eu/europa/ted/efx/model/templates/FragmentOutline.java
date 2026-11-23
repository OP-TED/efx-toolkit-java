package eu.europa.ted.efx.model.templates;

import java.util.Objects;

/**
 * Where a fragment is in the outline of its own section or callable template, as the markup
 * generator receives it: what the fragment is, its outline number, its nesting level, its heading
 * level and the body of its navigation id. It holds information only.
 *
 * <p>A callable template is displayed wherever it is invoked, so these values are those of the
 * fragment in its own section or template. Where the fragment is displayed, the calls that lead to
 * it add what they put above it (see {@link OutlineOffset}).
 */
public final class FragmentOutline {

  private static final FragmentOutline TEMPLATE =
      new FragmentOutline(FragmentKind.TEMPLATE, "", 0, 0, "");

  private final FragmentKind kind;
  private final String number;
  private final int nestingLevel;
  private final int headingLevel;
  private final String navIdBody;

  private FragmentOutline(final FragmentKind kind, final String number, final int nestingLevel,
      final int headingLevel, final String navIdBody) {
    this.kind = kind;
    this.number = Objects.requireNonNull(number, "number");
    this.nestingLevel = nestingLevel;
    this.headingLevel = headingLevel;
    this.navIdBody = Objects.requireNonNull(navIdBody, "navIdBody");
  }

  /**
   * @param number the outline number of the heading, or "" when it has none
   * @param nestingLevel the nesting level of the heading, 1 or more
   * @param headingLevel the heading level of the heading: the number of headings it is nested in,
   *        plus 1, so 1 or more, and not more than its nesting level
   * @param navIdBody the body of the navigation id of the heading, or "" when it has none
   * @return the outline of a heading
   */
  public static FragmentOutline heading(final String number, final int nestingLevel,
      final int headingLevel, final String navIdBody) {
    requireLineLevel(nestingLevel);
    if (headingLevel < 1 || headingLevel > nestingLevel) {
      throw new IllegalArgumentException("The heading level of a heading must be between 1 and its "
          + "nesting level " + nestingLevel + ", not " + headingLevel + ".");
    }
    return new FragmentOutline(FragmentKind.HEADING, number, nestingLevel, headingLevel, navIdBody);
  }

  /**
   * @param number the outline number of the line, or "" when it has none; in EFX-1, a normal line
   *        can be numbered
   * @param nestingLevel the nesting level of the line, 1 or more
   * @return the outline of a normal line, which has heading level 0 and no navigation id body
   */
  public static FragmentOutline normalLine(final String number, final int nestingLevel) {
    requireLineLevel(nestingLevel);
    return new FragmentOutline(FragmentKind.NORMAL, number, nestingLevel, 0, "");
  }

  /**
   * @param nestingLevel the nesting level of the line, 1 or more
   * @return the outline of a line that invokes a template in each of its alternatives, which has no
   *         outline number, heading level 0 and no navigation id body
   */
  public static FragmentOutline invocation(final int nestingLevel) {
    requireLineLevel(nestingLevel);
    return new FragmentOutline(FragmentKind.INVOCATION, "", nestingLevel, 0, "");
  }

  /**
   * @return the outline of a callable template that holds lines, which has no outline number,
   *         nesting level 0, heading level 0 and no navigation id body
   */
  public static FragmentOutline template() {
    return TEMPLATE;
  }

  private static void requireLineLevel(final int nestingLevel) {
    if (nestingLevel < 1) {
      throw new IllegalArgumentException(
          "The nesting level of a template line must be 1 or more, not " + nestingLevel + ".");
    }
  }

  /**
   * @return what the fragment is
   */
  public FragmentKind getKind() {
    return this.kind;
  }

  /**
   * @return the outline number of the fragment, or "" when it has none
   */
  public String getNumber() {
    return this.number;
  }

  /**
   * @return the nesting level of the fragment in its own section or template: 1 for a line at the
   *         top, one more for each line it is nested in, and 0 for a callable template that holds
   *         lines
   */
  public int getNestingLevel() {
    return this.nestingLevel;
  }

  /**
   * @return the heading level of the fragment in its own section or template: for a heading, the
   *         number of headings it is nested in there, plus 1; for any other fragment, 0
   */
  public int getHeadingLevel() {
    return this.headingLevel;
  }

  /**
   * @return the body of the navigation id of the fragment, as plain text: the outline numbers and
   *         tags of the headings above the line inside its own section or template, then its own,
   *         joined by {@link OutlineAddress#SEPARATOR}, as in "5_1_10"; empty for a fragment without
   *         an outline number or tag
   */
  public String getNavIdBody() {
    return this.navIdBody;
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof FragmentOutline)) {
      return false;
    }
    final FragmentOutline that = (FragmentOutline) other;
    return this.kind == that.kind && this.number.equals(that.number)
        && this.nestingLevel == that.nestingLevel && this.headingLevel == that.headingLevel
        && this.navIdBody.equals(that.navIdBody);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.kind, this.number, this.nestingLevel, this.headingLevel,
        this.navIdBody);
  }

  @Override
  public String toString() {
    return this.kind + " number '" + this.number + "' nestingLevel " + this.nestingLevel
        + " headingLevel " + this.headingLevel + " navIdBody '" + this.navIdBody + "'";
  }
}
