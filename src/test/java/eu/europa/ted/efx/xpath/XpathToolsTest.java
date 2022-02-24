package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class XpathToolsTest {

  @Test
  public void testContextualizeFromXpathIdentical() {
    final String xpathPa = "/a/";
    final String context = xpathPa;
    final String expected = ".";
    final String contextualizedXpath = XpathTools.contextualizeFromXPath(xpathPa, context);
    assertEquals(expected, contextualizedXpath);
  }

  @Test
  public void testContextualizeFromXpathSimple1() {
    final String xpathPa = "/a/b/c";
    final String context = "/a/b/d";
    final String expected = "../c";
    final String contextualizedXpath = XpathTools.contextualizeFromXPath(xpathPa, context);
    assertEquals(expected, contextualizedXpath);
  }

  @Test
  public void testContextualizeFromXpathSimple2() {
    final String xpathPa = "/a/b/";
    final String context = "/a/b/c";
    final String expected = "../";
    final String contextualizedXpath = XpathTools.contextualizeFromXPath(xpathPa, context);
    assertEquals(expected, contextualizedXpath);
  }

  @Test
  public void testContextualizeFromXpathImpossible() {
    final String xpathPa = "/x/y/";
    final String context = "/a/b/";
    final String expected = xpathPa;
    final String contextualizedXpath = XpathTools.contextualizeFromXPath(xpathPa, context);
    assertEquals(expected, contextualizedXpath);
  }

  @Test
  public void testContextualizeFromXpathWithPredicates() {
    final String xpathPa = "/a[i/j]/b/";
    final String context = "/a[i/j]/c/";
    final String expected = "../b";
    final String contextualizedXpath = XpathTools.contextualizeFromXPath(xpathPa, context);
    assertEquals(expected, contextualizedXpath);
  }

  @Test
  public void testContextualizeFromXpathWithNestedPredicates() {
    final String xpath = "/a/b[c/d[e]]/g";
    System.out.println(XpathTools.buildXPathLocation(xpath).getSteps());
  }

  @Test
  void checkContextualizationSpecialCase1() {
    // ​Case given by GUIDON-THIESSELIN Mathieu (ESTAT-EXT).
    // NOTE: use -ea when testing.
    final String xpathPa =

        "/*/cac:TenderingProcess/cac:ProcessJustification/cbc:ProcessReasonCode[@listName='accelerated-procedure']/normalize-space(text())='accelerated-procedure-justification'";

    final String context =

        "/*/cac:TenderingProcess/cac:ProcessJustification[cbc:ProcessReasonCode@listName='accelerated-procedure']";

    final String expected =
        "cbc:ProcessReasonCode[@listName='accelerated-procedure']/normalize-space(text())='accelerated-procedure-justification'";

    assertEquals(expected, XpathTools.contextualizeFromXPath(xpathPa, context));
  }


}
