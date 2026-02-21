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
 * Exception thrown when the translator's internal configuration is incomplete.
 * This typically means a new type, enum value, or case was added without updating the
 * corresponding map, switch, or handler in the translator code. Not a user error.
 */
public class TranslatorConfigurationException extends IllegalStateException {

    public enum ErrorCode {
        TYPE_NOT_REGISTERED,
        UNSUPPORTED_TYPE_IN_CONDITIONAL,
        MISSING_TYPE_MAPPING,
        MISSING_TYPE_ANNOTATION,
        UNKNOWN_EXPRESSION_TYPE,
        UNHANDLED_VARIABLE_CONTEXT,
        UNHANDLED_PRIVACY_SETTING,
        UNHANDLED_LINKED_FIELD_PROPERTY,
        UNHANDLED_PREDICATE_CONTEXT
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

    private static final String UNHANDLED_VARIABLE_CONTEXT =
        "Variable context type '%s' is not handled. " +
        "Add a handler for this context type in exitContextVariableSpecifier().";

    private static final String UNHANDLED_PRIVACY_SETTING =
        "Privacy setting '%s' is not handled. " +
        "This indicates a bug in the translator. " +
        "Add the missing case to the switch in getPrivacySettingOfField().";

    private static final String UNHANDLED_LINKED_FIELD_PROPERTY =
        "Linked field property '%s' is not handled. " +
        "This indicates a bug in the translator. " +
        "Add the missing case to getLinkedFieldId().";

    private static final String UNHANDLED_PREDICATE_CONTEXT =
        "Predicate used in unhandled context '%s'. " +
        "If the grammar was updated to allow predicates in new contexts, " +
        "add a handler for this case in enterPredicate().";

    private final ErrorCode errorCode;

    private TranslatorConfigurationException(ErrorCode errorCode, String template, Object... args) {
        super(args.length > 0 ? String.format(template, args) : template);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static TranslatorConfigurationException typeNotRegistered(Class<?> type) {
        return new TranslatorConfigurationException(ErrorCode.TYPE_NOT_REGISTERED, TYPE_NOT_REGISTERED, type.getName());
    }

    public static TranslatorConfigurationException unsupportedTypeInConditional(Class<?> type) {
        return new TranslatorConfigurationException(ErrorCode.UNSUPPORTED_TYPE_IN_CONDITIONAL, UNSUPPORTED_TYPE_IN_CONDITIONAL, type.getName());
    }

    public static TranslatorConfigurationException missingTypeMapping(Class<?> type, String mapName) {
        return new TranslatorConfigurationException(ErrorCode.MISSING_TYPE_MAPPING, MISSING_TYPE_MAPPING, type.getName(), mapName);
    }

    public static TranslatorConfigurationException missingTypeAnnotation(Class<?> type) {
        return new TranslatorConfigurationException(ErrorCode.MISSING_TYPE_ANNOTATION, MISSING_TYPE_ANNOTATION, type.getName());
    }

    public static TranslatorConfigurationException unknownExpressionType(Class<?> type) {
        return new TranslatorConfigurationException(ErrorCode.UNKNOWN_EXPRESSION_TYPE, UNKNOWN_EXPRESSION_TYPE, type.getName());
    }

    public static TranslatorConfigurationException unhandledVariableContext(String contextClassName) {
        return new TranslatorConfigurationException(ErrorCode.UNHANDLED_VARIABLE_CONTEXT, UNHANDLED_VARIABLE_CONTEXT, contextClassName);
    }

    public static TranslatorConfigurationException unhandledPrivacySetting(Object setting) {
        return new TranslatorConfigurationException(ErrorCode.UNHANDLED_PRIVACY_SETTING, UNHANDLED_PRIVACY_SETTING, setting);
    }

    public static TranslatorConfigurationException unhandledLinkedFieldProperty(String property) {
        return new TranslatorConfigurationException(ErrorCode.UNHANDLED_LINKED_FIELD_PROPERTY, UNHANDLED_LINKED_FIELD_PROPERTY, property);
    }

    public static TranslatorConfigurationException unhandledPredicateContext(String contextClassName) {
        return new TranslatorConfigurationException(ErrorCode.UNHANDLED_PREDICATE_CONTEXT, UNHANDLED_PREDICATE_CONTEXT, contextClassName);
    }
}
