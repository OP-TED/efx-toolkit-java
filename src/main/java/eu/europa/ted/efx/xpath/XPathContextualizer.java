package eu.europa.ted.efx.xpath;

import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.model.expressions.path.PathExpression;

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

    return PathExpression.instantiate(result, xpath.getDataType());
  }

  public static PathExpression join(final PathExpression first, final PathExpression second) {

    String joinedXPath = XPathProcessor.join(first.getScript(), second.getScript());

    return PathExpression.instantiate(joinedXPath, second.getDataType());
  }
}
