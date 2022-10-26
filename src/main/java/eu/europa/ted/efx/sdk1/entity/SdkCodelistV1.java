package eu.europa.ted.efx.sdk1.entity;

import java.util.List;
import java.util.Optional;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
@VersionDependentComponent(versions = {"1"}, componentType = VersionDependentComponentType.CODELIST)
public class SdkCodelistV1 extends SdkCodelist {

  public SdkCodelistV1(final String codelistId, final String codelistVersion,
      final List<String> codes, final Optional<String> parentCodelistIdOpt) {
    super(codelistId, codelistVersion, codes, parentCodelistIdOpt);
  }
}
