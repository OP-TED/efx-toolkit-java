package eu.europa.ted.efx.sdk0.v6.model;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.annotation.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.efx.model.SdkField;

@SdkComponent(versions = {"0.6"}, componentType = SdkComponentTypeEnum.FIELD)
public class SdkField06 extends SdkField {

  public SdkField06(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkField06(JsonNode field) {
    super(field);
  }
}
