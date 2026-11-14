package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.CharStream;

import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.model.CombinedText;
import eu.europa.ted.efx.model.LineMap;

/**
 * Processes {@code #include} directives in EFX rules text by performing text substitution.
 *
 * <p>
 * This processor runs before any ANTLR parsing. It scans the input text for {@code #include "path"}
 * lines, resolves them using the provided {@link IncludedFileResolver}, and substitutes each directive
 * with the resolved content. Recursive includes are supported with circular dependency detection.
 * </p>
 *
 * <p>
 * The text it gives, the combined text, keeps the {@link LineMap} of the file, so that an error in
 * the combined text can give the file and the line it was written on.
 * </p>
 */
public class IncludeProcessor {

  private static final Pattern INCLUDE_PATTERN =
      Pattern.compile("^[ \\t]*#include[ \\t]+\"([^\"]+)\"[ \\t]*(?://[^\\r\\n]*)?[ \\t]*$",
          Pattern.MULTILINE);

  private final IncludedFileResolver resolver;
  /** The files being resolved, in the order in which they include each other. */
  private final Set<String> resolving = new LinkedHashSet<>();

  public IncludeProcessor(IncludedFileResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Resolves all {@code #include} directives in the given CharStream.
   *
   * @param input The CharStream potentially containing include directives.
   * @return The combined text, with all includes substituted recursively, and its line map.
   * @throws IOException If an included file cannot be resolved.
   */
  public CombinedText resolve(CharStream input) throws IOException {
    final LineMap lineMap = new LineMap();
    return new CombinedText(this.process(input.toString(), lineMap), lineMap);
  }

  /**
   * Resolves all {@code #include} directives in the given text, and adds an insert to its line map
   * for each.
   *
   * @param text The EFX rules text potentially containing include directives.
   * @param lineMap The line map of the text, empty.
   * @return The text with all includes substituted recursively.
   * @throws IOException If an included file cannot be resolved.
   */
  private String process(final String text, final LineMap lineMap) throws IOException {
    if (this.resolver == null) {
      if (INCLUDE_PATTERN.matcher(text).find()) {
        throw TranslatorConfigurationException.includeResolverNotConfigured();
      }
      return text;
    }

    StringBuilder result = new StringBuilder();
    Matcher matcher = INCLUDE_PATTERN.matcher(text);
    int lastEnd = 0;
    // the line of the result that the text appended next starts on
    int line = 1;
    // the line of the text that the directive found ends on
    int endLine = 1;

    while (matcher.find()) {
      result.append(text, lastEnd, matcher.start());
      line += lineEndsIn(text, lastEnd, matcher.start());
      endLine += lineEndsIn(text, lastEnd, matcher.end());

      String path = matcher.group(1);

      if (!this.resolving.add(path)) {
        throw InvalidUsageException.circularInclude(this.cycleClosedBy(path));
      }

      try {
        String included = this.resolver.resolve(path);
        LineMap includedLineMap = new LineMap();
        String processed = this.process(included, includedLineMap);
        String inserted =
            processed.isEmpty() || processed.endsWith("\n") ? processed : processed + "\n";
        result.append(inserted);
        int lineCount = lineEndsIn(inserted, 0, inserted.length());
        lineMap.addInsert(line, path, lineCount, includedLineMap, endLine,
            columnOf(text, matcher.end()));
        line += lineCount;
      } finally {
        this.resolving.remove(path);
      }

      lastEnd = matcher.end();
    }

    result.append(text, lastEnd, text.length());
    return result.toString();
  }

  /**
   * @param text a text
   * @param start where to start counting, included
   * @param end where to stop counting, excluded
   * @return how many line ends ('\n', the only line end the lexer counts) the text has between the
   *         two
   */
  private static int lineEndsIn(final String text, final int start, final int end) {
    int count = 0;
    for (int i = start; i < end; i++) {
      if (text.charAt(i) == '\n') {
        count++;
      }
    }
    return count;
  }

  /**
   * @param text a text
   * @param index a place in the text
   * @return the column of the place, as the lexer counts it: how many Unicode characters (code
   *         points, where a Java string counts some as two) precede it on its line, which starts
   *         after a line end ('\n')
   */
  private static int columnOf(final String text, final int index) {
    return text.codePointCount(text.lastIndexOf('\n', index - 1) + 1, index);
  }

  /**
   * @param path a file being resolved, which is included again
   * @return the files that include each other in a circle, from that file back to it
   */
  private List<String> cycleClosedBy(final String path) {
    final List<String> files = new ArrayList<>(this.resolving);
    final List<String> cycle = new ArrayList<>(files.subList(files.indexOf(path), files.size()));
    cycle.add(path);
    return cycle;
  }
}
