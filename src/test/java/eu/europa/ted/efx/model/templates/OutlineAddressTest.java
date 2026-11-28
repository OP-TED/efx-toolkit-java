package eu.europa.ted.efx.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.Parameter;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.mock.DependencyFactoryWithMarkupGenerator;
import eu.europa.ted.efx.mock.MarkupGeneratorMock;
import eu.europa.ted.efx.model.DecimalFormat;

/**
 * Checks the body of the navigation id that the template translator gives the markup generator for
 * each template line, made of its outline address in its own section or template, and what each
 * call puts above the fragment it invokes: a nesting level offset, a heading level offset and a
 * navigation id prefix.
 */
class OutlineAddressTest {

  private static final TranslatorOptions OPTIONS =
      new EfxTranslatorOptions("udf", DecimalFormat.EFX_DEFAULT);

  /**
   * Records, in order, the navigation id body of each fragment definition, and what each call puts
   * above the fragment it invokes.
   */
  private static final class RecordingMarkupGenerator extends MarkupGeneratorMock {
    final List<String> records = new ArrayList<>();

    @Override
    public Markup composeFragmentDefinition(String name, FragmentOutline outline,
        List<Conditional> conditionals, Markup caption, Markup content, Markup children,
        Set<Parameter> parameters, TranslatorContext translatorContext) {
      final String navIdBody = outline.getNavIdBody();
      this.records.add(name + ": " + (navIdBody.isEmpty() ? "no address" : navIdBody));
      return super.composeFragmentDefinition(name, outline, conditionals, caption, content, children,
          parameters, translatorContext);
    }

    @Override
    public Markup renderFragmentInvocation(String name, OutlineOffset offset,
        Set<Argument> arguments, TranslatorContext translatorContext) {
      this.records.add("call " + name + " with nestingLevelOffset " + offset.getNestingLevelOffset()
          + ", headingLevelOffset " + offset.getHeadingLevelOffset() + " and navIdPrefix '"
          + offset.getNavIdPrefix() + "'");
      return super.renderFragmentInvocation(name, offset, arguments, translatorContext);
    }
  }

  private static List<String> recordsOf(final String sdkVersion, final String... template)
      throws Exception {
    final RecordingMarkupGenerator markupGenerator = new RecordingMarkupGenerator();
    EfxTranslator.translateTemplate(new DependencyFactoryWithMarkupGenerator(markupGenerator),
        sdkVersion, String.join("\n", template) + "\n", OPTIONS);
    return markupGenerator.records;
  }

  private static List<String> definitionsOf(final String... template) throws Exception {
    return recordsOf("eforms-sdk-2.0", template).stream()
        .filter(record -> !record.startsWith("call ")).collect(Collectors.toList());
  }

  private static List<String> callsOf(final String... template) throws Exception {
    return recordsOf("eforms-sdk-2.0", template).stream()
        .filter(record -> record.startsWith("call ")).collect(Collectors.toList());
  }

  @Test
  void testEfx2_AHeadingIsAddressedByTheHeadingsAboveItAndItsOwnNumberOrTag() throws Exception {
    // The number of a heading without nested lines is not displayed, but it addresses the heading.
    assertEquals(List.of(
        "body01: notice-info",
        "body02: 5",
        "body0201: 5_1",
        "body020101: 5_1_10",
        "body0202: 5_lala"),
        definitionsOf(
            "#notice-info WITH ND-Root DISPLAY a;",
            "5 WITH ND-Root DISPLAY b;",
            "\t? WITH ND-Root DISPLAY c;",
            "\t\t10 WITH BT-00-Text DISPLAY d;",
            "\t#lala WITH BT-00-Text DISPLAY e;"));
  }

  @Test
  void testEfx2_ANormalLineOrABareHashAddsNothingToTheAddress() throws Exception {
    assertEquals(List.of(
        "body01: 1",
        "body0101: no address",
        "body010101: 1_2",
        "body0102: no address",
        "body010201: 1_lala"),
        definitionsOf(
            "1 WITH ND-Root DISPLAY a;",
            "\tWITH ND-Root DISPLAY b;",
            "\t\t2 WITH BT-00-Text DISPLAY c;",
            "\t# WITH ND-Root DISPLAY d;",
            "\t\t#lala WITH BT-00-Text DISPLAY e;"));
  }

  @Test
  void testEfx2_EverySectionHasAddresses() throws Exception {
    assertEquals(List.of(
        "body01: 1",
        "summary01: 1",
        "nav01: 1"),
        definitionsOf(
            "1 WITH BT-00-Text DISPLAY a;",
            "--- SUMMARY ---",
            "1 WITH BT-00-Text DISPLAY b;",
            "--- NAVIGATION ---",
            "1 WITH BT-00-Text DISPLAY c;"));
  }

  @Test
  void testEfx2_TheCallOfALinePutsNothingAboveIt() throws Exception {
    // The address and the levels of a line already hold those of the lines above it.
    assertEquals(List.of(
        "call body01 with nestingLevelOffset 0, headingLevelOffset 0 and navIdPrefix ''",
        "call body0101 with nestingLevelOffset 0, headingLevelOffset 0 and navIdPrefix ''"),
        callsOf(
            "1 WITH ND-Root DISPLAY a;",
            "\tWITH BT-00-Text DISPLAY b;"));
  }

  @Test
  void testEfx2_TheCallOfATemplatePutsTheLevelsAboveAndTheAddressOfTheNearestHeading()
      throws Exception {
    // The template takes the place of the invoking line: under heading 1, under the normal line
    // nested in heading 2, and at the top.
    assertEquals(List.of(
        "call info with nestingLevelOffset 1, headingLevelOffset 1 and navIdPrefix '1'",
        "call info with nestingLevelOffset 2, headingLevelOffset 1 and navIdPrefix '2'",
        "call info with nestingLevelOffset 0, headingLevelOffset 0 and navIdPrefix ''"),
        callsOf(
            "--- TEMPLATE info ---",
            "#lala WITH ND-Root DISPLAY a;",
            "--- MAIN ---",
            "1 WITH ND-Root DISPLAY c;",
            "\tWITH ND-Root INVOKE info();",
            "2 WITH ND-Root DISPLAY b;",
            "\tWITH ND-Root DISPLAY d;",
            "\t\tWITH ND-Root INVOKE info();",
            "WITH ND-Root INVOKE info();").stream()
            .filter(call -> call.startsWith("call info ")).collect(Collectors.toList()));
  }

  @Test
  void testEfx2_TheLinesOfATemplateAreAddressedWithinTheTemplate() throws Exception {
    assertEquals(List.of(
        "lines: no address",
        "lines01: details",
        "lines0101: details_1",
        "body01: 3",
        "body0101: no address"),
        definitionsOf(
            "--- TEMPLATE lines ---",
            "#details WITH ND-Root DISPLAY a;",
            "\t1 WITH BT-00-Text DISPLAY b;",
            "--- MAIN ---",
            "3 WITH ND-Root DISPLAY c;",
            "\tWITH ND-Root INVOKE lines();"));
  }

  @Test
  void testEfx2_ATemplateInvokedInATemplateGetsTheLevelsAndAddressInThatTemplate()
      throws Exception {
    assertEquals(List.of(
        "call info with nestingLevelOffset 1, headingLevelOffset 1 and navIdPrefix 'details'",
        "call outer with nestingLevelOffset 2, headingLevelOffset 2 and navIdPrefix '1_2'"),
        callsOf(
            "--- TEMPLATE info ---",
            "#lala WITH ND-Root DISPLAY a;",
            "--- TEMPLATE outer ---",
            "#details WITH ND-Root DISPLAY b;",
            "\tWITH ND-Root INVOKE info();",
            "--- MAIN ---",
            "1 WITH ND-Root DISPLAY c;",
            "\t2 WITH ND-Root DISPLAY e;",
            "\t\tWITH ND-Root INVOKE outer();").stream()
            .filter(call -> call.startsWith("call info ") || call.startsWith("call outer "))
            .collect(Collectors.toList()));
  }

  @Test
  void testEfx1_NoLineHasAnAddress() throws Exception {
    assertEquals(List.of(
        "block01: no address",
        "block0101: no address"),
        recordsOf("eforms-sdk-1.0",
            "1 {ND-Root} a",
            "\t2 {BT-00-Text} b").stream()
            .filter(record -> !record.startsWith("call ")).collect(Collectors.toList()));
  }
}
