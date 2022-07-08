package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.translator.EfxTranslator;

class EfxTemplateTranslatorTest {
  final private String SDK_VERSION = "eforms-sdk-0.7";

  private String translate(final String template) {
    return EfxTranslator.translateTemplate(template + "\n", DependencyFactoryMock.INSTANCE,
        SDK_VERSION);
  }

  private String lines(String... lines) {
    return String.join("\n", lines);
  }

  /*** Template line ***/

  @Test
  void testTemplateLineNoIdent() {
    assertEquals("block01 = text('foo'); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineOutline() {
    // Outline is ignored if the line has no children
    assertEquals(
        lines("block02 = text('foo')", "for-each(/*/PathNode/TextField) { block0201(); };",
            "block0201 = text('bar'); for-each(/*/PathNode/TextField) { block02(); }"),
        translate(lines("2 {BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineFirstIndented() {
    assertThrows(ParseCancellationException.class, () -> translate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineIdentTab() {
    assertEquals(
        lines("block01 = text('foo')", "for-each(/*/PathNode/TextField) { block0101(); };",
            "block0101 = text('bar'); for-each(/*/PathNode/TextField) { block01(); }"),
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentSpaces() {
    assertEquals(
        lines("block01 = text('foo')", "for-each(/*/PathNode/TextField) { block0101(); };",
            "block0101 = text('bar'); for-each(/*/PathNode/TextField) { block01(); }"),
        translate(lines("{BT-00-Text} foo", "    {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentMixed() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "\t  {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentMixedSpaceThenTab() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "  \t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentLower() {
    assertEquals(
        lines("block01 = text('foo')", "for-each(/*/PathNode/TextField) { block0101(); };",
            "block0101 = text('bar');",
            "block02 = text('code'); for-each(/*/PathNode/TextField) { block01(); }",
            "for-each(/*/PathNode/CodeField) { block02(); }"),
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar", "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLineIdentUnexpected() {
    assertThrows(ParseCancellationException.class,
        () -> translate(lines("{BT-00-Text} foo", "\t\t{BT-00-Text} bar")));
  }


  /*** Labels ***/

  @Test
  void testStandardLabelReference() {
    assertEquals(
        "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "block01 = label(concat('decoration', '|', 'name', '|', 'value')); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{decoration|name|value}"));
  }

  @Test
  void testShorthandBtLabelReference() {
    assertEquals(
        "block01 = label(concat('business_term', '|', 'name', '|', 'BT-00')); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testShorthandFieldLabelReference() {
    assertEquals(
        "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testShorthandBtLabelReference_MissingLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForIndicator() {
    assertEquals(
        "block01 = label(concat('indicator', '|', 'value', '-', ../IndicatorField/normalize-space(text()), '|', 'BT-00-Indicator')); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForCode() {
    assertEquals(
        "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ../CodeField/normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForInternalCode() {
    assertEquals(
        "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ../InternalCodeField/normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForText() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01-Text}"));
  }

  @Test
  void testShorthandContextLabelReference_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        "block01 = label(concat('indicator', '|', 'value', '-', ./normalize-space(text()), '|', 'BT-00-Indicator')); for-each(/*/PathNode/IndicatorField) { block01(); }",
        translate("{BT-00-Indicator}  #{value}"));
  }

  @Test
  void testShorthandContextLabelReference_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text()))); for-each(/*/PathNode/CodeField) { block01(); }",
        translate("{BT-00-Code}  #{value}"));
  }

  @Test
  void testShorthandContextLabelReference_WithValueLabelTypeAndTextField() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandContextLabelReference_WithOtherLabelType() {
    assertEquals(
        "block01 = label(concat('field', '|', 'name', '|', 'BT-00-Text')); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandContextLabelReference_WithUnknownLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testShorthandContextLabelReference_WithNodeContext() {
    // TODO: Check if Node -> business_term is intended
    assertEquals(
        "block01 = label(concat('business_term', '|', 'name', '|', 'ND-Root')); for-each(/*) { block01(); }",
        translate("{ND-Root}  #{name}"));
  }

  @Test
  void testShorthandContextFieldLabelReference() {
    assertEquals(
        "block01 = label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text()))); for-each(/*/PathNode/CodeField) { block01(); }",
        translate("{BT-00-Code} #value"));
  }

  @Test
  void testShorthandContextFieldLabelReference_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translate("{ND-Root} #value"));
  }


  /*** Expression block ***/

  @Test
  void testShorthandContextFieldValueReference() {
    assertEquals("block01 = eval(.); for-each(/*/PathNode/CodeField) { block01(); }",
        translate("{BT-00-Code} $value"));
  }

  @Test
  void testShorthandContextFieldValueReference_WithText() {
    assertEquals(
        "block01 = text('blah ')label(concat('code', '|', 'value', '|', 'main-activity', '.', ./normalize-space(text())))text(' ')text('blah ')eval(.)text(' ')text('blah'); for-each(/*/PathNode/CodeField) { block01(); }",
        translate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testShorthandContextFieldValueReference_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translate("{ND-Root} $value"));
  }


  /*** Other ***/

  @Test
  void testNestedExpression() {
    assertEquals(
        "block01 = label(concat('field', '|', 'name', '|', ./normalize-space(text()))); for-each(/*/PathNode/TextField) { block01(); }",
        translate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
  }
}
