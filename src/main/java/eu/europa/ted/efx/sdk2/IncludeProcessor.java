package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;

/**
 * Processes {@code #include} directives in EFX rules text by performing text substitution.
 *
 * <p>
 * This processor runs before any ANTLR parsing. It scans the input text for {@code #include "path"}
 * lines, resolves them using the provided {@link IncludedFileResolver}, and substitutes each directive
 * with the resolved content. Recursive includes are supported with circular dependency detection.
 * </p>
 */
public class IncludeProcessor {

  private static final Pattern INCLUDE_PATTERN =
      Pattern.compile("^[ \\t]*#include[ \\t]+\"([^\"]+)\"[ \\t]*(?://[^\\r\\n]*)?[ \\t]*$",
          Pattern.MULTILINE);

  private final IncludedFileResolver resolver;
  private final Set<String> resolving = new HashSet<>();

  public IncludeProcessor(IncludedFileResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Resolves all {@code #include} directives in the given CharStream.
   *
   * @param input The CharStream potentially containing include directives.
   * @return A CharStream with all includes substituted recursively.
   * @throws IOException If an included file cannot be resolved.
   */
  public CharStream resolve(CharStream input) throws IOException {
    String resolvedText = this.process(input.toString());
    return CharStreams.fromString(resolvedText);
  }

  /**
   * Resolves all {@code #include} directives in the given text.
   *
   * @param text The EFX rules text potentially containing include directives.
   * @return The text with all includes substituted recursively.
   * @throws IOException If an included file cannot be resolved.
   */
  public String process(String text) throws IOException {
    if (this.resolver == null) {
      if (INCLUDE_PATTERN.matcher(text).find()) {
        throw TranslatorConfigurationException.includeResolverNotConfigured();
      }
      return text;
    }

    StringBuilder result = new StringBuilder();
    Matcher matcher = INCLUDE_PATTERN.matcher(text);
    int lastEnd = 0;

    while (matcher.find()) {
      result.append(text, lastEnd, matcher.start());

      String path = matcher.group(1);

      if (!this.resolving.add(path)) {
        throw InvalidUsageException.circularInclude(path);
      }

      try {
        String included = this.resolver.resolve(path);
        String processed = this.process(included);
        result.append(processed);
        if (!processed.isEmpty() && !processed.endsWith("\n")) {
          result.append("\n");
        }
      } finally {
        this.resolving.remove(path);
      }

      lastEnd = matcher.end();
    }

    result.append(text, lastEnd, text.length());
    return result.toString();
  }
}
