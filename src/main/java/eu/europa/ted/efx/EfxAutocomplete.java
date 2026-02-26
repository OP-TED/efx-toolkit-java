/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import eu.europa.ted.eforms.sdk.SdkVersion;
import eu.europa.ted.efx.autocomplete.CompletionItem;
import eu.europa.ted.efx.autocomplete.CompletionKind;
import eu.europa.ted.efx.autocomplete.EfxBuiltInFunction;
import eu.europa.ted.efx.autocomplete.EfxKeyword;
import eu.europa.ted.efx.autocomplete.EfxLinkedProperty;

/**
 * Exposes autocomplete suggestions for the EFX graphical UI editors.
 */
public class EfxAutocomplete {

  private static final List<CompletionItem> SDK2_ITEMS = buildSdk2Items();

  private EfxAutocomplete() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  private static List<CompletionItem> buildSdk2Items() {
    List<CompletionItem> items = new ArrayList<>();
    for (EfxBuiltInFunction f : EfxBuiltInFunction.values()) {
      items.add(new CompletionItem(f.getLabel(), CompletionKind.FUNCTION, f.getDataType(), f.getParameters()));
    }
    for (EfxKeyword k : EfxKeyword.values()) {
      items.add(new CompletionItem(k.getLabel(), CompletionKind.KEYWORD, k.getDataType(), null));
    }
    for (EfxLinkedProperty p : EfxLinkedProperty.values()) {
      items.add(new CompletionItem(p.getLabel(), CompletionKind.PROPERTY, p.getDataType(), null));
    }
    return Collections.unmodifiableList(items);
  }

  /**
   * Returns a complete list of built-in EFX language elements for the given SDK version.
   *
   * @param sdkVersion The SDK version (e.g. "1.14.0", "2.0.0", "eforms-sdk-2.0.0").
   * @return A list of autocomplete suggestions for functions, keywords, and properties.
   */
  public static List<CompletionItem> getEfxCompletions(String sdkVersion) {
    if ("2".equals(new SdkVersion(sdkVersion).getMajor())) {
      return SDK2_ITEMS;
    }
    return Collections.emptyList();
  }
}
