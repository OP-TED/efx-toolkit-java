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

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Exception thrown when invalid indentation is detected in EFX templates.
 */
public class InvalidIndentationException extends EfxCompilationException {

    public enum ErrorCode {
        INCONSISTENT_INDENTATION_SPACES,
        INDENTATION_LEVEL_SKIPPED,
        START_INDENT_AT_ZERO,
        MIXED_INDENTATION,
        NO_INDENT_ON_TEMPLATE_DECLARATIONS
    }

    private static final String INCONSISTENT_INDENTATION_SPACES = "This line is indented with %d spaces, not a multiple of %d, the indentation of the first indented line.";
    private static final String INDENTATION_LEVEL_SKIPPED = "This line is indented more than one level deeper than the line above it.";
    private static final String START_INDENT_AT_ZERO = "This line cannot be indented: there is no line above it to nest under.";
    private static final String MIXED_INDENTATION = "Indentation mixes tabs and spaces: a template is indented with tabs only or with spaces only.";
    private static final String NO_INDENT_ON_TEMPLATE_DECLARATIONS = "A template declaration cannot be indented.";

    private final ErrorCode errorCode;

    private InvalidIndentationException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    /**
     * @param ctx the line, or its indentation
     * @param spaces the number of spaces the line is indented with
     * @param levelSpaces the number of spaces of one level of indentation: those of the first
     *        indented line
     * @return the error for a line whose spaces are not a whole number of levels
     */
    public static InvalidIndentationException inconsistentSpaces(ParserRuleContext ctx, int spaces, int levelSpaces) {
        return new InvalidIndentationException(ErrorCode.INCONSISTENT_INDENTATION_SPACES, ctx, INCONSISTENT_INDENTATION_SPACES, spaces, levelSpaces);
    }

    public static InvalidIndentationException indentationLevelSkipped(ParserRuleContext ctx) {
        return new InvalidIndentationException(ErrorCode.INDENTATION_LEVEL_SKIPPED, ctx, INDENTATION_LEVEL_SKIPPED);
    }

    public static InvalidIndentationException startIndentAtZero(ParserRuleContext ctx) {
        return new InvalidIndentationException(ErrorCode.START_INDENT_AT_ZERO, ctx, START_INDENT_AT_ZERO);
    }

    public static InvalidIndentationException mixedIndentation(ParserRuleContext ctx) {
        return new InvalidIndentationException(ErrorCode.MIXED_INDENTATION, ctx, MIXED_INDENTATION);
    }

    public static InvalidIndentationException noIndentOnTemplateDeclarations(ParserRuleContext ctx) {
        return new InvalidIndentationException(ErrorCode.NO_INDENT_ON_TEMPLATE_DECLARATIONS, ctx, NO_INDENT_ON_TEMPLATE_DECLARATIONS);
    }
}
