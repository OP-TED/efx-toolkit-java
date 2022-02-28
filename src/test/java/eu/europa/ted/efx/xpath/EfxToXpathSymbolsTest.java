package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class EfxToXpathSymbolsTest {

  // TODO: Currently handling multiple SDK versions is not implemented.
  final private String testSdkVersion = "latest";
  final private Boolean testNewContextualizer = true;

  @Test
  public void testSymbolsContext() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols(testSdkVersion);
    symbols.useNewContextualizer(testNewContextualizer);
    final String fieldId = "BT-01(c)-Procedure";

    final String contextPathOfField = symbols.getContextPathOfField(fieldId);
    assertEquals("/*/cac:TenderingTerms", contextPathOfField);

    final String relativePathOfField =
        symbols.getRelativeXpathOfFieldOrNode(fieldId, contextPathOfField);
    assertEquals("cac:ProcurementLegislationDocumentReference/cbc:ID[not(text()='CrossBorderLaw')]",
        relativePathOfField);

    // TODO What about cases like this:
    // BT-71-Lot
    // /*/cac:ProcurementProjectLot[cbc:ID/@schemeName='Lot']/cac:TenderingTerms/cac:TendererQualificationRequest[not(cbc:CompanyLegalFormCode)][not(cac:SpecificTendererRequirement/cbc:TendererRequirementTypeCode[@listName='missing-info-submission'])]/cac:SpecificTendererRequirement/cbc:TendererRequirementTypeCode[@listName='reserved-procurement']
  }

  @Test
  public void testSymbolsFieldParentNode() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols(testSdkVersion);
    symbols.useNewContextualizer(testNewContextualizer);
    final String fieldId = "BT-01(c)-Procedure";
    final String parentNodeId = symbols.getParentNodeOfField(fieldId);
    assertEquals("ND-1", parentNodeId);
  }

  @Test
  public void testSymbolsFieldXpath() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols(testSdkVersion);
    symbols.useNewContextualizer(testNewContextualizer);
    final String fieldId = "BT-01(c)-Procedure";
    final String xpath = symbols.getXpathOfFieldOrNode(fieldId);
    assertEquals(
        "/*/cac:TenderingTerms/cac:ProcurementLegislationDocumentReference/cbc:ID[not(text()='CrossBorderLaw')]",
        xpath);
  }

  @Test
  public void testSymbolsNodeXpath() {
    final EfxToXpathSymbols symbols = new EfxToXpathSymbols(testSdkVersion);
    symbols.useNewContextualizer(testNewContextualizer);
    final String nodeId = "ND-609";
    final String xpath = symbols.getXpathOfFieldOrNode(nodeId);
    assertEquals("/*/cac:AdditionalDocumentReference", xpath);
  }

}
