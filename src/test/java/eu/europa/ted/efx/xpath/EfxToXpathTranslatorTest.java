package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class) // Simple first, more complex expressions later.
public class EfxToXpathTranslatorTest {

  /**
   * Use this when the context does not matter.
   */
  private static final String DUMMY_CONTEXT = "OPP-050-Organization: ";
  // TODO: Currently handling multiple SDK versions is not implemented.
  final private String testSdkVersion = "latest";

  final private Boolean testNewContextualizer = true;

  @Test
  public void aTranslateConditionAlways() {
    final String condition = "ALWAYS";
    final String expected = "true";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void bTranslateConditionSimpleEquals() {
    assertEquals("'bla42'='bla42'", EfxToXpathTranslator.translateCondition(
        DUMMY_CONTEXT + "'bla42' == 'bla42'", testSdkVersion, testNewContextualizer));
  }

  @Test
  public void cTranslateConditionSimpleDifferent() {
    final String condition = "'bla42' != 'bla42'";
    final String expected = "'bla42'!='bla42'";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void cTranslateSimpleEqualsTrue() {
    final String condition = "BT-751-Lot == 'true'";
    final String expected =
        "../../../../../../cac:ProcurementProjectLot[cbc:ID/@schemeName='Lot']/cac:TenderingTerms/cac:RequiredFinancialGuarantee/cbc:GuaranteeTypeCode[@listName='tender-guarantee-required']/text()='true'";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void dTranslateConditionFieldEqField() {
    final String expected =
        "efac:Company/cac:PartyIdentification/cbc:ID/text()=../../../../../../cac:ContractingParty/cac:Party/cac:PartyIdentification/cbc:ID/text()";
    assertEquals(expected,
        EfxToXpathTranslator.translateCondition(
            "OPP-050-Organization: OPT-200-Organization-Company == OPT-300-Procedure-Buyer",
            testSdkVersion, testNewContextualizer));
  }

  @Test
  public void dTranslateConditionNestedParenthesis() {
    final String condition = "(('bla42'))";
    final String expected = "(('bla42'))";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  /**
   * This is a bit more than just a unit test as it tests many features of the language.
   */
  @Test
  public void yTranslateCondition111Complex() {
    final String context = "BT-1311(d)-Lot";
    final String expected =
        "(../../cac:TenderingProcess/cbc:ProcedureCode/text()='oth-mult' and null (cac:TenderSubmissionDeadlinePeriod/cbc:EndDate/text() = '')) or (../../cac:TenderingProcess/cbc:ProcedureCode/text()='oth-single' and null (cac:TenderSubmissionDeadlinePeriod/cbc:EndDate/text() = ''))";
    final String condition =
        "(BT-105-Procedure == 'oth-mult' and not (BT-131(d)-Lot is not empty)) or (BT-105-Procedure == 'oth-single' and not (BT-131(d)-Lot is not empty))";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(context + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void zTemplateParsing() {
    // TODO see this later.
    // String output =
    // EfxtToXsltTranslator.translateTemplateFile("src/test/resources/efxt-test1.efxt");
    // System.out.println(output);
  }

  @Test
  public void zTranslateFile() throws IOException {
    System.out.print(EfxToXpathTranslator.translateTestFile("src/test/resources/examples.efx",
        testSdkVersion, testNewContextualizer));
  }

}
