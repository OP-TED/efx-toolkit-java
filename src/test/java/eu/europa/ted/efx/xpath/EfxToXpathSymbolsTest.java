package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class EfxToXpathSymbolsTest {

  @Test
  @SuppressWarnings("static-method")
  public void testSymbolsContext() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols();
    final String fieldId = "BT-01(c)-Procedure";

    final String contextPathOfField = symbols.getContextPathOfField(fieldId);
    assertEquals("/*/cac:TenderingTerms", contextPathOfField);

    // Condition example: field('BT-14-Part')!=''
    // It is not xpath.
    // What we do is we replace the field by the already contextualized xpath to avoid parsing
    // issues.

    // TODO it will not work as-is. Some contexts could be complex and will be hard to parse.
    // TODO ideally we solve this context stuff before we reach xpath.
    // String relativePathOfField =
    // symbols.getRelativeXpathOfField(fieldId, contextPathOfField);
    // System.out.println(relativePathOfField);
  }

  @Test
  @SuppressWarnings("static-method")
  public void testSymbolsFieldParentNode() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols();
    final String fieldId = "BT-01(c)-Procedure";
    final String parentNodeId = symbols.getParentNodeOfField(fieldId);
    assertEquals("ND-1", parentNodeId);
  }

  @Test
  @SuppressWarnings("static-method")
  public void testSymbolsFieldXpath() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols();
    final String fieldId = "BT-01(c)-Procedure";
    final String xpath = symbols.getXpathOfFieldOrNode(fieldId);
    assertEquals(
        "/*/cac:TenderingTerms/cac:ProcurementLegislationDocumentReference/cbc:ID[not(text()='CrossBorderLaw')]",
        xpath);
  }

  @Test
  @SuppressWarnings("static-method")
  public void testSymbolsNodeXpath() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols();
    final String nodeId = "ND-609";
    final String xpath = symbols.getXpathOfFieldOrNode(nodeId);
    assertEquals("/*/cac:AdditionalDocumentReference", xpath);
  }

}
