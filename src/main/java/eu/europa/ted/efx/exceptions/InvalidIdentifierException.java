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
 * Exception thrown when identifier-related errors occur during EFX template processing.
 * This includes undeclared identifiers, duplicate declarations, and scope violations.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing
 * and bypass error recovery mechanisms.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class InvalidIdentifierException extends ParseCancellationException {

    public enum ErrorCode {
        UNDECLARED_IDENTIFIER,
        IDENTIFIER_ALREADY_DECLARED,
        NOT_A_CONTEXT_VARIABLE
    }

    private static final String UNDECLARED_IDENTIFIER = "Identifier '%s' is not declared.";
    private static final String IDENTIFIER_ALREADY_DECLARED = "Identifier '%s' is already declared in this scope.";
    private static final String NOT_A_CONTEXT_VARIABLE = "Variable '%s' is not a context variable.";

    private final ErrorCode errorCode;

    private InvalidIdentifierException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static InvalidIdentifierException undeclaredIdentifier(String identifierName) {
        return new InvalidIdentifierException(ErrorCode.UNDECLARED_IDENTIFIER, String.format(UNDECLARED_IDENTIFIER, identifierName));
    }

    public static InvalidIdentifierException alreadyDeclared(String identifierName) {
        return new InvalidIdentifierException(ErrorCode.IDENTIFIER_ALREADY_DECLARED, String.format(IDENTIFIER_ALREADY_DECLARED, identifierName));
    }

    public static InvalidIdentifierException notAContextVariable(String variableName) {
        return new InvalidIdentifierException(ErrorCode.NOT_A_CONTEXT_VARIABLE, String.format(NOT_A_CONTEXT_VARIABLE, variableName));
    }
}