package eu.europa.ted.efx.model.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where a heading is in the outline of a view template: the outline numbers and tags of the
 * headings it is nested in, the outermost first, then its own, joined by {@link #SEPARATOR}, as in
 * "5_1_10" or "notice-info_1". A tag is made of lower-case letters, digits and hyphens, and an
 * outline number of digits, so neither contains the separator.
 *
 * <p>An address is local to its section, or to its callable template. A template is displayed
 * wherever it is invoked, so the address of one of its headings, where it is displayed, is the
 * address of the line that invokes it, followed by its own address in the template, joined by
 * {@link #SEPARATOR} (see {@link #after(OutlineAddress)}).
 */
public final class OutlineAddress {

  /** The separator between the outline numbers and tags of an address. */
  public static final String SEPARATOR = "_";

  /** The address above the lines at the top of a section or of a template. */
  public static final OutlineAddress TOP = new OutlineAddress(List.of());

  private final List<String> segments;

  private OutlineAddress(final List<String> segments) {
    this.segments = Collections.unmodifiableList(segments);
  }

  /**
   * @param segment the outline number or the tag of a heading nested in the place of this address
   * @return the address of that heading
   */
  public OutlineAddress append(final String segment) {
    final List<String> appended = new ArrayList<>(this.segments);
    appended.add(segment);
    return new OutlineAddress(appended);
  }

  /**
   * @param invocation the address of the line that invokes the template that holds this address
   * @return this address, as it is where the template is displayed
   */
  public OutlineAddress after(final OutlineAddress invocation) {
    final List<String> composed = new ArrayList<>(invocation.segments);
    composed.addAll(this.segments);
    return new OutlineAddress(composed);
  }

  /**
   * @return the outline numbers and tags of the address, joined by {@link #SEPARATOR}
   */
  @Override
  public String toString() {
    return String.join(SEPARATOR, this.segments);
  }

  @Override
  public boolean equals(final Object other) {
    return other instanceof OutlineAddress
        && this.segments.equals(((OutlineAddress) other).segments);
  }

  @Override
  public int hashCode() {
    return this.segments.hashCode();
  }
}
