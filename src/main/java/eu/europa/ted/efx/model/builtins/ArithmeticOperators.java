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
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The arithmetic operators: on numbers, and on dates and durations.
 */
public enum ArithmeticOperators implements BuiltIn {

  PLUS("+",
      numeric("+"),
      overload(signature(parameter("left", DURATION), parameter("right", DURATION)).returning(DURATION),
          (script, left, right) -> script.composeAddition(left, right)),
      overload(signature(parameter("date", DATE), parameter("duration", DURATION)).returning(DATE),
          (script, date, duration) -> script.composeAddition(date, duration)),
      overload(signature(parameter("duration", DURATION), parameter("date", DATE)).returning(DATE),
          (script, duration, date) -> script.composeAddition(date, duration))),

  MINUS("-",
      numeric("-"),
      overload(signature(parameter("left", DURATION), parameter("right", DURATION)).returning(DURATION),
          (script, left, right) -> script.composeSubtraction(left, right)),
      overload(signature(parameter("end", DATE), parameter("start", DATE)).returning(DURATION),
          (script, end, start) -> script.composeSubtraction(start, end)),
      overload(signature(parameter("date", DATE), parameter("duration", DURATION)).returning(DATE),
          (script, date, duration) -> script.composeSubtraction(date, duration))),

  MULTIPLY("*",
      numeric("*"),
      overload(signature(parameter("number", NUMBER), parameter("duration", DURATION)).returning(DURATION),
          (script, number, duration) -> script.composeMultiplication(number, duration)),
      overload(signature(parameter("duration", DURATION), parameter("number", NUMBER)).returning(DURATION),
          (script, duration, number) -> script.composeMultiplication(number, duration))),

  DIVIDE("/",
      numeric("/")),

  MODULO("%",
      numeric("%"));

  private final String name;
  private final List<Overload> overloads;

  ArithmeticOperators(final String name, final Overload... overloads) {
    this.name = name;
    this.overloads = List.of(overloads);
  }

  private static Overload numeric(final String symbol) {
    return overload(signature(parameter("left", NUMBER), parameter("right", NUMBER)).returning(NUMBER),
        (script, left, right) -> script.composeNumericOperation(left, symbol, right));
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public List<Overload> getOverloads() {
    return this.overloads;
  }
}
