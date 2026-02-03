/*
 * Copyright 2023 European Union
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
package eu.europa.ted.efx.mock.sdk1;

import static java.util.Map.entry;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import eu.europa.ted.eforms.sdk.SdkSymbolResolver;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.v1.SdkCodelistV1;
import eu.europa.ted.eforms.sdk.repository.SdkFieldRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNodeRepository;

@SdkComponent(versions = {"1"}, componentType = SdkComponentType.SYMBOL_RESOLVER, qualifier = "mock")
public class SymbolResolverMockV1 extends SdkSymbolResolver {

  private static final String SDK_VERSION = "eforms-sdk-1.0";
  private static final Path JSON_PATH = Path.of("src", "test", "resources", "json", "sdk1-fields.json");

  public SymbolResolverMockV1() throws InstantiationException {
    super();
    loadTestData();
  }

  private void loadTestData() throws InstantiationException {
    // Load nodes and fields using SDK repositories
    this.nodeById = new SdkNodeRepository(SDK_VERSION, JSON_PATH);
    this.nodeByAlias = new HashMap<>();  // SDK1 doesn't support aliases

    this.fieldById = new SdkFieldRepository(SDK_VERSION, JSON_PATH, this.nodeById);
    this.fieldByAlias = new HashMap<>();  // SDK1 doesn't support aliases

    // Mock codelists
    this.codelistById = createMockCodelists();

    // Mock notice types - not needed, we override getAllNoticeSubtypeIds()
    this.noticeTypesById = new HashMap<>();
  }

  private static Entry<String, SdkCodelist> buildCodelistMock(final String codelistId,
      final Optional<String> parentId) {
    return entry(codelistId, new SdkCodelistV1(codelistId, "0.0.1",
        Arrays.asList("code1", "code2", "code3"), parentId));
  }

  private Map<String, SdkCodelist> createMockCodelists() {
    return new HashMap<>(Map.ofEntries(
        buildCodelistMock("accessibility", Optional.empty()),
        buildCodelistMock("authority-activity", Optional.of("main-activity")),
        buildCodelistMock("main-activity", Optional.empty())));
  }

  @Override
  public String getFieldIdFromAlias(String alias) {
    throw new UnsupportedOperationException("Alias resolution is not supported in SDK-1.");
  }

  @Override
  public String getNodeIdFromAlias(String alias) {
    throw new UnsupportedOperationException("Alias resolution is not supported in SDK-1.");
  }

  @Override
  public List<String> getAllNoticeSubtypeIds() {
    return Arrays.asList("1", "2", "3", "4", "5", "E1", "E2", "X01");
  }
}
