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
 * the Lic
 */
package eu.europa.ted.efx.interfaces;

import java.util.List;
import eu.europa.ted.efx.model.Expression.PathExpression;

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
   */
  public PathExpression getRelativePathOfField(final String fieldId,
      final PathExpression contextPath);

  /**
   * Gets the path that can be used to locate the given node in the data source, relative to another
   * given path.
   * 
   * See {@link getRelativePathOfField} for a description of the concept of "path".
   * 
   * @param nodeId The identifier of the node to look for.
   * @param contextPath The path relative to which we expect to find the return value.
   * @return The path to the given node relative to the given context path.
   */
  public PathExpression getRelativePathOfNode(final String nodeId,
      final PathExpression contextPath);

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
   * Gets the list of all codes in a given codelist as a list of strings.
   * 
   * This information is typically retrieved directly from the eForms SDK.
   * 
   * @param codelistId The identifier of the codelist to expand.
   * @return The list of codes in the given codelist.
   */
  public List<String> expandCodelist(final String codelistId);
}
