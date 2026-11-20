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
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;

/**
 * Exception thrown when type mismatches are detected in EFX templates.
 *
 * <p>An EFX-2 type error gives a {@link Diagnosis}: what the construct was given, and what the
 * operand at fault must be. Where the reference the operand is written as explains the mistake,
 * such as a field that may repeat, the error adds that reason, and has the code of the
 * reference.</p>
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
        DICTIONARY_IS_SEQUENCE
    }

    private static final String CANNOT_CONVERT = "Type mismatch. Expected %s instead of %s.";
    private static final String CANNOT_COMPARE = "Type mismatch. Cannot compare values of different types: %s and %s.";

    /** A diagnosis: its two sentences, the reason the operand gives, if any, and its suggestion. */
    private static final String DIAGNOSED = "Type mismatch. %s";

    private static final String FIELD_MAY_REPEAT = "Field '%s' may return multiple values from %s: use a sequence expression or change the context.";
    private static final String FROM_CONTEXT = "context '%s'";
    private static final String FROM_ROOT = "the root";
    private static final String FIELD_IS_MULTILINGUAL = "Field '%s' is multilingual and has one value per language: use :preferredLanguageText to select the preferred language, or an index [at n] to select a specific value.";
    private static final String NODE_CONTEXT_AS_VALUE = "%s refers to node '%s', which has no value: only field context variables can be used as values.";
    private static final String IDENTIFIER_IS_SEQUENCE = "%s is declared as a sequence.";
    private static final String DICTIONARY_IS_SEQUENCE = "Dictionary lookup '$%s' returns a sequence: use an index to select a single value, as in $%s['key'][at 1].";

    private final ErrorCode errorCode;

    private TypeMismatchException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    /**
     * A value of one class where another is expected, as EFX-1 and the call stack report it.
     *
     * @param ctx where the value is taken
     * @param expectedType the class expected
     * @param actualType the class of the value
     * @return the exception, whose message names both classes
     */
    public static TypeMismatchException cannotConvert(ParserRuleContext ctx,
            Class<? extends ParsedEntity> expectedType,
            Class<? extends ParsedEntity> actualType) {
        if (TypedExpression.class.isAssignableFrom(actualType)
                && TypedExpression.class.isAssignableFrom(expectedType)) {
            return new TypeMismatchException(ErrorCode.CANNOT_CONVERT, ctx, CANNOT_CONVERT,
                    nameOf(expectedType), nameOf(actualType));
        }
        return new TypeMismatchException(ErrorCode.CANNOT_CONVERT, ctx, CANNOT_CONVERT,
                expectedType.getSimpleName(), actualType.getSimpleName());
    }

    /**
     * Names the class of an expression by the EFX data type it is associated with. A class that
     * stands for several data types, such as a path or a sequence of any primitive, has no
     * association, and is named by its own name.
     */
    private static String nameOf(Class<? extends ParsedEntity> expressionType) {
        final EfxDataTypeAssociation association =
                expressionType.getAnnotation(EfxDataTypeAssociation.class);
        return association != null ? association.dataType().getSimpleName()
                : expressionType.getSimpleName();
    }

    /**
     * Two values of different types compared, as EFX-1 reports it.
     *
     * @param ctx the comparison
     * @param left the left operand
     * @param right the right operand
     * @return the exception, whose message names the classes of both operands
     */
    public static TypeMismatchException cannotCompare(ParserRuleContext ctx, Expression left, Expression right) {
        return new TypeMismatchException(ErrorCode.CANNOT_COMPARE, ctx, CANNOT_COMPARE,
                left.getClass().getSimpleName(), right.getClass().getSimpleName());
    }

    /**
     * An operand that the construct taking it cannot convert to what its position takes.
     *
     * @param ctx where the construct is written
     * @param diagnosis what the construct was given, and what the operand must be
     * @return the exception, whose message gives the diagnosis
     */
    public static TypeMismatchException cannotConvert(ParserRuleContext ctx, Diagnosis diagnosis) {
        return diagnosed(ErrorCode.CANNOT_CONVERT, ctx, diagnosis);
    }

    /**
     * Operands of an infix operator that no overload of the operator takes.
     *
     * @param ctx the operation
     * @param diagnosis what the operator was given, and what the operand at fault must be
     * @return the exception, whose message gives the diagnosis
     */
    public static TypeMismatchException incompatibleOperands(ParserRuleContext ctx, Diagnosis diagnosis) {
        return diagnosed(ErrorCode.INCOMPATIBLE_OPERANDS, ctx, diagnosis);
    }

    /**
     * A field that may give several values, where a single value is required.
     *
     * @param ctx the reference to the field
     * @param diagnosis what the construct taking the field was given, and what it requires
     * @param fieldId the field
     * @param contextSymbol the context the field is read from, or {@code null} if it is read from
     *        the root
     * @return the exception, whose message gives the diagnosis and the reason
     */
    public static TypeMismatchException fieldMayRepeat(ParserRuleContext ctx, Diagnosis diagnosis,
            String fieldId, String contextSymbol) {
        return diagnosed(ErrorCode.FIELD_MAY_REPEAT, ctx, diagnosis, String.format(FIELD_MAY_REPEAT,
                fieldId, contextSymbol != null ? String.format(FROM_CONTEXT, contextSymbol) : FROM_ROOT));
    }

    /**
     * A multilingual field, which has a value for each language, where a single value is required.
     *
     * @param ctx the reference to the field
     * @param diagnosis what the construct taking the field was given, and what it requires
     * @param fieldId the field
     * @return the exception, whose message gives the diagnosis and the reason
     */
    public static TypeMismatchException fieldIsMultilingual(ParserRuleContext ctx, Diagnosis diagnosis,
            String fieldId) {
        return diagnosed(ErrorCode.FIELD_IS_MULTILINGUAL, ctx, diagnosis,
                String.format(FIELD_IS_MULTILINGUAL, fieldId));
    }

    /**
     * A context variable that holds a node, which has no value, where a value is required.
     *
     * @param ctx the reference to the variable
     * @param diagnosis what the construct taking the variable was given, and what it requires
     * @param variable the variable as an error names it, such as "Variable '$x'": see
     *        {@link eu.europa.ted.efx.model.identifiers.Identifier#describe()}
     * @param nodeId the node it holds
     * @return the exception, whose message gives the diagnosis and the reason
     */
    public static TypeMismatchException nodeContextUsedAsValue(ParserRuleContext ctx, Diagnosis diagnosis,
            String variable, String nodeId) {
        return diagnosed(ErrorCode.NODE_CONTEXT_AS_VALUE, ctx, diagnosis,
                String.format(NODE_CONTEXT_AS_VALUE, variable, nodeId));
    }

    /**
     * A variable, a parameter or a function declared as a sequence, where a single value is
     * required.
     *
     * @param ctx the reference to the identifier
     * @param diagnosis what the construct taking the identifier was given, and what it requires
     * @param identifier the identifier as an error names it, such as "Variable '$x'" or
     *        "Function '?f'": see {@link eu.europa.ted.efx.model.identifiers.Identifier#describe()}
     * @return the exception, whose message gives the diagnosis and the reason
     */
    public static TypeMismatchException identifierIsSequence(ParserRuleContext ctx, Diagnosis diagnosis,
            String identifier) {
        return diagnosed(ErrorCode.IDENTIFIER_IS_SEQUENCE, ctx, diagnosis,
                String.format(IDENTIFIER_IS_SEQUENCE, identifier));
    }

    /**
     * A lookup in a dictionary, which gives a sequence, where a single value is required.
     *
     * @param ctx the reference to the dictionary
     * @param diagnosis what the construct taking the lookup was given, and what it requires
     * @param dictionaryName the name of the dictionary
     * @return the exception, whose message gives the diagnosis and the reason
     */
    public static TypeMismatchException dictionaryIsSequence(ParserRuleContext ctx, Diagnosis diagnosis,
            String dictionaryName) {
        return diagnosed(ErrorCode.DICTIONARY_IS_SEQUENCE, ctx, diagnosis,
                String.format(DICTIONARY_IS_SEQUENCE, dictionaryName, dictionaryName));
    }

    private static TypeMismatchException diagnosed(ErrorCode errorCode, ParserRuleContext ctx,
            Diagnosis diagnosis) {
        return new TypeMismatchException(errorCode, ctx, DIAGNOSED, diagnosis.describe());
    }

    private static TypeMismatchException diagnosed(ErrorCode errorCode, ParserRuleContext ctx,
            Diagnosis diagnosis, String reason) {
        return new TypeMismatchException(errorCode, ctx, DIAGNOSED, diagnosis.describe(reason));
    }
}
