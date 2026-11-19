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
 * Exception thrown when identifier-related errors occur during EFX template processing.
 * This includes undeclared identifiers, duplicate declarations, and scope violations.
 */
public class InvalidIdentifierException extends EfxCompilationException {

    public enum ErrorCode {
        UNDECLARED_IDENTIFIER,
        IDENTIFIER_ALREADY_DECLARED,
        NOT_A_CONTEXT_VARIABLE,
        UNDECLARED_ENDPOINT,
        IDENTIFIER_IS_RESERVED,
        DICTIONARY_WITHOUT_KEY
    }

    private static final String UNDECLARED_IDENTIFIER = "%s is not declared.";
    private static final String IDENTIFIER_ALREADY_DECLARED = "%s is already declared in this scope.";
    private static final String NAME_ALREADY_DECLARED = "%s has the same name as %s, already declared in this scope.";
    private static final String NOT_A_CONTEXT_VARIABLE = "%s is not a context variable.";
    private static final String UNDECLARED_ENDPOINT = "%s references undeclared API endpoint '%s'.";
    private static final String IDENTIFIER_IS_RESERVED = "%s is reserved: the translator generates the names that start with '%s'.";
    private static final String DICTIONARY_WITHOUT_KEY = "Dictionary '$%1$s' has no value of its own: look up a key in it, as in $%1$s['key'].";

    private final ErrorCode errorCode;

    private InvalidIdentifierException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    /**
     * A reference to a name that nothing of its kind declares.
     *
     * @param ctx the reference
     * @param identifier the identifier as an error names it, such as "Variable '$x'" or
     *        "Function '?f'": see {@link eu.europa.ted.efx.model.identifiers.Identifier#describe()}
     * @return the exception
     */
    public static InvalidIdentifierException undeclaredIdentifier(ParserRuleContext ctx,
            String identifier) {
        return new InvalidIdentifierException(ErrorCode.UNDECLARED_IDENTIFIER, ctx,
                UNDECLARED_IDENTIFIER, identifier);
    }

    /**
     * A declaration of a name that is already declared in the scope. When the identifier that
     * holds the name is of another kind, and so named otherwise, the message names both.
     *
     * @param ctx the declaration
     * @param identifier the identifier declared, as an error names it, such as "Variable '$x'"
     * @param declared the identifier that holds the name, as an error names it, such as
     *        "Function '?x'"
     * @return the exception
     */
    public static InvalidIdentifierException alreadyDeclared(ParserRuleContext ctx,
            String identifier, String declared) {
        if (identifier.equals(declared)) {
            return new InvalidIdentifierException(ErrorCode.IDENTIFIER_ALREADY_DECLARED, ctx,
                    IDENTIFIER_ALREADY_DECLARED, identifier);
        }
        return new InvalidIdentifierException(ErrorCode.IDENTIFIER_ALREADY_DECLARED, ctx,
                NAME_ALREADY_DECLARED, identifier, declared);
    }

    /**
     * A context taken from a variable that holds no context.
     *
     * @param ctx the reference to the variable
     * @param variable the variable as an error names it, such as "Variable '$x'"
     * @return the exception
     */
    public static InvalidIdentifierException notAContextVariable(ParserRuleContext ctx,
            String variable) {
        return new InvalidIdentifierException(ErrorCode.NOT_A_CONTEXT_VARIABLE, ctx,
                NOT_A_CONTEXT_VARIABLE, variable);
    }

    /**
     * A dynamic function whose endpoint the validation plan does not declare.
     *
     * @param ctx the declaration of the function
     * @param function the function as an error names it, such as "Function '?check'"
     * @param endpointName the name of the endpoint
     * @return the exception
     */
    public static InvalidIdentifierException undeclaredEndpoint(ParserRuleContext ctx,
            String function, String endpointName) {
        return new InvalidIdentifierException(ErrorCode.UNDECLARED_ENDPOINT, ctx,
                UNDECLARED_ENDPOINT, function, endpointName);
    }

    /**
     * A declaration that gives a name the translator keeps for the variables it generates.
     *
     * @param ctx the declaration
     * @param identifier the identifier declared, as an error names it, such as "Variable '$__x'"
     * @param reservedPrefix what the names the translator generates start with
     * @return the exception that rejects the declaration
     */
    public static InvalidIdentifierException identifierIsReserved(ParserRuleContext ctx,
            String identifier, String reservedPrefix) {
        return new InvalidIdentifierException(ErrorCode.IDENTIFIER_IS_RESERVED, ctx,
                IDENTIFIER_IS_RESERVED, identifier, reservedPrefix);
    }

    /**
     * A use of the name of a dictionary other than a lookup of a key in it.
     *
     * @param ctx the name, as written
     * @param dictionaryName the name of the dictionary
     * @return the exception that rejects the use
     */
    public static InvalidIdentifierException dictionaryWithoutKey(ParserRuleContext ctx,
            String dictionaryName) {
        return new InvalidIdentifierException(ErrorCode.DICTIONARY_WITHOUT_KEY, ctx,
                DICTIONARY_WITHOUT_KEY, dictionaryName);
    }
}
