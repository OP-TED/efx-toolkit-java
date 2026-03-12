package eu.europa.ted.efx.autocomplete;

import static eu.europa.ted.efx.autocomplete.EfxDataType.*;

/**
 * Built-in EFX field properties (prefixed with ":") available for autocompletion.
 * Data types are derived from the expression type each property appears in within the grammar.
 */
public enum EfxLinkedProperty {

  // Linked field properties (linkedFieldProperty rule)
  PUBLICATION_DATE(":publicationDate", DATE),
  JUSTIFICATION_CODE(":justificationCode", TEXT),
  JUSTIFICATION_DESCRIPTION(":justificationDescription", TEXT),

  // Metadata property (textExpression rule)
  PRIVACY_CODE(":privacyCode", TEXT),

  // Computed properties (indicatorExpression rule)
  WAS_WITHHELD(":wasWithheld", INDICATOR),
  IS_WITHHELD(":isWithheld", INDICATOR),
  IS_WITHHOLDABLE(":isWithholdable", INDICATOR),
  IS_DISCLOSED(":isDisclosed", INDICATOR),
  IS_MASKED(":isMasked", INDICATOR),

  // Raw value (textExpression rule)
  RAW_VALUE(":rawValue", TEXT),

  // Preferred language properties (stringExpression rule, template-only)
  PREFERRED_LANGUAGE(":preferredLanguage", TEXT),
  PREFERRED_LANGUAGE_TEXT(":preferredLanguageText", TEXT);

  private final String label;
  private final EfxDataType dataType;

  EfxLinkedProperty(String label, EfxDataType dataType) {
    this.label = label;
    this.dataType = dataType;
  }

  public String getLabel() {
    return label;
  }

  public EfxDataType getDataType() {
    return dataType;
  }
}
