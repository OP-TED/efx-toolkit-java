package eu.europa.ted.efx.autocomplete;

/**
 * Built-in EFX keywords (type names and boolean literals) available for autocompletion.
 */
public enum EfxKeyword {

  // Type names
  TEXT("text", EfxDataType.TEXT),
  NUMBER("number", EfxDataType.NUMBER),
  INDICATOR("indicator", EfxDataType.INDICATOR),
  DATE("date", EfxDataType.DATE),
  TIME("time", EfxDataType.TIME),

  // Indicator literals
  TRUE("TRUE", EfxDataType.INDICATOR),
  FALSE("FALSE", EfxDataType.INDICATOR),
  ALWAYS("ALWAYS", EfxDataType.INDICATOR),
  NEVER("NEVER", EfxDataType.INDICATOR);

  private final String label;
  private final EfxDataType dataType;

  EfxKeyword(String label, EfxDataType dataType) {
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
