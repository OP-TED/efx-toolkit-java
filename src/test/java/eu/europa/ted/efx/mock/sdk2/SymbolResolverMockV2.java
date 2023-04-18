package eu.europa.ted.efx.mock.sdk2;

import static java.util.Map.entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.sdk2.entity.SdkCodelistV2;
import eu.europa.ted.efx.sdk2.entity.SdkFieldV2;
import eu.europa.ted.efx.sdk2.entity.SdkNodeV2;
import eu.europa.ted.efx.xpath.XPathContextualizer;

public class SymbolResolverMockV2 implements SymbolResolver {
  private static final Logger logger = LoggerFactory.getLogger(SymbolResolverMockV2.class);

  protected Map<String, SdkFieldV2> fieldById;
  protected Map<String, SdkFieldV2> fieldByAlias;
  protected Map<String, SdkNodeV2> nodeById;
  protected Map<String, SdkNodeV2> nodeByAlias;
  protected Map<String, SdkCodelistV2> codelistById;

  public SymbolResolverMockV2() {
    try {
      this.loadMapData();
    } catch (JsonProcessingException e) {
      logger.error(e.toString(), e);
    }
  }

  private static JsonNode fromString(final String jsonString)
      throws JsonMappingException, JsonProcessingException {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(jsonString, JsonNode.class);
  }

  public void loadMapData() throws JsonMappingException, JsonProcessingException {
    this.fieldById = Map.ofEntries(//
        entry("BT-00-Text", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Text\",\"alias\":\"text\",\"type\":\"text\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/TextField\",\"xpathRelative\":\"PathNode/TextField\"}"))),
        entry("BT-00-Attribute", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Attribute\",\"alias\":\"attribute\",\"type\":\"text\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/TextField/@Attribute\",\"xpathRelative\":\"PathNode/TextField/@Attribute\"}"))),
        entry("BT-00-Indicator", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-indicator\",\"alias\":\"indicator\",\"type\":\"indicator\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/IndicatorField\",\"xpathRelative\":\"PathNode/IndicatorField\"}"))),
        entry("BT-00-Code", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Code\",\"alias\":\"code\",\"type\":\"code\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/CodeField\",\"xpathRelative\":\"PathNode/CodeField\",\"codeList\":{\"value\":{\"id\":\"authority-activity\",\"type\":\"flat\",\"parentId\":\"main-activity\"}}}"))),
        entry("BT-00-Internal-Code", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Internal-Code\",\"alias\":\"internalCode\",\"type\":\"internal-code\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/InternalCodeField\",\"xpathRelative\":\"PathNode/CodeField\",\"codeList\":{\"value\":{\"id\":\"authority-activity\",\"type\":\"flat\",\"parentId\":\"main-activity\"}}}"))),
        entry("BT-00-CodeAttribute", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-CodeAttribute\",\"alias\":\"codeAttribute\",\"type\":\"code\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/CodeField/@attribute\",\"xpathRelative\":\"PathNode/CodeField/@attribute\",\"codeList\":{\"value\":{\"id\":\"authority-activity\",\"type\":\"flat\",\"parentId\":\"main-activity\"}}}"))),
        entry("BT-00-Text-Multilingual", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Text-Multilingual\",\"alias\":\"textMultilingual\",\"type\":\"text-multilingual\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/TextMultilingualField\",\"xpathRelative\":\"PathNode/TextMultilingualField\"}}"))),
        entry("BT-00-StartDate", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-StartDate\",\"alias\":\"startDate\",\"type\":\"date\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/StartDateField\",\"xpathRelative\":\"PathNode/StartDateField\"}}"))),
        entry("BT-00-EndDate", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-EndDate\",\"alias\":\"endDate\",\"type\":\"date\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/EndDateField\",\"xpathRelative\":\"PathNode/EndDateField\"}}"))),
        entry("BT-00-StartTime", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-StartTime\",\"alias\":\"startTime\",\"type\":\"time\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/StartTimeField\",\"xpathRelative\":\"PathNode/StartTimeField\"}}"))),
        entry("BT-00-EndTime", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-EndTime\",\"alias\":\"endTime\",\"type\":\"time\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/EndTimeField\",\"xpathRelative\":\"PathNode/EndTimeField\"}}"))),
        entry("BT-00-Measure", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Measure\",\"alias\":\"measure\",\"type\":\"measure\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/MeasureField\",\"xpathRelative\":\"PathNode/MeasureField\"}}"))),
        entry("BT-00-Integer", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Integer\",\"alias\":\"integer\",\"type\":\"integer\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/IntegerField\",\"xpathRelative\":\"PathNode/IntegerField\"}}"))),
        entry("BT-00-Amount", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Amount\",\"alias\":\"amount\",\"type\":\"amount\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/AmountField\",\"xpathRelative\":\"PathNode/AmountField\"}}"))),
        entry("BT-00-Url", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Url\",\"alias\":\"url\",\"type\":\"url\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/UrlField\",\"xpathRelative\":\"PathNode/UrlField\"}}"))),
        entry("BT-00-Zoned-Date", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Zoned-Date\",\"alias\":\"zonedDate\",\"type\":\"zoned-date\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/ZonedDateField\",\"xpathRelative\":\"PathNode/ZonedDateField\"}}"))),
        entry("BT-00-Zoned-Time", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Zoned-Time\",\"alias\":\"zonedTime\",\"type\":\"zoned-time\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/ZonedTimeField\",\"xpathRelative\":\"PathNode/ZonedTimeField\"}}"))),
        entry("BT-00-Id-Ref", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Id-Ref\",\"alias\":\"idRef\",\"type\":\"id-ref\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/IdRefField\",\"xpathRelative\":\"PathNode/IdRefField\"}}"))),
        entry("BT-00-Number", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Number\",\"alias\":\"number\",\"type\":\"number\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/NumberField\",\"xpathRelative\":\"PathNode/NumberField\"}}"))),
        entry("BT-00-Phone", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Phone\",\"alias\":\"phone\",\"type\":\"phone\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/PhoneField\",\"xpathRelative\":\"PathNode/PhoneField\"}}"))),
        entry("BT-00-Email", new SdkFieldV2(fromString(
            "{\"id\":\"BT-00-Email\",\"alias\":\"email\",\"type\":\"email\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/EmailField\",\"xpathRelative\":\"PathNode/EmailField\"}}"))),
        entry("BT-01-SubLevel-Text", new SdkFieldV2(fromString(
            "{\"id\":\"BT-01-SubLevel-Text\",\"alias\":\"subLevel_text\",\"type\":\"text\",\"parentNodeId\":\"ND-Root\",\"xpathAbsolute\":\"/*/PathNode/ChildNode/SubLevelTextField\",\"xpathRelative\":\"PathNode/ChildNode/SubLevelTextField\"}}"))),
        entry("BT-01-SubNode-Text", new SdkFieldV2(fromString(
            "{\"id\":\"BT-01-SubNode-Text\",\"alias\":\"subNode_text\",\"type\":\"text\",\"parentNodeId\":\"ND-SubNode\",\"xpathAbsolute\":\"/*/SubNode/SubTextField\",\"xpathRelative\":\"SubTextField\"}}"))));

    this.fieldByAlias = this.fieldById.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getValue().getAlias(), e -> e.getValue()));

    this.nodeById = Map.ofEntries(//
        entry("ND-Root", new SdkNodeV2("ND-Root", null, "/*", "/*", false, "Root")),
        entry("ND-SubNode",
            new SdkNodeV2("ND-SubNode", "ND-Root", "/*/SubNode", "SubNode", false, "SubNode")));

    this.nodeByAlias = this.nodeById.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getValue().getAlias(), e -> e.getValue()));

    this.codelistById = new HashMap<>(Map.ofEntries(
        buildCodelistMock("accessibility", Optional.empty()),
        buildCodelistMock("authority-activity", Optional.of("main-activity")),
        buildCodelistMock("main-activity", Optional.empty())));
  }

  private static Entry<String, SdkCodelistV2> buildCodelistMock(final String codelistId,
      final Optional<String> parentId) {
    return entry(codelistId, new SdkCodelistV2(codelistId, "0.0.1",
        Arrays.asList("code1", "code2", "code3"), parentId));
  }

  public SdkField getFieldById(String fieldId) {
    return this.fieldById.containsKey(fieldId) ? this.fieldById.get(fieldId)
        : this.fieldByAlias.get(fieldId);
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
    final SdkField sdkField = getFieldById(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(String.format("Unknown field '%s'.", fieldId));
    }
    return sdkField.getType();
  }

  @Override
  public String getRootCodelistOfField(final String fieldId) {
    final SdkField sdkField = getFieldById(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(String.format("Unknown field '%s'.", fieldId));
    }
    final String codelistId = sdkField.getCodelistId();
    if (codelistId == null) {
      throw new ParseCancellationException(String.format("No codelist for field '%s'.", fieldId));
    }

    final SdkCodelist sdkCodelist = codelistById.get(codelistId);
    if (sdkCodelist == null) {
      throw new ParseCancellationException(String.format("Unknown codelist '%s'.", codelistId));
    }

    return sdkCodelist.getRootCodelistId();
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
    final SdkField sdkField = getFieldById(fieldId);
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
    final SdkField sdkField = getFieldById(fieldId);
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
    final SdkNode sdkNode =
        nodeById.containsKey(nodeId) ? nodeById.get(nodeId) : nodeByAlias.get(nodeId);
    if (sdkNode == null) {
      throw new ParseCancellationException(String.format("Unknown node identifier '%s'.", nodeId));
    }
    return new PathExpression(sdkNode.getXpathAbsolute());
  }
}
