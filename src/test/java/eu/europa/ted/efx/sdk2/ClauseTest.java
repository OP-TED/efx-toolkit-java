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
package eu.europa.ted.efx.sdk2;

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.ParameterType.TEXT_SEQUENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;

/**
 * Proves that a clause gives its expression converted to its parameter: the class the statement
 * it belongs to reads.
 */
class ClauseTest {

  @Test
  void testClauseOfOneValue_GivesItConvertedToItsParameter() {
    assertConverted(StringSequenceExpression.class,
        Clause.of("Iterator 'text:$x'", parameter("sequence", TEXT_SEQUENCE)),
        value(StringExpression.class));
  }

  @Test
  void testClauseOfAValueAsGiven_KeepsASingleValue() {
    assertConverted(StringExpression.class, Clause.ofValueAsGiven("'COMPUTE'", "expression"),
        value(StringExpression.class));
  }

  @Test
  void testClauseOfAValueAsGiven_KeepsASequence() {
    assertConverted(StringSequenceExpression.class,
        Clause.ofValueAsGiven("'COMPUTE'", "expression"), value(StringSequenceExpression.class));
  }

  private static void assertConverted(final Class<? extends TypedExpression> expected,
      final Clause clause, final TypedExpression argument) {
    assertEquals(expected,
        clause.match(List.of(argument)).orElseThrow().at(0, TypedExpression.class).getClass());
  }

  private static TypedExpression value(final Class<? extends TypedExpression> type) {
    return Expression.instantiate(type.getSimpleName(), type);
  }
}
