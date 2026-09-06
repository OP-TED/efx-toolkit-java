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
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;

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
}
