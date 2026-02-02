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

    public enum ErrorCode {
        CANNOT_CONVERT,
        CANNOT_COMPARE,
        EXPECTED_SEQUENCE,
        EXPECTED_FIELD_CONTEXT
    }

    private static final String CANNOT_CONVERT = "Type mismatch. Expected %s instead of %s.";
    private static final String CANNOT_COMPARE = "Type mismatch. Cannot compare values of different types: %s and %s";
    private static final String EXPECTED_SEQUENCE = "Type mismatch. Field '%s' may return multiple values from context '%s', but is used as a scalar. Use a sequence expression or change the context.";
    private static final String EXPECTED_FIELD_CONTEXT = "Type mismatch. Context variable '$%s' refers to node '%s', but is used as a value. Only field context variables can be used in value expressions.";

    private final ErrorCode errorCode;

    private TypeMismatchException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static TypeMismatchException cannotConvert(Class<? extends ParsedEntity> expectedType,
            Class<? extends ParsedEntity> actualType) {
        if (TypedExpression.class.isAssignableFrom(actualType)
                && TypedExpression.class.isAssignableFrom(expectedType)) {
            var actual = actualType.asSubclass(TypedExpression.class);
            var expected = expectedType.asSubclass(TypedExpression.class);

            return new TypeMismatchException(ErrorCode.CANNOT_CONVERT, String.format(CANNOT_CONVERT,
                    TypedExpression.getEfxDataType(expected).getSimpleName(),
                    TypedExpression.getEfxDataType(actual).getSimpleName()));
        }
        return new TypeMismatchException(ErrorCode.CANNOT_CONVERT, String.format(CANNOT_CONVERT,
                expectedType.getSimpleName(), actualType.getSimpleName()));
    }

    public static TypeMismatchException cannotCompare(Expression left, Expression right) {
        return new TypeMismatchException(ErrorCode.CANNOT_COMPARE, String.format(CANNOT_COMPARE,
                left.getClass().getSimpleName(), right.getClass().getSimpleName()));
    }

    public static TypeMismatchException fieldMayRepeat(String fieldId, String contextSymbol) {
        return new TypeMismatchException(ErrorCode.EXPECTED_SEQUENCE, String.format(EXPECTED_SEQUENCE, fieldId,
                contextSymbol != null ? contextSymbol : "root"));
    }

    public static TypeMismatchException nodesHaveNoValue(String variableName, String nodeId) {
        return new TypeMismatchException(ErrorCode.EXPECTED_FIELD_CONTEXT, String.format(EXPECTED_FIELD_CONTEXT, variableName, nodeId));
    }
}

