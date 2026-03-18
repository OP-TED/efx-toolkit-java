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
 * Exception thrown when an EFX construct is used incorrectly, such as referencing
 * a non-withholdable field for privacy properties, or calling a template-only
 * function in an expression or validation rule.
 */
public class InvalidUsageException extends EfxCompilationException {

    public enum ErrorCode {
        SHORTHAND_REQUIRES_CODE_OR_INDICATOR,
        SHORTHAND_REQUIRES_FIELD_CONTEXT,
        INVALID_NOTICE_SUBTYPE_RANGE_ORDER,
        INVALID_NOTICE_SUBTYPE_TOKEN,
        FIELD_NOT_WITHHOLDABLE,
        TEMPLATE_ONLY_FUNCTION,
        UNSUPPORTED_REGEX_CONSTRUCT,
        CIRCULAR_INCLUDE,
        EMPTY_RULES_FILE,
        API_CALL_IN_COMPOUND_INITIALIZER,
        NOT_A_DYNAMIC_FUNCTION,
        DYNAMIC_FUNCTION_OUTSIDE_RULE
    }

    private static final String SHORTHAND_REQUIRES_CODE_OR_INDICATOR = "Indirect label reference shorthand #{%1$s}, requires a field of type 'code' or 'indicator'. Field %1$s is of type %2$s.";
    private static final String SHORTHAND_REQUIRES_FIELD_CONTEXT = "The %s shorthand syntax can only be used when a field is declared as context.";
    private static final String INVALID_NOTICE_SUBTYPE_RANGE_ORDER = "Notice subtype range '%s-%s' is not in ascending order.";
    private static final String INVALID_NOTICE_SUBTYPE_TOKEN = "Invalid notice subtype token '%s'. Expected format: 'X' or 'X-Y'.";
    private static final String FIELD_NOT_WITHHOLDABLE = "Field '%s' is always published and cannot be withheld from publication.";
    private static final String TEMPLATE_ONLY_FUNCTION = "Function '%s' can only be used in templates, not in expressions or validation rules.";
    private static final String UNSUPPORTED_REGEX_CONSTRUCT = "Invalid regex pattern %s at position %d: %s";
    private static final String CIRCULAR_INCLUDE = "Circular #include detected: '%s'.";
    private static final String EMPTY_RULES_FILE = "Rules file must contain at least one validation stage.";
    private static final String API_CALL_IN_COMPOUND_INITIALIZER = "Dynamic function '%s' cannot be used inside a compound expression in a variable initializer. Declare a separate variable for the dynamic function call.";
    private static final String NOT_A_DYNAMIC_FUNCTION = "Function '%s' is not declared as a dynamic function. Only functions declared with CALL API can be used in a dynamic variable initializer.";
    private static final String DYNAMIC_FUNCTION_OUTSIDE_RULE = "Dynamic function '%s' can only be called inline within a rule expression (ASSERT/REPORT). Use 'LET dynamic : $var = ?%s(...)' to declare a dynamic variable at this scope.";

    private final ErrorCode errorCode;

    private InvalidUsageException(ErrorCode errorCode, String template, Object... args) {
        super(template, args);
        this.errorCode = errorCode;
    }

    private InvalidUsageException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public static InvalidUsageException shorthandRequiresCodeOrIndicator(ParserRuleContext ctx, String fieldName, String fieldType) {
        return new InvalidUsageException(ErrorCode.SHORTHAND_REQUIRES_CODE_OR_INDICATOR, ctx, SHORTHAND_REQUIRES_CODE_OR_INDICATOR, fieldName, fieldType);
    }

    public static InvalidUsageException shorthandRequiresFieldContext(ParserRuleContext ctx, String shorthandType) {
        return new InvalidUsageException(ErrorCode.SHORTHAND_REQUIRES_FIELD_CONTEXT, ctx, SHORTHAND_REQUIRES_FIELD_CONTEXT, shorthandType);
    }

    public static InvalidUsageException invalidNoticeSubtypeRangeOrder(String start, String end) {
        return new InvalidUsageException(ErrorCode.INVALID_NOTICE_SUBTYPE_RANGE_ORDER, INVALID_NOTICE_SUBTYPE_RANGE_ORDER, start, end);
    }

    public static InvalidUsageException invalidNoticeSubtypeToken(String tokenText) {
        return new InvalidUsageException(ErrorCode.INVALID_NOTICE_SUBTYPE_TOKEN, INVALID_NOTICE_SUBTYPE_TOKEN, tokenText);
    }

    public static InvalidUsageException fieldNotWithholdable(ParserRuleContext ctx, String fieldId) {
        return new InvalidUsageException(ErrorCode.FIELD_NOT_WITHHOLDABLE, ctx, FIELD_NOT_WITHHOLDABLE, fieldId);
    }

    public static InvalidUsageException templateOnlyFunction(ParserRuleContext ctx, String functionName) {
        return new InvalidUsageException(ErrorCode.TEMPLATE_ONLY_FUNCTION, ctx, TEMPLATE_ONLY_FUNCTION, functionName);
    }

    public static InvalidUsageException unsupportedRegexConstruct(String pattern, int position, String reason) {
        return new InvalidUsageException(ErrorCode.UNSUPPORTED_REGEX_CONSTRUCT, UNSUPPORTED_REGEX_CONSTRUCT, pattern, position, reason);
    }

    public static InvalidUsageException circularInclude(String path) {
        return new InvalidUsageException(ErrorCode.CIRCULAR_INCLUDE, CIRCULAR_INCLUDE, path);
    }

    public static InvalidUsageException emptyRulesFile() {
        return new InvalidUsageException(ErrorCode.EMPTY_RULES_FILE, EMPTY_RULES_FILE);
    }

    public static InvalidUsageException apiCallInCompoundInitializer(String functionName) {
        return new InvalidUsageException(ErrorCode.API_CALL_IN_COMPOUND_INITIALIZER, API_CALL_IN_COMPOUND_INITIALIZER, functionName);
    }

    public static InvalidUsageException notADynamicFunction(String functionName) {
        return new InvalidUsageException(ErrorCode.NOT_A_DYNAMIC_FUNCTION, NOT_A_DYNAMIC_FUNCTION, functionName);
    }

    public static InvalidUsageException dynamicFunctionOutsideRule(String functionName) {
        return new InvalidUsageException(ErrorCode.DYNAMIC_FUNCTION_OUTSIDE_RULE, DYNAMIC_FUNCTION_OUTSIDE_RULE, functionName, functionName);
    }
}
