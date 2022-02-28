package eu.europa.ted.efx.xpath;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * XPath fragment representing an XPath location step. There is no leading or trailing slash. For
 * example, the XPath "/a/b[u/v='z']/c" is made of 3 steps: "a", "b[u/v='z']", and "c".
 *
 * <p>
 * An instance can also represent several steps (a/b/c), for situation when we don't need to handle
 * a, b and c separately.
 * </p>
 */
public class XPathStep implements Comparable<XPathStep> {

  private final String step;
  private final Optional<String> nodeId;

  public XPathStep(final String step) {
    this(step, null);
  }

  public XPathStep(final String step, final String nodeId) {
    this.nodeId = Optional.ofNullable(nodeId);
    var step2 = StringUtils.strip(step);
    var step3 = StringUtils.strip(step2, "/");
    this.step = step3;
  }

  /**
   * Returns the identifier of the node if the instance corresponds to an entry from the NODE table,
   * empty otherwise.
   */
  public Optional<String> getNodeId() {
    return nodeId;
  }

  public boolean isContainerNode() {
    return nodeId.isPresent();
  }

  public boolean isAttribute() {
    return step.startsWith("@");
  }

  public boolean hasPredicate() {
    return step.contains("[");
  }

  @Override
  public String toString() {
    return step;
  }

  @Override
  public int hashCode() {
    return Objects.hash(step);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    XPathStep other = (XPathStep) obj;
    return Objects.equals(step, other.step);
  }

  @Override
  public int compareTo(XPathStep other) {
    return step.compareTo(other.toString());
  }
}
