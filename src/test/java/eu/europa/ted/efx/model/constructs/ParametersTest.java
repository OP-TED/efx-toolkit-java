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
package eu.europa.ted.efx.model.constructs;

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.identifiers.ParsedParameters;

class ParametersTest {

  @Test
  void testEquality_IsThatOfAList() {
    final ParsedParameters none = new ParsedParameters();
    assertEquals(List.of(), none);
    assertEquals(none, List.of());
    assertEquals(List.of().hashCode(), none.hashCode());
    final Set<List<?>> lists = new HashSet<>();
    lists.add(none);
    assertTrue(lists.contains(List.of()));

    final Parameter x = parameter("x", ParameterType.TEXT);
    final Parameters<Parameter> one = Parameters.of(List.of(x));
    assertEquals(List.of(x), one);
    assertEquals(one, List.of(x));
    assertEquals(List.of(x).hashCode(), one.hashCode());
  }

  @Test
  void testParameter_IsEqualToOneOfTheSameNameAndType() {
    final Parameter x = parameter("x", ParameterType.TEXT);
    final Parameter bound = parameter("x", ParameterType.of(StringExpression.class));
    assertEquals(x, bound);
    assertEquals(x.hashCode(), bound.hashCode());
    assertNotEquals(x, parameter("y", ParameterType.TEXT));
    assertNotEquals(x, parameter("x", ParameterType.NUMBER));
  }

  @Test
  void testParameter_WithoutANameOrAType_IsEqualToItself() {
    final Parameter unnamed = parameter(null, ParameterType.TEXT);
    final Parameter untyped = parameter("x", ParameterType.of(null));
    assertEquals(unnamed, unnamed);
    assertEquals(unnamed.hashCode(), parameter(null, ParameterType.TEXT).hashCode());
    assertEquals(untyped, parameter("x", ParameterType.of(null)));
    assertEquals(untyped.hashCode(), parameter("x", ParameterType.of(null)).hashCode());
    assertNotEquals(untyped, parameter("x", ParameterType.TEXT));
  }
}
