package eu.europa.ted.efx.sdk0.v8.entity;

import java.util.List;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.selector.component.SdkComponent;
import eu.europa.ted.eforms.sdk.selector.component.SdkComponentType;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@SdkComponent(versions = {"0.8"}, componentType = SdkComponentType.CODELIST)
public class SdkCodelist08 extends SdkCodelist {

    public SdkCodelist08(String codelistId, String codelistVersion, List<String> codes) {
        super(codelistId, codelistVersion, codes);
    }
}
