package eu.europa.ted.efx.sdk1.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;

/**
 * Represents a notice subtype from the SDK's notice-types.json file.
 */
@SdkComponent(versions = {"1"}, componentType = SdkComponentType.NOTICE_TYPE)
public class SdkNoticeSubtypeV1 extends SdkNoticeSubtype {

  public SdkNoticeSubtypeV1(String subTypeId, String documentType, String type) {
    super(subTypeId, documentType, type);
  }

  public SdkNoticeSubtypeV1(JsonNode json) {
    super(json);
  }
}
