package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;

class IncludeProcessorTest {

  /**
   * Simple in-memory resolver backed by a map.
   */
  private static IncludedFileResolver mapResolver(Map<String, String> files) {
    return path -> {
      String content = files.get(path);
      if (content == null) {
        throw new IOException("File not found: " + path);
      }
      return content;
    };
  }

  @Test
  void testNoIncludes_PassesThrough() throws IOException {
    String input = "---- STAGE 1a ----\nWITH ND-Root\n";
    IncludeProcessor processor = new IncludeProcessor(null);
    assertEquals(input, processor.process(input));
  }

  @Test
  void testNoResolver_WithInclude_ThrowsError() {
    String input = "#include \"extra-rules.efx\"\n---- STAGE 1a ----\n";
    IncludeProcessor processor = new IncludeProcessor(null);
    assertThrows(TranslatorConfigurationException.class, () -> processor.process(input));
  }

  @Test
  void testSingleInclude_SubstitutesContent() throws IOException {
    Map<String, String> files = new HashMap<>();
    files.put("extra.efx", "---- STAGE 2a ----\n");

    String input = "---- STAGE 1a ----\n#include \"extra.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    String result = processor.process(input);

    assertEquals("---- STAGE 1a ----\n---- STAGE 2a ----\n\n", result);
  }

  @Test
  void testMultipleIncludes_SubstitutesAll() throws IOException {
    Map<String, String> files = new HashMap<>();
    files.put("a.efx", "// content A\n");
    files.put("b.efx", "// content B\n");

    String input = "#include \"a.efx\"\n#include \"b.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    String result = processor.process(input);

    assertEquals("// content A\n\n// content B\n\n", result);
  }

  @Test
  void testRecursiveIncludes_ResolvesTransitively() throws IOException {
    Map<String, String> files = new HashMap<>();
    files.put("a.efx", "#include \"b.efx\"\n// from A\n");
    files.put("b.efx", "// from B\n");

    String input = "#include \"a.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    String result = processor.process(input);

    assertEquals("// from B\n\n// from A\n\n", result);
  }

  @Test
  void testCircularInclude_ThrowsError() {
    Map<String, String> files = new HashMap<>();
    files.put("a.efx", "#include \"b.efx\"\n");
    files.put("b.efx", "#include \"a.efx\"\n");

    String input = "#include \"a.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    assertThrows(InvalidUsageException.class, () -> processor.process(input));
  }

  @Test
  void testSelfInclude_ThrowsError() {
    Map<String, String> files = new HashMap<>();
    files.put("self.efx", "#include \"self.efx\"\n");

    String input = "#include \"self.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    assertThrows(InvalidUsageException.class, () -> processor.process(input));
  }

  @Test
  void testIncludeWithComment_IsRecognized() throws IOException {
    Map<String, String> files = new HashMap<>();
    files.put("extra.efx", "// included\n");

    String input = "#include \"extra.efx\" // hand-written rules\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    String result = processor.process(input);

    assertEquals("// included\n\n", result);
  }

  @Test
  void testIncludeWithLeadingWhitespace_IsRecognized() throws IOException {
    Map<String, String> files = new HashMap<>();
    files.put("extra.efx", "// included\n");

    String input = "  #include \"extra.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    String result = processor.process(input);

    assertEquals("// included\n\n", result);
  }

  @Test
  void testIncludeBetweenContent_PreservesContext() throws IOException {
    Map<String, String> files = new HashMap<>();
    files.put("middle.efx", "// middle\n");

    String input = "// before\n#include \"middle.efx\"\n// after\n";
    IncludeProcessor processor = new IncludeProcessor(mapResolver(files));
    String result = processor.process(input);

    assertEquals("// before\n// middle\n\n// after\n", result);
  }

  @Test
  void testUnresolvablePath_ThrowsIOException() {
    IncludedFileResolver resolver = path -> {
      throw new IOException("Not found: " + path);
    };

    String input = "#include \"missing.efx\"\n";
    IncludeProcessor processor = new IncludeProcessor(resolver);
    assertThrows(IOException.class, () -> processor.process(input));
  }
}
