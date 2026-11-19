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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

import eu.europa.ted.efx.EfxAutocomplete;

/**
 * Pins the completion items of EFX-2, which are derived from the signature catalogue, to the file
 * {@code sdk2-completions.json} that the jar ships for editors. A change to the catalogue that the
 * editors would see fails here, until the file is updated.
 *
 * <p>The file is a JSON array with one item per line. An item has a {@code label}, a {@code kind}
 * and a {@code dataType}; a function also has its {@code parameters}, each with a {@code name}, a
 * {@code dataType}, and whether it is {@code optional} and {@code variadic} (it repeats). Keywords
 * and properties have no {@code parameters}.</p>
 *
 * <p>Order: the functions in the order of the signature catalogue, {@code BuiltIns.getAll()}, enum
 * by enum (LogicalOperators, StringFunctions, NumericFunctions, DateTimeFunctions,
 * ConversionFunctions, SequenceFunctions, TemplateFunctions), each built-in's overloads in their
 * order, and an overload over T once per value type in the order T knows them (text, indicator,
 * number, date, time, duration); a form folded into a longer one, whose extra parameters it makes
 * optional, has no item of its own. Then the keywords and the properties, in the order of
 * {@link EfxKeyword} and {@link EfxLinkedProperty}.</p>
 *
 * <p>When the test fails, it also tries to write the file as it should be to {@code target/}, to be
 * copied over the one in {@code src/main/resources}. The failure message says where it is, or why
 * it could not be written.</p>
 */
class EfxAutocompleteTest {

  private static final String FILE_NAME = "sdk2-completions.json";

  private static final Path EXPECTED_FILE = Paths.get("target", FILE_NAME);

  @Test
  void testCompletions_OfSdk2_AreTheShippedFile() throws IOException {
    final List<CompletionItem> items = EfxAutocomplete.getEfxCompletions("2.0.0");
    final List<String> expected = lines(items);
    final List<String> actual = shippedLines();
    final String hint = expected.equals(actual) ? "" : export(expected);
    for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
      assertEquals(expected.get(i), actual.get(i), describeLine(i, items) + hint);
    }
    assertEquals(expected.size(), actual.size(), "Number of lines of " + FILE_NAME + hint);
  }

  @Test
  void testCompletions_OfSdk1_AreNone() {
    assertTrue(EfxAutocomplete.getEfxCompletions("1.14.0").isEmpty());
  }

  /**
   * Tries to write the file as it should be, so that a failure does not depend on it.
   *
   * @return the end of the failure message: where the file is, or why it could not be written
   */
  private static String export(final List<String> lines) {
    Path file = EXPECTED_FILE;
    try {
      file = EXPECTED_FILE.toAbsolutePath();
      Files.createDirectories(file.getParent());
      Files.write(file, lines, StandardCharsets.UTF_8);
      return "; the file as it should be is " + file;
    } catch (IOException | SecurityException e) {
      return "; the file as it should be could not be written to " + file + ": " + e;
    }
  }

  /**
   * The lines of {@code sdk2-completions.json} as the jar ships it.
   */
  private static List<String> shippedLines() throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        EfxAutocomplete.class.getResourceAsStream("autocomplete/" + FILE_NAME),
        StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.toList());
    }
  }

  /**
   * Names a line of {@code sdk2-completions.json}: line 2 holds the first item.
   */
  private static String describeLine(final int index, final List<CompletionItem> items) {
    final int item = index - 1;
    return item >= 0 && item < items.size()
        ? "Item " + item + " (" + items.get(item).getLabel() + ") of " + FILE_NAME
        : "Line " + (index + 1) + " of " + FILE_NAME;
  }

  /**
   * Writes the items as the lines of {@code sdk2-completions.json}.
   */
  private static List<String> lines(final List<CompletionItem> items) {
    final List<String> lines = new ArrayList<>();
    lines.add("[");
    for (int i = 0; i < items.size(); i++) {
      lines.add("  " + toJson(items.get(i)) + (i < items.size() - 1 ? "," : ""));
    }
    lines.add("]");
    return lines;
  }

  private static String toJson(final CompletionItem item) {
    final StringBuilder json = new StringBuilder("{")
        .append(field("label", quote(item.getLabel()))).append(", ")
        .append(field("kind", quote(item.getKind().name()))).append(", ")
        .append(field("dataType", quote(item.getDataType().name())));
    if (item.getParameters() != null) {
      json.append(", ").append(field("parameters", item.getParameters().stream()
          .map(EfxAutocompleteTest::toJson)
          .collect(Collectors.joining(", ", "[", "]"))));
    }
    return json.append('}').toString();
  }

  private static String toJson(final EfxFunctionParameter parameter) {
    return "{" + field("name", quote(parameter.getName())) + ", "
        + field("dataType", quote(parameter.getDataType().name())) + ", "
        + field("optional", String.valueOf(parameter.isOptional())) + ", "
        + field("variadic", String.valueOf(parameter.isVarargs())) + "}";
  }

  private static String field(final String name, final String value) {
    return quote(name) + ": " + value;
  }

  private static String quote(final String text) {
    return '"' + new String(JsonStringEncoder.getInstance().quoteAsString(text)) + '"';
  }
}
