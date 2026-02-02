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
package eu.europa.ted.efx.interfaces;

import java.util.List;

import eu.europa.ted.efx.model.expressions.PathExpression;

/**
 * A SymbolResolver is a mechanism used by EFX translators to resolve symbols.
 * 
 * EFX expressions contain references to eForms entities (fields, nodes or codelists). These
 * references are in the form of entity identifiers (a.k.a. symbols). The role of the
 * {@link SymbolResolver} is to provide further information on these referenced entities by looking
 * them up in a symbol dictionary (or repository). This symbol repository is typically the eForms
 * SDK itself.
 */
public interface SymbolResolver {

  /**
   * Gets the identifier of the parent node of the given field.
   * 
   * This information is typically retrieved directly from the eForms SDK.
   * 
   * @param fieldId The identifier of the field to look for.
   * @return The identifier of the parent node of the given field.
   */
  public String getParentNodeOfField(final String fieldId);

  /**
   * Gets the path that can be used to locate the given field in the data source, relative to
   * another given path.
   * 
   * The "path" points to a location in the data source. The path will be eventually used to
   * retrieve the data from the data source. Typically, the data source is an XML file, in which
   * case the path should be an XPath. If the data source is a JSON file, then the path should be a
   * JsonPath. If you intend to use a function call to retrieve the data from the data source then
   * that is what you should return as path. In general keep in mind that the path is used as target
   * language script.
   *
   * @param fieldId The identifier of the field to look for.
   * @param contextPath The path relative to which we expect to find the return value.
   * @return The path to the given field relative to the given contextPath.
   * @deprecated Use {@link #getRelativePathOfField(String, String)} instead.
   */
  @Deprecated(since = "2.0.0-alpha.6", forRemoval = true)
  public PathExpression getRelativePathOfField(final String fieldId,
      final PathExpression contextPath);

  /**
   * Gets the path that can be used to locate the given field in the data source, relative to the
   * given context (node or field).
   *
   * @param fieldId The identifier of the field to look for.
   * @param contextId The identifier of the context node or field. If a field ID is provided,
   *                  its parent node is used as the context.
   * @return The path to the given field relative to the given context.
   */
  public PathExpression getRelativePathOfField(final String fieldId, final String contextId);

  /**
   * Gets the path that can be used to locate the given node in the data source, relative to another
   * given path.
   *
   * See {@link getRelativePathOfField} for a description of the concept of "path".
   *
   * @param nodeId The identifier of the node to look for.
   * @param contextPath The path relative to which we expect to find the return value.
   * @return The path to the given node relative to the given context path.
   * @deprecated Use {@link #getRelativePathOfNode(String, String)} instead.
   */
  @Deprecated(since = "2.0.0-alpha.6", forRemoval = true)
  public PathExpression getRelativePathOfNode(final String nodeId,
      final PathExpression contextPath);

  /**
   * Gets the path that can be used to locate the given node in the data source, relative to the
   * given context (node or field).
   *
   * @param nodeId The identifier of the node to look for.
   * @param contextId The identifier of the context node or field. If a field ID is provided,
   *                  its parent node is used as the context.
   * @return The path to the given node relative to the given context.
   */
  public PathExpression getRelativePathOfNode(final String nodeId, final String contextId);

  /**
   * Converts an absolute path to a relative path based on the given context.
   *
   * @param absolutePath The absolute path to convert.
   * @param contextPath The context path to make the result relative to.
   * @return The path relative to the given context.
   * @deprecated Use {@link ScriptGenerator#contextualizePath(PathExpression, PathExpression)} instead.
   *             Path contextualization is target-language-specific and belongs on ScriptGenerator.
   */
  @Deprecated(forRemoval = true)
  public PathExpression getRelativePath(PathExpression absolutePath, PathExpression contextPath);

  /**
   * Gets the absolute path that can be used to locate a field in the data source.
   * 
   * See {@link getRelativePathOfField} for a description of the concept of "path".
   * 
   * @param fieldId The identifier of the field to look for.
   * @return The absolute path to the field as a PathExpression.
   */
  public PathExpression getAbsolutePathOfField(final String fieldId);

  /**
   * Gets the absolute path the can be used to locate a node in the data source.
   * 
   * See {@link getRelativePathOfField} for a description of the concept of "path".
   * 
   * @param nodeId The identifier of the node to look for.
   * @return The absolute path to the node as a PathExpression.
   */
  public PathExpression getAbsolutePathOfNode(final String nodeId);

  /**
   * Gets the type of the given field.
   * 
   * This information is typically retrieved directly from the eForms SDK.
   * 
   * @param fieldId The identifier of the field to look for.
   * @return The type of the field as a string.
   */
  public String getTypeOfField(final String fieldId);

  /**
   * Gets the codelist associated with the given field. If the codelist is a tailored codelist then
   * this method will return the identifier of its parent codelist.
   *
   * This information is typically retrieved directly from the eForms SDK.
   * 
   * @param fieldId The identifier of the field to look for.
   * @return The "root" codelist associated ith the given field.
   */
  public String getRootCodelistOfField(final String fieldId);

  /**
   * Gets a boolean value indicating whether or not the given field points to
   * an @attribute.
   * 
   * @param fieldId The identifier of the field to look for.
   * @return True if the field points to an @attribute, false otherwise.
   */
  public boolean isAttributeField(final String fieldId);

  /**
   * Gets the attribute name of the given field (if the field points to
   * an @attribute).
   * 
   * @param fieldId The identifier of the field to look for.
   * @return The attribute name of the given field, without the @ prefix, or an
   *         empty string if the field does not point to an @attribute.
   */
  public String getAttributeNameFromAttributeField(final String fieldId);

  /**
   * Gets the absolute path of the given field, without the attribute part.
   * This method is meant to be used with fields that point to an @attribute.
   * If the given field does not point to an @attribute then this method returns
   * the same as {@link #getAbsolutePathOfField}
   * 
   * @param fieldId The identifier of the field to look for.
   * @return The absolute path of the given field, without the attribute part.
   */
  public PathExpression getAbsolutePathOfFieldWithoutTheAttribute(final String fieldId);

  /**
   * Gets the list of all codes in a given codelist as a list of strings.
   * 
   * This information is typically retrieved directly from the eForms SDK.
   * 
   * @param codelistId The identifier of the codelist to expand.
   * @return The list of codes in the given codelist.
   */
  public List<String> expandCodelist(final String codelistId);

  /**
   * Gets a list of all valid notice subtype IDs.
   * Used for validating notice subtype references in EFX.
   *
   * @return List of notice type IDs (e.g., ["1", "2", ..., "40", "CEI", "E1", ..., "X02"])
   */
  public List<String> getAllNoticeSubtypeIds();

  /**
   * Resolves a field alias to its canonical field identifier.
   *
   * Returns null if the alias is not recognized as a field alias. This allows callers to implement
   * fallback logic (e.g., trying {@link #getNodeIdFromAlias} next). The translator throws
   * {@code SymbolResolutionException.unknownAlias()} only when both field and node lookups fail.
   *
   * @param alias The alias to resolve.
   * @return The canonical field ID, or null if the alias is not a known field alias.
   */
  public String getFieldIdFromAlias(final String alias);

  /**
   * Resolves a node alias to its canonical node identifier.
   *
   * Returns null if the alias is not recognized as a node alias. This allows callers to implement
   * fallback logic. The translator throws {@code SymbolResolutionException.unknownAlias()} only
   * when both field and node lookups fail.
   *
   * @param alias The alias to resolve.
   * @return The canonical node ID, or null if the alias is not a known node alias.
   */
  public String getNodeIdFromAlias(final String alias);

  /**
   * Determines if a field reference would return multiple values when evaluated from a given
   * context.
   *
   * A field is considered repeatable from a context if:
   * 1. The field itself is marked as repeatable, OR
   * 2. Any node between the field's parent and the context (exclusive) is repeatable
   *
   * @param fieldId The identifier of the field to check.
   * @param contextNodeId The identifier of the context node, or null for root context.
   * @return true if the field would return multiple values from the given context.
   */
  public boolean isFieldRepeatableFromContext(final String fieldId, final String contextNodeId);

  /**
   * Determines if a node reference would return multiple values when evaluated from a given
   * context.
   *
   * A node is considered repeatable from a context if:
   * 1. The node itself is marked as repeatable, OR
   * 2. Any ancestor node between the node and the context (exclusive) is repeatable
   *
   * @param nodeId The identifier of the node to check.
   * @param contextNodeId The identifier of the context node, or null for root context.
   * @return true if the node would return multiple values from the given context.
   */
  public boolean isNodeRepeatableFromContext(final String nodeId, final String contextNodeId);

  /**
   * Gets the identifier of the root node.
   *
   * @return The root node ID (e.g., "ND-Root").
   */
  public String getRootNodeId();

  /**
   * Gets the absolute path to the root node.
   *
   * @return The absolute path of the root node as a PathExpression.
   */
  public PathExpression getRootPath();
}
