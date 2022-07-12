package eu.europa.ted.efx.sdk0.v7.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.selector.component.SdkComponent;
import eu.europa.ted.eforms.sdk.selector.component.SdkComponentType;

@SdkComponent(versions = {"0.7"}, componentType = SdkComponentType.FIELD)
public class SdkField07 extends SdkField {

  public SdkField07(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkField07(JsonNode field) {
    super(field);
  }
}
