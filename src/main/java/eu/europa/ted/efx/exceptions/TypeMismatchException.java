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

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * Exception thrown when type mismatches are detected in EFX templates.
 */
public class TypeMismatchException extends EfxCompilationException {

    public enum ErrorCode {
        CANNOT_CONVERT,
        CANNOT_COMPARE,
        INCOMPATIBLE_OPERANDS,
        FIELD_MAY_REPEAT,
        FIELD_IS_MULTILINGUAL,
        NODE_CONTEXT_AS_VALUE,
        IDENTIFIER_IS_SEQUENCE,
        IDENTIFIER_IS_SCALAR,
        DICTIONARY_IS_SEQUENCE
    }

    private static final String CANNOT_CONVERT = "Type mismatch. Expected %s instead of %s.";
    private static final String CANNOT_COMPARE = "Type mismatch. Cannot compare values of different types: %s and %s.";
    private static final String INCOMPATIBLE_OPERANDS = "Type mismatch. Operator '%s' cannot be applied to %s and %s.";
    private static final String FIELD_MAY_REPEAT = "Type mismatch. Field '%s' may return multiple values from context '%s', but is used as a scalar. Use a sequence expression or change the context.";
    private static final String FIELD_IS_MULTILINGUAL = "Type mismatch. Field '%s' is multilingual and has multiple values (one per language), but is used as a scalar. Use :preferredLanguageText to select the preferred language, or an indexer [n] to select a specific element.";
    private static final String NODE_CONTEXT_AS_VALUE = "Type mismatch. Context variable '$%s' refers to node '%s', but is used as a value. Only field context variables can be used in value expressions.";
    private static final String IDENTIFIER_IS_SEQUENCE = "Type mismatch. Variable '$%s' is declared as a sequence, but is used as a scalar.";
    private static final String IDENTIFIER_IS_SCALAR = "Type mismatch. Variable '$%s' is declared as a scalar, but is used where a sequence is expected. To use it as a single-element sequence, wrap it in square brackets: [$%s].";
    private static final String DICTIONARY_IS_SEQUENCE = "Type mismatch. Dictionary lookup '$%s' returns a sequence, but is used where a scalar is expected. Use an index to select a single value: $%s['key'][1].";

    private final ErrorCode errorCode;

    private TypeMismatchException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public static TypeMismatchException cannotConvert(ParserRuleContext ctx,
            Class<? extends ParsedEntity> expectedType,
            Class<? extends ParsedEntity> actualType) {
        if (TypedExpression.class.isAssignableFrom(actualType)
                && TypedExpression.class.isAssignableFrom(expectedType)) {
            var actual = actualType.asSubclass(TypedExpression.class);
            var expected = expectedType.asSubclass(TypedExpression.class);
            return new TypeMismatchException(ErrorCode.CANNOT_CONVERT, ctx, CANNOT_CONVERT,
                    TypedExpression.getEfxDataType(expected).getSimpleName(),
                    TypedExpression.getEfxDataType(actual).getSimpleName());
        }
        return new TypeMismatchException(ErrorCode.CANNOT_CONVERT, ctx, CANNOT_CONVERT,
                expectedType.getSimpleName(), actualType.getSimpleName());
    }

    public static TypeMismatchException cannotCompare(ParserRuleContext ctx, Expression left, Expression right) {
        return new TypeMismatchException(ErrorCode.CANNOT_COMPARE, ctx, CANNOT_COMPARE,
                left.getClass().getSimpleName(), right.getClass().getSimpleName());
    }

    public static TypeMismatchException incompatibleOperands(ParserRuleContext ctx, String operator, Expression left, Expression right) {
        return new TypeMismatchException(ErrorCode.INCOMPATIBLE_OPERANDS, ctx, INCOMPATIBLE_OPERANDS,
                operator, left.getClass().getSimpleName(), right.getClass().getSimpleName());
    }

    public static TypeMismatchException fieldMayRepeat(ParserRuleContext ctx, String fieldId, String contextSymbol) {
        return new TypeMismatchException(ErrorCode.FIELD_MAY_REPEAT, ctx, FIELD_MAY_REPEAT, fieldId,
                contextSymbol != null ? contextSymbol : "root");
    }

    public static TypeMismatchException fieldIsMultilingual(ParserRuleContext ctx, String fieldId) {
        return new TypeMismatchException(ErrorCode.FIELD_IS_MULTILINGUAL, ctx, FIELD_IS_MULTILINGUAL, fieldId);
    }

    public static TypeMismatchException nodeContextUsedAsValue(ParserRuleContext ctx, String variableName, String nodeId) {
        return new TypeMismatchException(ErrorCode.NODE_CONTEXT_AS_VALUE, ctx, NODE_CONTEXT_AS_VALUE, variableName, nodeId);
    }

    public static TypeMismatchException identifierIsSequence(ParserRuleContext ctx, String variableName) {
        return new TypeMismatchException(ErrorCode.IDENTIFIER_IS_SEQUENCE, ctx, IDENTIFIER_IS_SEQUENCE, variableName);
    }

    public static TypeMismatchException identifierIsScalar(ParserRuleContext ctx, String variableName) {
        return new TypeMismatchException(ErrorCode.IDENTIFIER_IS_SCALAR, ctx, IDENTIFIER_IS_SCALAR, variableName, variableName);
    }

    public static TypeMismatchException dictionaryIsSequence(ParserRuleContext ctx, String dictionaryName) {
        return new TypeMismatchException(ErrorCode.DICTIONARY_IS_SEQUENCE, ctx, DICTIONARY_IS_SEQUENCE, dictionaryName, dictionaryName);
    }
}
