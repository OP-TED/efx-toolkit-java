package eu.europa.ted.efx.mock;

import static java.util.Map.entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.sdk.SdkCodelist;
import eu.europa.ted.eforms.sdk.SdkField;
import eu.europa.ted.eforms.sdk.SdkNode;
import eu.europa.ted.eforms.xpath.XPathContextualizer;
import eu.europa.ted.efx.interfaces.SymbolMap;


public class MockSymbolMap implements SymbolMap {

    private static final Map<String, MockSymbolMap> instances = new HashMap<>();

    public static MockSymbolMap getInstance(final String sdkVersion) {
        return instances.computeIfAbsent(sdkVersion, k -> new MockSymbolMap());
    }

    protected Map<String, SdkField> fieldById;
    protected Map<String, SdkNode> nodeById;
    protected Map<String, SdkCodelist> codelistById;

    /**
     * Maps efx operators to xPath operators.
     */
    static final Map<String, String> operators = Map.ofEntries(entry("+", "+"), entry("-", "-"),
            entry("*", "*"), entry("/", "div"), entry("%", "mod"), entry("and", "and"),
            entry("or", "or"), entry("not", "not"), entry("==", "="), entry("!=", "!="),
            entry("<", "<"), entry("<=", "<="), entry(">", ">"), entry(">=", ">="));

    protected MockSymbolMap() {
        try {
            this.loadMapData();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private JsonNode fromString(final String jsonString)
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, JsonNode.class);
    }

    public void loadMapData() throws JsonMappingException, JsonProcessingException {
        this.fieldById = Map.ofEntries(entry("BT-01-text", new SdkField(fromString(
                "{\"id\":\"BT-01-text\",\"type\":\"text\",\"parentNodeId\":\"ND-0\",\"xpathAbsolute\":\"/*/text\",\"xpathRelative\":\"text\"}"))),
                entry("BT-02-indicator", new SdkField(fromString(
                        "{\"id\":\"BT-02-indicator\",\"type\":\"indicator\",\"parentNodeId\":\"ND-0\",\"xpathAbsolute\":\"/*/indicator\",\"xpathRelative\":\"indicator\"}"))),
                entry("BT-03-code", new SdkField(fromString(
                        "{\"id\":\"BT-03-code\",\"type\":\"code\",\"parentNodeId\":\"ND-0\",\"xpathAbsolute\":\"/*/code\",\"xpathRelative\":\"code\",\"codeList\":{\"value\":{\"id\":\"authority-activity\",\"type\":\"flat\",\"parentId\":\"main-activity\"}}}"))));

        this.nodeById = Map.ofEntries(entry("ND-0", new SdkNode("ND-0", null, "/*", "/*", false)));

        this.codelistById =
                new HashMap<>(Map.ofEntries(entry("accessibility", new SdkCodelist("accessibility",
                        "0.0.1", Arrays.asList("code1", "code2", "code3")))));
    }

    public SdkField getFieldById(String fieldId) {
        return this.fieldById.get(fieldId);
    }

    @Override
    public String contextPathOfField(String fieldId) {
        return absoluteXpathOfNode(parentNodeOfField(fieldId));
    }

    @Override
    public String contextPathOfField(String fieldId, String broaderContextPath) {
        return relativeXpathOfNode(parentNodeOfField(fieldId), broaderContextPath);
    }

    @Override
    public String relativeXpathOfField(String fieldId, String contextPath) {
        final String xpath = absoluteXpathOfField(fieldId);
        return XPathContextualizer.contextualize(contextPath, xpath);
    }

    @Override
    public String relativeXpathOfNode(String nodeId, String contextPath) {
        final String xpath = absoluteXpathOfNode(nodeId);
        return XPathContextualizer.contextualize(contextPath, xpath);
    }

    @Override
    public String typeOfField(String fieldId) {
        final SdkField sdkField = fieldById.get(fieldId);
        if (sdkField == null) {
            throw new InputMismatchException(String.format("Unknown field '%s'.", fieldId));
        }
        return sdkField.getType();
    }

    @Override
    public String rootCodelistOfField(String fieldId) {
        final SdkField sdkField = fieldById.get(fieldId);
        if (sdkField == null) {
            throw new InputMismatchException(String.format("Unknown field '%s'.", fieldId));
        }
        return sdkField.getRootCodelistId();
    }

    @Override
    public String expandCodelist(String codelistId) {
        SdkCodelist codelist = codelistById.get(codelistId);
        if (codelist == null) {
            throw new InputMismatchException(String.format("Codelist '%s' not found.", codelistId));
        }
        return codelist.toString(", ", "(", ")", '\'');
    }

    @Override
    public String mapOperator(String operator) {
        return MockSymbolMap.operators.get(operator);
    }

    /**
     * Gets the id of the parent node of a given field.
     *
     * @param fieldId The id of the field who's parent node we are looking for.
     * @return The id of the parent node of the given field.
     */
    public String parentNodeOfField(final String fieldId) {
        final SdkField sdkField = fieldById.get(fieldId);
        if (sdkField != null) {
            return sdkField.getParentNodeId();
        }
        throw new InputMismatchException(String.format("Unknown field '%s'", fieldId));
    }

    /**
     * @param fieldId The id of a field.
     * @return The xPath of the given field.
     */
    public String absoluteXpathOfField(final String fieldId) {
        final SdkField sdkField = fieldById.get(fieldId);
        if (sdkField == null) {
            throw new InputMismatchException(
                    String.format("Unknown field identifier '%s'.", fieldId));
        }
        return sdkField.getXpathAbsolute();
    }

    /**
     * @param nodeId The id of a node or a field.
     * @return The xPath of the given node or field.
     */
    public String absoluteXpathOfNode(final String nodeId) {
        final SdkNode sdkNode = nodeById.get(nodeId);
        if (sdkNode == null) {
            throw new InputMismatchException(
                    String.format("Unknown node identifier '%s'.", nodeId));
        }
        return sdkNode.getXpathAbsolute();
    }
}
