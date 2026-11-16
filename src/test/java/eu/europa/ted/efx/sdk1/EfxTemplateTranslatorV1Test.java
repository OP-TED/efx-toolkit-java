package eu.europa.ted.efx.sdk1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.component.EfxTranslatorFactory;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.templates.Markup;
import eu.europa.ted.efx.model.templates.ViewTemplateSection;
import eu.europa.ted.efx.sdk1.EfxParser.TemplateFileContext;

class EfxTemplateTranslatorV1Test extends EfxTestsBase {

  @Test
  void testTranslatorReused_DoesNotKeepThePreviousTemplate() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    translator.renderTemplate("{BT-00-Text} foo\n", DEFAULT_OPTIONS);
    assertEquals(translateTemplate("{BT-00-Text} bar"), translator.renderTemplate("{BT-00-Text} bar\n", DEFAULT_OPTIONS));
  }

  @Test
  void testTranslatorReused_WithAnotherIndentation() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    translator.renderTemplate(lines("{BT-00-Text} foo", "  {ND-Root} bar") + "\n", DEFAULT_OPTIONS);
    assertEquals(translateTemplate(lines("{BT-00-Text} foo", "\t{ND-Root} bar")),
        translator.renderTemplate(lines("{BT-00-Text} foo", "\t{ND-Root} bar") + "\n", DEFAULT_OPTIONS));
  }

  /**
   * The context stack keeps the context of each context variable by its declaration, and every
   * render declares its variables anew, so a translator that kept one context stack for all its
   * renders would keep the contexts of every one of them.
   */
  @Test
  void testTranslatorReused_TakesANewContextStackForEachTemplate() throws InstantiationException {
    final EfxTemplateTranslatorV1 translator = (EfxTemplateTranslatorV1) EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    final String template = "{ND-Root} ${for context:$x in BT-00-Text return $x::BT-00-Text}\n";
    translator.renderTemplate(template, DEFAULT_OPTIONS);
    final ContextStack contextsOfTheFirst = translator.efxContext;
    translator.renderTemplate(template, DEFAULT_OPTIONS);
    assertNotSame(contextsOfTheFirst, translator.efxContext);
  }

  @Test
  void testTranslatorReused_AfterAFailedTemplate_RendersAsANewOne() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    assertThrows(InvalidIdentifierException.class, () -> translator.renderTemplate(
        lines("{ND-Root} foo", "\t{BT-00-Text} ${$missing}") + "\n", DEFAULT_OPTIONS));
    final String template = lines("{ND-Root} foo", "\t{BT-00-Text} bar");
    assertEquals(translateTemplate(template),
        translator.renderTemplate(template + "\n", DEFAULT_OPTIONS));
  }

  @Test
  void testTranslatorReused_AfterASyntaxError_RendersAsANewOne() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    assertThrows(InvalidIdentifierException.class, () -> translator.renderTemplate(
        lines("{ND-Root} foo", "\t{ND-Root} ${for context:$c in BT-00-Text return $missing}") + "\n",
        DEFAULT_OPTIONS));
    assertThrows(ParseCancellationException.class,
        () -> translator.renderTemplate("{ND-Root} ${1 + }\n", DEFAULT_OPTIONS));
    final String template = lines("{ND-Root} foo", "\t{BT-00-Text} bar");
    assertEquals(translateTemplate(template),
        translator.renderTemplate(template + "\n", DEFAULT_OPTIONS));
  }

  @Test
  void testTranslatorReused_ForAnExpression_KeepsNothingOfTheTemplateBefore()
      throws InstantiationException {
    final EfxTemplateTranslatorV1 translator = (EfxTemplateTranslatorV1) EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    translator.renderTemplate(lines("{ND-Root} foo", "\t{BT-00-Text} bar") + "\n", DEFAULT_OPTIONS);
    final ViewTemplateSection sectionOfTheTemplate = translator.section;
    translator.translateExpression("{ND-Root} ${1}");
    assertNotSame(sectionOfTheTemplate, translator.section);
  }

  /**
   * The translator is a listener that a walk of a template file can call directly, without
   * {@code renderTemplate}: the walk leaves the translated file on the stack.
   */
  @Test
  void testTranslatorWalkedDirectly_TranslatesTheTemplateAsANewOne() throws InstantiationException {
    final EfxTemplateTranslatorV1 translator = (EfxTemplateTranslatorV1) EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    ParseTreeWalker.DEFAULT.walk(translator, templateFileOf("{ND-Root} first\n"));
    translator.stack.pop(Markup.class);
    ParseTreeWalker.DEFAULT.walk(translator, templateFileOf("{ND-Root} second\n"));
    assertEquals(translateTemplate("{ND-Root} second"), translator.stack.pop(Markup.class).script);
  }

  @Test
  void testTranslatorWalkedDirectly_AfterARender_TranslatesTheTemplateAsANewOne()
      throws InstantiationException {
    final EfxTemplateTranslatorV1 translator = (EfxTemplateTranslatorV1) EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    translator.renderTemplate("{ND-Root} first\n", DEFAULT_OPTIONS);
    ParseTreeWalker.DEFAULT.walk(translator, templateFileOf("{ND-Root} second\n"));
    assertEquals(translateTemplate("{ND-Root} second"), translator.stack.pop(Markup.class).script);
  }

  /**
   * The template file is the parser context of the call stack while the file is translated, so that
   * an error raised at the level of the file names its position.
   */
  @Test
  void testTranslation_TakesTheTemplateFileAsItsParserContext() {
    final DependencyFactoryMock dependencies = DependencyFactoryMock.INSTANCE;
    final FileParserContextRecorder translator = new FileParserContextRecorder(
        dependencies.createMarkupGenerator(this.getSdkVersion(), "", DEFAULT_OPTIONS),
        dependencies.createSymbolResolver(this.getSdkVersion(), ""),
        dependencies.createScriptGenerator(this.getSdkVersion(), "", DEFAULT_OPTIONS),
        dependencies.createErrorListener());
    translator.renderTemplate("{ND-Root} x\n", DEFAULT_OPTIONS);
    assertSame(translator.templateFile, translator.parserContextOfTheFile);
  }

  /**
   * Records the parser context of the call stack where the walk leaves the template file. It is
   * declared with a qualifier of its own, as it would otherwise inherit the declaration of the
   * translator it extends, and two translators would be declared as one.
   */
  @SdkComponent(versions = {"1"}, componentType = SdkComponentType.EFX_TEMPLATE_TRANSLATOR,
      qualifier = "file-parser-context")
  static final class FileParserContextRecorder extends EfxTemplateTranslatorV1 {
    ParserRuleContext templateFile;
    ParserRuleContext parserContextOfTheFile;

    FileParserContextRecorder(final MarkupGenerator markupGenerator,
        final SymbolResolver symbolResolver, final ScriptGenerator scriptGenerator,
        final BaseErrorListener errorListener) {
      super(markupGenerator, symbolResolver, scriptGenerator, errorListener);
    }

    @Override
    public void exitTemplateFile(final TemplateFileContext ctx) {
      this.templateFile = ctx;
      this.parserContextOfTheFile = this.stack.peekParserContext();
      super.exitTemplateFile(ctx);
    }
  }

  private static ParseTree templateFileOf(final String template) {
    return new EfxParser(new CommonTokenStream(new EfxLexer(CharStreams.fromString(template))))
        .templateFile();
  }
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-1.0";
  }

  /*** Template line ***/

  @Test
  void testTemplateLineNoIdent() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { text('foo') }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} foo"));
  }

  /**
   * All nodes that contain any children get an auto generated outline number.
   */
  @Test
  void testTemplateLineOutline_Autogenerated() {
    assertEquals(lines("TEMPLATES:",
        "let block01() -> { #1: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #1.1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "MAIN:",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for a node can be overridden. Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Explicit() {
    assertEquals(lines("TEMPLATES:",
        "let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #2.3: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "MAIN:",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(
            lines("2{BT-00-Text} foo", "\t3{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for some nodes can be overridden. The other nodes get an auto
   * generated outline number. Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Mixed() {
    assertEquals(lines("TEMPLATES:",
        "let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #2.1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "MAIN:",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("2{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value zero. Leaf nodes
   * don't get an outline number.
   */
  @Test
  void testTemplateLineOutline_Suppressed() {
    assertEquals(lines("TEMPLATES:",
        "let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "MAIN:",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(
            lines("2{BT-00-Text} foo", "\t0{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value zero. Child nodes
   * will still get an outline number. Leaf nodes still won't get an outline number.
   */
  @Test
  void testTemplateLineOutline_SuppressedAtParent() {
    // Outline is ignored if the line has no children
    assertEquals(lines("TEMPLATES:", "let block01() -> { text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "MAIN:",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("0{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLineOutline_TooLarge() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("2147483648 {BT-00-Text} foo"));
    assertEquals(InvalidUsageException.ErrorCode.OUTLINE_NUMBER_TOO_LARGE, exception.getErrorCode());
    assertEquals("line 1:0 Outline number '2147483648' is too large: the largest is 2147483647.",
        exception.getMessage());
  }

  @Test
  void testTemplateLineFirstIndented() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLineIdentTab() {
    assertEquals(
        lines("TEMPLATES:", 
            "let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentSpaces() {
    assertEquals(
        lines("TEMPLATES:", 
            "let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "    {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLineIdentMixed() {
    final String lines = lines("{BT-00-Text} foo", "\t  {BT-00-Text} bar");
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate(lines));
  }

  @Test
  void testTemplateLineIdentMixedSpaceThenTab() {
    final String lines = lines("{BT-00-Text} foo", "  \t{BT-00-Text} bar");
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate(lines));
  }

  @Test
  void testTemplateLineIdentLower() {
    assertEquals(
        lines("TEMPLATES:", 
            "let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }",
            "let block0101() -> { text('bar') }",
            "let block02() -> { text('code') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(block01())",
            "for-each(/*/PathNode/CodeField).call(block02())"),
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar", "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLineIdentUnexpected() {
    final String lines = lines("{BT-00-Text} foo", "\t\t{BT-00-Text} bar");
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate(lines));
  }

  @Test
  void testTemplateLine_VariableScope() {
    assertEquals(
        lines("TEMPLATES:", 
            "let block01() -> { #1: eval(for $x in ./normalize-space(text()) return $x)", //
            "for-each(.).call(block0101()) }", //
            "let block0101() -> { eval(for $x in ./normalize-space(text()) return $x) }", //
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} ${for text:$x in BT-00-Text return $x}",
            "    {BT-00-Text} ${for text:$x in BT-00-Text return $x}")));

  }

  /*** Labels ***/

  @Test
  void testStandardLabelReference() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('auxiliary', '|', 'text', '|', 'value')) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testStandardLabelReference_WithAssetIdIterator() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in for $t in ./normalize-space(text()) return $t return concat('field', '|', 'name', '|', $item))) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|${for text:$t in BT-00-Text return $t}}"));
  }

  @Test
  void testShorthandBtLabelReference() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('business-term', '|', 'name', '|', 'BT-00')) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testShorthandFieldLabelReference() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testShorthandBtLabelReference_MissingLabelType() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForIndicator() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ../IndicatorField return concat('indicator', '|', 'when', '-', $item, '|', 'BT-00-Indicator'))) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ../CodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ../InternalCodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameAttributeInContext() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nMAIN:\nfor-each(/*/PathNode/CodeField/@attribute).call(block01())",
        translateTemplate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nMAIN:\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForText() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Text}"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_CODE_OR_INDICATOR,
        exception.getErrorCode());
    assertEquals(
        "line 1:14 '#{BT-00-Text}' needs a field of type 'code' or 'indicator', but 'BT-00-Text' is of type 'text'.",
        exception.getMessage());
  }

  @Test
  void testShorthandIndirectLabelReferenceForAttribute() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Attribute}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }\nMAIN:\nfor-each(/*/PathNode/IndicatorField).call(block01())",
        translateTemplate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }\nMAIN:\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndTextField() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithOtherLabelType() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithUnknownLabelType() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithNodeContext() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('node', '|', 'name', '|', 'ND-Root')) }\nMAIN:\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root}  #{name}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nMAIN:\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} #value"));
  }

  @Test
  void testShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{ND-Root} #value"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_FIELD_CONTEXT,
        exception.getErrorCode());
    assertEquals("line 1:10 '#value' can only be used when the context is a field.",
        exception.getMessage());
  }


  /*** Expression block ***/

  @Test
  void testShorthandFieldValueReferenceFromContextField() {
    assertEquals("TEMPLATES:\nlet block01() -> { eval(./normalize-space(text())) }\nMAIN:\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} $value"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithText() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { text('blah ')label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item)))text(' ')text('blah ')eval(./normalize-space(text()))text(' ')text('blah') }\nMAIN:\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testShorthandFieldValueReferenceFromContextField_WithNodeContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{ND-Root} $value"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_FIELD_CONTEXT,
        exception.getErrorCode());
    assertEquals("line 1:10 '$value' can only be used when the context is a field.",
        exception.getMessage());
  }


  /*** Other ***/

  @Test
  void testNestedExpression() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', ./normalize-space(text()))) }\nMAIN:\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|${BT-00-Text}}"));
  }

  @Test
  void testEndOfLineComments() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' ')text('blah blah') }\nMAIN:\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root} #{name|BT-00-Text} blah blah // comment blah blah"));
  }

  @Test
  void testImplicitFormatting_Dates() {
    assertEquals("TEMPLATES:\nlet block01() -> { eval(for $item in PathNode/StartDateField/xs:date(text()) return format-date($item, '[D01]/[M01]/[Y0001]')) }\nMAIN:\nfor-each(/*).call(block01())", translateTemplate("{ND-Root} ${BT-00-StartDate}"));
  }

  @Test
  void testImplicitFormatting_Times() {
    assertEquals("TEMPLATES:\nlet block01() -> { eval(for $item in PathNode/StartTimeField/xs:time(text()) return format-time($item, '[H01]:[m01] [Z]')) }\nMAIN:\nfor-each(/*).call(block01())", translateTemplate("{ND-Root} ${BT-00-StartTime}"));
  }

  /*** Multilingual fields ***/

  /**
   * In a view template the value of a multilingual field must be retrieved in the preferred
   * language, so the reference is wrapped in a call to efx:preferred-language-text().
   * This is the behaviour that template translation must keep.
   */
  @Test
  void testMultilingualTextField_IsRetrievedInPreferredLanguage() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { eval(efx:preferred-language-text(PathNode/TextMultilingualField)) }\nMAIN:\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root} ${BT-00-Text-Multilingual}"));
  }

  /**
   * When the reference already pins a language, the value is retrieved as-is: adding the preferred
   * language call on top would override the language the template author asked for.
   */
  @Test
  void testMultilingualTextField_WithLanguagePredicate_IsRetrievedAsIs() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { eval(PathNode/TextMultilingualField[./@languageID = 'eng']/normalize-space(text())) }\nMAIN:\nfor-each(/*).call(block01())",
        translateTemplate(
            "{ND-Root} ${BT-00-Text-Multilingual[BT-00-Text-Multilingual/@languageID == 'eng']}"));
  }

  /**
   * A multilingual field used as a sequence goes through its own code path, so it needs its own
   * coverage.
   */
  @Test
  void testMultilingualTextField_AsSequence_IsRetrievedInPreferredLanguage() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { eval(for $t in efx:preferred-language-text(PathNode/TextMultilingualField) return $t) }\nMAIN:\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root} ${for text:$t in BT-00-Text-Multilingual return $t}"));
  }

  /**
   * The $value shorthand goes through a template-only code path, so it needs its own coverage.
   */
  @Test
  void testMultilingualTextField_ShorthandValueReference_IsRetrievedInPreferredLanguage() {
    assertEquals(
        "TEMPLATES:\nlet block01() -> { eval(efx:preferred-language-text(.)) }\nMAIN:\nfor-each(/*/PathNode/TextMultilingualField).call(block01())",
        translateTemplate("{BT-00-Text-Multilingual} $value"));
  }

  /**
   * A name declared again, as a variable that holds a value, no longer names the context that an
   * earlier declaration of it held.
   */
  @Test
  void testContextVariableNameDeclaredAgain_AsAValue_IsNotAContextVariable() {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate(lines(
            "{ND-Root} ${for context:$x in BT-00-Text return $x::BT-00-Text}",
            "{ND-Root} ${for text:$x in BT-00-Text return $x::BT-00-Text}")));
    assertEquals(InvalidIdentifierException.ErrorCode.NOT_A_CONTEXT_VARIABLE,
        exception.getErrorCode());
    assertEquals("line 2:45 Variable '$x' is not a context variable.", exception.getMessage());
  }
}
