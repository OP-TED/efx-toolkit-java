package eu.europa.ted.efx.model;

import java.util.Objects;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;

public class SdkField implements Comparable<SdkField> {
  private final String id;
  private final String xpathAbsolute;
  private final String xpathRelative;
  private final String parentNodeId;
  private final String type;
  private final String rootCodelistId;

  public SdkField(final String id, final String type, final String parentNodeId,
      final String xpathAbsolute, final String xpathRelative, final String rootCodelistId) {
    this.id = id;
    this.parentNodeId = parentNodeId;
    this.xpathAbsolute = xpathAbsolute;
    this.xpathRelative = xpathRelative;
    this.type = type;
    this.rootCodelistId = rootCodelistId;
  }

  public SdkField(JsonNode field) {
    this.id = field.get("id").asText(null);
    this.parentNodeId = field.get("parentNodeId").asText(null);
    this.xpathAbsolute = field.get("xpathAbsolute").asText(null);
    this.xpathRelative = field.get("xpathRelative").asText(null);
    this.type = field.get("type").asText(null);
    Supplier<String> rootCodelistIdSupplier = () -> {
      final JsonNode codelistNode = field.get("codeList");
      if (codelistNode == null) {
        return null;
      }

      final JsonNode valueNode = codelistNode.get("value");
      if (valueNode == null) {
        return null;
      }

      final String parentCodelistId = valueNode.has("parentId") ? valueNode.get("parentId").asText(null) : null;
      return parentCodelistId == null ? valueNode.get("id").asText(null) : parentCodelistId;
    };

    this.rootCodelistId = rootCodelistIdSupplier.get();
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

  public String getType() {
    return type;
  }

  public String getRootCodelistId() {
    return rootCodelistId;
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

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "SdkField [id=" + id + "]";
  }
}
