package eu.europa.ted.efx.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.europa.ted.efx.util.JavaTools;

public class EfxToXpathSymbols {

  /**
   * EfxToXpathSymbols is implemented as a "kind-of" singleton. One instance per version of the
   * eForms SDK.
   */
  private static Map<String, EfxToXpathSymbols> instances;

  /**
   * Gets the single instance containing the sysmbls definned in the given version of the eForms
   * SDK.
   *
   * @param sdkVersion
   * @return
   */
  public static EfxToXpathSymbols getInstance(final String sdkVersion) {
    if (instances == null) {
      instances = new HashMap<>();
    }
    if (!instances.containsKey(sdkVersion)) {
      instances.put(sdkVersion, new EfxToXpathSymbols(sdkVersion));
    }
    return instances.get(sdkVersion);
  }

  /**
   * Temporary flag used while we test the new conextualizer. Will be removed when we are done with
   * its implementation.
   */
  private boolean useRegexParse = false;

  /**
   * Sets a flag indicating weather the new contextualizer should be used in subsequent calls to
   * {@link #getRelativeXpathOfFieldOrNode(String, String)}.
   *
   * @param useNewContextualizer true if we want to use the new contextualizer, otherwise false
   */
  public void useNewContextualizer(final boolean useNewContextualizer) {
    this.useRegexParse = !useNewContextualizer;
  }

  private final Map<String, TedefoField> fieldByFieldId = new LinkedHashMap<>();
  private final Map<String, TedefoNode> nodeByNodeId = new LinkedHashMap<>();

  /**
   *
   * @param sdkVersion
   */
  public EfxToXpathSymbols(final String sdkVersion) {
    try {
      this.populateFieldByIdAndNodeByIdMaps(sdkVersion);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Unable to load Symbols for eForms-SDK %s", sdkVersion), e);
    }
  }

  /**
   * Gets the id of the parent node of a given field.
   *
   * @param fieldId The id of the field who's parent node we are looking for.
   * @return The id of the parent node of the given field.
   */
  String getParentNodeOfField(final String fieldId) {
    final TedefoField tedefoField = fieldByFieldId.get(fieldId);
    if (tedefoField != null) {
      return tedefoField.getParentNodeId();
    }
    return null;
  }

  /**
   * @param fieldOrNodeId The id of a node or a field.
   * @return The xPath of the given node or field.
   */
  String getXpathOfFieldOrNode(final String fieldOrNodeId) {
    // We assume NO node and field have the same id.
    final TedefoField tedefoField = fieldByFieldId.get(fieldOrNodeId);
    if (tedefoField != null) {
      return tedefoField.getXpathAbsolute();
    }
    final TedefoNode tedefoNode = nodeByNodeId.get(fieldOrNodeId);
    if (tedefoNode != null) {
      return tedefoNode.getXpathAbsolute();
    }
    return fieldOrNodeId; // TODO Or should it fail if not found ?
  }

  /**
   * Find the context of a rule that applies to a given field. The context of the rule applied to a
   * field, is typically the xPathAbsolute of that field's parent node.
   *
   * @param fieldId The id of the field of which we want to find the context.
   * @return The absolute xPath of the parent node of the passed field
   */
  String getContextPathOfField(String fieldId) {
    //
    // Algorithm to be implemented.
    // Find the field with the given id,
    // and return the xPathAbsolute of its parent node.
    //
    return getXpathOfFieldOrNode(getParentNodeOfField(fieldId)); // dummy implementation
  }

  /**
   * Find the context for nested predicate that applies to a given field taking into account the
   * pre-existing context.
   *
   * @param fieldId
   * @param broaderContextPath
   * @return
   */
  String getNarrowerContextPathOf(String fieldId, String broaderContextPath) {
    //
    // Algorithm to be implemented:
    //
    // First find the parent node of the given field
    // Then find the xPath of that parent node, relative to the given context
    // and return it.
    //
    return getRelativeXpathOfFieldOrNode(getParentNodeOfField(fieldId), broaderContextPath);
  }

  /**
   * Gets the xPath of the given field relative to the given context.
   *
   * @param fieldIdOrNodeId The id of the field for which we want to find the relative xPath
   * @param contextPath xPath indicating the context.
   * @return The xPath of the given field relative to the given context.
   */
  String getRelativeXpathOfFieldOrNode(String fieldIdOrNodeId, String contextPath) {
    final String xpath = getXpathOfFieldOrNode(fieldIdOrNodeId);
    if (this.useRegexParse) {
      return XpathTools.contextualizeFromXPath(xpath, contextPath);
    }
    return XpathContextualizer.contextualize(contextPath, xpath);
  }

  /**
   * @return A reusable Jackson object mapper instance.
   */
  private static ObjectMapper buildStandardJacksonObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    // https://fasterxml.github.io/jackson-annotations/javadoc/2.7/com/fasterxml/jackson/annotation/JsonInclude.Include.html

    // Value that indicates that only properties with non-null values are to be included.
    objectMapper.setSerializationInclusion(Include.NON_NULL);

    // Value that indicates that only properties with null value, or what is considered empty, are
    // not to be included.
    objectMapper.setSerializationInclusion(Include.NON_EMPTY);

    return objectMapper;
  }

  private static final String getTextNullOtherwise(final JsonNode node, final String key) {
    final JsonNode otherNode = node.get(key);
    if (otherNode == null) {
      return null;
    }
    return otherNode.asText(null);
  }

  /**
   *
   * @param sdkVersion Currently ignored. It will be effective in a later implementation.
   * @throws IOException
   * @throws URISyntaxException
   */
  private final void populateFieldByIdAndNodeByIdMaps(final String sdkVersion) throws IOException {
    System.out.println("Populating maps for context, sdkVersion=" + sdkVersion);
    final ObjectMapper objectMapper = buildStandardJacksonObjectMapper();
    final InputStream fieldsJsonInputStream = JavaTools.getResourceAsStream(
        EfxToXpathSymbols.class.getClassLoader(), "eforms-sdk/fields/fields.json");
    if (fieldsJsonInputStream == null) {
      throw new RuntimeException("Cannot find fields.json file.");
    }
    if (fieldsJsonInputStream.available() == 0) {
      throw new RuntimeException("Cannot find fields.json file (not available).");
    }
    final JsonNode json = objectMapper.readTree(fieldsJsonInputStream);

    // POPULATE FIELDS.
    {
      final ArrayNode fields = (ArrayNode) json.get("fields");
      for (final JsonNode field : fields) {
        final String id = field.get("id").asText(null);
        final String parentNodeId = getTextNullOtherwise(field, "parentNodeId");
        final String xpathAbsolute = field.get("xpathAbsolute").asText(null);
        final String xpathRelative = field.get("xpathRelative").asText(null);
        fieldByFieldId.put(id, new TedefoField(id, parentNodeId, xpathAbsolute, xpathRelative));
      }
    }
    if (fieldByFieldId.isEmpty()) {
      throw new RuntimeException("fieldByFieldId is empty!");
    }

    // POPULATE NODES.
    {
      final ArrayNode nodes = (ArrayNode) json.get("xmlStructure");
      for (final JsonNode node : nodes) {
        final String id = node.get("id").asText(null);
        final String parentId = getTextNullOtherwise(node, "parentId");
        final String xpathAbsolute = node.get("xpathAbsolute").asText(null);
        final String xpathRelative = node.get("xpathRelative").asText(null);
        final JsonNode jsonRep = node.get("repeatable");
        final boolean repeatable = jsonRep == null ? false : jsonRep.asBoolean(false);
        nodeByNodeId.put(id,
            new TedefoNode(id, parentId, xpathAbsolute, xpathRelative, repeatable));
      }
    }
    if (nodeByNodeId.isEmpty()) {
      throw new RuntimeException("nodeByNodeId is empty!");
    }
  }
}
