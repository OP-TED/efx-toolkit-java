package eu.europa.ted.efx.sdk2.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.sdk1.entity.SdkNoticeSubtypeV1;

/**
 * Represents a notice subtype from the SDK's notice-types.json file.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.NOTICE_TYPE)
public class SdkNoticeSubtypeV2 extends SdkNoticeSubtypeV1 {

  public SdkNoticeSubtypeV2(String subTypeId, String documentType, String type) {
    super(subTypeId, documentType, type);
  }

  public SdkNoticeSubtypeV2(JsonNode json) {
    super(json);
  }
}
