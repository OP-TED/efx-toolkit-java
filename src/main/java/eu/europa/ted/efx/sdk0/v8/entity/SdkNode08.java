package eu.europa.ted.efx.sdk0.v8.entity;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.selector.component.SdkComponent;
import eu.europa.ted.eforms.sdk.selector.component.SdkComponentType;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
@SdkComponent(versions = {"0.7"}, componentType = SdkComponentType.NODE)
public class SdkNode08 extends SdkNode {

    public SdkNode08(String id, String parentId, String xpathAbsolute, String xpathRelative,
            boolean repeatable) {
        super(id, parentId, xpathAbsolute, xpathRelative, repeatable);
    }

    public SdkNode08(JsonNode node) {
        super(node);
    }
}
