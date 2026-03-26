/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.eforms.sdk;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.exceptions.SdkInconsistencyException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.entity.SdkDataType;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.sdk.repository.SdkCodelistRepository;
import eu.europa.ted.eforms.sdk.repository.SdkDataTypeRepository;
import eu.europa.ted.eforms.sdk.repository.SdkFieldRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNodeRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNoticeTypeRepository;
import eu.europa.ted.eforms.sdk.resource.SdkResourceLoader;
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.PrivacySetting;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.eforms.sdk.entity.v2.SdkFieldV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkNodeV2;
import eu.europa.ted.efx.xpath.XPathContextualizer;

@SdkComponent(versions = { "1", "2" }, componentType = SdkComponentType.SYMBOL_RESOLVER)
public class SdkSymbolResolver implements SymbolResolver {
  protected SdkFieldRepository fieldById;

  protected Map<String, SdkField> fieldByAlias;

  protected SdkNodeRepository nodeById;

  protected Map<String, SdkNode> nodeByAlias;

  protected Map<String, SdkCodelist> codelistById;

  protected Map<String, SdkNoticeSubtype> noticeTypesById;

  protected SdkDataTypeRepository dataTypeById;

  private SdkNode cachedRootNode;

  @Override
  public final List<String> expandCodelist(final String codelistId) {
    final SdkCodelist codelist = codelistById.get(codelistId);
    if (codelist == null) {
      throw SymbolResolutionException.unknownCodelist(codelistId);
    }
    return codelist.getCodes();
  }

  /**
   * Protected constructor for subclasses that load data differently (e.g., test mocks).
   * Subclasses must populate the field/node/codelist maps themselves.
   */
  protected SdkSymbolResolver() {
  }

  /**
   * Creates a symbol resolver by loading SDK metadata from the given path.
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

    // Load nodes first (fields depend on nodes for parent wiring)
    this.nodeById = new SdkNodeRepository(sdkVersion, jsonPath);
    this.nodeByAlias = indexNodesByAlias();

    // Load fields with parent node wiring
    this.fieldById = new SdkFieldRepository(sdkVersion, jsonPath, this.nodeById);
    this.fieldByAlias = indexFieldsByAlias();

    this.codelistById = new SdkCodelistRepository(sdkVersion, codelistsPath);
    this.noticeTypesById = new SdkNoticeTypeRepository(sdkVersion, noticeTypesPath);
    this.dataTypeById = SdkDataTypeRepository.createDefault();
  }

  @Override
  public String getParentNodeOfField(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField != null) {
      return sdkField.getParentNodeId();
    }
    throw SymbolResolutionException.unknownSymbol(fieldId);
  }

  /**
   * @param fieldId The id of a field.
   * @return The xPath of the given field as a {@link SequencePath} if the field
   *         is repeatable from root context, {@link ScalarPath} otherwise.
   */
  @Override
  public PathExpression getAbsolutePathOfField(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }
    return this.getAbsolutePathOfField(sdkField);
  }

  private PathExpression getAbsolutePathOfField(final SdkField sdkField) {
    FieldTypes fieldType = FieldTypes.fromString(sdkField.getType());
    if (this.isFieldRepeatableFromContext(sdkField, this.getRootNode())) {
      return SequencePath.instantiate(sdkField.getXpathAbsolute(), fieldType);
    } else {
      return ScalarPath.instantiate(sdkField.getXpathAbsolute(), fieldType);
    }
  }

  /**
   * @param nodeId The id of a node.
   * @return The xPath of the given node as a {@link NodeSequencePath} if
   *         the node is repeatable from root context, {@link NodePath} otherwise.
   */
  @Override
  public PathExpression getAbsolutePathOfNode(final String nodeId) {
    final SdkNode sdkNode = this.resolveNode(nodeId);
    if (sdkNode == null) {
      throw SymbolResolutionException.unknownSymbol(nodeId);
    }
    return this.getAbsolutePathOfNode(sdkNode);
  }

  private PathExpression getAbsolutePathOfNode(final SdkNode sdkNode) {
    if (this.isNodeRepeatableFromContext(sdkNode, this.getRootNode())) {
      return new NodeSequencePath(sdkNode.getXpathAbsolute());
    } else {
      return new NodePath(sdkNode.getXpathAbsolute());
    }
  }

  /**
   * Gets the xPath of the given field relative to the given context.
   *
   * @param fieldId     The id of the field for which we want to find the relative
   *                    xPath.
   * @param contextPath xPath indicating the context.
   * @return The xPath of the given field relative to the given context.
   * @deprecated Use {@link #getRelativePathOfField(String, String)} instead.
   */
  @Deprecated(forRemoval = true)
  @Override
  public PathExpression getRelativePathOfField(String fieldId, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, this.getAbsolutePathOfField(fieldId));
  }

  @Override
  public PathExpression getRelativePathOfField(String fieldId, String contextId) {
    SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }

    // null contextId means root context - return absolute path with correct type
    if (contextId == null) {
      return this.getAbsolutePathOfField(sdkField);
    }

    var context = this.resolveSymbol(contextId);
    if (context.isEmpty()) {
      throw SymbolResolutionException.unknownSymbol(contextId);
    }

    if (context.isField()) {
      return this.getRelativePathOfField(sdkField, context.field);
    }

    return this.getRelativePathOfField(sdkField, context.node);
  }

  private PathExpression getRelativePathOfField(SdkField sdkField, SdkNode context) {

    String relativeScript = XPathProcessor.contextualize(context.getXpathAbsolute(), sdkField.getXpathAbsolute());

    if (isFieldRepeatableFromContext(sdkField, context)) {
      return SequencePath.instantiate(relativeScript, FieldTypes.fromString(sdkField.getType()));
    } else {
      return ScalarPath.instantiate(relativeScript, FieldTypes.fromString(sdkField.getType()));
    }
  }

  private PathExpression getRelativePathOfField(SdkField sdkField, SdkField context) {
    String relativeScript = XPathProcessor.contextualize(context.getXpathAbsolute(), sdkField.getXpathAbsolute());

    if (this.isFieldRepeatableFromContext(sdkField, context)) {
      return SequencePath.instantiate(relativeScript, FieldTypes.fromString(sdkField.getType()));
    } else {
      return ScalarPath.instantiate(relativeScript, FieldTypes.fromString(sdkField.getType()));
    }
  }

  /**
   * Gets the xPath of the given node relative to the given context.
   *
   * @param nodeId      The id of the node for which we want to find the relative
   *                    xPath.
   * @param contextPath XPath indicating the context.
   * @return The XPath of the given node relative to the given context.
   * @deprecated Use {@link #getRelativePathOfNode(String, String)} instead.
   */
  @Deprecated(forRemoval = true)
  @Override
  public PathExpression getRelativePathOfNode(String nodeId, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, this.getAbsolutePathOfNode(nodeId));
  }

  @Override
  public PathExpression getRelativePathOfNode(String nodeId, String contextId) {
    SdkNode sdkNode = this.resolveNode(nodeId);
    if (sdkNode == null) {
      throw SymbolResolutionException.unknownSymbol(nodeId);
    }

    // null contextId means root context - return absolute path with correct type
    if (contextId == null) {
      return this.getAbsolutePathOfNode(sdkNode);
    }

    var context = this.resolveSymbol(contextId);
    if (context.isEmpty()) {
      throw SymbolResolutionException.unknownSymbol(contextId);
    }
    if (context.isField()) {
      return this.getRelativePathOfNode(sdkNode, context.field);
    }

    return this.getRelativePathOfNode(sdkNode, context.node);
  }

  private PathExpression getRelativePathOfNode(SdkNode sdkNode, SdkNode context) {
    String relativeScript = XPathProcessor.contextualize(context.getXpathAbsolute(), sdkNode.getXpathAbsolute());

    if (this.isNodeRepeatableFromContext(sdkNode, context)) {
      return new NodeSequencePath(relativeScript);
    } else {
      return new NodePath(relativeScript);
    }
  }

  private PathExpression getRelativePathOfNode(SdkNode sdkNode, SdkField context) {
    String relativeScript = XPathProcessor.contextualize(context.getXpathAbsolute(), sdkNode.getXpathAbsolute());

    if (this.isNodeRepeatableFromContext(sdkNode, context.getParentNode())) {
      return new NodeSequencePath(relativeScript);
    } else {
      return new NodePath(relativeScript);
    }
  }

  @Deprecated(forRemoval = true)
  @Override
  public PathExpression getRelativePath(PathExpression absolutePath, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, absolutePath);
  }

  @Override
  public String getTypeOfField(String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }

    // Temporary: the SDK does not yet distinguish duration from measure.
    // Both are "measure" in the SDK, but durations use the "duration-unit" codelist.
    // Remove this when the SDK adds "duration" as a proper data type.
    if (FieldTypes.MEASURE.getName().equals(sdkField.getType())) {
      SdkField unitCodeField = sdkField.getAttributeField("unitCode");
      if (unitCodeField != null && "duration-unit".equals(unitCodeField.getCodelistId())) {
        return FieldTypes.DURATION.getName();
      }
    }

    return sdkField.getType();
  }

  @Override
  public String getRootCodelistOfField(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
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
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }
    return sdkField.getXpathInfo().isAttribute();
  }

  @Override
  public String getAttributeNameFromAttributeField(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }
    return sdkField.getXpathInfo().getAttributeName();
  }

  @Override
  public PathExpression getAbsolutePathOfFieldWithoutTheAttribute(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }

    if (!sdkField.getXpathInfo().isAttribute()) {
      return this.getAbsolutePathOfField(sdkField);
    }

    String pathToElement = sdkField.getXpathInfo().getPathToLastElement();
    FieldTypes fieldType = FieldTypes.fromString(sdkField.getType());
    if (this.isFieldRepeatableFromContext(sdkField, this.getRootNode())) {
      return SequencePath.instantiate(pathToElement, fieldType);
    } else {
      return ScalarPath.instantiate(pathToElement, fieldType);
    }
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
  public List<SdkNoticeSubtype> getAllNoticeSubtypes() {
    return List.copyOf(this.noticeTypesById.values());
  }

  protected HashMap<String, SdkField> indexFieldsByAlias() {
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

  protected HashMap<String, SdkNode> indexNodesByAlias() {
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

  @Override
  public boolean isFieldRepeatableFromContext(final String fieldId, final String contextNodeId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }
    var contextNode = contextNodeId != null ? this.resolveNode(contextNodeId) : this.getRootNode();
    if (contextNode == null) {
      throw SymbolResolutionException.unknownSymbol(contextNodeId);
    }
    return this.isFieldRepeatableFromContext(sdkField, contextNode);
  }

  private boolean isFieldRepeatableFromContext(final SdkField sdkField, final SdkField context) {
    // If the field itself is repeatable, it returns multiple values UNLESS it IS the context
    // (e.g., inside a predicate on this field: BT-Repeatable[BT-Repeatable != ''])
    if (sdkField.isRepeatable()) {
      return !sdkField.equals(context);
    }

    // Multilingual fields have multiple XML elements (one per language)
    if (FieldTypes.TEXT_MULTILINGUAL.getName().equals(sdkField.getType())) {
      return true;
    }

    // Use cached ancestry from node
    List<String> contextAncestry = context != null
        ? context.getParentNode().getAncestry()
        : Collections.emptyList();

    // Walk up from the field's parent node toward root, looking for a repeatable
    // node
    String currentNodeId = sdkField.getParentNodeId();
    while (currentNodeId != null) {
      // Context boundary reached - the context node (even if repeatable) doesn't
      // count because we're positioned inside one instance of it. No repeatable node exists
      // between the field and context, so the field doesn't repeat from this context.
      if (contextAncestry.contains(currentNodeId)) {
        return false;
      }

      SdkNode node = nodeById.get(currentNodeId);
      if (node == null) {
        break;
      }

      // If this node is repeatable, the field returns multiple values from context
      if (node.isRepeatable()) {
        return true;
      }

      currentNodeId = node.getParentId();
    }

    return false;
  }

  private boolean isFieldRepeatableFromContext(final SdkField sdkField, final SdkNode context) {
    // If the field itself is repeatable, it returns multiple values
    if (sdkField.isRepeatable()) {
      return true;
    }

    // Multilingual fields have multiple XML elements (one per language)
    if (FieldTypes.TEXT_MULTILINGUAL.getName().equals(sdkField.getType())) {
      return true;
    }

    // Use cached ancestry from node
    List<String> contextAncestry = context != null
        ? context.getAncestry()
        : Collections.emptyList();

    // Walk up from the field's parent node toward root, looking for a repeatable
    // node
    String currentNodeId = sdkField.getParentNodeId();
    while (currentNodeId != null) {
      // Context boundary reached - the context node (even if repeatable) doesn't
      // count
      // because we're positioned inside one instance of it. No repeatable node exists
      // between the field and context, so the field doesn't repeat from this context.
      if (contextAncestry.contains(currentNodeId)) {
        return false;
      }

      SdkNode node = nodeById.get(currentNodeId);
      if (node == null) {
        break;
      }

      // If this node is repeatable, the field returns multiple values from context
      if (node.isRepeatable()) {
        return true;
      }

      currentNodeId = node.getParentId();
    }

    return false;
  }

  @Override
  public boolean isNodeRepeatableFromContext(final String nodeId, final String contextNodeId) {
    final SdkNode sdkNode = this.resolveNode(nodeId);
    if (sdkNode == null) {
      throw SymbolResolutionException.unknownSymbol(nodeId);
    }

    final SdkNode contextNode = contextNodeId != null
        ? this.resolveNode(contextNodeId)
        : this.getRootNode();
    if (contextNodeId != null && contextNode == null) {
      throw SymbolResolutionException.unknownSymbol(contextNodeId);
    }

    return this.isNodeRepeatableFromContext(sdkNode, contextNode);
  }

  private boolean isNodeRepeatableFromContext(final SdkNode sdkNode, final SdkNode contextNode) {
    // Use cached ancestry from node
    List<String> contextAncestry = contextNode != null
        ? contextNode.getAncestry()
        : Collections.emptyList();

    // Walk up from the node toward root, looking for a repeatable node
    String currentNodeId = sdkNode.getId();
    while (currentNodeId != null) {
      // Context boundary reached - the context node (even if repeatable) doesn't count
      // because we're positioned inside one instance of it. No repeatable node exists
      // between the target node and context, so it doesn't repeat from this context.
      if (contextAncestry.contains(currentNodeId)) {
        return false;
      }

      SdkNode node = this.nodeById.get(currentNodeId);
      if (node == null) {
        break;
      }

      // If this node is repeatable, return true
      if (node.isRepeatable()) {
        return true;
      }

      currentNodeId = node.getParentId();
    }

    return false;
  }

  @Override
  public String getRootNodeId() {
    return this.getRootNode().getId();
  }

  @Override
  public PathExpression getRootPath() {
    return new NodePath(this.getRootNode().getXpathAbsolute());
  }

  private SdkNode getRootNode() {
    if (this.cachedRootNode == null) {
      this.cachedRootNode = this.nodeById.values().stream()
          .filter(node -> node.getParentId() == null)
          .findFirst()
          .orElseThrow(SymbolResolutionException::rootNodeNotFound);
    }
    return this.cachedRootNode;
  }
  // #region Identifier Resolution ------------------------------------------------

  static class SdkEntity {
    final SdkNode node;
    final SdkField field;

    static SdkEntity EMPTY = new SdkEntity(null, null);

    static SdkEntity ofNode(SdkNode node) {
      return new SdkEntity(node, null);
    }

    static SdkEntity ofField(SdkField field) {
      return new SdkEntity(null, field);
    }

    private SdkEntity(SdkNode node, SdkField field) {
      this.node = node;
      this.field = field;
    }

    boolean isNode() {
      return this.node != null;
    }

    boolean isField() {
      return this.field != null;
    }

    boolean isEmpty() {
      return this.node == null && this.field == null;
    }
  }

  private SdkEntity resolveSymbol(String symbol) {
    if (symbol == null) {
      return SdkEntity.EMPTY;
    }

    SdkNode node = this.resolveNode(symbol);
    if (node != null) {
      return SdkEntity.ofNode(node);
    }

    SdkField field = this.resolveField(symbol);
    return SdkEntity.ofField(field);
  }

  private SdkField resolveField(String fieldId) {
    return Optional.ofNullable(this.fieldById.get(fieldId))
        .orElseGet(() -> this.fieldByAlias.get(fieldId));
  }

  private SdkNode resolveNode(String nodeId) {
    return Optional.ofNullable(this.nodeById.get(nodeId))
        .orElseGet(() -> this.nodeByAlias.get(nodeId));
  }

  // #endregion Identifier Resolution ------------------------------------------------

  @Override
  public String getPrivacyCodeOfField(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }

    return sdkField.getPrivacyCode();
  }

  @Override
  public String getPrivacySettingOfField(final String fieldId, final PrivacySetting privacyField) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }

    final SdkField.PrivacySettings privacy = sdkField.getPrivacySettings();
    if (privacy == null) {
      return null;
    }

    switch (privacyField) {
      case PRIVACY_CODE_FIELD:
        return privacy.getPrivacyCodeFieldId();
      case PUBLICATION_DATE_FIELD:
        return privacy.getPublicationDateFieldId();
      case JUSTIFICATION_CODE_FIELD:
        return privacy.getJustificationCodeFieldId();
      case JUSTIFICATION_DESCRIPTION_FIELD:
        return privacy.getJustificationDescriptionFieldId();
      default:
        throw TranslatorConfigurationException.unhandledPrivacySetting(privacyField);
    }
  }

  @Override
  public String getPrivacyMask(final String fieldId) {
    final SdkField sdkField = this.resolveField(fieldId);
    if (sdkField == null) {
      throw SymbolResolutionException.unknownSymbol(fieldId);
    }

    final SdkDataType dataType = this.dataTypeById.get(sdkField.getType());
    if (dataType == null) {
      throw SdkInconsistencyException.unknownDataType(sdkField.getType());
    }
    return dataType.getPrivacyMask();
  }

}
