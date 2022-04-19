package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.InputMismatchException;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.MarkupGeneratorMock;
import eu.europa.ted.efx.mock.SymbolResolverMock;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

public class EfxTemplateTranslatorTest {

    final private String SDK_VERSION = "latest";

    private String translate(final String template) {
        return EfxTemplateTranslator.renderTemplate(template + "\n",
                SymbolResolverMock.getInstance(SDK_VERSION), new XPathScriptGenerator(), new MarkupGeneratorMock(),
                ThrowingErrorListener.INSTANCE);
    }

    @Test
    public void testStandardLabelReference() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}::#{field|name|BT-00-Text}"));
    }

    @Test
    public void testShorthandBtLabelTypeReference() {
        assertEquals(
                "block01 = label(concat('business_term', '|', 'name', '|', 'BT-00')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}::#{name|BT-00}"));
    }

    @Test
    public void testShorthandFieldLabelTypeReference() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}::#{name|BT-00-Text}"));
    }

    @Test
    public void testShorthandBtLabelReference() {
        assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}::#{BT-01}"));
    }

    @Test
    public void testShorthandFieldLabelReference() {
        assertThrows(InputMismatchException.class, () -> translate("{BT-00-Text}::#{BT-01-Text}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForIndicators() {
        assertEquals(
                "block01 = label(concat('indicator', '|', 'value', '-', ../IndicatorField/normalize-space(text()), '|', 'BT-00-Indicator')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}::#{BT-00-Indicator}"));
    }


    @Test
    public void testSelfLabelReference_WithValueLabelTypeAndIdicatorField() {
        assertEquals(
                "block01 = label(concat('indicator', '|', 'value', '-', ./normalize-space(text()), '|', 'BT-00-Indicator')); for-each(/*/PathNode/IndicatorField) { block01(); }",
                translate("{BT-00-Indicator}::#{value}"));
    }

    @Test
    public void testSelfLabelReference_WithValueLabelTypeAndCodeField() {
        assertEquals(
                "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text()))); for-each(/*/PathNode/CodeField) { block01(); }",
                translate("{BT-00-Code}::#{value}"));
    }

    @Test
    public void testSelfLabelReference_WithValueLabelTypeAndTextField() {
        assertThrows(InputMismatchException.class, () -> translate("{BT-00-Text}::#{value}"));
    }

    @Test
    public void testSelfLabelReference_WithOtherLabelType() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}::#{name}"));
    }

    @Test
    public void testSelfLabelReference_WithUnknownLabelType() {
        assertThrows(ParseCancellationException.class,
                () -> translate("{BT-00-Text}::#{whatever}"));
    }

    @Test
    public void testNestedExpression() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', ./normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}::#{field|name|${BT-00-Text}}"));
    }


    @Test
    public void testShorthandContextFieldValueReference() {
        assertEquals(
                "block01 = text('blah ')label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text())))text(' ')text('blah ')eval(./normalize-space(text()))text(' ')text('blah'); for-each(/*/PathNode/CodeField) { block01(); }",
                translate("{BT-00-Code} :: blah #value blah $value blah"));
    }
}
