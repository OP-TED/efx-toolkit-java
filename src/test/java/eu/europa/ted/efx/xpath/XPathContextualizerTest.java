/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.expressions.scalar.NodePath;

class XPathContextualizerTest {

  private static String removeDeclared(String path, String declaredPath) {
    return XPathContextualizer
        .removeDeclaredPredicatesOfLastStep(new NodePath(path), new NodePath(declaredPath))
        .getScript();
  }

  @Test
  void testRemoveDeclaredPredicates_WrittenPredicateIdenticalToDeclared() {
    assertEquals("/*/A/B[C='x']", removeDeclared("/*/A/B[C='x'][C='x']", "/*/A/B[C='x']"));
  }

  @Test
  void testRemoveDeclaredPredicates_AncestorPredicatesKept() {
    assertEquals("/*/A[F='y']/B", removeDeclared("/*/A[F='y']/B[C='x']", "/*/A[F='y']/B[C='x']"));
  }

  @Test
  void testRemoveDeclaredPredicates_PredicateWithSlashAndQuotes() {
    assertEquals("/*/A/B", removeDeclared("/*/A/B[C/D='a/b[c]']", "/*/A/B[C/D='a/b[c]']"));
  }

  @Test
  void testRemoveDeclaredPredicates_NoDeclaredPredicate() {
    assertEquals("/*/A/B[D]", removeDeclared("/*/A/B[D]", "/*/A/B"));
  }

  @Test
  void testRemoveDeclaredPredicates_DeclaredPredicateNotFirst() {
    // The declared predicates always come first; a path that does not start with them is kept.
    assertEquals("/*/A/B[D][C='x']", removeDeclared("/*/A/B[D][C='x']", "/*/A/B[C='x']"));
  }

  @Test
  void testRemoveDeclaredPredicates_FewerPredicatesThanDeclared() {
    assertEquals("/*/A/B[C]", removeDeclared("/*/A/B[C]", "/*/A/B[C][D]"));
  }
}
