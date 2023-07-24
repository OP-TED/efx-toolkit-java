package eu.europa.ted.efx.mock.sdk1;

import static java.util.Map.entry;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import eu.europa.ted.efx.mock.AbstractSymbolResolverMock;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.sdk1.entity.SdkCodelistV1;
import eu.europa.ted.efx.sdk1.entity.SdkFieldV1;
import eu.europa.ted.efx.sdk1.entity.SdkNodeV1;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

public class SymbolResolverMockV1
    extends AbstractSymbolResolverMock<SdkFieldV1, SdkNodeV1, SdkCodelistV1> {

  public SymbolResolverMockV1() throws IOException {
    super();
  }

  private static Entry<String, SdkCodelistV1> buildCodelistMock(final String codelistId,
      final Optional<String> parentId) {
    return entry(codelistId, new SdkCodelistV1(codelistId, "0.0.1",
        Arrays.asList("code1", "code2", "code3"), parentId));
  }

  @Override
  protected Map<String, SdkNodeV1> createNodeById() {
    return Map.ofEntries(
        entry("ND-Root", new SdkNodeV1("ND-Root", null, "/*", "/*", false)),
        entry("ND-SubNode",
            new SdkNodeV1("ND-SubNode", "ND-Root", "/*/SubNode", "SubNode", false)));
  }

  @Override
  protected Map<String, SdkCodelistV1> createCodelistById() {
    return new HashMap<>(Map.ofEntries(
        buildCodelistMock("accessibility", Optional.empty()),
        buildCodelistMock("authority-activity", Optional.of("main-activity")),
        buildCodelistMock("main-activity", Optional.empty())));
  }

  @Override
  protected Class<SdkFieldV1> getSdkFieldClass() {
    return SdkFieldV1.class;
  }

  @Override
  protected String getFieldsJsonFilename() {
    return "fields-sdk1.json";
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
