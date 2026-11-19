/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.builtins.SequenceFunctions;
import eu.europa.ted.efx.model.builtins.StringFunctions;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;

/**
 * The concrete forms of a signature, which the autocomplete catalogue lists.
 */
class SignatureTest {

  @Test
  void testConcreteForms_OfAGenericSignature_BindTOncePerValueType() {
    final List<Signature> forms =
        SequenceFunctions.DISTINCT_VALUES.getOverloads().get(0).getSignature().concreteForms();
    final List<Class<?>> sequences = List.of(StringSequenceExpression.class,
        BooleanSequenceExpression.class, NumericSequenceExpression.class,
        DateSequenceExpression.class, TimeSequenceExpression.class,
        DurationSequenceExpression.class);
    assertEquals(sequences, forms.stream()
        .map(form -> form.getParameters().get(0).getType().getExpressionType())
        .collect(Collectors.toList()));
    assertEquals(sequences, forms.stream()
        .map(form -> form.getResult().getExpressionType())
        .collect(Collectors.toList()));
  }

  @Test
  void testConcreteForms_OfAConcreteSignature_IsTheSignature() {
    final Signature signature = StringFunctions.SUBSTRING.getOverloads().get(0).getSignature();
    assertEquals(List.of(signature), signature.concreteForms());
  }
}
