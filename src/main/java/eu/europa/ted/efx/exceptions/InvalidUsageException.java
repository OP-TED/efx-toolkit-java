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

/**
 * Exception thrown when field validation fails during EFX template processing.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // Necessary to integrate with ANTLR4 parser cancellation
public class InvalidUsageException extends ParseCancellationException {

    public enum ErrorCode {
        SHORTHAND_REQUIRES_CODE_OR_INDICATOR,
        SHORTHAND_REQUIRES_FIELD_CONTEXT,
        INVALID_NOTICE_SUBTYPE_RANGE_ORDER,
        INVALID_NOTICE_SUBTYPE_TOKEN
    }

    private static final String SHORTHAND_REQUIRES_CODE_OR_INDICATOR = "Indirect label reference shorthand #{%1$s}, requires a field of type 'code' or 'indicator'. Field %1$s is of type %2$s.";
    private static final String SHORTHAND_REQUIRES_FIELD_CONTEXT = "The %s shorthand syntax can only be used when a field is declared as context.";
    private static final String INVALID_NOTICE_SUBTYPE_RANGE_ORDER = "Notice subtype range '%s-%s' is not in ascending order.";
    private static final String INVALID_NOTICE_SUBTYPE_TOKEN = "Invalid notice subtype token '%s'. Expected format: 'X' or 'X-Y'.";

    private final ErrorCode errorCode;

    private InvalidUsageException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static InvalidUsageException shorthandRequiresCodeOrIndicator(String fieldName, String fieldType) {
        return new InvalidUsageException(ErrorCode.SHORTHAND_REQUIRES_CODE_OR_INDICATOR, String.format(SHORTHAND_REQUIRES_CODE_OR_INDICATOR, fieldName, fieldType));
    }

    public static InvalidUsageException shorthandRequiresFieldContext(String shorthandType) {
        return new InvalidUsageException(ErrorCode.SHORTHAND_REQUIRES_FIELD_CONTEXT, String.format(SHORTHAND_REQUIRES_FIELD_CONTEXT, shorthandType));
    }

    public static InvalidUsageException invalidNoticeSubtypeRangeOrder(String start, String end) {
        return new InvalidUsageException(ErrorCode.INVALID_NOTICE_SUBTYPE_RANGE_ORDER, String.format(INVALID_NOTICE_SUBTYPE_RANGE_ORDER, start, end));
    }

    public static InvalidUsageException invalidNoticeSubtypeToken(String token) {
        return new InvalidUsageException(ErrorCode.INVALID_NOTICE_SUBTYPE_TOKEN, String.format(INVALID_NOTICE_SUBTYPE_TOKEN, token));
    }
}