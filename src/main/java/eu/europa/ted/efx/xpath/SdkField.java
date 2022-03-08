package eu.europa.ted.efx.xpath;

import java.util.Objects;

public class SdkField implements Comparable<SdkField> {
  private final String id;
  private final String xpathAbsolute;
  private final String xpathRelative;
  private final String parentNodeId;

  public SdkField(final String id, final String parentNodeId, final String xpathAbsolute,
      final String xpathRelative) {
    this.id = id;
    this.parentNodeId = parentNodeId;
    this.xpathAbsolute = xpathAbsolute;
    this.xpathRelative = xpathRelative;
  }

  /**
   * Helps with hash maps collisions. Should be consistent with equals.
   */
  @Override
  public int compareTo(SdkField o) {
    return this.getId().compareTo(o.getId());
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
    SdkField other = (SdkField) obj;
    return Objects.equals(id, other.id);
  }

  public String getId() {
    return id;
  }

  public String getParentNodeId() {
    return parentNodeId;
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

  @Override
  public String toString() {
    return "TedefoField [id=" + id + "]";
  }
}
