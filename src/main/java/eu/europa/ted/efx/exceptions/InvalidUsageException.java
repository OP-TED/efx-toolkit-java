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

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;

import eu.europa.ted.efx.model.identifiers.DynamicFunction;
import eu.europa.ted.efx.model.templates.FormatOptions.FormatStyle;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeTokenLookup;

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
        TEMPLATE_ONLY_CONTEXT,
        UNSUPPORTED_REGEX_CONSTRUCT,
        CIRCULAR_INCLUDE,
        EMPTY_RULES_FILE,
        DYNAMIC_FUNCTION_OUTSIDE_RULE,
        INVALID_ENDPOINT_NAME,
        DECIMALS_NOT_APPLICABLE,
        STYLE_NOT_APPLICABLE,
        UNIT_FOR_LIST,
        NO_UNIT_NOT_APPLICABLE
    }

    private static final String SHORTHAND_REQUIRES_CODE_OR_INDICATOR = "Indirect label reference shorthand #{%1$s}, requires a field of type 'code' or 'indicator'. Field %1$s is of type %2$s.";
    private static final String SHORTHAND_REQUIRES_FIELD_CONTEXT = "The %s shorthand syntax can only be used when a field is declared as context.";
    private static final String INVALID_NOTICE_SUBTYPE_RANGE_ORDER = "Notice subtype range '%s-%s' is not in ascending order.";
    private static final String INVALID_NOTICE_SUBTYPE_TOKEN = "Invalid notice subtype token '%s'. Expected format: 'X' or 'X-Y'.";
    private static final String FIELD_NOT_WITHHOLDABLE = "Field '%s' is always published and cannot be withheld from publication.";
    private static final String TEMPLATE_ONLY_FUNCTION = "%s can only be used in templates, not in expressions or validation rules.";
    private static final String TEMPLATE_ONLY_CONTEXT = "Context '%s' can only be used in templates, not in validation rules.";
    private static final String UNSUPPORTED_REGEX_CONSTRUCT = "Invalid regex pattern %s at position %d: %s";
    private static final String CIRCULAR_INCLUDE = "Circular #include: %s.";
    private static final String EMPTY_RULES_FILE = "Rules file must contain at least one validation stage.";
    private static final String DYNAMIC_FUNCTION_OUTSIDE_RULE = "%s can only be called inline within a rule expression (ASSERT/REPORT). Use 'LET dynamic : $var = ?%s(...)' to declare a dynamic variable at this scope.";
    private static final String INVALID_ENDPOINT_NAME = "Endpoint name '%s' is not valid. Endpoint names must start with a letter and contain only letters, digits, hyphens, and underscores.";
    private static final String DECIMALS_NOT_APPLICABLE = "Format option '%d' cannot format a value of type '%s': it is for a number or a duration.";
    private static final String STYLE_NOT_APPLICABLE = "Format option '%s' cannot format a value of type '%s': it is for a date or a time.";
    private static final String UNIT_FOR_LIST = "A unit can only be displayed with a single value. Add no-unit to display the values without their unit.";
    private static final String NO_UNIT_NOT_APPLICABLE = "Format option 'no-unit' cannot format a value of type '%s': it is for an amount, a measure or a duration.";

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

    /**
     * A call, outside a template, of a built-in that only templates can call.
     *
     * @param ctx the call
     * @param call how the error names the call, as a type error does: "Function 'format-short'",
     *        or "Operator ':preferredLanguage'" after a field
     * @return the exception
     */
    public static InvalidUsageException templateOnlyFunction(ParserRuleContext ctx, String call) {
        return new InvalidUsageException(ErrorCode.TEMPLATE_ONLY_FUNCTION, ctx, TEMPLATE_ONLY_FUNCTION, call);
    }

    /**
     * A context written {@code .} or {@code ..} in a validation rule. Both are relative to the
     * context of the line above in a template; a rule set stands on its own, with no such context.
     *
     * @param ctx the context declaration
     * @param shortcut the context as written: "." or ".."
     * @return the exception
     */
    public static InvalidUsageException templateOnlyContext(ParserRuleContext ctx, String shortcut) {
        return new InvalidUsageException(ErrorCode.TEMPLATE_ONLY_CONTEXT, ctx, TEMPLATE_ONLY_CONTEXT, shortcut);
    }

    public static InvalidUsageException unsupportedRegexConstruct(String pattern, int position, String reason) {
        return new InvalidUsageException(ErrorCode.UNSUPPORTED_REGEX_CONSTRUCT, UNSUPPORTED_REGEX_CONSTRUCT, pattern, position, reason);
    }

    /**
     * @param cycle the files that include each other in a circle, from the first one back to it
     * @return the error, which names each file of the circle
     */
    public static InvalidUsageException circularInclude(List<String> cycle) {
        final StringBuilder includes = new StringBuilder("'").append(cycle.get(0))
            .append("' includes '").append(cycle.get(1)).append('\'');
        for (final String path : cycle.subList(2, cycle.size())) {
            includes.append(", which includes '").append(path).append('\'');
        }
        return new InvalidUsageException(ErrorCode.CIRCULAR_INCLUDE, CIRCULAR_INCLUDE, includes);
    }

    public static InvalidUsageException emptyRulesFile() {
        return new InvalidUsageException(ErrorCode.EMPTY_RULES_FILE, EMPTY_RULES_FILE);
    }

    /**
     * A dynamic function called outside the expression of a rule. The message names the function
     * as an error names it, such as "Function '?check'", and shows how to call it there.
     *
     * @param ctx the call
     * @param function the function called
     * @return the exception
     */
    public static InvalidUsageException dynamicFunctionOutsideRule(ParserRuleContext ctx,
            DynamicFunction function) {
        return new InvalidUsageException(ErrorCode.DYNAMIC_FUNCTION_OUTSIDE_RULE, ctx,
                DYNAMIC_FUNCTION_OUTSIDE_RULE, function.describe(), function.name);
    }

    public static InvalidUsageException invalidEndpointName(ParserRuleContext ctx, String endpointName) {
        return new InvalidUsageException(ErrorCode.INVALID_ENDPOINT_NAME, ctx, INVALID_ENDPOINT_NAME, endpointName);
    }

    /**
     * A number of decimals given to a value that is neither a number nor a duration.
     *
     * @param ctx the expression block
     * @param decimals the number of decimals given
     * @param type the type of the value
     * @return the exception
     */
    public static InvalidUsageException decimalsNotApplicable(ParserRuleContext ctx, int decimals,
            Class<? extends EfxDataType> type) {
        return new InvalidUsageException(ErrorCode.DECIMALS_NOT_APPLICABLE, ctx, DECIMALS_NOT_APPLICABLE, decimals,
                EfxTypeTokenLookup.nameOf(type));
    }

    /**
     * A style given to a value that is neither a date nor a time.
     *
     * @param ctx the expression block
     * @param style the style given
     * @param type the type of the value
     * @return the exception
     */
    public static InvalidUsageException styleNotApplicable(ParserRuleContext ctx, FormatStyle style,
            Class<? extends EfxDataType> type) {
        return new InvalidUsageException(ErrorCode.STYLE_NOT_APPLICABLE, ctx, STYLE_NOT_APPLICABLE, style.getName(),
                EfxTypeTokenLookup.nameOf(type));
    }

    public static InvalidUsageException unitForList(ParserRuleContext ctx) {
        return new InvalidUsageException(ErrorCode.UNIT_FOR_LIST, ctx, UNIT_FOR_LIST);
    }

    /**
     * No-unit given to a value that has no unit.
     *
     * @param ctx the expression block
     * @param type the type of the value
     * @return the exception
     */
    public static InvalidUsageException noUnitNotApplicable(ParserRuleContext ctx, Class<? extends EfxDataType> type) {
        return new InvalidUsageException(ErrorCode.NO_UNIT_NOT_APPLICABLE, ctx, NO_UNIT_NOT_APPLICABLE,
                EfxTypeTokenLookup.nameOf(type));
    }
}
