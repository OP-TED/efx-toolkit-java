package eu.europa.ted.efx.sdk0.v7.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

@VersionDependentComponent(versions = {"0.7"}, componentType = VersionDependentComponentType.FIELD)
public class SdkField07 extends SdkField {

  public SdkField07(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String codelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, codelistId);
  }

  public SdkField07(JsonNode field) {
    super(field);
  }
}
