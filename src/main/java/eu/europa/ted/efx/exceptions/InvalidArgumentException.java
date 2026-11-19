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

import eu.europa.ted.efx.model.identifiers.Invocable;

/**
 * Exception thrown when invalid arguments are detected in EFX templates.
 */
public class InvalidArgumentException extends EfxCompilationException {

    public enum ErrorCode {
        ARGUMENT_NUMBER_MISMATCH,
        UNSUPPORTED_SEQUENCE_TYPE,
        MISSING_ARGUMENT
    }

    private static final String ARGUMENT_NUMBER_MISMATCH = "Argument number mismatch. %s takes %s but got %d.";
    private static final String UNSUPPORTED_SEQUENCE_TYPE = "Unsupported sequence type '%s' in call to %s.";
    private static final String MISSING_ARGUMENT = "No argument passed for %s.";

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

    /**
     * A call of a declared function or template with another number of arguments than it
     * declares parameters.
     *
     * @param ctx the call
     * @param invocable the function or template called
     * @param actualNumber the number of arguments of the call
     * @return the exception, whose message names what is called by its spelling, as a type error
     *         does
     */
    public static InvalidArgumentException argumentNumberMismatch(ParserRuleContext ctx,
            Invocable invocable, int actualNumber) {
        return new InvalidArgumentException(ErrorCode.ARGUMENT_NUMBER_MISMATCH, ctx,
                ARGUMENT_NUMBER_MISMATCH, invocable.describe(),
                invocable.getSpelling().count(invocable.parameters.size()), actualNumber);
    }

    public static InvalidArgumentException unsupportedSequenceType(ParserRuleContext ctx, String type, String functionName) {
        return new InvalidArgumentException(ErrorCode.UNSUPPORTED_SEQUENCE_TYPE, ctx, UNSUPPORTED_SEQUENCE_TYPE, type, functionName);
    }

    /**
     * A parameter of the expression that no argument was passed for.
     *
     * @param ctx the declaration of the parameter
     * @param parameter the parameter as an error names it, such as "Parameter '$p'"
     * @return the exception
     */
    public static InvalidArgumentException missingArgument(ParserRuleContext ctx,
            String parameter) {
        return new InvalidArgumentException(ErrorCode.MISSING_ARGUMENT, ctx, MISSING_ARGUMENT,
                parameter);
    }
}
