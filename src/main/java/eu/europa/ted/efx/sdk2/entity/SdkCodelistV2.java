package eu.europa.ted.efx.sdk2.entity;

import java.util.List;
import eu.europa.ted.efx.sdk1.entity.SdkCodelistV1;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@VersionDependentComponent(versions = {"2"}, componentType = VersionDependentComponentType.CODELIST)
public class SdkCodelistV2 extends SdkCodelistV1 {

    public SdkCodelistV2(String codelistId, String codelistVersion, List<String> codes) {
        super(codelistId, codelistVersion, codes);
    }
}
