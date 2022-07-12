package eu.europa.ted.efx.sdk0.v6.model;

import java.util.List;
import eu.europa.ted.eforms.sdk.annotation.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.efx.model.SdkCodelistBase;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@SdkComponent(versions = {"0.6"}, componentType = SdkComponentTypeEnum.CODELIST)
public class SdkCodelist06 extends SdkCodelistBase {

    public SdkCodelist06(String codelistId, String codelistVersion, List<String> codes) {
        super(codelistId, codelistVersion, codes);
    }
}
