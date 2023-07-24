package eu.europa.ted.efx.mock.sdk2;

import static java.util.Map.entry;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.europa.ted.efx.mock.AbstractSymbolResolverMock;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.sdk2.entity.SdkCodelistV2;
import eu.europa.ted.efx.sdk2.entity.SdkFieldV2;
import eu.europa.ted.efx.sdk2.entity.SdkNodeV2;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

public class SymbolResolverMockV2
    extends AbstractSymbolResolverMock<SdkFieldV2, SdkNodeV2, SdkCodelistV2> {
  protected Map<String, SdkFieldV2> fieldByAlias;
  protected Map<String, SdkNodeV2> nodeByAlias;

  public SymbolResolverMockV2() throws IOException {
    super();
  }

  private static Entry<String, SdkCodelistV2> buildCodelistMock(final String codelistId,
      final Optional<String> parentId) {
    return entry(codelistId, new SdkCodelistV2(codelistId, "0.0.1",
        Arrays.asList("code1", "code2", "code3"), parentId));
  }

  @Override
  public void loadMapData() throws IOException {
    super.loadMapData();

    this.fieldByAlias = this.fieldById.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getValue().getAlias(), e -> e.getValue()));

    this.nodeByAlias = this.nodeById.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getValue().getAlias(), e -> e.getValue()));
  }

  @Override
  public SdkFieldV2 getFieldById(final String fieldId) {
    return this.fieldById.containsKey(fieldId) ? this.fieldById.get(fieldId)
        : this.fieldByAlias.get(fieldId);
  }

  @Override
  public SdkNodeV2 getNodeById(final String nodeId) {
    return this.nodeById.containsKey(nodeId) ? this.nodeById.get(nodeId)
        : this.nodeByAlias.get(nodeId);
  }

  @Override
  protected Map<String, SdkNodeV2> createNodeById() {
    return Map.ofEntries(
        entry("ND-Root", new SdkNodeV2("ND-Root", null, "/*", "/*", false, "Root")),
        entry("ND-SubNode",
            new SdkNodeV2("ND-SubNode", "ND-Root", "/*/SubNode", "SubNode", false, "SubNode")));
  }

  @Override
  protected Map<String, SdkCodelistV2> createCodelistById() {
    return new HashMap<>(Map.ofEntries(
        buildCodelistMock("accessibility", Optional.empty()),
        buildCodelistMock("authority-activity", Optional.of("main-activity")),
        buildCodelistMock("main-activity", Optional.empty())));
  }

  @Override
  protected Class<SdkFieldV2> getSdkFieldClass() {
    return SdkFieldV2.class;
  }

  @Override
  protected String getFieldsJsonFilename() {
    return "fields-sdk2.json";
  }

  @Override
  public Boolean isAttributeField(final String fieldId) {
    return XPathAttributeLocator.findAttribute(this.getAbsolutePathOfField(fieldId)).hasAttribute();
  }

  @Override
  public String getAttributeOfField(String fieldId) {
      return XPathAttributeLocator.findAttribute(this.getAbsolutePathOfField(fieldId)).getAttribute();
  }

  @Override
  public PathExpression getAbsolutePathOfFieldWithoutTheAttribute(String fieldId) {
      return XPathAttributeLocator.findAttribute(this.getAbsolutePathOfField(fieldId)).getPath();
  }
}
