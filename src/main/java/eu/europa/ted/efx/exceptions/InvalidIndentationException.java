package eu.europa.ted.efx.exceptions;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Exception thrown when invalid indentation is detected in EFX templates.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing
 * and bypass error recovery mechanisms.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class InvalidIndentationException extends ParseCancellationException {
  private static final String INCONSISTENT_INDENTATION_SPACES =
      "Inconsistent indentation. Expected a multiple of %d spaces.";
  private static final String INDENTATION_LEVEL_SKIPPED = "Indentation level skipped.";
  private static final String START_INDENT_AT_ZERO =
      "Incorrect indentation. Please do not indent the first level in your template.";
  private static final String MIXED_INDENTATION =
      "Do not mix indentation methods. Stick with either tabs or spaces.";
  private static final String NO_NESTING_ON_INVOCATIONS =
      "Nesting content under a template invocation is not allowed.";
  private static final String NO_INDENT_ON_TEMPLATE_DECLARATIONS = "Indentation is not allowed on template declaration lines.";

    private InvalidIndentationException(String message) {
        super(message);
    }

    public static InvalidIndentationException inconsistentSpaces(int spaces) {
        return new InvalidIndentationException(String.format(INCONSISTENT_INDENTATION_SPACES, spaces));
    }

    public static InvalidIndentationException indentationLevelSkipped() {
        return new InvalidIndentationException(INDENTATION_LEVEL_SKIPPED);
    }

    public static InvalidIndentationException startIndentAtZero() {
        return new InvalidIndentationException(START_INDENT_AT_ZERO);
    }

    public static InvalidIndentationException mixedIndentation() {
        return new InvalidIndentationException(MIXED_INDENTATION);
    }

    public static InvalidIndentationException noNestingOnInvocations() {
        return new InvalidIndentationException(NO_NESTING_ON_INVOCATIONS);
    }
    public static InvalidIndentationException noIndentOnTemplateDeclarations() {
        return new InvalidIndentationException(NO_INDENT_ON_TEMPLATE_DECLARATIONS);
    }
}