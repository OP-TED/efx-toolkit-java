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
 * Exception thrown when the toolkit encounters inconsistent data in the SDK.
 * This indicates a problem with the SDK data, not a user error or a toolkit bug.
 */
public class SdkInconsistencyException extends IllegalStateException {

    public enum ErrorCode {
        MISSING_PRIVACY_CODE_FIELD,
        MISSING_PUBLICATION_DATE_FIELD,
        UNKNOWN_DATA_TYPE
    }

    private static final String MISSING_PRIVACY_CODE_FIELD =
        "Field '%s' has a privacy code but no privacy code field ID. "
        + "This indicates inconsistent privacy settings in the SDK data.";

    private static final String MISSING_PUBLICATION_DATE_FIELD =
        "Field '%s' has a privacy code but no publication date field ID. "
        + "This indicates inconsistent privacy settings in the SDK data.";

    private static final String UNKNOWN_DATA_TYPE =
        "Unknown data type '%s'. "
        + "This indicates a field type that is not defined in the SDK.";

    private final ErrorCode errorCode;

    private SdkInconsistencyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static SdkInconsistencyException missingPrivacyCodeField(String fieldId) {
        return new SdkInconsistencyException(ErrorCode.MISSING_PRIVACY_CODE_FIELD,
                String.format(MISSING_PRIVACY_CODE_FIELD, fieldId));
    }

    public static SdkInconsistencyException missingPublicationDateField(String fieldId) {
        return new SdkInconsistencyException(ErrorCode.MISSING_PUBLICATION_DATE_FIELD,
                String.format(MISSING_PUBLICATION_DATE_FIELD, fieldId));
    }

    public static SdkInconsistencyException unknownDataType(String fieldType) {
        return new SdkInconsistencyException(ErrorCode.UNKNOWN_DATA_TYPE,
                String.format(UNKNOWN_DATA_TYPE, fieldType));
    }
}
