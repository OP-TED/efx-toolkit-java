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
package eu.europa.ted.efx.model.builtins;

import static eu.europa.ted.efx.model.constructs.Overload.overload;
import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.ParameterType.INDICATOR;
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER;
import static eu.europa.ted.efx.model.constructs.ParameterType.TEXT;
import static eu.europa.ted.efx.model.constructs.ParameterType.TEXT_SEQUENCE;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The functions on texts.
 */
public enum StringFunctions implements BuiltIn {

  EMPTY("empty",
      overload(signature(parameter("value", TEXT)).returning(INDICATOR),
          (script, value) -> script.composeComparisonOperation(value, "==",
              script.getStringLiteralFromUnquotedString("")))),

  CONTAINS("contains",
      overload(signature(parameter("haystack", TEXT), parameter("needle", TEXT)).returning(INDICATOR),
          (script, haystack, needle) -> script.composeContainsCondition(haystack, needle))),

  STARTS_WITH("starts-with",
      overload(signature(parameter("haystack", TEXT), parameter("needle", TEXT)).returning(INDICATOR),
          (script, haystack, needle) -> script.composeStartsWithCondition(haystack, needle))),

  ENDS_WITH("ends-with",
      overload(signature(parameter("haystack", TEXT), parameter("needle", TEXT)).returning(INDICATOR),
          (script, haystack, needle) -> script.composeEndsWithCondition(haystack, needle))),

  STRING_LENGTH("string-length",
      overload(signature(parameter("value", TEXT)).returning(NUMBER),
          (script, value) -> script.composeStringLengthCalculation(value))),

  INDEX_OF_SUBSTRING("index-of-substring",
      overload(signature(parameter("value", TEXT), parameter("substring", TEXT)).returning(NUMBER),
          (script, value, substring) -> script.composeIndexOfSubstringFunction(value, substring))),

  SUBSTRING("substring",
      overload(signature(parameter("value", TEXT), parameter("start", NUMBER)).returning(TEXT),
          (script, value, start) -> script.composeSubstringExtraction(value, start)),
      overload(signature(parameter("value", TEXT), parameter("start", NUMBER), parameter("length", NUMBER))
          .returning(TEXT),
          (script, value, start, length) ->
              script.composeSubstringExtraction(value, start, length))),

  SUBSTRING_BEFORE("substring-before",
      overload(signature(parameter("value", TEXT), parameter("delimiter", TEXT)).returning(TEXT),
          (script, value, delimiter) -> script.composeSubstringBeforeFunction(value, delimiter))),

  SUBSTRING_AFTER("substring-after",
      overload(signature(parameter("value", TEXT), parameter("delimiter", TEXT)).returning(TEXT),
          (script, value, delimiter) -> script.composeSubstringAfterFunction(value, delimiter))),

  UPPER_CASE("upper-case",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeToUpperCaseConversion(value))),

  LOWER_CASE("lower-case",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeToLowerCaseConversion(value))),

  NORMALIZE_SPACE("normalize-space",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeNormalizeSpaceFunction(value))),

  TRIM("trim",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeTrimFunction(value))),

  TRIM_LEFT("trim-left",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeTrimLeftFunction(value))),

  TRIM_RIGHT("trim-right",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeTrimRightFunction(value))),

  PAD_LEFT("pad-left",
      overload(signature(parameter("value", TEXT), parameter("length", NUMBER), parameter("padChar", TEXT))
          .returning(TEXT),
          (script, value, length, padChar) ->
              script.composePadLeftFunction(value, length, padChar))),

  PAD_RIGHT("pad-right",
      overload(signature(parameter("value", TEXT), parameter("length", NUMBER), parameter("padChar", TEXT))
          .returning(TEXT),
          (script, value, length, padChar) ->
              script.composePadRightFunction(value, length, padChar))),

  REPEAT("repeat",
      overload(signature(parameter("value", TEXT), parameter("count", NUMBER)).returning(TEXT),
          (script, value, count) -> script.composeRepeatFunction(value, count))),

  REPLACE("replace",
      overload(signature(parameter("value", TEXT), parameter("search", TEXT), parameter("replacement", TEXT))
          .returning(TEXT),
          (script, value, search, replacement) ->
              script.composeReplaceFunction(value, search, replacement))),

  REPLACE_REGEX("replace-regex",
      overload(signature(parameter("value", TEXT), parameter("pattern", TEXT), parameter("replacement", TEXT))
          .returning(TEXT),
          (script, value, pattern, replacement) ->
              script.composeReplaceRegexFunction(value, pattern, replacement))),

  URL_ENCODE("url-encode",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeUrlEncodeFunction(value))),

  CAPITALIZE_FIRST("capitalize-first",
      overload(signature(parameter("value", TEXT)).returning(TEXT),
          (script, value) -> script.composeCapitalizeFirstFunction(value))),

  CONCAT("concat",
      overload(signature().variadic(parameter("values", TEXT)).returning(TEXT),
          (script, values) -> script.composeStringConcatenation(values))),

  STRING_JOIN("string-join",
      overload(signature(parameter("values", TEXT_SEQUENCE), parameter("separator", TEXT)).returning(TEXT),
          (script, values, separator) -> script.composeStringJoin(values, separator))),

  SPLIT("split",
      overload(signature(parameter("value", TEXT), parameter("delimiter", TEXT)).returning(TEXT_SEQUENCE),
          (script, value, delimiter) -> script.composeSplitFunction(value, delimiter)));

  private final String name;
  private final List<Overload> overloads;

  StringFunctions(final String name, final Overload... overloads) {
    this.name = name;
    this.overloads = List.of(overloads);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public List<Overload> getOverloads() {
    return this.overloads;
  }

  @Override
  public boolean isFunction() {
    return true;
  }
}
