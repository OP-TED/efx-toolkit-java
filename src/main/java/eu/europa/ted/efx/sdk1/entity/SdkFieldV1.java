package eu.europa.ted.efx.sdk1.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

@VersionDependentComponent(versions = {"1"}, componentType = VersionDependentComponentType.FIELD)
public class SdkFieldV1 extends SdkField {

  public SdkFieldV1(final String id, final String type, final String parentNodeId,
      final String xpathAbsolute, final String xpathRelative, final String rootCodelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, rootCodelistId);
  }

  public SdkFieldV1(final JsonNode field) {
    super(field);
  }

}
