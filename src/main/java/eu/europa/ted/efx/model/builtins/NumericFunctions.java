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
import static eu.europa.ted.efx.model.constructs.ParameterType.NODES;
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER;
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER_SEQUENCE;
import static eu.europa.ted.efx.model.constructs.ParameterType.T_SEQUENCE;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The functions that count, aggregate or round numbers.
 */
public enum NumericFunctions implements BuiltIn {

  COUNT("count",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(NUMBER),
          (script, sequence) -> script.composeCountOperation(sequence)),
      overload(signature(parameter("nodes", NODES)).returning(NUMBER),
          (script, nodes) -> script.composeCountOperation(nodes))),

  SUM("sum",
      overload(signature(parameter("values", NUMBER_SEQUENCE)).returning(NUMBER),
          (script, values) -> script.composeSumOperation(values))),

  MIN("min",
      overload(signature(parameter("values", NUMBER_SEQUENCE)).returning(NUMBER),
          (script, values) -> script.composeMinFunction(values))),

  MAX("max",
      overload(signature(parameter("values", NUMBER_SEQUENCE)).returning(NUMBER),
          (script, values) -> script.composeMaxFunction(values))),

  AVERAGE("average",
      overload(signature(parameter("values", NUMBER_SEQUENCE)).returning(NUMBER),
          (script, values) -> script.composeAvgFunction(values))),

  ABSOLUTE("absolute",
      overload(signature(parameter("value", NUMBER)).returning(NUMBER),
          (script, value) -> script.composeAbsFunction(value))),

  ROUND("round",
      overload(signature(parameter("value", NUMBER)).returning(NUMBER),
          (script, value) -> script.composeRoundFunction(value))),

  ROUND_DOWN("round-down",
      overload(signature(parameter("value", NUMBER)).returning(NUMBER),
          (script, value) -> script.composeFloorFunction(value))),

  ROUND_UP("round-up",
      overload(signature(parameter("value", NUMBER)).returning(NUMBER),
          (script, value) -> script.composeCeilingFunction(value)));

  private final String name;
  private final List<Overload> overloads;

  NumericFunctions(final String name, final Overload... overloads) {
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
