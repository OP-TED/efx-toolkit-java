package eu.europa.ted.efx.exceptions;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Exception thrown when field validation fails during EFX template processing.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class InvalidUsageException extends ParseCancellationException {
    private static final String SHORTHAND_REQUIRES_CODE_OR_INDICATOR = "Indirect label reference shorthand #{%1$s}, requires a field of type 'code' or 'indicator'. Field %1$s is of type %2$s.";
    private static final String SHORTHAND_REQUIRES_FIELD_CONTEXT = "The %s shorthand syntax can only be used when a field is declared as context.";

    private InvalidUsageException(String message) {
        super(message);
    }

    public static InvalidUsageException shorthandRequiresCodeOrIndicator(String fieldName, String fieldType) {
        return new InvalidUsageException(String.format(SHORTHAND_REQUIRES_CODE_OR_INDICATOR, fieldName, fieldType));
    }

    public static InvalidUsageException shorthandRequiresFieldContext(String shorthandType) {
        return new InvalidUsageException(String.format(SHORTHAND_REQUIRES_FIELD_CONTEXT, shorthandType));
    }
}