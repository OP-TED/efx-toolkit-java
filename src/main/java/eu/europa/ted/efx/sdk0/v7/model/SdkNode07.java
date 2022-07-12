package eu.europa.ted.efx.sdk0.v7.model;

import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.annotation.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.efx.model.SdkNodeBase;

/**
 * A node is something like a section. Nodes can be parents of other nodes or parents of fields.
 */
@SdkComponent(versions = {"0.7"}, componentType = SdkComponentTypeEnum.NODE)
public class SdkNode07 extends SdkNodeBase {

    public SdkNode07(String id, String parentId, String xpathAbsolute, String xpathRelative,
            boolean repeatable) {
        super(id, parentId, xpathAbsolute, xpathRelative, repeatable);
    }

    public SdkNode07(JsonNode node) {
        super(node);
    }
}
