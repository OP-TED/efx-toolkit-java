package eu.europa.ted.efx.model;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.eforms.sdk.factory.AbstractSdkObjectFactory;
import eu.europa.ted.efx.interfaces.SdkCodelist;
import eu.europa.ted.efx.interfaces.SdkField;
import eu.europa.ted.efx.interfaces.SdkNode;

public class EfxEntityFactory extends AbstractSdkObjectFactory {
  public static final EfxEntityFactory INSTANCE = new EfxEntityFactory();

  private EfxEntityFactory() {
    super();
  }

  public static SdkCodelist getSdkCodelist(final String sdkVersion, final String codelistId,
      final String codelistVersion, final List<String> codes) throws InstantiationException {
    return EfxEntityFactory.INSTANCE.getComponentImpl(sdkVersion, SdkComponentTypeEnum.CODELIST,
        SdkCodelist.class, codelistId, codelistVersion, codes);
  }

  public static SdkField getSdkField(String sdkVersion, JsonNode field)
      throws InstantiationException {
    return EfxEntityFactory.INSTANCE.getComponentImpl(sdkVersion, SdkComponentTypeEnum.FIELD,
        SdkField.class, field);
  }

  public static SdkNode getSdkNode(String sdkVersion, JsonNode node) throws InstantiationException {
    return EfxEntityFactory.INSTANCE.getComponentImpl(sdkVersion, SdkComponentTypeEnum.NODE,
        SdkNode.class, node);
  }
}
