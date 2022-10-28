package eu.europa.ted.efx.sdk0.v6.entity;

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
@SdkComponent(versions = {"0.6"}, componentType = SdkComponentType.CODELIST)
public class SdkCodelist06 extends SdkCodelist {

    public SdkCodelist06(String codelistId, String codelistVersion, List<String> codes, Optional<String> parentId) {
        super(codelistId, codelistVersion, codes, parentId);
    }
}
