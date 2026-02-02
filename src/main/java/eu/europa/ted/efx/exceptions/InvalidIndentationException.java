/*
 * Copyright 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.exceptions;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Exception thrown when invalid indentation is detected in EFX templates.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing
 * and bypass error recovery mechanisms.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class InvalidIndentationException extends ParseCancellationException {

    public enum ErrorCode {
        INCONSISTENT_INDENTATION_SPACES,
        INDENTATION_LEVEL_SKIPPED,
        START_INDENT_AT_ZERO,
        MIXED_INDENTATION,
        NO_NESTING_ON_INVOCATIONS,
        NO_INDENT_ON_TEMPLATE_DECLARATIONS
    }

    private static final String INCONSISTENT_INDENTATION_SPACES = "Inconsistent indentation. Expected a multiple of %d spaces.";
    private static final String INDENTATION_LEVEL_SKIPPED = "Indentation level skipped.";
    private static final String START_INDENT_AT_ZERO = "Incorrect indentation. Please do not indent the first level in your template.";
    private static final String MIXED_INDENTATION = "Do not mix indentation methods. Stick with either tabs or spaces.";
    private static final String NO_NESTING_ON_INVOCATIONS = "Nesting content under a template invocation is not allowed.";
    private static final String NO_INDENT_ON_TEMPLATE_DECLARATIONS = "Indentation is not allowed on template declaration lines.";

    private final ErrorCode errorCode;

    private InvalidIndentationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static InvalidIndentationException inconsistentSpaces(int spaces) {
        return new InvalidIndentationException(ErrorCode.INCONSISTENT_INDENTATION_SPACES,
                String.format(INCONSISTENT_INDENTATION_SPACES, spaces));
    }

    public static InvalidIndentationException indentationLevelSkipped() {
        return new InvalidIndentationException(ErrorCode.INDENTATION_LEVEL_SKIPPED, INDENTATION_LEVEL_SKIPPED);
    }

    public static InvalidIndentationException startIndentAtZero() {
        return new InvalidIndentationException(ErrorCode.START_INDENT_AT_ZERO, START_INDENT_AT_ZERO);
    }

    public static InvalidIndentationException mixedIndentation() {
        return new InvalidIndentationException(ErrorCode.MIXED_INDENTATION, MIXED_INDENTATION);
    }

    public static InvalidIndentationException noNestingOnInvocations() {
        return new InvalidIndentationException(ErrorCode.NO_NESTING_ON_INVOCATIONS, NO_NESTING_ON_INVOCATIONS);
    }

    public static InvalidIndentationException noIndentOnTemplateDeclarations() {
        return new InvalidIndentationException(ErrorCode.NO_INDENT_ON_TEMPLATE_DECLARATIONS,
                NO_INDENT_ON_TEMPLATE_DECLARATIONS);
    }
}