package eu.europa.ted.efx.sdk0.v7.entity;

import java.util.List;
import java.util.Optional;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@VersionDependentComponent(versions = {"0.7"}, componentType = VersionDependentComponentType.CODELIST)
public class SdkCodelist07 extends SdkCodelist {

    public SdkCodelist07(String codelistId, String codelistVersion, List<String> codes, Optional<String> parentId) {
        super(codelistId, codelistVersion, codes,  parentId);
    }
}
