package eu.europa.ted.efx.sdk0.v8.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

@VersionDependentComponent(versions = {"0.8"}, componentType = VersionDependentComponentType.FIELD)
public class SdkField08 extends SdkField {

  public SdkField08(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkField08(JsonNode field) {
    super(field);
  }
}
