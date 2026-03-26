package eu.europa.ted.efx.autocomplete;

import static eu.europa.ted.efx.autocomplete.EfxDataType.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Built-in EFX functions available for autocompletion.
 * Return types and signatures are derived from the EFX grammar parser rules.
 *
 * <p>Polymorphic sequence functions (sort, reverse, count, etc.) are represented as
 * explicit overloads — one enum entry per sequence type — so that each overload
 * carries fully typed parameters and return types.</p>
 */
public enum EfxBuiltInFunction {

  // Boolean functions (booleanFunction rule)
  NOT("not", INDICATOR,
      p("value", INDICATOR)),
  CONTAINS("contains", INDICATOR,
      p("haystack", TEXT), p("needle", TEXT)),
  STARTS_WITH("starts-with", INDICATOR,
      p("haystack", TEXT), p("needle", TEXT)),
  ENDS_WITH("ends-with", INDICATOR,
      p("haystack", TEXT), p("needle", TEXT)),
  SEQUENCE_EQUAL_TEXT("sequence-equal", INDICATOR,
      p("left", TEXT_SEQUENCE), p("right", TEXT_SEQUENCE)),
  SEQUENCE_EQUAL_NUMBER("sequence-equal", INDICATOR,
      p("left", NUMBER_SEQUENCE), p("right", NUMBER_SEQUENCE)),
  SEQUENCE_EQUAL_INDICATOR("sequence-equal", INDICATOR,
      p("left", INDICATOR_SEQUENCE), p("right", INDICATOR_SEQUENCE)),
  SEQUENCE_EQUAL_DATE("sequence-equal", INDICATOR,
      p("left", DATE_SEQUENCE), p("right", DATE_SEQUENCE)),
  SEQUENCE_EQUAL_TIME("sequence-equal", INDICATOR,
      p("left", TIME_SEQUENCE), p("right", TIME_SEQUENCE)),
  SEQUENCE_EQUAL_DURATION("sequence-equal", INDICATOR,
      p("left", DURATION_SEQUENCE), p("right", DURATION_SEQUENCE)),
  EMPTY("empty", INDICATOR,
      p("value", TEXT)),

  // Numeric functions (numericFunction rule)
  COUNT_TEXT("count", NUMBER,
      p("sequence", TEXT_SEQUENCE)),
  COUNT_NUMBER("count", NUMBER,
      p("sequence", NUMBER_SEQUENCE)),
  COUNT_INDICATOR("count", NUMBER,
      p("sequence", INDICATOR_SEQUENCE)),
  COUNT_DATE("count", NUMBER,
      p("sequence", DATE_SEQUENCE)),
  COUNT_TIME("count", NUMBER,
      p("sequence", TIME_SEQUENCE)),
  COUNT_DURATION("count", NUMBER,
      p("sequence", DURATION_SEQUENCE)),
  SUM("sum", NUMBER,
      p("values", NUMBER_SEQUENCE)),
  MIN("min", NUMBER,
      p("values", NUMBER_SEQUENCE)),
  MAX("max", NUMBER,
      p("values", NUMBER_SEQUENCE)),
  AVERAGE("average", NUMBER,
      p("values", NUMBER_SEQUENCE)),
  STRING_LENGTH("string-length", NUMBER,
      p("value", TEXT)),
  INDEX_OF_TEXT("index-of", NUMBER,
      p("sequence", TEXT_SEQUENCE), p("value", TEXT)),
  INDEX_OF_NUMBER("index-of", NUMBER,
      p("sequence", NUMBER_SEQUENCE), p("value", NUMBER)),
  INDEX_OF_INDICATOR("index-of", NUMBER,
      p("sequence", INDICATOR_SEQUENCE), p("value", INDICATOR)),
  INDEX_OF_DATE("index-of", NUMBER,
      p("sequence", DATE_SEQUENCE), p("value", DATE)),
  INDEX_OF_TIME("index-of", NUMBER,
      p("sequence", TIME_SEQUENCE), p("value", TIME)),
  INDEX_OF_DURATION("index-of", NUMBER,
      p("sequence", DURATION_SEQUENCE), p("value", DURATION)),
  INDEX_OF_SUBSTRING("index-of-substring", NUMBER,
      p("value", TEXT), p("substring", TEXT)),
  ABSOLUTE("absolute", NUMBER,
      p("value", NUMBER)),
  ROUND("round", NUMBER,
      p("value", NUMBER)),
  ROUND_DOWN("round-down", NUMBER,
      p("value", NUMBER)),
  ROUND_UP("round-up", NUMBER,
      p("value", NUMBER)),
  YEAR("year", NUMBER,
      p("value", DATE)),
  MONTH("month", NUMBER,
      p("value", DATE)),
  DAY("day", NUMBER,
      p("value", DATE)),
  HOURS("hours", NUMBER,
      p("value", TIME)),
  MINUTES("minutes", NUMBER,
      p("value", TIME)),
  SECONDS("seconds", NUMBER,
      p("value", TIME)),
  YEARS("years", NUMBER,
      p("value", DURATION)),
  MONTHS("months", NUMBER,
      p("value", DURATION)),
  DAYS("days", NUMBER,
      p("value", DURATION)),

  // Text functions (stringFunction rule)
  SUBSTRING("substring", TEXT,
      p("value", TEXT), p("start", NUMBER), pOpt("length", NUMBER)),
  SUBSTRING_BEFORE("substring-before", TEXT,
      p("value", TEXT), p("delimiter", TEXT)),
  SUBSTRING_AFTER("substring-after", TEXT,
      p("value", TEXT), p("delimiter", TEXT)),
  CONCAT("concat", TEXT,
      p("first", TEXT), pVar("rest", TEXT)),
  STRING_JOIN("string-join", TEXT,
      p("values", TEXT_SEQUENCE), p("separator", TEXT)),
  FORMAT_NUMBER("format-number", TEXT,
      p("value", NUMBER), pOpt("format", TEXT)),
  FORMAT_SHORT("format-short", TEXT,
      p("value", DATE), pOpt("time", TIME)),
  FORMAT_MEDIUM("format-medium", TEXT,
      p("value", DATE), pOpt("time", TIME)),
  FORMAT_LONG("format-long", TEXT,
      p("value", DATE), pOpt("time", TIME)),
  UPPER_CASE("upper-case", TEXT,
      p("value", TEXT)),
  LOWER_CASE("lower-case", TEXT,
      p("value", TEXT)),
  NORMALIZE_SPACE("normalize-space", TEXT,
      p("value", TEXT)),
  TRIM("trim", TEXT,
      p("value", TEXT)),
  TRIM_LEFT("trim-left", TEXT,
      p("value", TEXT)),
  TRIM_RIGHT("trim-right", TEXT,
      p("value", TEXT)),
  PAD_LEFT("pad-left", TEXT,
      p("value", TEXT), p("length", NUMBER), p("padChar", TEXT)),
  PAD_RIGHT("pad-right", TEXT,
      p("value", TEXT), p("length", NUMBER), p("padChar", TEXT)),
  REPLACE("replace", TEXT,
      p("value", TEXT), p("search", TEXT), p("replacement", TEXT)),
  REPLACE_REGEX("replace-regex", TEXT,
      p("value", TEXT), p("pattern", TEXT), p("replacement", TEXT)),
  REPEAT("repeat", TEXT,
      p("value", TEXT), p("count", NUMBER)),
  URL_ENCODE("url-encode", TEXT,
      p("value", TEXT)),
  CAPITALIZE_FIRST("capitalize-first", TEXT,
      p("value", TEXT)),
  PREFERRED_LANGUAGE("preferred-language", TEXT,
      p("field", TEXT)),
  PREFERRED_LANGUAGE_TEXT("preferred-language-text", TEXT,
      p("field", TEXT)),

  // Date functions (dateFunction rule)
  ADD_DURATION("add-duration", DATE,
      p("date", DATE), p("duration", DURATION)),
  SUBTRACT_DURATION("subtract-duration", DATE,
      p("date", DATE), p("duration", DURATION)),

  // Duration functions (durationFunction rule)
  DAY_TIME_DURATION("day-time-duration", DURATION,
      p("value", TEXT)),
  YEAR_MONTH_DURATION("year-month-duration", DURATION,
      p("value", TEXT)),

  // Sequence functions — overloads per sequence type
  DISTINCT_VALUES_TEXT("distinct-values", TEXT_SEQUENCE,
      p("sequence", TEXT_SEQUENCE)),
  DISTINCT_VALUES_NUMBER("distinct-values", NUMBER_SEQUENCE,
      p("sequence", NUMBER_SEQUENCE)),
  DISTINCT_VALUES_INDICATOR("distinct-values", INDICATOR_SEQUENCE,
      p("sequence", INDICATOR_SEQUENCE)),
  DISTINCT_VALUES_DATE("distinct-values", DATE_SEQUENCE,
      p("sequence", DATE_SEQUENCE)),
  DISTINCT_VALUES_TIME("distinct-values", TIME_SEQUENCE,
      p("sequence", TIME_SEQUENCE)),
  DISTINCT_VALUES_DURATION("distinct-values", DURATION_SEQUENCE,
      p("sequence", DURATION_SEQUENCE)),

  VALUE_UNION_TEXT("value-union", TEXT_SEQUENCE,
      p("left", TEXT_SEQUENCE), p("right", TEXT_SEQUENCE)),
  VALUE_UNION_NUMBER("value-union", NUMBER_SEQUENCE,
      p("left", NUMBER_SEQUENCE), p("right", NUMBER_SEQUENCE)),
  VALUE_UNION_INDICATOR("value-union", INDICATOR_SEQUENCE,
      p("left", INDICATOR_SEQUENCE), p("right", INDICATOR_SEQUENCE)),
  VALUE_UNION_DATE("value-union", DATE_SEQUENCE,
      p("left", DATE_SEQUENCE), p("right", DATE_SEQUENCE)),
  VALUE_UNION_TIME("value-union", TIME_SEQUENCE,
      p("left", TIME_SEQUENCE), p("right", TIME_SEQUENCE)),
  VALUE_UNION_DURATION("value-union", DURATION_SEQUENCE,
      p("left", DURATION_SEQUENCE), p("right", DURATION_SEQUENCE)),

  VALUE_INTERSECT_TEXT("value-intersect", TEXT_SEQUENCE,
      p("left", TEXT_SEQUENCE), p("right", TEXT_SEQUENCE)),
  VALUE_INTERSECT_NUMBER("value-intersect", NUMBER_SEQUENCE,
      p("left", NUMBER_SEQUENCE), p("right", NUMBER_SEQUENCE)),
  VALUE_INTERSECT_INDICATOR("value-intersect", INDICATOR_SEQUENCE,
      p("left", INDICATOR_SEQUENCE), p("right", INDICATOR_SEQUENCE)),
  VALUE_INTERSECT_DATE("value-intersect", DATE_SEQUENCE,
      p("left", DATE_SEQUENCE), p("right", DATE_SEQUENCE)),
  VALUE_INTERSECT_TIME("value-intersect", TIME_SEQUENCE,
      p("left", TIME_SEQUENCE), p("right", TIME_SEQUENCE)),
  VALUE_INTERSECT_DURATION("value-intersect", DURATION_SEQUENCE,
      p("left", DURATION_SEQUENCE), p("right", DURATION_SEQUENCE)),

  VALUE_EXCEPT_TEXT("value-except", TEXT_SEQUENCE,
      p("left", TEXT_SEQUENCE), p("right", TEXT_SEQUENCE)),
  VALUE_EXCEPT_NUMBER("value-except", NUMBER_SEQUENCE,
      p("left", NUMBER_SEQUENCE), p("right", NUMBER_SEQUENCE)),
  VALUE_EXCEPT_INDICATOR("value-except", INDICATOR_SEQUENCE,
      p("left", INDICATOR_SEQUENCE), p("right", INDICATOR_SEQUENCE)),
  VALUE_EXCEPT_DATE("value-except", DATE_SEQUENCE,
      p("left", DATE_SEQUENCE), p("right", DATE_SEQUENCE)),
  VALUE_EXCEPT_TIME("value-except", TIME_SEQUENCE,
      p("left", TIME_SEQUENCE), p("right", TIME_SEQUENCE)),
  VALUE_EXCEPT_DURATION("value-except", DURATION_SEQUENCE,
      p("left", DURATION_SEQUENCE), p("right", DURATION_SEQUENCE)),

  SORT_TEXT("sort", TEXT_SEQUENCE,
      p("sequence", TEXT_SEQUENCE)),
  SORT_NUMBER("sort", NUMBER_SEQUENCE,
      p("sequence", NUMBER_SEQUENCE)),
  SORT_INDICATOR("sort", INDICATOR_SEQUENCE,
      p("sequence", INDICATOR_SEQUENCE)),
  SORT_DATE("sort", DATE_SEQUENCE,
      p("sequence", DATE_SEQUENCE)),
  SORT_TIME("sort", TIME_SEQUENCE,
      p("sequence", TIME_SEQUENCE)),
  SORT_DURATION("sort", DURATION_SEQUENCE,
      p("sequence", DURATION_SEQUENCE)),

  REVERSE_TEXT("reverse", TEXT_SEQUENCE,
      p("sequence", TEXT_SEQUENCE)),
  REVERSE_NUMBER("reverse", NUMBER_SEQUENCE,
      p("sequence", NUMBER_SEQUENCE)),
  REVERSE_INDICATOR("reverse", INDICATOR_SEQUENCE,
      p("sequence", INDICATOR_SEQUENCE)),
  REVERSE_DATE("reverse", DATE_SEQUENCE,
      p("sequence", DATE_SEQUENCE)),
  REVERSE_TIME("reverse", TIME_SEQUENCE,
      p("sequence", TIME_SEQUENCE)),
  REVERSE_DURATION("reverse", DURATION_SEQUENCE,
      p("sequence", DURATION_SEQUENCE)),

  SUBSEQUENCE_TEXT("subsequence", TEXT_SEQUENCE,
      p("sequence", TEXT_SEQUENCE), p("start", NUMBER), pOpt("length", NUMBER)),
  SUBSEQUENCE_NUMBER("subsequence", NUMBER_SEQUENCE,
      p("sequence", NUMBER_SEQUENCE), p("start", NUMBER), pOpt("length", NUMBER)),
  SUBSEQUENCE_INDICATOR("subsequence", INDICATOR_SEQUENCE,
      p("sequence", INDICATOR_SEQUENCE), p("start", NUMBER), pOpt("length", NUMBER)),
  SUBSEQUENCE_DATE("subsequence", DATE_SEQUENCE,
      p("sequence", DATE_SEQUENCE), p("start", NUMBER), pOpt("length", NUMBER)),
  SUBSEQUENCE_TIME("subsequence", TIME_SEQUENCE,
      p("sequence", TIME_SEQUENCE), p("start", NUMBER), pOpt("length", NUMBER)),
  SUBSEQUENCE_DURATION("subsequence", DURATION_SEQUENCE,
      p("sequence", DURATION_SEQUENCE), p("start", NUMBER), pOpt("length", NUMBER)),

  // Split always returns a text sequence
  SPLIT("split", TEXT_SEQUENCE,
      p("value", TEXT), p("delimiter", TEXT));

  private final String label;
  private final EfxDataType dataType;
  private final List<EfxFunctionParameter> parameters;

  EfxBuiltInFunction(String label, EfxDataType dataType, EfxFunctionParameter... parameters) {
    this.label = label;
    this.dataType = dataType;
    this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
  }

  public String getLabel() {
    return label;
  }

  /**
   * @return the return type of this function overload.
   */
  public EfxDataType getDataType() {
    return dataType;
  }

  public List<EfxFunctionParameter> getParameters() {
    return parameters;
  }

  private static EfxFunctionParameter p(String name, EfxDataType type) {
    return new EfxFunctionParameter(name, type);
  }

  private static EfxFunctionParameter pOpt(String name, EfxDataType type) {
    return new EfxFunctionParameter(name, type, true, false);
  }

  private static EfxFunctionParameter pVar(String name, EfxDataType type) {
    return new EfxFunctionParameter(name, type, false, true);
  }
}
