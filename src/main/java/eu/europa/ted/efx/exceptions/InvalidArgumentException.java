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

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.variables.Parametrised;

/**
 * Exception thrown when invalid arguments are detected in EFX templates.
 */
public class InvalidArgumentException extends EfxCompilationException {

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

    private InvalidArgumentException(ErrorCode errorCode, String template, Object... args) {
        super(template, args);
        this.errorCode = errorCode;
    }

    private InvalidArgumentException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public static InvalidArgumentException argumentNumberMismatch(Parametrised identifier, int expectedNumber, int actualNumber) {
        return new InvalidArgumentException(ErrorCode.ARGUMENT_NUMBER_MISMATCH,
                ARGUMENT_NUMBER_MISMATCH, identifier.getClass().getSimpleName().toLowerCase(),
                identifier.name, expectedNumber, actualNumber);
    }

    public static InvalidArgumentException argumentNumberMismatch(Parametrised identifier, int expectedNumber) {
        return argumentNumberMismatch(identifier, expectedNumber, expectedNumber + 1);
    }

    public static InvalidArgumentException argumentTypeMismatch(int position, Parametrised identifier,
            Class<? extends TypedExpression> expectedType, Class<? extends TypedExpression> actualType) {
        return new InvalidArgumentException(ErrorCode.ARGUMENT_TYPE_MISMATCH,
                ARGUMENT_TYPE_MISMATCH, position + 1, identifier.getClass().getSimpleName().toLowerCase(),
                identifier.name, TypedExpression.getEfxDataType(expectedType).getSimpleName(),
                TypedExpression.getEfxDataType(actualType).getSimpleName());
    }

    public static InvalidArgumentException unsupportedSequenceType(ParserRuleContext ctx, String type, String functionName) {
        return new InvalidArgumentException(ErrorCode.UNSUPPORTED_SEQUENCE_TYPE, ctx, UNSUPPORTED_SEQUENCE_TYPE, type, functionName);
    }

    public static InvalidArgumentException missingArgument(ParserRuleContext ctx, String parameterName) {
        return new InvalidArgumentException(ErrorCode.MISSING_ARGUMENT, ctx, MISSING_ARGUMENT, parameterName);
    }
}
