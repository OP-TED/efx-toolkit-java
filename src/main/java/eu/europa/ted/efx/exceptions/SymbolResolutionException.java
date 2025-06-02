package eu.europa.ted.efx.exceptions;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Exception thrown when symbol resolution fails during EFX template processing.
 * This includes unknown fields, nodes, codelists, or other symbol lookup failures.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing
 * and bypass error recovery mechanisms.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class SymbolResolutionException extends ParseCancellationException {
    private static final String UNKNOWN_FIELD = "Unknown field '%s'.";
    private static final String UNKNOWN_NODE = "Unknown node '%s'.";
    private static final String UNKNOWN_CODELIST = "Unknown codelist '%s'.";
    private static final String NO_CODELIST_FOR_FIELD = "Field '%s' is not associated with a codelist.";

    private SymbolResolutionException(String message) {
        super(message);
    }

    public static SymbolResolutionException unknownField(String fieldId) {
        return new SymbolResolutionException(String.format(UNKNOWN_FIELD, fieldId));
    }

    public static SymbolResolutionException unknownNode(String nodeId) {
        return new SymbolResolutionException(String.format(UNKNOWN_NODE, nodeId));
    }

    public static SymbolResolutionException unknownCodelist(String codelistId) {
        return new SymbolResolutionException(String.format(UNKNOWN_CODELIST, codelistId));
    }

    public static SymbolResolutionException noCodelistForField(String fieldId) {
        return new SymbolResolutionException(String.format(NO_CODELIST_FOR_FIELD, fieldId));
    }
}