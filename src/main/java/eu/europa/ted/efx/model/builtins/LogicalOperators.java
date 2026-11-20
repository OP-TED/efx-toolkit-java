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
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The logical operators {@code and} and {@code or}, and the function {@code not}.
 */
public enum LogicalOperators implements BuiltIn {

  AND("and",
      overload(signature(parameter("left", INDICATOR), parameter("right", INDICATOR)).returning(INDICATOR),
          (script, left, right) -> script.composeLogicalAnd(left, right))),

  OR("or",
      overload(signature(parameter("left", INDICATOR), parameter("right", INDICATOR)).returning(INDICATOR),
          (script, left, right) -> script.composeLogicalOr(left, right))),

  NOT("not",
      overload(signature(parameter("value", INDICATOR)).returning(INDICATOR),
          (script, value) -> script.composeLogicalNot(value)));

  private final String name;
  private final List<Overload> overloads;

  LogicalOperators(final String name, final Overload... overloads) {
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

  /**
   * @return {@code true} for {@code not}, which is called as a function
   */
  @Override
  public boolean isFunction() {
    return this == NOT;
  }
}
