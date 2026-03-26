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
package eu.europa.ted.efx.mock.sdk2;

import static java.util.Map.entry;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import eu.europa.ted.eforms.sdk.SdkSymbolResolver;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.sdk.entity.v2.SdkCodelistV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkNoticeSubtypeV2;
import eu.europa.ted.eforms.sdk.repository.SdkDataTypeRepository;
import eu.europa.ted.eforms.sdk.repository.SdkFieldRepository;
import eu.europa.ted.eforms.sdk.repository.SdkNodeRepository;

@SdkComponent(versions = {"2"}, componentType = SdkComponentType.SYMBOL_RESOLVER, qualifier = "mock")
public class SymbolResolverMockV2 extends SdkSymbolResolver {

  private static final String SDK_VERSION = "eforms-sdk-2.0";
  private static final Path JSON_PATH = Path.of("src", "test", "resources", "json", "sdk2-fields.json");

  public SymbolResolverMockV2() throws InstantiationException {
    super();
    loadTestData();
  }

  private void loadTestData() throws InstantiationException {
    // Load nodes and fields using SDK repositories
    this.nodeById = new SdkNodeRepository(SDK_VERSION, JSON_PATH);
    this.nodeByAlias = indexNodesByAlias();

    this.fieldById = new SdkFieldRepository(SDK_VERSION, JSON_PATH, this.nodeById);
    this.fieldByAlias = indexFieldsByAlias();

    // Mock codelists
    this.codelistById = createMockCodelists();

    this.noticeTypesById = createMockNoticeTypes();

    this.dataTypeById = SdkDataTypeRepository.createDefault();
  }

  private static Entry<String, SdkCodelist> buildCodelistMock(final String codelistId,
      final Optional<String> parentId) {
    return entry(codelistId, new SdkCodelistV2(codelistId, "0.0.1",
        Arrays.asList("code1", "code2", "code3"), parentId));
  }

  private Map<String, SdkCodelist> createMockCodelists() {
    return new HashMap<>(Map.ofEntries(
        buildCodelistMock("accessibility", Optional.empty()),
        buildCodelistMock("authority-activity", Optional.of("main-activity")),
        buildCodelistMock("main-activity", Optional.empty()),
        buildCodelistMock("legal-basis-1", Optional.empty()),
        buildCodelistMock("indicator", Optional.empty())));
  }

  private Map<String, SdkNoticeSubtype> createMockNoticeTypes() {
    Map<String, SdkNoticeSubtype> map = new HashMap<>();
    for (String id : Arrays.asList("1", "2", "3", "4", "5", "9", "10", "11", "E1", "E2", "X01")) {
      map.put(id, new SdkNoticeSubtypeV2(id, "notice", "planning"));
    }
    return map;
  }
}
