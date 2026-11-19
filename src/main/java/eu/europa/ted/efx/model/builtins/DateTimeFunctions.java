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
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER;
import static eu.europa.ted.efx.model.constructs.ParameterType.TIME;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The functions on dates, times and durations.
 */
public enum DateTimeFunctions implements BuiltIn {

  YEAR("year",
      overload(signature(parameter("value", DATE)).returning(NUMBER),
          (script, value) -> script.composeYearFunction(value))),

  MONTH("month",
      overload(signature(parameter("value", DATE)).returning(NUMBER),
          (script, value) -> script.composeMonthFunction(value))),

  DAY("day",
      overload(signature(parameter("value", DATE)).returning(NUMBER),
          (script, value) -> script.composeDayFunction(value))),

  HOURS("hours",
      overload(signature(parameter("value", TIME)).returning(NUMBER),
          (script, value) -> script.composeHoursFunction(value))),

  MINUTES("minutes",
      overload(signature(parameter("value", TIME)).returning(NUMBER),
          (script, value) -> script.composeMinutesFunction(value))),

  SECONDS("seconds",
      overload(signature(parameter("value", TIME)).returning(NUMBER),
          (script, value) -> script.composeSecondsFunction(value))),

  YEARS("years",
      overload(signature(parameter("value", DURATION)).returning(NUMBER),
          (script, value) -> script.composeYearsFromDurationFunction(value))),

  MONTHS("months",
      overload(signature(parameter("value", DURATION)).returning(NUMBER),
          (script, value) -> script.composeMonthsFromDurationFunction(value))),

  DAYS("days",
      overload(signature(parameter("value", DURATION)).returning(NUMBER),
          (script, value) -> script.composeDaysFromDurationFunction(value))),

  CURRENT_DATE("current-date",
      overload(signature().returning(DATE),
          script -> script.getCurrentDate())),

  CURRENT_TIME("current-time",
      overload(signature().returning(TIME),
          script -> script.getCurrentTime())),

  ADD_DURATION("add-duration",
      overload(signature(parameter("date", DATE), parameter("duration", DURATION)).returning(DATE),
          (script, date, duration) -> script.composeAddition(date, duration))),

  SUBTRACT_DURATION("subtract-duration",
      overload(signature(parameter("date", DATE), parameter("duration", DURATION)).returning(DATE),
          (script, date, duration) -> script.composeSubtraction(date, duration)));

  private final String name;
  private final List<Overload> overloads;

  DateTimeFunctions(final String name, final Overload... overloads) {
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
