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
import static eu.europa.ted.efx.model.constructs.ParameterType.T;
import static eu.europa.ted.efx.model.constructs.ParameterType.T_SEQUENCE;
import static eu.europa.ted.efx.model.constructs.Signature.signature;

import java.util.List;

import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.constructs.Composer;
import eu.europa.ted.efx.model.constructs.Overload;

/**
 * The functions on sequences of any value primitive.
 */
public enum SequenceFunctions implements BuiltIn {

  DISTINCT_VALUES("distinct-values",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequence(ScriptGenerator::composeDistinctValuesFunction))),

  COUNT_DUPLICATES("count-duplicates",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(NUMBER),
          (script, sequence) -> script.composeCountDuplicatesFunction(sequence))),

  GET_DUPLICATES("get-duplicates",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequence(ScriptGenerator::composeGetDuplicatesFunction))),

  VALUE_UNION("value-union",
      overload(signature(parameter("left", T_SEQUENCE), parameter("right", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequencePair(ScriptGenerator::composeUnionFunction))),

  VALUE_INTERSECT("value-intersect",
      overload(signature(parameter("left", T_SEQUENCE), parameter("right", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequencePair(ScriptGenerator::composeIntersectFunction))),

  VALUE_EXCEPT("value-except",
      overload(signature(parameter("left", T_SEQUENCE), parameter("right", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequencePair(ScriptGenerator::composeExceptFunction))),

  SEQUENCE_EQUAL("sequence-equal",
      overload(signature(parameter("left", T_SEQUENCE), parameter("right", T_SEQUENCE)).returning(INDICATOR),
          (script, left, right) -> script.composeSequenceEqualFunction(left, right))),

  SORT("sort",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequence(ScriptGenerator::composeSortFunction))),

  REVERSE("reverse",
      overload(signature(parameter("sequence", T_SEQUENCE)).returning(T_SEQUENCE),
          Composer.ofSequence(ScriptGenerator::composeReverseFunction))),

  SUBSEQUENCE("subsequence",
      overload(signature(parameter("sequence", T_SEQUENCE), parameter("start", NUMBER)).returning(T_SEQUENCE),
          Composer.ofSubsequence(ScriptGenerator::composeSubsequenceFunction)),
      overload(signature(parameter("sequence", T_SEQUENCE), parameter("start", NUMBER),
          parameter("length", NUMBER)).returning(T_SEQUENCE),
          Composer.ofSubsequenceWithLength(ScriptGenerator::composeSubsequenceFunction))),

  INDEX_OF("index-of",
      overload(signature(parameter("sequence", T_SEQUENCE), parameter("value", T)).returning(NUMBER),
          (script, sequence, value) -> script.composeIndexOfFunction(sequence, value)));

  private final String name;
  private final List<Overload> overloads;

  SequenceFunctions(final String name, final Overload... overloads) {
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
