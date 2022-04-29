package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.mock.DependencyFactoryMock;

public class EfxTemplateTranslatorTest {

    final private String SDK_VERSION = "latest";

    private String translate(final String template) {
        return EfxTemplateTranslator.renderTemplate(template + "\n", new DependencyFactoryMock(), SDK_VERSION);
    }

    private String lines(String... lines) {
        return String.join("\n", lines);
    }

    /*** Template line ***/

    @Test
    public void testTemplateLineNoIdent() {
        assertEquals("block01 = text('foo'); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text} foo"));
    }

    @Test
    public void testTemplateLineOutline() {
        // Outline is ignored if the line has no children
        assertEquals(lines(
                        "block02 = text('foo')",
                        "for-each(/*/PathNode/TextField) { block0201(); };",
                        "block0201 = text('bar'); for-each(/*/PathNode/TextField) { block02(); }"),
                translate(lines(
                        "2 {BT-00-Text} foo",
                        "\t{BT-00-Text} bar")));
    }

    @Test
    public void testTemplateLineFirstIndented() {
        assertThrows(ParseCancellationException.class, () -> translate("  {BT-00-Text} foo"));
    }

    @Test
    public void testTemplateLineIdentTab() {
        assertEquals(lines(
                        "block01 = text('foo')",
                        "for-each(/*/PathNode/TextField) { block0101(); };",
                        "block0101 = text('bar'); for-each(/*/PathNode/TextField) { block01(); }"),
                translate(lines(
                        "{BT-00-Text} foo",
                        "\t{BT-00-Text} bar")));
    }

    @Test
    public void testTemplateLineIdentSpaces() {
        assertEquals(lines(
                        "block01 = text('foo')",
                        "for-each(/*/PathNode/TextField) { block0101(); };",
                        "block0101 = text('bar'); for-each(/*/PathNode/TextField) { block01(); }"),
                translate(lines(
                        "{BT-00-Text} foo",
                        "    {BT-00-Text} bar")));
    }

    @Test
    public void testTemplateLineIdentMixed() {
        assertThrows(ParseCancellationException.class,
                () -> translate(lines(
                        "{BT-00-Text} foo",
                        "\t  {BT-00-Text} bar")));
    }

    @Test
    public void testTemplateLineIdentMixedSpaceThenTab() {
        assertThrows(ParseCancellationException.class,
                () -> translate(lines(
                        "{BT-00-Text} foo",
                        "  \t{BT-00-Text} bar")));
    }

    @Test
    public void testTemplateLineIdentLower() {
        assertEquals(lines(
                        "block01 = text('foo')",
                        "for-each(/*/PathNode/TextField) { block0101(); };",
                        "block0101 = text('bar');",
                        "block02 = text('code'); for-each(/*/PathNode/TextField) { block01(); }",
                        "for-each(/*/PathNode/CodeField) { block02(); }"),
                translate(lines(
                        "{BT-00-Text} foo",
                        "\t{BT-00-Text} bar",
                        "{BT-00-Code} code")));
    }

    @Test
    public void testTemplateLineIdentUnexpected() {
        assertThrows(ParseCancellationException.class,
                () -> translate(lines(
                        "{BT-00-Text} foo",
                        "\t\t{BT-00-Text} bar")));
    }


    /*** Labels ***/

    @Test
    public void testStandardLabelReference() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{field|name|BT-00-Text}"));
    }

    @Test
    public void testShorthandBtLabelReference() {
        assertEquals(
                "block01 = label(concat('business_term', '|', 'name', '|', 'BT-00')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{name|BT-00}"));
    }

    @Test
    public void testShorthandFieldLabelReference() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{name|BT-00-Text}"));
    }

    @Test
    public void testShorthandBtLabelReference_MissingLabelType() {
        assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForIndicator() {
        assertEquals(
                "block01 = label(concat('indicator', '|', 'value', '-', ../IndicatorField/normalize-space(text()), '|', 'BT-00-Indicator')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{BT-00-Indicator}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForCode() {
        assertEquals(
                "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ../CodeField/normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{BT-00-Code}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForInternalCode() {
        assertEquals(
                "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ../InternalCodeField/normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{BT-00-Internal-Code}"));
    }

    @Test
    public void testShorthandFieldValueLabelReferenceForText() {
        assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01-Text}"));
    }

    @Test
    public void testShorthandContextLabelReference_WithValueLabelTypeAndIndicatorField() {
        assertEquals(
                "block01 = label(concat('indicator', '|', 'value', '-', ./normalize-space(text()), '|', 'BT-00-Indicator')); for-each(/*/PathNode/IndicatorField) { block01(); }",
                translate("{BT-00-Indicator}  #{value}"));
    }

    @Test
    public void testShorthandContextLabelReference_WithValueLabelTypeAndCodeField() {
        assertEquals(
                "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text()))); for-each(/*/PathNode/CodeField) { block01(); }",
                translate("{BT-00-Code}  #{value}"));
    }

    @Test
    public void testShorthandContextLabelReference_WithValueLabelTypeAndTextField() {
        assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{value}"));
    }

    @Test
    public void testShorthandContextLabelReference_WithOtherLabelType() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{name}"));
    }

    @Test
    public void testShorthandContextLabelReference_WithUnknownLabelType() {
        assertThrows(ParseCancellationException.class,
                () -> translate("{BT-00-Text}  #{whatever}"));
    }

    @Test
    public void testShorthandContextLabelReference_WithNodeContext() {
        // TODO: Check if Node -> business_term is intended
        assertEquals(
                "block01 = label(concat('business_term', '|', 'name', '|', 'ND-0')); for-each(/*) { block01(); }",
                translate("{ND-0}  #{name}"));
    }

    @Test
    public void testShorthandContextFieldLabelReference() {
        assertEquals(
                "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text()))); for-each(/*/PathNode/CodeField) { block01(); }",
                translate("{BT-00-Code} #value"));
    }

    @Test
    public void testShorthandContextFieldLabelReference_WithNodeContext() {
        assertThrows(ParseCancellationException.class,
                () -> translate("{ND-0} #value"));
    }


    /*** Expression block***/

    @Test
    public void testShorthandContextFieldValueReference() {
        assertEquals(
                "block01 = eval(.); for-each(/*/PathNode/CodeField) { block01(); }",
                translate("{BT-00-Code} $value"));
    }

    @Test
    public void testShorthandContextFieldValueReference_WithText() {
        assertEquals(
                "block01 = text('blah ')label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text())))text(' ')text('blah ')eval(.)text(' ')text('blah'); for-each(/*/PathNode/CodeField) { block01(); }",
                translate("{BT-00-Code} blah #value blah $value blah"));
    }

    @Test
    public void testShorthandContextFieldValueReference_WithNodeContext() {
        assertThrows(ParseCancellationException.class,
                () -> translate("{ND-0} $value"));
    }


    /*** Other ***/

    @Test
    public void testNestedExpression() {
        assertEquals(
                "block01 = label(concat('field', '|', 'name', '|', ./normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
                translate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
    }
}
