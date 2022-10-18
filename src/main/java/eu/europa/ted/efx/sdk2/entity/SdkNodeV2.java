package eu.europa.ted.efx.sdk2.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.efx.sdk1.entity.SdkNodeV1;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
@VersionDependentComponent(versions = {"2"}, componentType = VersionDependentComponentType.NODE)
public class SdkNodeV2 extends SdkNodeV1 {

    public SdkNodeV2(String id, String parentId, String xpathAbsolute, String xpathRelative,
            boolean repeatable) {
        super(id, parentId, xpathAbsolute, xpathRelative, repeatable);
    }

    public SdkNodeV2(JsonNode node) {
        super(node);
    }
}
