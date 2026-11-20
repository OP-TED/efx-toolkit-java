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
import static eu.europa.ted.efx.model.constructs.ParameterType.DURATION;
import static eu.europa.ted.efx.model.constructs.ParameterType.INDICATOR;
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER;
import static eu.europa.ted.efx.model.constructs.ParameterType.TEXT;
import static eu.europa.ted.efx.model.constructs.ParameterType.TIME;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The functions that convert a value from one primitive to another.
 */
public enum ConversionFunctions implements BuiltIn {

  TO_TEXT("text",
      overload(signature(parameter("value", NUMBER)).returning(TEXT),
          (script, value) -> script.composeToStringConversion(value)),
      overload(signature(parameter("value", INDICATOR)).returning(TEXT),
          (script, value) -> script.composeToStringConversion(value)),
      overload(signature(parameter("value", DATE)).returning(TEXT),
          (script, value) -> script.composeToStringConversion(value)),
      overload(signature(parameter("value", TIME)).returning(TEXT),
          (script, value) -> script.composeToStringConversion(value)),
      overload(signature(parameter("value", DURATION)).returning(TEXT),
          (script, value) -> script.composeToStringConversion(value))),

  TO_NUMBER("number",
      overload(signature(parameter("value", TEXT)).returning(NUMBER),
          (script, value) -> script.composeToNumberConversion(value)),
      overload(signature(parameter("value", INDICATOR)).returning(NUMBER),
          (script, value) -> script.composeToNumberConversion(value))),

  TO_INDICATOR("indicator",
      overload(signature(parameter("value", NUMBER)).returning(INDICATOR),
          (script, value) -> script.composeToBooleanConversion(value))),

  TO_DATE("date",
      overload(signature(parameter("value", TEXT)).returning(DATE),
          (script, value) -> script.composeToDateConversion(value))),

  TO_TIME("time",
      overload(signature(parameter("value", TEXT)).returning(TIME),
          (script, value) -> script.composeToTimeConversion(value))),

  TO_DAY_TIME_DURATION("day-time-duration",
      overload(signature(parameter("value", TEXT)).returning(DURATION),
          (script, value) -> script.composeToDayTimeDurationConversion(value))),

  TO_YEAR_MONTH_DURATION("year-month-duration",
      overload(signature(parameter("value", TEXT)).returning(DURATION),
          (script, value) -> script.composeToYearMonthDurationConversion(value)));

  private final String name;
  private final List<Overload> overloads;

  ConversionFunctions(final String name, final Overload... overloads) {
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
