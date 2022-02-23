package eu.europa.ted.efx.xpath;

import java.util.Objects;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
public class TedefoNode {
  private final String id;
  private final String xpathAbsolute;
  private final String xpathRelative;
  private final String parentId;
  private final boolean repeatable;

  public TedefoNode(final String id, final String parentId, final String xpathAbsolute,
      final String xpathRelative, final boolean repeatable) {
    this.id = id;
    this.parentId = parentId;
    this.xpathAbsolute = xpathAbsolute;
    this.xpathRelative = xpathRelative;
    this.repeatable = repeatable;
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
    TedefoNode other = (TedefoNode) obj;
    return Objects.equals(id, other.id);
  }

  public String getId() {
    return id;
  }

  public String getParentId() {
    return parentId;
  }

  public String getXpathAbsolute() {
    return xpathAbsolute;
  }

  public String getXpathRelative() {
    return xpathRelative;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  @Override
  public String toString() {
    return "TedefoNode [id=" + id + "]";
  }
}
