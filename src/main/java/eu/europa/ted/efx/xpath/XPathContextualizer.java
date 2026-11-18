/*
 * Copyright 2022 European Union
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
package eu.europa.ted.efx.xpath;

import eu.europa.ted.eforms.xpath.XPathProcessor.Simplification;
import eu.europa.ted.eforms.xpath.XPathInfo;
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.eforms.xpath.XPathStep;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class XPathContextualizer {

  /**
   * Makes the given xpath relative to the given context xpath.
   *
   * @param contextXpath the context xpath
   * @param xpath        the xpath to contextualize
   * @return the contextualized xpath
   */
  public static PathExpression contextualize(final PathExpression contextXpath,
      final PathExpression xpath) {
    // If we are asked to contextualise against a null or empty context
    // then we must return the original xpath (instead of throwing an exception).
    if (contextXpath == null || contextXpath.getScript().isEmpty()) {
      return xpath;
    }

    String result = XPathProcessor.contextualize(contextXpath.getScript(), xpath.getScript());

    return Expression.instantiate(result, xpath.getClass());
  }

  /**
   * Joins the path of a context to a path that is relative to it, shortening the result as far as
   * the caller asks for.
   */
  public static PathExpression join(final PathExpression first, final PathExpression second,
      final Simplification simplification) {

    String joinedXPath =
        XPathProcessor.join(first.getScript(), second.getScript(), simplification);

    return Expression.instantiate(joinedXPath, second.getClass());
  }

  /**
   * Removes from the last step of a path the predicates that the last step of a declared path
   * carries. Predicates written in addition to the declared ones are kept.
   *
   * <p>
   * WITH EVERY uses it to build the context of a Schematron rule that applies to every element of
   * a node's or field's kind: for a node declared as
   * {@code /*}{@code /A/B[C='x']}, the path {@code /*}{@code /A/B[C='x'][D]} becomes
   * {@code /*}{@code /A/B[D]}.
   *
   * @param path the path to remove the predicates from
   * @param declaredPath the declared path of the node or field the path was built from
   * @return the path without the declared predicates of its last step
   */
  public static PathExpression removeDeclaredPredicatesOfLastStep(final PathExpression path,
      final PathExpression declaredPath) {
    final XPathInfo pathInfo = XPathProcessor.parse(path.getScript());
    final XPathInfo declaredInfo = XPathProcessor.parse(declaredPath.getScript());
    if (pathInfo.getSteps().isEmpty() || declaredInfo.getSteps().isEmpty()) {
      return path;
    }

    final List<XPathStep> steps = new ArrayList<>(pathInfo.getSteps());
    final XPathStep lastStep = steps.get(steps.size() - 1);
    // The declared predicates come first on the last step, followed by any written after the
    // reference. Only that leading run is removed, so a written predicate stays even when it is
    // identical to a declared one.
    final List<String> declaredPredicates = declaredInfo.getLastStep().getPredicates();
    final List<String> predicates = lastStep.getPredicates();
    if (declaredPredicates.isEmpty() || predicates.size() < declaredPredicates.size()
        || !predicates.subList(0, declaredPredicates.size()).equals(declaredPredicates)) {
      return path;
    }
    final List<String> keptPredicates =
        new ArrayList<>(predicates.subList(declaredPredicates.size(), predicates.size()));

    steps.set(steps.size() - 1, new XPathStep(lastStep.getStepText(), keptPredicates));
    final String result = pathInfo.getAnchor().getSeparator()
        + steps.stream().map(XPathStep::toString).collect(Collectors.joining("/"));
    return Expression.instantiate(result, path.getClass());
  }
}
