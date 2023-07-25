package eu.europa.ted.efx.sdk2.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.sdk1.entity.SdkNodeV1;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.NODE)
public class SdkNodeV2 extends SdkNodeV1 {
  private final String alias;

  public SdkNodeV2(String id, String parentId, String xpathAbsolute, String xpathRelative,
      boolean repeatable, String alias) {
    super(id, parentId, xpathAbsolute, xpathRelative, repeatable);
    this.alias = alias;
  }

  public SdkNodeV2(JsonNode node) {
    super(node);
    this.alias = node.has("alias") ? node.get("alias").asText(null) : null;
  }

  public String getAlias() {
    return alias;
  }
}
