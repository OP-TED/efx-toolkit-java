package eu.europa.ted.efx.sdk2.entity;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.sdk1.entity.SdkFieldV1;

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

  @JsonCreator
  public SdkFieldV2(
      @JsonProperty("id") final String id,
      @JsonProperty("type") final String type,
      @JsonProperty("parentNodeId") final String parentNodeId,
      @JsonProperty("xpathAbsolute") final String xpathAbsolute,
      @JsonProperty("xpathRelative") final String xpathRelative,
      @JsonProperty("codeList") final Map<String, Map<String, String>> codelist,
      @JsonProperty("alias") final String alias) {
    this(id, type, parentNodeId, xpathAbsolute, xpathRelative, getCodelistId(codelist), alias);
  }

  public String getAlias() {
    return alias;
  }
}
