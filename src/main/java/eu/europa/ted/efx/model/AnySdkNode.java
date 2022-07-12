package eu.europa.ted.efx.model;

import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.annotation.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.efx.interfaces.SdkNode;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
@SdkComponent(componentType = SdkComponentTypeEnum.NODE)
public class AnySdkNode implements Comparable<AnySdkNode>, SdkNode {
  private final String id;
  private final String xpathAbsolute;
  private final String xpathRelative;
  private final String parentId;
  private final boolean repeatable;

  public AnySdkNode(final String id, final String parentId, final String xpathAbsolute,
      final String xpathRelative, final boolean repeatable) {
    this.id = id;
    this.parentId = parentId;
    this.xpathAbsolute = xpathAbsolute;
    this.xpathRelative = xpathRelative;
    this.repeatable = repeatable;
  }

  public AnySdkNode(JsonNode node) {
    this.id = node.get("id").asText(null);
    this.parentId = node.has("parentId") ? node.get("parentId").asText(null) : null;
    this.xpathAbsolute = node.get("xpathAbsolute").asText(null);
    this.xpathRelative = node.get("xpathRelative").asText(null);
    this.repeatable =
        node.hasNonNull("repeatable") ? node.get("repeatable").asBoolean(false) : false;
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

  public boolean isRepeatable() {
    return repeatable;
  }

  @Override
  public int compareTo(AnySdkNode o) {
    return o.getId().compareTo(o.getId());
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
    AnySdkNode other = (AnySdkNode) obj;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return id;
  }
}
