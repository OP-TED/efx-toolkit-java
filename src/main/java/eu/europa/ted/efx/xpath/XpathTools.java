package eu.europa.ted.efx.xpath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class XpathTools {

  private static final Pattern XPATH_PART_TOKENIZER =
      Pattern.compile("(?<!\\[[^\\[\\]]*)(/)(?![^\\[\\]]*])");

  /**
   * NOTE: this was initially taken over from the MDC. We will have to see how to reconcile later.
   *
   * Creates xpath relative to the context xpath. For examples see unit tests on this method.
   *
   * @param xpath An XPath location, as a stripped and normalized string.
   * @param context XPath location of the context xpath, as a stripped and normalized string.
   * @return xpath relative to context
   */
  public static String contextualizeFromXPath(final String xpath, final String context) {
    // System.out.println(
    // String.format("contextualizeFromXPath attempting: context=%s, xpath=%s", context, xpath));

    if (xpath.isBlank()) {
      throw new IllegalArgumentException(
          String.format("contextualizeFromXPath: Invalid blank xpath for context=%s", context));
    }
    if (context.isBlank()) {
      throw new IllegalArgumentException(
          String.format("contextualizeFromXPath: Invalid blank context for xpath=%s", xpath));
    }
    if (!xpath.strip().equals(xpath)) {
      throw new IllegalArgumentException(
          String.format("contextualizeFromXPath: Expecting stripped xpath=%s", xpath));
    }
    if (!context.strip().equals(context)) {
      throw new IllegalArgumentException(
          String.format("contextualizeFromXPath: Expecting stripped context=%s", context));
    }
    if (context.startsWith("not(")) {
      throw new IllegalArgumentException(String.format(
          "contextualizeFromXPath: cannot start with not(, expecting simple xpath here, context=%s",
          context));
    }
    if (xpath.equals(context)) {
      // Return the XPath for the current node
      return ".";
    }

    // IMPORTANT:
    // This is not a full xpath parser.
    // This code will only handle known cases, see unit tests calling this method.
    // If more cases have to be supported, add a unit test and extend the code, of
    // course other tests must still pass.

    // In general: treat special cases before general cases.

    // -------------unmodifiableList: read only!
    final var xpathL =
        Collections.unmodifiableList(XpathTools.buildXPathLocation(xpath).getSteps());
    final var contextL =
        Collections.unmodifiableList(XpathTools.buildXPathLocation(context).getSteps());
    if (xpathL.isEmpty() || contextL.isEmpty()) {
      return xpath; // Abort.
    }

    // --------------------------------------------------------------------
    // Check if the xpath is contextualizable (general case)
    // --------------------------------------------------------------------
    {
      boolean isContextualizable = false;
      for (int k = 0; k < contextL.size(); k++) {
        if (k >= xpathL.size()) {
          break;
        }
        final XPathStep cStep = contextL.get(k);
        final XPathStep xStep = xpathL.get(k);

        assert !cStep.isAttribute();
        assert !xStep.isAttribute();

        // First non-start element must match!
        if (!"*".equalsIgnoreCase(cStep.toString())) {
          if (cStep.equals(xStep)) {
            isContextualizable = true;
          }
          break; // Always break here as searching further makes no sense in any case!
        }
      }
      if (!isContextualizable) {
        System.err.println(
            String.format("xpath not contextualizable: xpath=%s, context=%s", xpath, context));
        // Return the xpath as-is, it needs to stay absolute
        return xpath;
      }
    }

    // Handle general case:
    // Go through both and append if different.
    // If context overshoots, prepend "../"
    final var sb = new StringBuilder(xpath.length());
    int i;
    for (i = 0; i < contextL.size(); i++) {
      if (i >= xpathL.size()) {
        sb.insert(0, "../");
        continue;
      }
      XPathStep cStep = contextL.get(i);
      final XPathStep xStep = xpathL.get(i);

      assert !cStep.isAttribute();
      assert !xStep.isAttribute();

      if (cStep.hasPredicate() && !xStep.hasPredicate()) {
        cStep = new XPathStep(StringUtils.split(cStep.toString(), '[')[0]);
      }
      // Different step content ?
      // We know it is contextualizable (at some step later on).
      if (!cStep.equals(xStep)) {
        sb.append(xStep); // Preserve xpath step content.
        if (i < xpathL.size() - 1) {
          sb.append('/');
        }
        sb.insert(0, "../"); // Prepend ../
      }
      // Else: the step content is the same and is contextualized (aka ignored).
    }

    // Continue on tokenized xpath where we quit in previous loop.
    for (int j = i; j < xpathL.size(); j++) {
      final XPathStep xStep = xpathL.get(j);
      sb.append(xStep);
      if (j < xpathL.size() - 1) {
        sb.append('/');
      }
    }

    return sb.toString(); // NOTE: there are other returns in this method !
  }

  /**
   * Create the location from an xpath string. The xpath is split on "/" to get the list of xpath
   * steps. Blank parts are discarded.
   */
  public static XPathLocation buildXPathLocation(final String xpath) {
    assert !xpath.isBlank();
    String[] parts = XPATH_PART_TOKENIZER.split(xpath);

    List<XPathStep> steps = Arrays.stream(parts) //
        .filter(Predicate.not(String::isBlank)) //
        .map(element -> new XPathStep(element)) //
        .collect(Collectors.toList());

    return new XPathLocation(steps);
  }

}
