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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkDataType;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.sdk.entity.v1.SdkFieldV1;
import eu.europa.ted.eforms.sdk.entity.v1.SdkNodeV1;
import eu.europa.ted.eforms.sdk.entity.v2.SdkCodelistV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkFieldV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkNodeV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkNoticeSubtypeV2;
import eu.europa.ted.eforms.sdk.repository.SdkDataTypeRepository;
import eu.europa.ted.efx.interfaces.SymbolResolver;

/** Runs the shared contract with freshly loaded, unwired entities. */
class XPathSymbolResolverTest extends SymbolResolverContractTest {

  private static final Path FIXTURE_JSON =
      Path.of("src", "test", "resources", "json", "sdk2-fields.json");

  // Shared with the hook tests without inheriting the contract test lifecycle.
  static class MapBackedSymbolResolver extends XPathSymbolResolver {
    private final Map<String, SdkField> fields = new LinkedHashMap<>();
    private final Map<String, SdkNode> nodes = new LinkedHashMap<>();
    private final Map<String, SdkCodelist> codelists = new LinkedHashMap<>();
    private final Map<String, SdkField> fieldAliases = new LinkedHashMap<>();
    private final Map<String, SdkNode> nodeAliases = new LinkedHashMap<>();
    private final List<SdkNoticeSubtype> noticeSubtypes = new ArrayList<>();
    private final SdkDataTypeRepository dataTypes = SdkDataTypeRepository.createDefault();

    void addNode(String id, String parentId, String xpathAbsolute, boolean repeatable) {
      this.nodes.put(id, new SdkNodeV1(id, parentId, xpathAbsolute, xpathAbsolute, repeatable));
    }

    void addField(String id, String type, String parentNodeId, String xpathAbsolute,
        boolean repeatable) {
      this.fields.put(id,
          new SdkFieldV1(id, type, parentNodeId, xpathAbsolute, xpathAbsolute, null, repeatable));
    }

    void addCodelist(String id, String parentId, String... codes) {
      this.codelists.put(id,
          new SdkCodelistV2(id, "0.0.1", List.of(codes), Optional.ofNullable(parentId)));
    }

    void addFieldAlias(String alias, String fieldId) {
      this.fieldAliases.put(alias, this.fields.get(fieldId));
    }

    void addNodeAlias(String alias, String nodeId) {
      this.nodeAliases.put(alias, this.nodes.get(nodeId));
    }

    void loadFixture() throws IOException {
      final JsonNode json = new ObjectMapper().readTree(FIXTURE_JSON.toFile());

      StreamSupport.stream(json.get("xmlStructure").spliterator(), false)
          .map(SdkNodeV2::new)
          .forEach(node -> {
            this.nodes.put(node.getId(), node);
            if (node.getAlias() != null) {
              this.nodeAliases.putIfAbsent(node.getAlias(), node);
            }
          });

      StreamSupport.stream(json.get("fields").spliterator(), false)
          .map(SdkFieldV2::new)
          .forEach(field -> {
            this.fields.put(field.getId(), field);
            if (field.getAlias() != null) {
              this.fieldAliases.putIfAbsent(field.getAlias(), field);
            }
          });
    }

    @Override
    protected SdkField fieldById(String fieldId) {
      return this.fields.get(fieldId);
    }

    @Override
    protected SdkNode nodeById(String nodeId) {
      return this.nodes.get(nodeId);
    }

    @Override
    protected SdkCodelist codelistById(String codelistId) {
      return this.codelists.get(codelistId);
    }

    @Override
    protected SdkDataType dataTypeById(String dataTypeId) {
      return this.dataTypes.get(dataTypeId);
    }

    @Override
    protected SdkField fieldByAlias(String alias) {
      // The contract is to return null for an unknown alias, not to throw.
      return this.fieldAliases.get(alias);
    }

    @Override
    protected SdkNode nodeByAlias(String alias) {
      return this.nodeAliases.get(alias);
    }

    @Override
    protected Collection<SdkNode> allNodes() {
      return this.nodes.values();
    }

    @Override
    public List<SdkNoticeSubtype> getAllNoticeSubtypes() {
      return List.copyOf(this.noticeSubtypes);
    }

    // Exposed so the tests can assert on the graph helpers directly.
    List<String> ancestry(String nodeId) {
      return this.ancestryOf(this.nodeById(nodeId));
    }

    SdkNode parentNode(String fieldId) {
      return this.parentNodeOf(this.fieldById(fieldId));
    }
  }

  @Override
  protected SymbolResolver createResolver() throws IOException {
    return newFixtureResolver();
  }

  static MapBackedSymbolResolver newFixtureResolver() throws IOException {
    MapBackedSymbolResolver loaded = new MapBackedSymbolResolver();
    loaded.loadFixture();
    // Supply the same codelists and notice subtypes as SymbolResolverMockV2, using new entities.
    loaded.addCodelist("accessibility", null, "code1", "code2", "code3");
    loaded.addCodelist("authority-activity", "main-activity", "code1", "code2", "code3");
    loaded.addCodelist("main-activity", null, "code1", "code2", "code3");
    loaded.addCodelist("legal-basis-1", null, "code1", "code2", "code3");
    loaded.addCodelist("indicator", null, "code1", "code2", "code3");
    for (String id : List.of("1", "2", "3", "4", "5", "9", "10", "11", "E1", "E2", "X01")) {
      loaded.noticeSubtypes.add(new SdkNoticeSubtypeV2(id, "notice", "planning"));
    }
    return loaded;
  }

}
