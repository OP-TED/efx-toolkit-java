/*
 * Copyright 2026 European Union
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkDataType;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.exceptions.SdkInconsistencyException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.PrivacySetting;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.xpath.XPathContextualizer;

/**
 * A {@link SymbolResolver} that resolves eForms symbols to XPath expressions, without any
 * assumption about where the SDK metadata comes from.
 * <p>
 * All symbol resolution logic lives here. Obtaining the entities is left entirely to subclasses,
 * which implement the abstract methods of this class.
 * <p>
 * Subclasses may supply fields and nodes whose parent identifiers are populated but whose parent
 * object links have not yet been initialized. Resolution completes these links on demand through
 * {@link SdkField#setParentNode(SdkNode)} and {@link SdkNode#setParent(SdkNode)}, modifying the
 * supplied entities. All referenced parents must be available through {@link #nodeById(String)}.
 * This deferred initialization establishes the ancestry chain before calling
 * {@link SdkNode#getAncestry()}; subclasses must not expose incompletely linked nodes to ancestry
 * readers before then. Once initialized, the hierarchy must remain stable because ancestry is
 * cached on the entities and the root node is cached by this resolver.
 * <p>
 * This class does not synchronize parent initialization or cache access and provides no
 * thread-safety guarantee. Keep the resolver and its lazily initialized entities confined to one
 * thread, or externally synchronize access to the resolver and shared entities. Separate resolver
 * instances do not provide isolation if they share the same mutable entities.
 */
public abstract class XPathSymbolResolver implements SymbolResolver {

  private SdkNode cachedRootNode;

  // #region Loading strategy ------------------------------------------------

  /**
   * Looks up a field by its canonical identifier.
   *
   * @param fieldId The identifier of the field to look for.
   * @return The field, or null if no field has that identifier.
   */
  protected abstract SdkField fieldById(final String fieldId);

  /**
   * Looks up a node by its canonical identifier.
   *
   * @param nodeId The identifier of the node to look for.
   * @return The node, or null if no node has that identifier.
   */
  protected abstract SdkNode nodeById(final String nodeId);

  /**
   * Looks up a codelist by its identifier.
   *
   * @param codelistId The identifier of the codelist to look for.
   * @return The codelist, or null if no codelist has that identifier.
   */
  protected abstract SdkCodelist codelistById(final String codelistId);

  /**
   * Looks up a data type by its identifier, as used by {@link SdkField#getType()}.
   *
   * @param dataTypeId The name of the data type to look for.
   * @return The data type, or null if the name is not a known data type.
   */
  protected abstract SdkDataType dataTypeById(final String dataTypeId);

  /**
   * Looks up a field by its alias.
   *
   * @param alias The alias to look for.
   * @return The field, or null if the alias is not a known field alias.
   */
  protected abstract SdkField fieldByAlias(final String alias);

  /**
   * Looks up a node by its alias.
   *
   * @param alias The alias to look for.
   * @return The node, or null if the alias is not a known node alias.
   */
  protected abstract SdkNode nodeByAlias(final String alias);

  /**
   * All nodes known to this resolver, in any order.
   *
   * @return The complete node collection.
   */
  protected abstract Collection<SdkNode> allNodes();

  @Override
  public abstract List<SdkNoticeSubtype> getAllNoticeSubtypes();

  // #endregion Loading strategy ---------------------------------------------

  // #region Entity graph ----------------------------------------------------

  /**
   * The ancestry chain of the given node, from the node itself up to the root.
   * <p>
   * Fills in any missing parent link through {@link #nodeById(String)} and then defers to
   * {@link SdkNode#getAncestry()}, which computes the chain once and caches it on the entity.
   * Subclasses therefore do not need to wire the node graph before use.
   *
   * @param node The node whose ancestry we want.
   * @return The identifiers of the node and all of its ancestors.
   */
  protected List<String> ancestryOf(final SdkNode node) {
    if (node == null) {
      return Collections.emptyList();
    }

    // Wire before reading: setParent() invalidates the cached ancestry, so only touch links that
    // are genuinely missing. A root node legitimately has no parent, hence the parentId check.
    for (SdkNode current = node; current != null; current = current.getParent()) {
      if (current.getParent() == null && current.getParentId() != null) {
        current.setParent(this.nodeById(current.getParentId()));
      }
    }

    return node.getAncestry();
  }

  /**
   * The parent node of the given field.
   * <p>
   * Resolves a missing parent link on first access through {@link #nodeById(String)}
   * and stores it on the field using {@link SdkField#setParentNode(SdkNode)}.
   *
   * @param field The field whose parent node we want.
   * @return The parent node, or null if the field has no parent node.
   */
  protected SdkNode parentNodeOf(final SdkField field) {
    if (field == null) {
      return null;
    }

    if (field.getParentNode() == null && field.getParentNodeId() != null) {
      field.setParentNode(this.nodeById(field.getParentNodeId()));
    }

    return field.getParentNode();
  }

  // #endregion Entity graph -------------------------------------------------

  // #region Identifier resolution -------------------------------------------

  /**
   * Resolves a field identifier, falling back to an alias lookup.
   * <p>
   * First calls {@link #fieldById(String)} and falls back to {@link #fieldByAlias(String)}.
   * Override this method (together with {@link #resolveNode(String)}) to customize
   * symbol resolution.
   *
   * @param fieldId The identifier or alias of the field to look for.
   * @return The field, or null if the symbol is not a known field.
   */
  protected SdkField resolveField(final String fieldId) {
    return Optional.ofNullable(this.fieldById(fieldId))
        .orElseGet(() -> this.fieldByAlias(fieldId));
  }

  /**
   * Resolves a node identifier, falling back to an alias lookup.
   * <p>
   * First calls {@link #nodeById(String)} and falls back to {@link #nodeByAlias(String)}.
   * Override this method (together with {@link #resolveField(String)}) to customize
   * symbol resolution.
   *
   * @param nodeId The identifier or alias of the node to look for.
   * @return The node, or null if the symbol is not a known node.
   */
  protected SdkNode resolveNode(final String nodeId) {
    return Optional.ofNullable(this.nodeById(nodeId))
        .orElseGet(() -> this.nodeByAlias(nodeId));
  }

  private SdkEntity resolveSymbol(final String symbol) {
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

  /**
   * The root node, being the only node without a parent.
   * The root node is cached the first time it is resolved.
   *
   * @return The root node.
   */
  protected SdkNode getRootNode() {
    if (this.cachedRootNode == null) {
      this.cachedRootNode = this.allNodes().stream()
          .filter(node -> node.getParentId() == null)
          .findFirst()
          .orElseThrow(SymbolResolutionException::rootNodeNotFound);
    }
    return this.cachedRootNode;
  }

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

  // #endregion Identifier resolution ----------------------------------------

  // #region Symbol resolution -----------------------------------------------

  @Override
  public final List<String> expandCodelist(final String codelistId) {
    final SdkCodelist codelist = this.codelistById(codelistId);
    if (codelist == null) {
      throw SymbolResolutionException.unknownCodelist(codelistId);
    }
    return codelist.getCodes();
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

    if (this.isNodeRepeatableFromContext(sdkNode, this.parentNodeOf(context))) {
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

    final SdkCodelist sdkCodelist = this.codelistById(codelistId);
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
    final SdkField sdkField = this.fieldByAlias(alias);
    if (sdkField != null) {
      return sdkField.getId();
    }
    return null;
  }

  @Override
  public String getNodeIdFromAlias(String alias) {
    final SdkNode sdkNode = this.nodeByAlias(alias);
    if (sdkNode != null) {
      return sdkNode.getId();
    }
    return null;
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
        ? this.ancestryOf(this.parentNodeOf(context))
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

      SdkNode node = this.nodeById(currentNodeId);
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
    List<String> contextAncestry = this.ancestryOf(context);

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

      SdkNode node = this.nodeById(currentNodeId);
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
    List<String> contextAncestry = this.ancestryOf(contextNode);

    // Walk up from the node toward root, looking for a repeatable node
    String currentNodeId = sdkNode.getId();
    while (currentNodeId != null) {
      // Context boundary reached - the context node (even if repeatable) doesn't count
      // because we're positioned inside one instance of it. No repeatable node exists
      // between the target node and context, so it doesn't repeat from this context.
      if (contextAncestry.contains(currentNodeId)) {
        return false;
      }

      SdkNode node = this.nodeById(currentNodeId);
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

    final SdkDataType dataType = this.dataTypeById(sdkField.getType());
    if (dataType == null) {
      throw SdkInconsistencyException.unknownDataType(sdkField.getType());
    }
    return dataType.getPrivacyMask();
  }

  // #endregion Symbol resolution --------------------------------------------

}
