package eu.europa.ted.efx.sdk1.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

@VersionDependentComponent(versions = {"1"}, componentType = VersionDependentComponentType.FIELD)
public class SdkFieldV1 extends SdkField {

  public SdkFieldV1(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkFieldV1(JsonNode field) {
    super(field);
  }
}
