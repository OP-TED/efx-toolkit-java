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
import static eu.europa.ted.efx.model.constructs.ParameterType.DATE;
import static eu.europa.ted.efx.model.constructs.ParameterType.MULTILINGUAL_FIELD;
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER;
import static eu.europa.ted.efx.model.constructs.ParameterType.TEXT;
import static eu.europa.ted.efx.model.constructs.ParameterType.TIME;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.constructs.Composer;
import eu.europa.ted.efx.model.constructs.Overload;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;

/**
 * The functions that may only be used in a template: formatting, and the preferred language of a
 * multilingual field.
 */
public enum TemplateFunctions implements BuiltIn {

  FORMAT_NUMBER("format-number",
      overload(signature(parameter("value", NUMBER), parameter("format", TEXT)).returning(TEXT),
          (script, value, format) -> script.composeNumberFormatting(value, format))),

  FORMAT_SHORT("format-short",
      formats(ScriptGenerator::composeFormatDateShort, ScriptGenerator::composeFormatTimeShort)),

  FORMAT_MEDIUM("format-medium",
      formats(ScriptGenerator::composeFormatDateMedium, ScriptGenerator::composeFormatTimeMedium)),

  FORMAT_LONG("format-long",
      formats(ScriptGenerator::composeFormatDateLong, ScriptGenerator::composeFormatTimeLong)),

  PREFERRED_LANGUAGE("preferred-language",
      overload(signature(parameter("field", MULTILINGUAL_FIELD)).returning(TEXT),
          (script, field) -> script.getPreferredLanguage(field))),

  PREFERRED_LANGUAGE_TEXT("preferred-language-text",
      overload(signature(parameter("field", MULTILINGUAL_FIELD)).returning(TEXT),
          (script, field) -> script.getTextInPreferredLanguage(field)));

  private final String name;
  private final List<Overload> overloads;

  TemplateFunctions(final String name, final Overload... overloads) {
    this.name = name;
    this.overloads = List.of(overloads);
  }

  /**
   * Declares the three overloads of a date and time format: a date, a time, or a date and a time
   * separated by a space.
   */
  private static Overload[] formats(
      final Composer.Unary<DateExpression, StringExpression> formatDate,
      final Composer.Unary<TimeExpression, StringExpression> formatTime) {
    return new Overload[] {
        overload(signature(parameter("value", DATE)).returning(TEXT), formatDate),
        overload(signature(parameter("value", TIME)).returning(TEXT), formatTime),
        overload(signature(parameter("value", DATE), parameter("time", TIME)).returning(TEXT),
            (script, value, time) -> script.composeStringConcatenation(List.of(
                formatDate.compose(script, value),
                script.getStringLiteralFromUnquotedString(" "),
                formatTime.compose(script, time))))};
  }

  @Override
  public boolean isTemplateOnly() {
    return true;
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
