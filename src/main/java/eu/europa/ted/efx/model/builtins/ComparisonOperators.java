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
import static eu.europa.ted.efx.model.constructs.ParameterType.T;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The comparison operators. Each compares two single values of the same value primitive.
 */
public enum ComparisonOperators implements BuiltIn {

  EQUAL("=="),

  NOT_EQUAL("!="),

  LESS("<"),

  LESS_OR_EQUAL("<="),

  GREATER(">"),

  GREATER_OR_EQUAL(">=");

  private final String name;
  private final List<Overload> overloads;

  ComparisonOperators(final String symbol) {
    this.name = symbol;
    this.overloads = List.of(
        overload(signature(parameter("left", T), parameter("right", T)).returning(INDICATOR),
            (script, left, right) -> script.composeComparisonOperation(left, symbol, right)));
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
