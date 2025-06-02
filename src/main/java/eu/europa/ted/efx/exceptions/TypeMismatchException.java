package eu.europa.ted.efx.exceptions;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * Exception thrown when type mismatches are detected in EFX templates.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing
 * and bypass error recovery mechanisms.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class TypeMismatchException extends ParseCancellationException {
    private static final String TYPE_MISMATCH_CANNOT_CONVERT = "Type mismatch. Expected %s instead of %s.";
    private static final String TYPE_MISMATCH_CANNOT_COMPARE = "Type mismatch. Cannot compare values of different types: %s and %s";

    private TypeMismatchException(String message) {
        super(message);
    }

    public static TypeMismatchException cannotConvert(Class<? extends ParsedEntity> expectedType,
            Class<? extends ParsedEntity> actualType) {
        if (TypedExpression.class.isAssignableFrom(actualType)
                && TypedExpression.class.isAssignableFrom(expectedType)) {
            var actual = actualType.asSubclass(TypedExpression.class);
            var expected = expectedType.asSubclass(TypedExpression.class);

            return new TypeMismatchException(String.format(TYPE_MISMATCH_CANNOT_CONVERT,
                    TypedExpression.getEfxDataType(expected).getSimpleName(),
                    TypedExpression.getEfxDataType(actual).getSimpleName()));
        }
        return new TypeMismatchException(String.format(TYPE_MISMATCH_CANNOT_CONVERT,
                expectedType.getSimpleName(), actualType.getSimpleName()));
    }

    public static TypeMismatchException cannotCompare(Expression left, Expression right) {
        return new TypeMismatchException(String.format(TYPE_MISMATCH_CANNOT_COMPARE,
                left.getClass().getSimpleName(), right.getClass().getSimpleName()));
    }
}

