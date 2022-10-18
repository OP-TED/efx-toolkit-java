package eu.europa.ted.efx.sdk2.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.efx.sdk1.entity.SdkFieldV1;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

@VersionDependentComponent(versions = {"2"}, componentType = VersionDependentComponentType.FIELD)
public class SdkFieldV2 extends SdkFieldV1 {

  public SdkFieldV2(String id, String type, String parentNodeId, String xpathAbsolute,
      String xpathRelative, String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkFieldV2(JsonNode field) {
    super(field);
  }
}
