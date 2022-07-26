package eu.europa.ted.efx.sdk0.v6.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
@VersionDependentComponent(versions = {"0.6"}, componentType = VersionDependentComponentType.NODE)
public class SdkNode06 extends SdkNode {

    public SdkNode06(String id, String parentId, String xpathAbsolute, String xpathRelative,
            boolean repeatable) {
        super(id, parentId, xpathAbsolute, xpathRelative, repeatable);
    }

    public SdkNode06(JsonNode node) {
        super(node);
    }
}
