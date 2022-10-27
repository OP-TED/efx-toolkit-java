package eu.europa.ted.eforms.sdk.entity;

import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentFactory;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

public class SdkEntityFactory extends VersionDependentComponentFactory {
  public static final SdkEntityFactory INSTANCE = new SdkEntityFactory();

  private SdkEntityFactory() {
    super();
  }

  public static SdkCodelist getSdkCodelist(final String sdkVersion, final String codelistId,
      final String codelistVersion, final List<String> codes, final Optional<String> parentId) throws InstantiationException {
    return SdkEntityFactory.INSTANCE.getComponentImpl(sdkVersion, VersionDependentComponentType.CODELIST, 
        SdkCodelist.class, codelistId, codelistVersion, codes, parentId);
  }

  public static SdkField getSdkField(String sdkVersion, JsonNode field)
      throws InstantiationException {
    return SdkEntityFactory.INSTANCE.getComponentImpl(sdkVersion, VersionDependentComponentType.FIELD,
        SdkField.class, field);
  }

  public static SdkNode getSdkNode(String sdkVersion, JsonNode node) throws InstantiationException {
    return SdkEntityFactory.INSTANCE.getComponentImpl(sdkVersion, VersionDependentComponentType.NODE,
        SdkNode.class, node);
  }
}
