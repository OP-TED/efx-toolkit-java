package eu.europa.ted.efx.sdk2.entity;

import java.util.List;
import java.util.Optional;
import eu.europa.ted.efx.sdk1.entity.SdkCodelistV1;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.CODELIST)
public class SdkCodelistV2 extends SdkCodelistV1 {

    public SdkCodelistV2(final String codelistId, final String codelistVersion, final List<String> codes, final Optional<String> parentId) {
        super(codelistId, codelistVersion, codes, parentId);
    }
}
