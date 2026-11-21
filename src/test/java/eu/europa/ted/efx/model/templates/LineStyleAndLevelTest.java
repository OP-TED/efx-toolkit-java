package eu.europa.ted.efx.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.Parameter;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.mock.MarkupGeneratorMock;
import eu.europa.ted.efx.model.DecimalFormat;

/**
 * Checks the style and the level that the template translator gives the markup generator for each
 * template line.
 */
class LineStyleAndLevelTest {

  private static final TranslatorOptions OPTIONS =
      new EfxTranslatorOptions("udf", DecimalFormat.EFX_DEFAULT);

  /** Records the style and the level of each fragment definition, by fragment name. */
  private static final class RecordingMarkupGenerator extends MarkupGeneratorMock {
    final Map<String, String> lines = new LinkedHashMap<>();

    @Override
    public Markup composeFragmentDefinition(String name, String number, LineStyle style, int level,
        Set<Conditional> conditionals, Markup content, Markup children, Set<Parameter> parameters,
        TranslatorContext translatorContext) {
      this.lines.put(name, style + " " + level);
      return super.composeFragmentDefinition(name, number, style, level, conditionals, content,
          children, parameters, translatorContext);
    }
  }

  /** Gives the translator a recording markup generator, and the mocks for the rest. */
  private static final class RecordingFactory implements TranslatorDependencyFactory {
    final RecordingMarkupGenerator markupGenerator = new RecordingMarkupGenerator();

    @Override
    public SymbolResolver createSymbolResolver(String sdkVersion, String qualifier) {
      return DependencyFactoryMock.INSTANCE.createSymbolResolver(sdkVersion, qualifier);
    }

    @Override
    public ScriptGenerator createScriptGenerator(String sdkVersion, String qualifier,
        TranslatorOptions options) {
      return DependencyFactoryMock.INSTANCE.createScriptGenerator(sdkVersion, qualifier, options);
    }

    @Override
    public MarkupGenerator createMarkupGenerator(String sdkVersion, String qualifier,
        TranslatorOptions options) {
      return this.markupGenerator;
    }

    @Override
    public ValidatorGenerator createValidatorGenerator(String sdkVersion, String qualifier,
        TranslatorOptions options) {
      return null;
    }

    @Override
    public BaseErrorListener createErrorListener() {
      return DependencyFactoryMock.INSTANCE.createErrorListener();
    }
  }

  private static String linesOf(final String sdkVersion, final String... template)
      throws Exception {
    final RecordingFactory factory = new RecordingFactory();
    EfxTranslator.translateTemplate(factory, sdkVersion, String.join("\n", template) + "\n",
        OPTIONS);
    return factory.markupGenerator.lines.entrySet().stream()
        .map(entry -> entry.getKey() + ": " + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  @Test
  void testEfx2_AMarkerMakesAHeading() throws Exception {
    assertEquals(String.join("\n",
        "body01: HEADING 1",
        "body02: HEADING 1",
        "body03: HEADING 1",
        "body04: NORMAL 1"),
        linesOf("eforms-sdk-2.0",
            "1 WITH BT-00-Text DISPLAY numbered;",
            "? WITH BT-00-Text DISPLAY auto-numbered;",
            "# WITH BT-00-Text DISPLAY unnumbered;",
            "WITH BT-00-Text DISPLAY normal;"));
  }

  @Test
  void testEfx2_TheLevelFollowsTheNesting() throws Exception {
    assertEquals(String.join("\n",
        "body01: HEADING 1",
        "body0101: NORMAL 2",
        "body010101: HEADING 3",
        "body0102: NORMAL 2",
        "body02: NORMAL 1"),
        linesOf("eforms-sdk-2.0",
            "1 WITH BT-00-Text DISPLAY a;",
            "\tWITH ND-Root DISPLAY b;",
            "\t\t# WITH BT-00-Number DISPLAY c;",
            "\tWITH ND-Root DISPLAY d;",
            "WITH BT-00-Text DISPLAY e;"));
  }

  @Test
  void testEfx2_TheLevelStartsAgainInATemplateSection() throws Exception {
    // A template section that holds its lines is at level 0; its top line is at level 1.
    assertEquals(String.join("\n",
        "lines: NORMAL 0",
        "lines01: HEADING 1",
        "lines0101: NORMAL 2",
        "body01: NORMAL 1"),
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
        "single: NORMAL 1",
        "single01: NORMAL 2",
        "body01: NORMAL 1"),
        linesOf("eforms-sdk-2.0",
            "--- TEMPLATE single ---",
            "DISPLAY a;",
            "\tWITH ND-Root DISPLAY b;",
            "--- MAIN ---",
            "INVOKE single();"));
  }

  @Test
  void testEfx1_EveryLineIsNormal() throws Exception {
    assertEquals(String.join("\n",
        "block01: NORMAL 1",
        "block0101: NORMAL 2",
        "block02: NORMAL 1"),
        linesOf("eforms-sdk-1.0",
            "1 {BT-00-Text} a",
            "\t0 {ND-Root} b",
            "{BT-00-Text} c"));
  }
}
