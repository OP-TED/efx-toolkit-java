package eu.europa.ted.efx.mock;

import static java.util.Map.entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.sdk1.entity.*;
import eu.europa.ted.efx.xpath.XPathContextualizer;

public class SymbolResolverMock implements SymbolResolver {

  private static final Map<String, SymbolResolverMock> instances = new HashMap<>();

  public static SymbolResolverMock getInstance(final String sdkVersion) {
    return instances.computeIfAbsent(sdkVersion, k -> new SymbolResolverMock());
  }

  protected Map<String, SdkField> fieldById;
  protected Map<String, SdkNode> nodeById;
  protected Map<String, SdkCodelist> codelistById;

  protected SymbolResolverMock() {
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
    this.fieldById = Map.ofEntries(//
        entry("BT-00-Text", new SdkField08(fromString(
            "{\"id\":\"BT-00-Text\",\"type\":\"text\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/TextField\",\"xpathRelative\":\"PathNode/TextField\"}"))),
        entry("BT-00-Attribute", new SdkField08(fromString(
            "{\"id\":\"BT-00-Attribute\",\"type\":\"text\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/TextField/@Attribute\",\"xpathRelative\":\"PathNode/TextField/@Attribute\"}"))),
        entry("BT-00-Indicator", new SdkField08(fromString(
            "{\"id\":\"BT-00-indicator\",\"type\":\"indicator\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/IndicatorField\",\"xpathRelative\":\"PathNode/IndicatorField\"}"))),
        entry("BT-00-Code", new SdkField08(fromString(
            "{\"id\":\"BT-00-Code\",\"type\":\"code\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/CodeField\",\"xpathRelative\":\"PathNode/CodeField\",\"codeList\":{\"value\":{\"id\":\"authority-activity\",\"type\":\"flat\",\"parentId\":\"main-activity\"}}}"))),
        entry("BT-00-Internal-Code", new SdkField08(fromString(
            "{\"id\":\"BT-00-Internal-Code\",\"type\":\"internal-code\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/InternalCodeField\",\"xpathRelative\":\"PathNode/CodeField\",\"codeList\":{\"value\":{\"id\":\"authority-activity\",\"type\":\"flat\",\"parentId\":\"main-activity\"}}}"))),
        entry("BT-00-Text-Multilingual", new SdkField08(fromString(
            "{\"id\":\"BT-00-Text-Multilingual\",\"type\":\"text-multilingual\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/TextMultilingualField\",\"xpathRelative\":\"PathNode/TextMultilingualField\"}}"))),
        entry("BT-00-StartDate", new SdkField08(fromString(
            "{\"id\":\"BT-00-StartDate\",\"type\":\"date\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/StartDateField\",\"xpathRelative\":\"PathNode/StartDateField\"}}"))),
        entry("BT-00-EndDate", new SdkField08(fromString(
            "{\"id\":\"BT-00-EndDate\",\"type\":\"date\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/EndDateField\",\"xpathRelative\":\"PathNode/EndDateField\"}}"))),
        entry("BT-00-StartTime", new SdkField08(fromString(
            "{\"id\":\"BT-00-StartTime\",\"type\":\"time\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/StartTimeField\",\"xpathRelative\":\"PathNode/StartTimeField\"}}"))),
        entry("BT-00-EndTime", new SdkField08(fromString(
            "{\"id\":\"BT-00-EndTime\",\"type\":\"time\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/EndTimeField\",\"xpathRelative\":\"PathNode/EndTimeField\"}}"))),
        entry("BT-00-Measure", new SdkField08(fromString(
            "{\"id\":\"BT-00-Measure\",\"type\":\"measure\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/MeasureField\",\"xpathRelative\":\"PathNode/MeasureField\"}}"))),
        entry("BT-00-Integer", new SdkField08(fromString(
            "{\"id\":\"BT-00-Integer\",\"type\":\"integer\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/IntegerField\",\"xpathRelative\":\"PathNode/IntegerField\"}}"))),
        entry("BT-00-Amount", new SdkField08(fromString(
            "{\"id\":\"BT-00-Amount\",\"type\":\"amount\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/AmountField\",\"xpathRelative\":\"PathNode/AmountField\"}}"))),
        entry("BT-00-Url", new SdkField08(fromString(
            "{\"id\":\"BT-00-Url\",\"type\":\"url\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/UrlField\",\"xpathRelative\":\"PathNode/UrlField\"}}"))),
        entry("BT-00-Zoned-Date", new SdkField08(fromString(
            "{\"id\":\"BT-00-Zoned-Date\",\"type\":\"zoned-date\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/ZonedDateField\",\"xpathRelative\":\"PathNode/ZonedDateField\"}}"))),
        entry("BT-00-Zoned-Time", new SdkField08(fromString(
            "{\"id\":\"BT-00-Zoned-Time\",\"type\":\"zoned-time\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/ZonedTimeField\",\"xpathRelative\":\"PathNode/ZonedTimeField\"}}"))),
        entry("BT-00-Id-Ref", new SdkField08(fromString(
            "{\"id\":\"BT-00-Id-Ref\",\"type\":\"id-ref\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/IdRefField\",\"xpathRelative\":\"PathNode/IdRefField\"}}"))),
        entry("BT-00-Number", new SdkField08(fromString(
            "{\"id\":\"BT-00-Number\",\"type\":\"number\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/NumberField\",\"xpathRelative\":\"PathNode/NumberField\"}}"))),
        entry("BT-00-Phone", new SdkField08(fromString(
            "{\"id\":\"BT-00-Phone\",\"type\":\"phone\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/PhoneField\",\"xpathRelative\":\"PathNode/PhoneField\"}}"))),
        entry("BT-00-Email", new SdkField08(fromString(
            "{\"id\":\"BT-00-Email\",\"type\":\"email\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/EmailField\",\"xpathRelative\":\"PathNode/EmailField\"}}"))),
        entry("BT-01-SubLevel-Text", new SdkField08(fromString(
            "{\"id\":\"BT-01-SubLevel-Text\",\"type\":\"text\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/ChildNode/SubLevelTextField\",\"xpathRelative\":\"PathNode/ChildNode/SubLevelTextField\"}}"))),
        entry("BT-01-SubNode-Text", new SdkField08(fromString(
            "{\"id\":\"BT-01-SubNode-Text\",\"type\":\"text\",\"parentNodeId\":\"ND-SubNode\",\"xpathAbsolute\":\"/*/SubNode/SubTextField\",\"xpathRelative\":\"SubTextField\"}}"))));

    this.nodeById =Map.ofEntries(//
        entry("ND-Root", new SdkNode08("ND-Root", null, "/*", "/*", false)),
        entry("ND-SubNode", new SdkNode08("ND-SubNode", "ND-Root", "/*/SubNode", "SubNode", false)));

    this.codelistById = new HashMap<>(Map.ofEntries(entry("accessibility",
        new SdkCodelist08("accessibility", "0.0.1", Arrays.asList("code1", "code2", "code3")))));
  }

  public SdkField getFieldById(String fieldId) {
    return this.fieldById.get(fieldId);
  }

  @Override
  public PathExpression getRelativePathOfField(String fieldId, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, getAbsolutePathOfField(fieldId));
  }

  @Override
  public PathExpression getRelativePathOfNode(String nodeId, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, getAbsolutePathOfNode(nodeId));
  }

  @Override
  public PathExpression getRelativePath(PathExpression absolutePath, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, absolutePath);
  }

  @Override
  public String getTypeOfField(String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(String.format("Unknown field '%s'.", fieldId));
    }
    return sdkField.getType();
  }

  @Override
  public String getRootCodelistOfField(String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(String.format("Unknown field '%s'.", fieldId));
    }
    return sdkField.getRootCodelistId();
  }

  @Override
  public List<String> expandCodelist(String codelistId) {
    SdkCodelist codelist = codelistById.get(codelistId);
    if (codelist == null) {
      throw new ParseCancellationException(String.format("Codelist '%s' not found.", codelistId));
    }
    return codelist.getCodes();
  }

  /**
   * Gets the id of the parent node of a given field.
   *
   * @param fieldId The id of the field who's parent node we are looking for.
   * @return The id of the parent node of the given field.
   */
  @Override
  public String getParentNodeOfField(final String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField != null) {
      return sdkField.getParentNodeId();
    }
    throw new ParseCancellationException(String.format("Unknown field '%s'", fieldId));
  }

  /**
   * @param fieldId The id of a field.
   * @return The xPath of the given field.
   */
  @Override
  public PathExpression getAbsolutePathOfField(final String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(
          String.format("Unknown field identifier '%s'.", fieldId));
    }
    return new PathExpression(sdkField.getXpathAbsolute());
  }

  /**
   * @param nodeId The id of a node or a field.
   * @return The xPath of the given node or field.
   */
  @Override
  public PathExpression getAbsolutePathOfNode(final String nodeId) {
    final SdkNode sdkNode = nodeById.get(nodeId);
    if (sdkNode == null) {
      throw new ParseCancellationException(String.format("Unknown node identifier '%s'.", nodeId));
    }
    return new PathExpression(sdkNode.getXpathAbsolute());
  }
}
