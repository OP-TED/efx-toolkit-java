package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

class EfxTemplateTranslatorTest {
  final private String SDK_VERSION = "eforms-sdk-0.8";

  private String translate(final String template) {
    try {
      return EfxTranslator.translateTemplate(template + "\n", DependencyFactoryMock.INSTANCE,
          SDK_VERSION);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private String lines(String... lines) {
    return String.join("\n", lines);
  }

  /*** Template line ***/

  @Test
  void testTemplateLineNoIdent() {
    assertEquals("declare block01 = { text('foo') }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineOutline() {
    // Outline is ignored if the line has no children
    assertEquals(lines("declare block01 = { text('foo')", 
    "for-each(../..).call(block0101) }", 
    "declare block0101 = { text('bar')", 
    "for-each(PathNode/NumberField).call(block010101) }", 
    "declare block010101 = { text('foo') }", 
    "for-each(/*/PathNode/TextField).call(block01)"), //
        translate(lines("2 {BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLineFirstIndented() {
    assertThrows(ParseCancellationException.class, () -> translate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineIdentTab() {
    assertEquals(
        lines("declare block01 = { text('foo')", "for-each(.).call(block0101) }", //
        "declare block0101 = { text('bar') }", //
        "for-each(/*/PathNode/TextField).call(block01)"),//
        translate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentSpaces() {
    assertEquals(
        lines("declare block01 = { text('foo')", "for-each(.).call(block0101) }", //
        "declare block0101 = { text('bar') }", //
        "for-each(/*/PathNode/TextField).call(block01)"),//
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
        lines("declare block01 = { text('foo')", "for-each(.).call(block0101) }", 
        "declare block0101 = { text('bar') }", 
        "declare block02 = { text('code') }", 
        "for-each(/*/PathNode/TextField).call(block01)", 
        "for-each(/*/PathNode/CodeField).call(block02)"),
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
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "declare block01 = { label(concat('auxiliary', '|', 'text', '|', 'value')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testShorthandBtLabelReference() {
    assertEquals(
        "declare block01 = { label(concat('business-term', '|', 'name', '|', 'BT-00')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testShorthandFieldLabelReference() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testShorthandBtLabelReference_MissingLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForIndicator() {
    assertEquals(
        "declare block01 = { label(concat('indicator', '|', 'when', '-', ../IndicatorField/normalize-space(text()), '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForCode() {
    assertEquals(
        "declare block01 = { label(concat('code', '|', 'name', '|', 'main-activity', '.', ../CodeField/normalize-space(text()))) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForInternalCode() {
    assertEquals(
        "declare block01 = { label(concat('code', '|', 'name', '|', 'main-activity', '.', ../InternalCodeField/normalize-space(text()))) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testShorthandFieldValueLabelReferenceForText() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{BT-01-Text}"));
  }

  @Test
  void testShorthandContextLabelReference_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/IndicatorField).call(block01)",
        translate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandContextLabelReference_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code}  #{name}"));
  }

  @Test
  void testShorthandContextLabelReference_WithValueLabelTypeAndTextField() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandContextLabelReference_WithOtherLabelType() {
    assertEquals(
        "declare block01 = { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandContextLabelReference_WithUnknownLabelType() {
    assertThrows(ParseCancellationException.class, () -> translate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testShorthandContextLabelReference_WithNodeContext() {
    // TODO: Check if Node -> business-term is intended
    assertEquals(
        "declare block01 = { label(concat('node', '|', 'name', '|', 'ND-Root')) }\nfor-each(/*).call(block01)",
        translate("{ND-Root}  #{name}"));
  }

  @Test
  void testShorthandContextFieldLabelReference() {
    assertEquals(
        "declare block01 = { label(concat('code', '|', 'name', '|', 'main-activity', '.', ./normalize-space(text()))) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code} #value"));
  }

  @Test
  void testShorthandContextFieldLabelReference_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translate("{ND-Root} #value"));
  }


  /*** Expression block ***/

  @Test
  void testShorthandContextFieldValueReference() {
    assertEquals("declare block01 = { eval(.) }\nfor-each(/*/PathNode/CodeField).call(block01)",
        translate("{BT-00-Code} $value"));
  }

  @Test
  void testShorthandContextFieldValueReference_WithText() {
    assertEquals(
        "declare block01 = { text('blah ')label(concat('code', '|', 'name', '|', 'main-activity', '.', ./normalize-space(text())))text(' ')text('blah ')eval(.)text(' ')text('blah') }\nfor-each(/*/PathNode/CodeField).call(block01)",
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
        "declare block01 = { label(concat('field', '|', 'name', '|', ./normalize-space(text()))) }\nfor-each(/*/PathNode/TextField).call(block01)",
        translate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
  }
}
