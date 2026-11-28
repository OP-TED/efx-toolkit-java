package eu.europa.ted.efx.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

/**
 * Checks the kind, the nesting level and the heading level that the template translator gives the
 * markup generator for each fragment, and the offsets that it gives for each call of a template.
 */
class FragmentKindAndLevelTest {

  private static final TranslatorOptions OPTIONS =
      new EfxTranslatorOptions("udf", DecimalFormat.EFX_DEFAULT);

  /**
   * Records the kind, the nesting level and the heading level of each fragment definition, by
   * fragment name, and the nesting level offset, the heading level offset and the navigation id
   * prefix of each call of a fragment, in the order they are rendered.
   */
  private static final class RecordingMarkupGenerator extends MarkupGeneratorMock {
    final Map<String, String> lines = new LinkedHashMap<>();
    final List<String> calls = new ArrayList<>();
    final List<String> headers = new ArrayList<>();

    @Override
    public Markup composeNoticeHeader(Map<HeaderPart, Markup> parts, List<Markup> lines,
        TranslatorContext translatorContext) {
      this.headers.add(parts.keySet() + " " + lines.size());
      return super.composeNoticeHeader(parts, lines, translatorContext);
    }

    @Override
    public Markup composeFragmentDefinition(String name, FragmentOutline outline,
        List<Conditional> conditionals, Markup caption, Markup content, Markup children,
        Set<Parameter> parameters, TranslatorContext translatorContext) {
      this.lines.put(name, outline.getKind() + " " + outline.getNestingLevel() + " "
          + outline.getHeadingLevel());
      return super.composeFragmentDefinition(name, outline, conditionals, caption, content, children,
          parameters, translatorContext);
    }

    @Override
    public Markup renderFragmentInvocation(String name, OutlineOffset offset,
        Set<Argument> arguments, TranslatorContext translatorContext) {
      this.calls.add(name + ": " + offset.getNestingLevelOffset() + " "
          + offset.getHeadingLevelOffset() + " '" + offset.getNavIdPrefix() + "'");
      return super.renderFragmentInvocation(name, offset, arguments, translatorContext);
    }
  }

  private static RecordingMarkupGenerator translate(final String sdkVersion,
      final String... template) throws Exception {
    final RecordingMarkupGenerator markupGenerator = new RecordingMarkupGenerator();
    EfxTranslator.translateTemplate(new DependencyFactoryWithMarkupGenerator(markupGenerator),
        sdkVersion, String.join("\n", template) + "\n", OPTIONS);
    return markupGenerator;
  }

  /**
   * @return each fragment, by name, with its kind, its nesting level and its heading level
   */
  private static String linesOf(final String sdkVersion, final String... template)
      throws Exception {
    return translate(sdkVersion, template).lines.entrySet().stream()
        .map(entry -> entry.getKey() + ": " + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  /**
   * @return each call of the given template, with its nesting level offset, its heading level offset
   *         and its navigation id prefix
   */
  private static String callsOf(final String templateName, final String sdkVersion,
      final String... template) throws Exception {
    return translate(sdkVersion, template).calls.stream()
        .filter(call -> call.startsWith(templateName + ": "))
        .collect(Collectors.joining("\n"));
  }

  @Test
  void testEfx2_AMarkerMakesAHeading() throws Exception {
    assertEquals(String.join("\n",
        "body01: HEADING 1 1",
        "body02: HEADING 1 1",
        "body03: HEADING 1 1",
        "body04: NORMAL 1 0"),
        linesOf("eforms-sdk-2.0",
            "1 WITH BT-00-Text DISPLAY numbered;",
            "? WITH BT-00-Text DISPLAY auto-numbered;",
            "# WITH BT-00-Text DISPLAY unnumbered;",
            "WITH BT-00-Text DISPLAY normal;"));
  }

  @Test
  void testEfx2_TheLevelsFollowTheNesting() throws Exception {
    // The nesting level counts the lines above; the heading level counts the headings above, and
    // only a heading has one.
    assertEquals(String.join("\n",
        "body01: HEADING 1 1",
        "body0101: NORMAL 2 0",
        "body010101: HEADING 3 2",
        "body01010101: HEADING 4 3",
        "body0102: NORMAL 2 0",
        "body02: NORMAL 1 0"),
        linesOf("eforms-sdk-2.0",
            "1 WITH BT-00-Text DISPLAY a;",
            "\tWITH ND-Root DISPLAY b;",
            "\t\t# WITH BT-00-Number DISPLAY c;",
            "\t\t\t#tag WITH BT-00-Number DISPLAY c2;",
            "\tWITH ND-Root DISPLAY d;",
            "WITH BT-00-Text DISPLAY e;"));
  }

  @Test
  void testEfx2_TheLevelsStartAgainInATemplateSection() throws Exception {
    // A template section that holds its lines is of kind TEMPLATE, at nesting level 0; its top line
    // is at nesting level 1, and a heading there at heading level 1.
    assertEquals(String.join("\n",
        "lines: TEMPLATE 0 0",
        "lines01: HEADING 1 1",
        "lines0101: NORMAL 2 0",
        "body01: INVOCATION 1 0"),
        linesOf("eforms-sdk-2.0",
            "--- TEMPLATE lines ---",
            "# WITH BT-00-Text DISPLAY a;",
            "\tWITH ND-Root DISPLAY b;",
            "--- MAIN ---",
            "INVOKE lines();"));
  }

  @Test
  void testEfx2_ATemplateOfASingleLineIsAtTheLevelOfTheLine() throws Exception {
    // A template section whose single line has no context and no marker displays the line itself.
    assertEquals(String.join("\n",
        "single: NORMAL 1 0",
        "single01: NORMAL 2 0",
        "body01: INVOCATION 1 0"),
        linesOf("eforms-sdk-2.0",
            "--- TEMPLATE single ---",
            "DISPLAY a;",
            "\tWITH ND-Root DISPLAY b;",
            "--- MAIN ---",
            "INVOKE single();"));
  }

  private static final String[] EXAMPLE_WITHOUT_HEADING_ABOVE = {
      "--- TEMPLATE t ---",
      "DISPLAY T1;",
      "\t#tag DISPLAY T2;",
      "\t\tDISPLAY T3;",
      "\t\t\tDISPLAY T4;",
      "--- MAIN ---",
      "DISPLAY A;",
      "\tDISPLAY B;",
      "\t\tINVOKE t();",
      "\t\tDISPLAY D;"};

  @Test
  void testEfx2_TheExampleOfAnInvokingLine() throws Exception {
    // Line 10 invokes t; the template takes its place: its top line is displayed at nesting level
    // 2 + 1.
    assertEquals(String.join("\n",
        "t: NORMAL 1 0",
        "t01: HEADING 2 1",
        "t0101: NORMAL 3 0",
        "t010101: NORMAL 4 0",
        "body01: NORMAL 1 0",
        "body0101: NORMAL 2 0",
        "body010101: INVOCATION 3 0",
        "body010102: NORMAL 3 0"),
        linesOf("eforms-sdk-2.0", EXAMPLE_WITHOUT_HEADING_ABOVE));
  }

  @Test
  void testEfx2_ACallFromNoHeadingPutsNoHeadingLevelAbove() throws Exception {
    // No heading is above line 10, so the heading of t is displayed at heading level 0 + 1.
    assertEquals("t: 2 0 ''", callsOf("t", "eforms-sdk-2.0", EXAMPLE_WITHOUT_HEADING_ABOVE));
  }

  private static final String[] EXAMPLE_WITH_HEADING_ABOVE = {
      "--- TEMPLATE t ---",
      "DISPLAY T1;",
      "\t#tag DISPLAY T2;",
      "\t\tDISPLAY T3;",
      "\t\t\tDISPLAY T4;",
      "--- MAIN ---",
      "DISPLAY A;",
      "\t1 DISPLAY B;",
      "\t\tINVOKE t();",
      "\t\tDISPLAY D;"};

  @Test
  void testEfx2_TheExampleWithAHeadingAboveTheInvokingLine() throws Exception {
    // Line 9 is a heading at heading level 1. The #tag line of t is at nesting level 2 and heading
    // level 1 in its template.
    assertEquals(String.join("\n",
        "t: NORMAL 1 0",
        "t01: HEADING 2 1",
        "t0101: NORMAL 3 0",
        "t010101: NORMAL 4 0",
        "body01: NORMAL 1 0",
        "body0101: HEADING 2 1",
        "body010101: INVOCATION 3 0",
        "body010102: NORMAL 3 0"),
        linesOf("eforms-sdk-2.0", EXAMPLE_WITH_HEADING_ABOVE));
  }

  @Test
  void testEfx2_ACallUnderAHeadingPutsItsHeadingLevelAbove() throws Exception {
    // Line 10 is at nesting level 3 under one heading, so the #tag line of t is displayed at nesting
    // level 2 + 2 and heading level 1 + 1, with the navigation id prefix of line 9.
    assertEquals("t: 2 1 '1'", callsOf("t", "eforms-sdk-2.0", EXAMPLE_WITH_HEADING_ABOVE));
  }

  @Test
  void testEfx2_TheExampleWithASecondTopLineInTheTemplate() throws Exception {
    // The template holds lines (kind TEMPLATE, nesting level 0): its two top lines take the place of
    // line 10, one after the other, at nesting level 2 + 1.
    assertEquals(String.join("\n",
        "t: TEMPLATE 0 0",
        "t01: NORMAL 1 0",
        "t0101: HEADING 2 1",
        "t010101: NORMAL 3 0",
        "t01010101: NORMAL 4 0",
        "t02: NORMAL 1 0",
        "body01: NORMAL 1 0",
        "body0101: NORMAL 2 0",
        "body010101: INVOCATION 3 0",
        "body010102: NORMAL 3 0"),
        linesOf("eforms-sdk-2.0",
            "--- TEMPLATE t ---",
            "DISPLAY T1;",
            "\t#tag DISPLAY T2;",
            "\t\tDISPLAY T3;",
            "\t\t\tDISPLAY T4;",
            "DISPLAY T5;",
            "--- MAIN ---",
            "DISPLAY A;",
            "\tDISPLAY B;",
            "\t\tINVOKE t();",
            "\t\tDISPLAY D;"));
  }

  @Test
  void testEfx2_ATemplateThatHoldsLinesIsOfKindTemplate() throws Exception {
    // Its own fragment is not a line: a template with a single line of its own displays that line.
    assertEquals(String.join("\n",
        "holder: TEMPLATE 0 0",
        "holder01: NORMAL 1 0",
        "holder02: NORMAL 1 0",
        "single: NORMAL 1 0",
        "body01: INVOCATION 1 0",
        "body02: INVOCATION 1 0"),
        linesOf("eforms-sdk-2.0",
            "--- TEMPLATE holder ---",
            "DISPLAY a;",
            "DISPLAY b;",
            "--- TEMPLATE single ---",
            "DISPLAY c;",
            "--- MAIN ---",
            "INVOKE holder();",
            "INVOKE single();"));
  }

  @Test
  @SuppressWarnings({"deprecation", "removal"})
  void testTheDeprecatedDefinitionIsANormalLineAtTheTop() {
    // Nesting level 0 belongs to a template that holds lines, which a call of the old method is not.
    // The old method keeps the outline number, and the order of the alternatives.
    final List<String> received = new ArrayList<>();
    final MarkupGeneratorMock generator = new MarkupGeneratorMock() {
      @Override
      public Markup composeFragmentDefinition(String name, FragmentOutline outline,
          List<Conditional> conditionals, Markup caption, Markup content, Markup children,
          Set<Parameter> parameters, TranslatorContext translatorContext) {
        received.add(outline.toString());
        received.add(conditionals.stream().map(conditional -> conditional.getMarkup().script)
            .collect(Collectors.joining(", ")));
        return content;
      }
    };
    final Set<Conditional> alternatives = new LinkedHashSet<>(List.of(
        new Conditional(new BooleanExpression("first"), new Markup("one")),
        new Conditional(new BooleanExpression("second"), new Markup("two"))));
    generator.composeFragmentDefinition("legacy", "2.1", alternatives, new Markup("x"),
        Markup.empty(), Set.of(), TranslatorContext.DEFAULT);
    assertEquals(List.of(FragmentOutline.normalLine("2.1", 1).toString(), "one, two"), received);
  }

  @Test
  @SuppressWarnings({"deprecation", "removal"})
  void testTheDeprecatedInvocationPutsNothingAbove() {
    final List<OutlineOffset> received = new ArrayList<>();
    final MarkupGeneratorMock generator = new MarkupGeneratorMock() {
      @Override
      public Markup renderFragmentInvocation(String name, OutlineOffset offset,
          Set<Argument> arguments, TranslatorContext translatorContext) {
        received.add(offset);
        return Markup.empty();
      }
    };
    generator.renderFragmentInvocation("legacy", Set.of(), TranslatorContext.DEFAULT);
    assertEquals(List.of(OutlineOffset.NONE), received);
  }

  @Test
  @SuppressWarnings({"deprecation", "removal"})
  void testTheDeprecatedOutputFileHasNoHeader() {
    final List<Markup> received = new ArrayList<>();
    final MarkupGeneratorMock generator = new MarkupGeneratorMock() {
      @Override
      public Markup composeOutputFile(List<Markup> globals, List<Markup> body,
          List<Markup> summary, List<Markup> navigation, Markup header, List<Markup> fragments) {
        received.add(header);
        return super.composeOutputFile(globals, body, summary, navigation, header, fragments);
      }
    };
    assertEquals("MAIN:\nm", generator.composeOutputFile(List.of(), List.of(new Markup("m")),
        List.of(), List.of(), List.of()).script);
    assertEquals(1, received.size());
    assertEquals("", received.get(0).script);
  }

  @Test
  void testEfx2_ALineThatDisplaysAPartOfTheHeaderIsANormalLineAtTheTop() throws Exception {
    assertEquals(String.join("\n",
        "body01: NORMAL 1 0",
        "header01: NORMAL 1 0",
        "header02: NORMAL 1 0",
        "header0201: NORMAL 2 0"),
        linesOf("eforms-sdk-2.0",
            "DISPLAY a;",
            "--- HEADER ---",
            "#TITLE DISPLAY b;",
            "DISPLAY c;",
            "\tDISPLAY d;"));
  }

  @Test
  void testEfx2_TheHeaderIsComposedOnce() throws Exception {
    assertEquals(List.of("[TITLE, COUNTRY] 1"), translate("eforms-sdk-2.0",
        "DISPLAY a;", "--- HEADER ---", "#TITLE DISPLAY b;", "#COUNTRY DISPLAY c;", "DISPLAY d;")
        .headers);
  }

  @Test
  void testEfx2_TheHeaderIsComposedWithoutAHeaderSection() throws Exception {
    assertEquals(List.of("[] 0"), translate("eforms-sdk-2.0", "DISPLAY a;").headers);
  }

  @Test
  void testEfx1_EveryLineIsNormal() throws Exception {
    assertEquals(String.join("\n",
        "block01: NORMAL 1 0",
        "block0101: NORMAL 2 0",
        "block02: NORMAL 1 0"),
        linesOf("eforms-sdk-1.0",
            "1 {BT-00-Text} a",
            "\t0 {ND-Root} b",
            "{BT-00-Text} c"));
  }
}
