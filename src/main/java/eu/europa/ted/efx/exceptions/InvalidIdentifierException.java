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
    private static final String UNDECLARED_IDENTIFIER = "Identifier '%s' is not declared.";
    private static final String IDENTIFIER_ALREADY_DECLARED = "Identifier '%s' is already declared in this scope.";

    private InvalidIdentifierException(String message) {
        super(message);
    }

    public static InvalidIdentifierException undeclaredIdentifier(String identifierName) {
        return new InvalidIdentifierException(String.format(UNDECLARED_IDENTIFIER, identifierName));
    }

    public static InvalidIdentifierException alreadyDeclared(String identifierName) {
        return new InvalidIdentifierException(String.format(IDENTIFIER_ALREADY_DECLARED, identifierName));
    }
}