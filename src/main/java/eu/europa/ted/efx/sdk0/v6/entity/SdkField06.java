package eu.europa.ted.efx.sdk0.v6.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkField;

@SdkComponent(versions = {"0.6"}, componentType = SdkComponentType.FIELD)
public class SdkField06 extends SdkField {

  public SdkField06(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String codelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, codelistId);
  }

  public SdkField06(JsonNode field) {
    super(field);
  }
}
