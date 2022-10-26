package eu.europa.ted.eforms.sdk.entity;

import java.util.Objects;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class SdkField implements Comparable<SdkField> {
  private final String id;
  private final String xpathAbsolute;
  private final String xpathRelative;
  private final String parentNodeId;
  private final String type;
  private final String codelistId;

  @SuppressWarnings("unused")
  private SdkField() {
    throw new UnsupportedOperationException();
  }

  public SdkField(final String id, final String type, final String parentNodeId,
      final String xpathAbsolute, final String xpathRelative, final String codelistId) {
    this.id = id;
    this.parentNodeId = parentNodeId;
    this.xpathAbsolute = xpathAbsolute;
    this.xpathRelative = xpathRelative;
    this.type = type;
    this.codelistId = codelistId;
  }

  public SdkField(final JsonNode field) {
    this.id = field.get("id").asText(null);
    this.parentNodeId = field.get("parentNodeId").asText(null);
    this.xpathAbsolute = field.get("xpathAbsolute").asText(null);
    this.xpathRelative = field.get("xpathRelative").asText(null);
    this.type = field.get("type").asText(null);
    this.codelistId = createCodelistId(field);
  }

  /**
   * @return The codelist id from json, it can be null.
   */
  @SuppressWarnings("static-method")
  protected String createCodelistId(final JsonNode field) {
    final Supplier<String> codelistIdSupplier = () -> {
      final JsonNode codelistNode = field.get("codeList");
      if (codelistNode == null) {
        return null;
      }

      final JsonNode valueNode = codelistNode.get("value");
      if (valueNode == null) {
        return null;
      }

      // Get the codelist id.
      return valueNode.get("id").asText(null);
    };

    return codelistIdSupplier.get();
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

  public String getCodelistId() {
    return codelistId;
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
