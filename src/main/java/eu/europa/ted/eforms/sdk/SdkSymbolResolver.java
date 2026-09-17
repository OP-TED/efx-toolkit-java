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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkDataType;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.sdk.entity.v2.SdkFieldV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkNodeV2;
import eu.europa.ted.eforms.sdk.repository.SdkCodelistRepository;
import eu.europa.ted.eforms.sdk.repository.SdkDataTypeRepository;
import eu.europa.ted.eforms.sdk.repository.SdkFieldRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNodeRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNoticeTypeRepository;
import eu.europa.ted.eforms.sdk.resource.SdkResourceLoader;

/**
 * A {@link XPathSymbolResolver} that loads its SDK metadata from an SDK directory on the
 * filesystem.
 * <p>
 * All symbol resolution logic lives in {@link XPathSymbolResolver}. What this class adds is the
 * loading strategy: the SDK repositories, the eager wiring of the entity graph, and the alias
 * indexes.
 */
@SdkComponent(versions = { "1", "2" }, componentType = SdkComponentType.SYMBOL_RESOLVER)
public class SdkSymbolResolver extends XPathSymbolResolver {
  protected SdkFieldRepository fieldById;

  protected Map<String, SdkField> fieldByAlias;

  protected SdkNodeRepository nodeById;

  protected Map<String, SdkNode> nodeByAlias;

  protected Map<String, SdkCodelist> codelistById;

  protected Map<String, SdkNoticeSubtype> noticeTypesById;

  protected SdkDataTypeRepository dataTypeById;

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
  protected SdkField fieldById(final String fieldId) {
    return this.fieldById.get(fieldId);
  }

  @Override
  protected SdkNode nodeById(final String nodeId) {
    return this.nodeById.get(nodeId);
  }

  @Override
  protected SdkCodelist codelistById(final String codelistId) {
    return this.codelistById.get(codelistId);
  }

  @Override
  protected SdkDataType dataTypeById(final String dataTypeId) {
    return this.dataTypeById.get(dataTypeId);
  }

  @Override
  protected SdkField fieldByAlias(final String alias) {
    return this.fieldByAlias.get(alias);
  }

  @Override
  protected SdkNode nodeByAlias(final String alias) {
    return this.nodeByAlias.get(alias);
  }

  @Override
  protected Collection<SdkNode> allNodes() {
    return this.nodeById.values();
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
}
