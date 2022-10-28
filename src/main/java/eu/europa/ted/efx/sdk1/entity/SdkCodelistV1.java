package eu.europa.ted.efx.sdk1.entity;

import java.util.List;
import java.util.Optional;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@SdkComponent(versions = {"1"}, componentType = SdkComponentType.CODELIST)
public class SdkCodelistV1 extends SdkCodelist {

  public SdkCodelistV1(final String codelistId, final String codelistVersion, final List<String> codes, final Optional<String> parentId) {
    super(codelistId, codelistVersion, codes, parentId);
  }
}
