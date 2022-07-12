package eu.europa.ted.efx.sdk0.v7.model;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.annotation.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.efx.model.SdkField;

@SdkComponent(versions = {"0.7"}, componentType = SdkComponentTypeEnum.FIELD)
public class SdkField07 extends SdkField {

  public SdkField07(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkField07(JsonNode field) {
    super(field);
  }
}
