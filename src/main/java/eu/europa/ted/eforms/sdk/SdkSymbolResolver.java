package eu.europa.ted.eforms.sdk;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.europa.ted.efx.exceptions.SymbolResolutionException;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.sdk.repository.SdkCodelistRepository;
import eu.europa.ted.eforms.sdk.repository.SdkFieldRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNodeRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNoticeTypeRepository;
import eu.europa.ted.eforms.sdk.resource.SdkResourceLoader;
import eu.europa.ted.eforms.xpath.XPathInfo;
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.path.NodePathExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.sdk2.entity.SdkFieldV2;
import eu.europa.ted.efx.sdk2.entity.SdkNodeV2;
import eu.europa.ted.efx.xpath.XPathContextualizer;

@SdkComponent(versions = { "1", "2" }, componentType = SdkComponentType.SYMBOL_RESOLVER)
public class SdkSymbolResolver implements SymbolResolver {
  protected Map<String, SdkField> fieldById;

  protected Map<String, SdkField> fieldByAlias;

  protected Map<String, SdkNode> nodeById;

  protected Map<String, SdkNode> nodeByAlias;

  protected Map<String, SdkCodelist> codelistById;

  protected Map<String, SdkNoticeSubtype> noticeTypesById;

  /**
   * Builds EFX list from the passed codelist reference. This will lazily compute
   * and cache the
   * result for reuse as the operation can be costly on some large lists.
   *
   * @param codelistId A reference to an SDK codelist.
   * @return The EFX string representation of the list of all the codes of the
   *         referenced codelist.
   */
  @Override
  public final List<String> expandCodelist(final String codelistId) {
    final SdkCodelist codelist = codelistById.get(codelistId);
    if (codelist == null) {
      throw SymbolResolutionException.unknownCodelist(codelistId);
    }
    return codelist.getCodes();
  }

  /**
   * Private, use getInstance method instead.
   *
   * @param sdkVersion  The version of the SDK.
   * @param sdkRootPath The path to the root of the SDK.
   * @throws InstantiationException If the SDK version is not supported.
   */
  public SdkSymbolResolver(final String sdkVersion, final Path sdkRootPath)
      throws InstantiationException {
    this.loadMapData(sdkVersion, sdkRootPath);
  }

  protected void loadMapData(final String sdkVersion, final Path sdkRootPath)
      throws InstantiationException {
    Path jsonPath = SdkResourceLoader.getResourceAsPath(sdkVersion,
        SdkConstants.SdkResource.FIELDS_JSON, sdkRootPath);
    Path codelistsPath = SdkResourceLoader.getResourceAsPath(sdkVersion,
        SdkConstants.SdkResource.CODELISTS, sdkRootPath);
    Path noticeTypesPath = SdkResourceLoader.getResourceAsPath(sdkVersion,
        SdkConstants.SdkResource.NOTICE_TYPES_JSON, sdkRootPath);

    this.fieldById = new SdkFieldRepository(sdkVersion, jsonPath);
    this.fieldByAlias = indexFieldsByAlias();
    this.nodeById = new SdkNodeRepository(sdkVersion, jsonPath);
    this.nodeByAlias = indexNodesByAlias();
    this.codelistById = new SdkCodelistRepository(sdkVersion, codelistsPath);
    this.noticeTypesById = new SdkNoticeTypeRepository(sdkVersion, noticeTypesPath);
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
    throw SymbolResolutionException.unknownField(fieldId);
  }

  /**
   * @param fieldId The id of a field.
   * @return The xPath of the given field.
   */
  @Override
  public PathExpression getAbsolutePathOfField(final String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownField(fieldId);
    }
    return PathExpression.instantiate(sdkField.getXpathAbsolute(), FieldTypes.fromString(sdkField.getType()));
  }

  /**
   * @param nodeId The id of a node or a field.
   * @return The xPath of the given node or field.
   */
  @Override
  public PathExpression getAbsolutePathOfNode(final String nodeId) {
    final SdkNode sdkNode = nodeById.get(nodeId);
    if (sdkNode == null) {
      throw SymbolResolutionException.unknownNode(nodeId);
    }
    return new NodePathExpression(sdkNode.getXpathAbsolute());
  }

  /**
   * Gets the xPath of the given field relative to the given context.
   *
   * @param fieldId     The id of the field for which we want to find the relative
   *                    xPath.
   * @param contextPath xPath indicating the context.
   * @return The xPath of the given field relative to the given context.
   */
  @Override
  public PathExpression getRelativePathOfField(String fieldId, PathExpression contextPath) {
    final PathExpression xpath = getAbsolutePathOfField(fieldId);
    return XPathContextualizer.contextualize(contextPath, xpath);
  }

  /**
   * Gets the xPath of the given node relative to the given context.
   *
   * @param nodeId      The id of the node for which we want to find the relative
   *                    xPath.
   * @param contextPath XPath indicating the context.
   * @return The XPath of the given node relative to the given context.
   */
  @Override
  public PathExpression getRelativePathOfNode(String nodeId, PathExpression contextPath) {
    final PathExpression xpath = getAbsolutePathOfNode(nodeId);
    return XPathContextualizer.contextualize(contextPath, xpath);
  }

  @Override
  public PathExpression getRelativePath(PathExpression absolutePath, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, absolutePath);
  }

  @Override
  public String getTypeOfField(String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownField(fieldId);
    }
    return sdkField.getType();
  }

  @Override
  public String getRootCodelistOfField(final String fieldId) {
    final SdkField sdkField = fieldById.get(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownField(fieldId);
    }
    final String codelistId = sdkField.getCodelistId();
    if (codelistId == null) {
      throw SymbolResolutionException.noCodelistForField(fieldId);
    }

    final SdkCodelist sdkCodelist = codelistById.get(codelistId);
    if (sdkCodelist == null) {
      throw SymbolResolutionException.unknownCodelist(codelistId);
    }

    return sdkCodelist.getRootCodelistId();
  }

  @Override
  public boolean isAttributeField(final String fieldId) {
    if (!additionalFieldInfoMap.containsKey(fieldId)) {
      this.cacheAdditionalFieldInfo(fieldId);
    }
    return additionalFieldInfoMap.get(fieldId).isAttribute();
  }

  @Override
  public String getAttributeNameFromAttributeField(final String fieldId) {
    if (!additionalFieldInfoMap.containsKey(fieldId)) {
      this.cacheAdditionalFieldInfo(fieldId);
    }
    return additionalFieldInfoMap.get(fieldId).getAttributeName();
  }

  @Override
  public PathExpression getAbsolutePathOfFieldWithoutTheAttribute(final String fieldId) {
    if (!additionalFieldInfoMap.containsKey(fieldId)) {
      this.cacheAdditionalFieldInfo(fieldId);
    }
    return Expression.instantiate(additionalFieldInfoMap.get(fieldId).getPathToLastElement(), NodePathExpression.class);
  }

  @Override
  public String getFieldIdFromAlias(String alias) {
    if (this.fieldByAlias.containsKey(alias)) {
      return this.fieldByAlias.get(alias).getId();
    }
    return null;
  }

  @Override
  public String getNodeIdFromAlias(String alias) {
    if (this.nodeByAlias.containsKey(alias)) {
      return this.nodeByAlias.get(alias).getId();
    }
    return null;
  }

  @Override
  public List<String> getAllNoticeSubtypeIds() {
    return noticeTypesById.keySet().stream().map(String::toUpperCase).sorted()
        .collect(Collectors.toList());
  }

  private HashMap<String, SdkField> indexFieldsByAlias() {
    return this.fieldById.values().stream()
      .filter(SdkFieldV2.class::isInstance)
      .map(SdkFieldV2.class::cast)
      .filter(field -> field.getAlias() != null)
      .collect(Collectors.toMap(
        SdkFieldV2::getAlias,
        Function.identity(),
        (existing, replacement) -> existing,
        HashMap::new
      ));
  }

  private HashMap<String, SdkNode> indexNodesByAlias() {
    return this.nodeById.values().stream()
      .filter(SdkNodeV2.class::isInstance)
      .map(SdkNodeV2.class::cast)
      .filter(node -> node.getAlias() != null)
      .collect(Collectors.toMap(
        SdkNodeV2::getAlias,
        Function.identity(),
        (existing, replacement) -> existing,
        HashMap::new
      ));
  }

  // #region Temporary helpers ------------------------------------------------

  /**
   * Caches the results of xpath parsing to mitigate performance impact.
   * This is a temporary solution until we move the additional info to the SdkField class.
   */
  Map<String, XPathInfo> additionalFieldInfoMap = new HashMap<>();

  private void cacheAdditionalFieldInfo(final String fieldId) {
    if (additionalFieldInfoMap.containsKey(fieldId)) {
      return;
    }
    XPathInfo xpathInfo = XPathProcessor.parse(this.getAbsolutePathOfField(fieldId).getScript());
    additionalFieldInfoMap.put(fieldId, xpathInfo);
  }

  // #endregion Temporary helpers ------------------------------------------------

}
