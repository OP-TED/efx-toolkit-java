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
package eu.europa.ted.efx.model;

import eu.europa.ted.efx.interfaces.SymbolResolver;

/**
 * Identifies the companion fields associated with a withholdable field's privacy settings.
 * Used with {@link SymbolResolver#getPrivacySettingOfField(String, PrivacySetting)} to look up
 * the field ID of a specific privacy companion field.
 */
public enum PrivacySetting {
    PRIVACY_CODE_FIELD,
    PUBLICATION_DATE_FIELD,
    JUSTIFICATION_CODE_FIELD,
    JUSTIFICATION_DESCRIPTION_FIELD
}
