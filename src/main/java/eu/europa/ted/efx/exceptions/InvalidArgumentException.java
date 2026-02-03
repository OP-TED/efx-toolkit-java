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

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.variables.Parametrised;

/**
 * Exception thrown when invalid arguments are detected in EFX templates.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing
 * and bypass error recovery mechanisms.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class InvalidArgumentException extends ParseCancellationException {

    public enum ErrorCode {
        ARGUMENT_NUMBER_MISMATCH,
        ARGUMENT_TYPE_MISMATCH,
        UNSUPPORTED_SEQUENCE_TYPE,
        MISSING_ARGUMENT
    }

    private static final String ARGUMENT_NUMBER_MISMATCH = "Argument number mismatch in call to %s '%s'. Expected %d but got %d.";
    private static final String ARGUMENT_TYPE_MISMATCH = "Argument type mismatch for argument %d in call to %s '%s'. Expected %s but got %s.";
    private static final String UNSUPPORTED_SEQUENCE_TYPE = "Unsupported sequence type '%s' in call to %s.";
    private static final String MISSING_ARGUMENT = "No argument passed for parameter '%s'.";

    private final ErrorCode errorCode;

    private InvalidArgumentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static InvalidArgumentException argumentNumberMismatch(Parametrised identifier, int expectedNumber, int actualNumber) {
        return new InvalidArgumentException(ErrorCode.ARGUMENT_NUMBER_MISMATCH,
                String.format(ARGUMENT_NUMBER_MISMATCH, identifier.getClass().getSimpleName().toLowerCase(),
                        identifier.name, expectedNumber, actualNumber));
    }

    public static InvalidArgumentException argumentNumberMismatch(Parametrised identifier, int expectedNumber) {
        return argumentNumberMismatch(identifier, expectedNumber, expectedNumber + 1);
    }

    public static InvalidArgumentException argumentTypeMismatch(int position, Parametrised identifier,
            Class<? extends TypedExpression> expectedType, Class<? extends TypedExpression> actualType) {
        return new InvalidArgumentException(ErrorCode.ARGUMENT_TYPE_MISMATCH,
                String.format(ARGUMENT_TYPE_MISMATCH, position + 1, identifier.getClass().getSimpleName().toLowerCase(),
                        identifier.name, TypedExpression.getEfxDataType(expectedType).getSimpleName(),
                        TypedExpression.getEfxDataType(actualType).getSimpleName()));
    }

    public static InvalidArgumentException unsupportedSequenceType(String type, String functionName) {
        return new InvalidArgumentException(ErrorCode.UNSUPPORTED_SEQUENCE_TYPE, String.format(UNSUPPORTED_SEQUENCE_TYPE, type, functionName));
    }

    public static InvalidArgumentException missingArgument(String parameterName) {
        return new InvalidArgumentException(ErrorCode.MISSING_ARGUMENT, String.format(MISSING_ARGUMENT, parameterName));
    }
}