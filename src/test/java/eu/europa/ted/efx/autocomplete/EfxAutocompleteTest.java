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
package eu.europa.ted.efx.autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxAutocomplete;

/**
 * Pins the completion items of EFX-2, which are derived from the signature catalogue, so that a
 * change to the catalogue that the editors would see fails here.
 */
class EfxAutocompleteTest {

  @Test
  void testCompletions_OfSdk2_AreThePinnedList() throws IOException {
    assertEquals(String.join("\n", pinnedItems()),
        EfxAutocomplete.getEfxCompletions("2.0.0").stream()
            .map(EfxAutocompleteTest::render)
            .collect(Collectors.joining("\n")));
  }

  @Test
  void testCompletions_OfSdk1_AreNone() {
    assertTrue(EfxAutocomplete.getEfxCompletions("1.14.0").isEmpty());
  }

  /**
   * The lines of {@code sdk2-completions.txt} that are not comments.
   */
  private static List<String> pinnedItems() throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        EfxAutocompleteTest.class.getResourceAsStream("sdk2-completions.txt"),
        StandardCharsets.UTF_8))) {
      return reader.lines().filter(line -> !line.startsWith("#")).collect(Collectors.toList());
    }
  }

  /**
   * Writes an item as a line of {@code sdk2-completions.txt}.
   */
  private static String render(final CompletionItem item) {
    final String parameters = item.getParameters() == null ? ""
        : item.getParameters().stream()
            .map(EfxAutocompleteTest::render)
            .collect(Collectors.joining(", ", "(", ")"));
    return item.getKind() + " " + item.getLabel() + parameters + " -> " + item.getDataType();
  }

  private static String render(final EfxFunctionParameter parameter) {
    return parameter.getName() + " " + parameter.getDataType()
        + (parameter.isOptional() ? "?" : "") + (parameter.isVarargs() ? "..." : "");
  }
}
