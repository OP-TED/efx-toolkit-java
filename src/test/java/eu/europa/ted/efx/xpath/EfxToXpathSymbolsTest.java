package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class EfxToXpathSymbolsTest {

  // TODO: Currently handling multiple SDK versions is not implemented.
  private final String testSdkVersion = "latest";
  private final Boolean testNewContextualizer = true;

  private EfxToXpathSymbols getDummyInstance() {
    return EfxToXpathSymbols.getInstance(testSdkVersion);
  }

  @Test
  void testGetCodelistCodesNonTailored() {
    final EfxToXpathSymbols symbols = getDummyInstance();
    final String expected =
        "('all-rev-tic', 'cost-comp', 'exc-right', 'other', 'publ-ser-obl', 'soc-stand')";
    final String codelistReference = "contract-detail";
    final String efxList = symbols.getCodelistCodesAsEfxList(codelistReference); // Has no parent.
    assertEquals(expected, efxList);
  }

  @Test
  void testGetCodelistCodesTailored() {
    final EfxToXpathSymbols symbols = getDummyInstance();
    final String codelistReference = "eu-official-language";
    final String expected =
        "('BUL', 'CES', 'DAN', 'DEU', 'ELL', 'ENG', 'EST', 'FIN', 'FRA', 'GLE', 'HRV', 'HUN', 'ITA', 'LAV', 'LIT', 'MLT', 'NLD', 'POL', 'POR', 'RON', 'SLK', 'SLV', 'SPA', 'SWE')";
    final String efxList = symbols.getCodelistCodesAsEfxList(codelistReference);
    assertEquals(expected, efxList);
  }

  @Test
  public void testSymbolsContext() {
    final EfxToXpathSymbols symbols = getDummyInstance();
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
    final EfxToXpathSymbols symbols = getDummyInstance();
    symbols.useNewContextualizer(testNewContextualizer);
    final String fieldId = "BT-01(c)-Procedure";
    final String parentNodeId = symbols.getParentNodeOfField(fieldId);
    assertEquals("ND-1", parentNodeId);
  }

  @Test
  public void testSymbolsFieldXpath() {
    final EfxToXpathSymbols symbols = getDummyInstance();
    symbols.useNewContextualizer(testNewContextualizer);
    final String fieldId = "BT-01(c)-Procedure";
    final String xpath = symbols.getXpathOfFieldOrNode(fieldId);
    assertEquals(
        "/*/cac:TenderingTerms/cac:ProcurementLegislationDocumentReference/cbc:ID[not(text()='CrossBorderLaw')]",
        xpath);
  }

  @Test
  public void testSymbolsNodeXpath() {
    final EfxToXpathSymbols symbols = getDummyInstance();
    symbols.useNewContextualizer(testNewContextualizer);
    final String nodeId = "ND-609";
    final String xpath = symbols.getXpathOfFieldOrNode(nodeId);
    assertEquals("/*/cac:AdditionalDocumentReference", xpath);
  }

}
