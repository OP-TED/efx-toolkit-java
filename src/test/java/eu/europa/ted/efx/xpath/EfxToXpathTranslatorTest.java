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
    final String condition = "ALWAYS;";
    final String expected = "true";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void bTranslateConditionSimpleEquals() {
    assertEquals("'bla42'='bla42'", EfxToXpathTranslator.translateCondition(
        DUMMY_CONTEXT + "'bla42' == 'bla42';", testSdkVersion, testNewContextualizer));
  }

  @Test
  public void cTranslateConditionSimpleDifferent() {
    final String condition = "'bla42' != 'bla42';";
    final String expected = "'bla42'!='bla42'";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void cTranslateSimpleEqualsTrue() {
    final String condition = "BT-751-Lot == 'true';";
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
            "OPP-050-Organization: OPT-200-Organization-Company == OPT-300-Procedure-Buyer;",
            testSdkVersion, testNewContextualizer));
  }

  @Test
  public void dTranslateConditionNestedParenthesis() {
    final String condition = "(('bla42'));";
    final String expected = "(('bla42'))";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void TranslateConditionNegated() {
    final String condition = "not ('a' == 'b');";
    final String expected = "not('a'='b')";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void TranslateConditionValueInList() {
    final String condition = "'a' in {'a','b','c'};";
    final String expected = "'a'=('a', 'b', 'c')";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void TranslateConditionValueNotInList() {
    final String condition = "'z' not in {'a','b','c'}";
    final String expected = "not('z'=('a', 'b', 'c'))";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void TranslateConditionValueLike() {
    final String condition = "'abc' like 'ab.*';";
    final String expected = "fn:matches('abc', 'ab.*')";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + condition,
        testSdkVersion, testNewContextualizer));
  }

  @Test
  public void TranslateConditionValueNotLike() {
    final String condition = "'zzz' not like 'ab.*';";
    final String expected = "not(fn:matches('zzz', 'ab.*'))";
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
        "(../../cac:TenderingProcess/cbc:ProcedureCode/text()='oth-mult' and not(cac:TenderSubmissionDeadlinePeriod/cbc:EndDate/text() != '')) or (../../cac:TenderingProcess/cbc:ProcedureCode/text()='oth-single' and not(cac:TenderSubmissionDeadlinePeriod/cbc:EndDate/text() != ''))";
    final String condition =
        "(BT-105-Procedure == 'oth-mult' and not (BT-131(d)-Lot is not empty)) or (BT-105-Procedure == 'oth-single' and not (BT-131(d)-Lot is not empty));";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(context + ": " + condition,
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

  @Test
  public void translateCodelist() {
    String expected = "../../../../../../cbc:RegulatoryDomain/text()=('31985R2137', '31992L0013', '31994D0001', '31994D0800', '32001R2157', '32002D0309', '32002R2342', '32003R1435', '32004L0017', '32004L0018', '32005D0015', '32007D0005_01', '32007L0066', '32007R0718', '32007R1370', '32008E0124', '32008R1008', '32009L0081', '32012R0966', '32012R1268', '32013L0016', '32014D0115', '32014D0486', '32014D0691', '32014L0023', '32014L0024', '32014L0025', '32014R0230', '32014R0231', '32014R0232', '32014R0233', '32014R0236', '32014R0237', '32015R0323', '32016D0002', '32017D2263', '32018R1046', '32019D0312', 'other')";
    assertEquals(expected, EfxToXpathTranslator.translateCondition(DUMMY_CONTEXT + "BT-01-notice in codelist(legal-basis);", testSdkVersion, testNewContextualizer));
  }
}
