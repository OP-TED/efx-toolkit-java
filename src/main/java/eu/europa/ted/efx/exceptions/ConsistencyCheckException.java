/*
 * Copyright 2026 European Union
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

/**
 * Exception thrown when the toolkit encounters an internal consistency check failure.
 * This indicates a bug in the toolkit implementation, not a user error.
 * These are "should never happen" errors that suggest developer mistakes in the toolkit code.
 */
public class ConsistencyCheckException extends IllegalStateException {

    public enum ErrorCode {
        TYPE_NOT_REGISTERED,
        UNSUPPORTED_TYPE_IN_CONDITIONAL,
        MISSING_TYPE_MAPPING,
        MISSING_TYPE_ANNOTATION,
        UNKNOWN_EXPRESSION_TYPE,
        INVALID_VARIABLE_CONTEXT,
        UNHANDLED_PRIVACY_SETTING,
        UNHANDLED_LINKED_FIELD_PROPERTY
    }

    private static final String TYPE_NOT_REGISTERED =
        "EfxDataType %s is not registered in TYPE_VARIANTS. " +
        "This indicates a bug in the type system. " +
        "Add the missing type to TYPE_VARIANTS with its scalar and sequence variants, " +
        "ensuring subtypes appear before supertypes. " +
        "Run EfxTypeLatticeTest to verify the registration.";

    private static final String UNSUPPORTED_TYPE_IN_CONDITIONAL =
        "Type %s is not supported in conditional expressions. " +
        "This indicates the translator is missing a handler for this type. " +
        "Add an else-if branch for this type in exitConditionalExpression().";

    private static final String MISSING_TYPE_MAPPING =
        "Type %s is not mapped in %s. " +
        "This indicates the translator is missing a type mapping. " +
        "Add the missing type to the map.";

    private static final String MISSING_TYPE_ANNOTATION =
        "TypedExpression class %s is missing @EfxDataTypeAssociation annotation. " +
        "This indicates a bug in the type system. " +
        "Add the annotation to specify which EfxDataType this expression represents.";

    private static final String UNKNOWN_EXPRESSION_TYPE =
        "Expression type %s is not handled by the type conversion logic. " +
        "This indicates a bug in the type system. " +
        "The target type must be PathExpression, SequenceExpression, or ScalarExpression.";

    private static final String INVALID_VARIABLE_CONTEXT =
        "Variable context is neither a field nor a node context. " +
        "This indicates a bug in the translator. " +
        "Ensure all variable contexts are properly classified as FieldContext or NodeContext.";

    private static final String UNHANDLED_PRIVACY_SETTING =
        "Privacy setting '%s' is not handled. " +
        "This indicates a bug in the translator. " +
        "Add the missing case to the switch in getPrivacySettingOfField().";

    private static final String UNHANDLED_LINKED_FIELD_PROPERTY =
        "Linked field property '%s' is not handled. " +
        "This indicates a bug in the translator. " +
        "Add the missing case to getLinkedFieldId().";

    private final ErrorCode errorCode;

    private ConsistencyCheckException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static ConsistencyCheckException typeNotRegistered(Class<?> type) {
        return new ConsistencyCheckException(ErrorCode.TYPE_NOT_REGISTERED,
                String.format(TYPE_NOT_REGISTERED, type.getName()));
    }

    public static ConsistencyCheckException unsupportedTypeInConditional(Class<?> type) {
        return new ConsistencyCheckException(ErrorCode.UNSUPPORTED_TYPE_IN_CONDITIONAL,
                String.format(UNSUPPORTED_TYPE_IN_CONDITIONAL, type.getName()));
    }

    public static ConsistencyCheckException missingTypeMapping(Class<?> type, String mapName) {
        return new ConsistencyCheckException(ErrorCode.MISSING_TYPE_MAPPING,
                String.format(MISSING_TYPE_MAPPING, type.getName(), mapName));
    }

    public static ConsistencyCheckException missingTypeAnnotation(Class<?> type) {
        return new ConsistencyCheckException(ErrorCode.MISSING_TYPE_ANNOTATION,
                String.format(MISSING_TYPE_ANNOTATION, type.getName()));
    }

    public static ConsistencyCheckException unknownExpressionType(Class<?> type) {
        return new ConsistencyCheckException(ErrorCode.UNKNOWN_EXPRESSION_TYPE,
                String.format(UNKNOWN_EXPRESSION_TYPE, type.getName()));
    }

    public static ConsistencyCheckException invalidVariableContext() {
        return new ConsistencyCheckException(ErrorCode.INVALID_VARIABLE_CONTEXT, INVALID_VARIABLE_CONTEXT);
    }

    public static ConsistencyCheckException unhandledPrivacySetting(Object setting) {
        return new ConsistencyCheckException(ErrorCode.UNHANDLED_PRIVACY_SETTING,
                String.format(UNHANDLED_PRIVACY_SETTING, setting));
    }

    public static ConsistencyCheckException unhandledLinkedFieldProperty(String property) {
        return new ConsistencyCheckException(ErrorCode.UNHANDLED_LINKED_FIELD_PROPERTY,
                String.format(UNHANDLED_LINKED_FIELD_PROPERTY, property));
    }
}
