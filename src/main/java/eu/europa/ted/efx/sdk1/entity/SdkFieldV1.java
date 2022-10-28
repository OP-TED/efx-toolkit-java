package eu.europa.ted.efx.sdk1.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkField;

@SdkComponent(versions = {"1"}, componentType = SdkComponentType.FIELD)
public class SdkFieldV1 extends SdkField {

  public SdkFieldV1(final String id, final String type, final String parentNodeId, final String xpathAbsolute, 
      final String xpathRelative, final String codelistId) {
    super(id, type, parentNodeId, xpathAbsolute, xpathRelative, codelistId);
  }

  public SdkFieldV1(final JsonNode field) {
    super(field);
  }
}
