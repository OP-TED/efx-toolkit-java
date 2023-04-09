package eu.europa.ted.efx.sdk2.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.efx.sdk1.entity.SdkFieldV1;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;

@SdkComponent(versions = {"2"}, componentType = SdkComponentType.FIELD)
public class SdkFieldV2 extends SdkFieldV1 {
  private final String alias;

  public SdkFieldV2(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId, String alias) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
    this.alias = alias;
  }

  public SdkFieldV2(JsonNode fieldNode) {
    super(fieldNode);
    this.alias = fieldNode.has("alias") ? fieldNode.get("alias").asText(null) : null;
  }

  public String getAlias() {
    return alias;
  }
}
