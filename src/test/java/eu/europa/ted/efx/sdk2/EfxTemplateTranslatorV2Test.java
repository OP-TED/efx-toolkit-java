/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import eu.europa.ted.efx.exceptions.EfxCompilationException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.component.EfxTranslatorFactory;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIndentationException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;

class EfxTemplateTranslatorV2Test extends EfxTestsBase {
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  @Test
  void testTranslatorReused_WithAnotherIndentation() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    translator.renderTemplate(lines("{BT-00-Text} foo", "  {ND-Root} bar") + "\n", DEFAULT_OPTIONS);
    assertEquals(translateTemplate(lines("{BT-00-Text} foo", "\t{ND-Root} bar")),
        translator.renderTemplate(lines("{BT-00-Text} foo", "\t{ND-Root} bar") + "\n", DEFAULT_OPTIONS));
  }

  @Test
  void testTranslatorReused_WithTheSameDeclarations() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    final String template = lines("let text:$t = 'test';", "let template:greeting() display Hello;",
        "display this is a ${$t};", "invoke greeting();");
    translator.renderTemplate(template + "\n", DEFAULT_OPTIONS);
    assertEquals(translateTemplate(template), translator.renderTemplate(template + "\n", DEFAULT_OPTIONS));
  }

  @Test
  void testTranslatorReused_DoesNotKeepThePreviousDeclarations() throws InstantiationException {
    final EfxTemplateTranslator translator = EfxTranslatorFactory
        .getEfxTemplateTranslator(this.getSdkVersion(), DependencyFactoryMock.INSTANCE, DEFAULT_OPTIONS);
    translator.renderTemplate(lines("let template:greeting() display Hello;", "invoke greeting();") + "\n",
        DEFAULT_OPTIONS);
    assertThrows(InvalidIdentifierException.class, () -> translator.renderTemplate("invoke greeting();\n", DEFAULT_OPTIONS));
  }

  // #region Core Template Structure -------------------------------------------

  // #region templateDefinition ------------------------------------------------

  @Test
  void testTemplateDefinition_InvokeTemplate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let some-template(string:content) -> { text('Content: ')eval($content) }",
            "let body01() -> { call(some-template(string:content='test')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:some-template(text:$content) display Content: ${$content};",
            "invoke some-template('test');")));
  }

  @Test
  void testTemplateDefinition_NestedInvokeTemplate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let other-template(string:value) -> { text('Value: ')eval($value) }",
            "let invoke-template(string:param) -> { call(other-template(string:value=$param)) }",
            "let body01() -> { call(invoke-template(string:param='test')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:other-template(text:$value) display Value: ${$value};",
            "let template:invoke-template(text:$param) invoke other-template($param);",
            "invoke invoke-template('test');")));
  }

  @Test
  void testTemplateDefinition_ChooseTemplate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let conditional-template(boolean:condition, string:value) -> { choose { when $condition: text('Condition met: ')eval($value), when $condition = false(): text('Condition not met'), otherwise: text('Unknown condition: ')eval($condition) } }",
            "let body01() -> { call(conditional-template(boolean:condition=true(), string:value='test')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:conditional-template(indicator:$condition, text:$value)",
            "when $condition display Condition met: ${$value}",
            "when $condition == FALSE display Condition not met",
            "otherwise display Unknown condition: ${$condition};",
            "invoke conditional-template(TRUE, 'test');")));
  }

  @Test
  void testTemplateDefinition_ParameterValidation() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let param-validation-template(string:param1, decimal:param2) -> { text('Valid parameters') }",
            "let body01() -> { call(param-validation-template(string:param1='test', decimal:param2=42)) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
            "invoke param-validation-template('test', 42);")));
  }

  // #endregion templateDefinition ---------------------------------------------

  // #region templateDeclaration -----------------------------------------------

  @Test
  void testTemplateDeclaration_NoParameters() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let simple-template() -> { text('Hello World') }",
            "let body01() -> { call(simple-template()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:simple-template() display Hello World;",
            "invoke simple-template();")));
  }

  @Test
  void testTemplateDeclaration_SingleParameter() {
    assertEquals(
        lines(
            "TEMPLATES:", 
            "let greeting-template(string:name) -> { text('Hello ')eval($name) }",
            "let body01() -> { call(greeting-template(string:name='World')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:greeting-template(text:$name) display Hello ${$name};",
            "invoke greeting-template('World');")));
  }

  @Test
  void testTemplateDeclaration_MultipleParameters() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let complex-template(string:first, decimal:second, boolean:third) -> { text('Values: ')eval($first)text(', ')eval(for $__item in $second return format-number($__item, '# ##0,#########'))text(', ')eval($third) }",
            "let body01() -> { call(complex-template(string:first='test', decimal:second=42, boolean:third=true())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:complex-template(text:$first, number:$second, indicator:$third) display Values: ${$first}, ${$second}, ${$third};",
            "invoke complex-template('test', 42, TRUE);")));
  }

  @Test
  void testTemplateDeclaration_AllParameterTypes() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let all-types-template(string:str, decimal:num, boolean:bool, date:dt, time:tm, duration:dur) -> { text('Params: ')eval($str)text(', ')eval(for $__item in $num return format-number($__item, '# ##0,#########'))text(', ')eval($bool)text(', ')eval(format-date($dt, '[D01]/[M01]/[Y0001]'))text(', ')eval(format-time($tm, '[H01]:[m01] [Z]'))text(', ')eval(for $__item in (if ((years-from-duration($dur) * 12 + months-from-duration($dur)) != 0) then (years-from-duration($dur) * 12 + months-from-duration($dur)) else days-from-duration($dur)) return format-number($__item, '# ##0,#########'))eval(for $code in for $__item in $dur return (if ((years-from-duration($__item) * 12 + months-from-duration($__item)) != 0) then 'MONTH' else 'DAY') return ' ')label(for $code in for $__item in $dur return (if ((years-from-duration($__item) * 12 + months-from-duration($__item)) != 0) then 'MONTH' else 'DAY') return concat('code', '|', 'name', '|', 'timeperiod.', $code), (if ((years-from-duration($dur) * 12 + months-from-duration($dur)) != 0) then (years-from-duration($dur) * 12 + months-from-duration($dur)) else days-from-duration($dur))) }",
            "let body01() -> { call(all-types-template(string:str='text', decimal:num=123, boolean:bool=true(), date:dt=xs:date('2023-01-01'), time:tm=xs:time('12:00:00'), duration:dur=xs:dayTimeDuration('P1D'))) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:all-types-template(text:$str, number:$num, indicator:$bool, date:$dt, time:$tm, duration:$dur) display Params: ${$str}, ${$num}, ${$bool}, ${$dt}, ${$tm}, ${$dur};",
            "invoke all-types-template('text', 123, TRUE, date('2023-01-01'), time('12:00:00'), day-time-duration('P1D'));")));
  }

  @Test
  void testTemplateDeclaration_SpecialCharactersInName() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let template-with-dashes(string:param) -> { text('Template: ')eval($param) }",
            "let body01() -> { call(template-with-dashes(string:param='test')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:template-with-dashes(text:$param) display Template: ${$param};",
            "invoke template-with-dashes('test');")));
  }

  // #endregion templateDeclaration --------------------------------------------

  // #region templateFragment --------------------------------------------------

  @Test
  void testTemplateFragment_TextAndExpression() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Value is: ')eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text} Value is: ${BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_TextAndLabel() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Field: ')label(concat('field', '|', 'name', '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text} Field: #{field|name|BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_MixedContent() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Label: ')label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' Value: ')eval(./normalize-space(text()))text(' End') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text} Label: #{field|name|BT-00-Text} Value: ${BT-00-Text} End"));
  }

  @Test
  void testTemplateFragment_OnlyExpression() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text} ${BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_OnlyLabel() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text} #{field|name|BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_MultipleExpressions() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(for $__item in ./number() return format-number($__item, '# ##0,#########'))text(' + ')eval(for $__item in ./number() return format-number($__item, '# ##0,#########'))text(' = ')eval(for $__item in ./number() + ./number() return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*/PathNode/NumberField).call(body01())"),
        translateTemplate("{BT-00-Number} ${BT-00-Number} + ${BT-00-Number} = ${BT-00-Number + BT-00-Number}"));
  }

  // #region textBlock ---------------------------------------------------------

  @Test
  void testTextBlock_SimpleText() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Simple text content') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display Simple text content;"));
  }

  @Test
  void testTextBlock_WithWhitespace() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Text with   spaces') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display   Text with   spaces  ;"));
  }

  @Test
  void testTextBlock_WithSpecialCharacters() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Text with special chars: &#60;&#62;&#38;&#34;&#39;') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display Text with special chars: <>&#38;\"';"));
  }

  @Test
  void testTextBlock_MultipleTextBlocks() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('First block ')eval('')text(' Second block') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display First block ${''} Second block;"));
  }

  @Test
  void testTextBlock_WithNewlines() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Line 1')line-break()text('Line 2')line-break()text('Line 3') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display Line 1 \\nLine 2\\n  Line 3;"));
  }

  @Test
  void testTextBlock_WithEscapedCharacters() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Text with quotes: &#34;&#39; and backslash: \\\\') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display Text with quotes: \"' and backslash: \\\\;"));
  }

  // #endregion textBlock ------------------------------------------------------

  // #region linkedTextBlock ---------------------------------------------------
  
  @Test
  void testLinkedTextBlock() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('here is a ')hyperlink(text('Link'), 'http://example.com')text('. How about it?') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("DISPLAY here is a Link@{'http://example.com'}. How about it?;"));
  }

  @Test
  void testLinkedTextBlock_WithSpecialCharacters_IsEscaped() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('see ')hyperlink(text('a&#60;b'), 'http://example.com') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("DISPLAY see a<b@{'http://example.com'};"));
  }

  @Test
  void testLinkedLabelBlock() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('here is a linked label: ')hyperlink(label(concat('field', '|', 'name', '|', 'BT-00-Text')), 'http://example.com')text('. How about it?') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("DISPLAY here is a linked label: #{field|name|BT-00-Text}@{'http://example.com'}. How about it?;"));
  }

    @Test
  void testLinkedExpressionBlock() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('here is a ')hyperlink(eval('multi word link'), 'http://example.com')text('. How about it?') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("DISPLAY here is a ${'multi word link'}@{'http://example.com'}. How about it?;"));
  }

  // #endregion linkedTextBlock ------------------------------------------------

  // #endregion templateFragment -----------------------------------------------

  @Test
  void testTemplate_ComplexNesting() {
    assertEquals(
        lines(
            "TEMPLATES:", 
            "let complex-template(string:field) -> { text('name ')eval($field)text(' label ')label(concat('field', '|', 'name', '|', $field)) }",
            "let body01() -> { call(complex-template(string:field='BT-00-Text')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:complex-template(text:$field) display name ${$field} label #{field|name|${$field}};",
            "invoke complex-template('BT-00-Text');")));
  }

  // #endregion Core Template Structure ----------------------------------------

  // #region Globals -----------------------------------------------------------

  @Test
  void testGlobals_VariableDeclaration() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:t3='a'",
            "decimal:n1=12",
            "string:t1=$t3",
            "string:test(string:p1, decimal:p2) -> { concat($p1, $t1) }",
            "decimal:n2=$n1 + 1",
            "string:t4=udf:test($t3, 22)",
            "TEMPLATES:",
            "let body01(string:t2) -> { eval(PathNode/TextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01(string:t2=udf:test($t3, 99)))"), //
        translateTemplate(lines(
            "// comment", //
            "let text:$t3='a'; // comment",
            "let number:$n1=12;",
            " // comment",
            "let text:$t1=$t3;",
            "let text:?test(text:$p1, number:$p2) = concat($p1, $t1);",
            "let number:$n2 = $n1 + 1;",
            "let text:$t4= ?test($t3, 22);",
            "{ND-Root, text:$t2=?test($t3, 99)} ${BT-00-Text}")));
  }

  @Test
  void testGlobals_NamedTemplates() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:t3='a'",
            "decimal:n1=12",
            "string:t1=$t3",
            "string:test(string:p1, decimal:p2) -> { concat($p1, $t1) }",
            "decimal:n2=$n1 + 1",
            "string:t4=udf:test($t3, 22)",
            "TEMPLATES:",
            "let some-template(string:text1, string:text2) -> { eval(/*/PathNode/TextField/normalize-space(text()))text(' dokimi&#59; ')eval($text2)text(' &#59;')",
            "for-each(/*/PathNode/NumberField).call(some-template01(string:text1=$text1, string:text2=$text2)) }",
            "let some-template01(string:text1, string:text2) -> { eval(../TextField/normalize-space(text())) }",
            "let body01(string:ctx2) -> { #2: eval(./normalize-space(text()))text(' lala')",
            "for-each(../NumberField).call(body0101(string:ctx2=$ctx2, decimal:ctx3=.))",
            "for-each(.).call(body0102(string:ctx2=$ctx2, string:ctx=., string:t2=udf:test($t3, 99)))",
            "for-each(.).call(body0103(string:ctx2=$ctx2, string:ctx4=., string:t5=udf:test($t3, 99))) }",
            "let body0101(string:ctx2, decimal:ctx3) -> { eval(../TextField/normalize-space(text())) }",
            "let body0102(string:ctx2, string:ctx, string:t2) -> { call(some-template(string:text1=$ctx/normalize-space(text()), string:text2=$t2)) }",
            "let body0103(string:ctx2, string:ctx4, string:t5) -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:ctx2=.))"),
        translateTemplate(lines(
            "// comment", //
            "let text:$t3='a';// comment",
            "let number:$n1 = 12; // comment",
            "let text:$t1 = $t3;",
            "let text:?test(text:$p1, number:$p2) = concat($p1, $t1);",
            "let number:$n2 = $n1 + 1;",
            "let text:$t4= ?test($t3, 22);",
            "let template:some-template(text:$text1, text:$text2) display ${BT-00-Text} dokimi&#59; ${$text2} &#59;;",
            "  {BT-00-Number} ${BT-00-Text}",
            "2 {context:$ctx2=BT-00-Text} ${BT-00-Text} lala",
            "  3 with context:$ctx3=BT-00-Number display ${BT-00-Text};",
            "  4 with context:$ctx = BT-00-Text, text:$t2 = ?test($t3, 99) invoke some-template($ctx, $t2);",
            "  with context:$ctx4 = BT-00-Text, text:$t5 = ?test($t3, 99) display ${BT-00-Text};")));
  }

  @Test
  void testInvokeTemplate_Nesting() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let some-template(string:t) -> { text('--')eval($t)text('--') }",
            "let body01(string:ctx1, string:tx) -> { #1: call(some-template(string:t=$tx))",
            "for-each(../StartDateField).call(body0101(string:ctx1=$ctx1, string:tx=$tx)) }",
            "let body0101(string:ctx1, string:tx) -> { text('Nested content allowed') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:ctx1=., string:tx='++'))"), //
        translateTemplate(lines(
            "let template:some-template(text:$t) display --${$t}--;",
            "with context:$ctx1 = BT-00-Text, text:$tx='++' invoke some-template($tx);",
            "  {BT-00-StartDate} Nested content allowed")));
  }

  @Test
  void testGlobals_NoTemplateLines() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:t3='a'",
            "decimal:n1=12",
            "string:t1=$t3",
            "string:test(string:p1, decimal:p2) -> { concat($p1, $t1) }",
            "decimal:n2=$n1 + 1",
            "string:t4=udf:test($t3, 22)"), //
        translateTemplate(lines(
            "// comment", //
            "let text:$t3='a';// comment",
            "let number:$n1=12;",
            "let text:$t1=$t3;",
            "let text:?test(text:$p1, number:$p2) = concat($p1, $t1);",
            "let number:$n2 = $n1 + 1;",
            "let text:$t4= ?test($t3, 22);")));
  }

  @Test
  void testGlobals_DictionaryDeclaration() {
    assertEquals(
        lines(
            "GLOBALS:", 
            "let dic index /*/PathNode/NumberField by ../TextField/normalize-space(text());",
            "TEMPLATES:",
            "let body01() -> { eval(for $__item in key('dic', 'key') return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "display ${$dic['key']};")));
  }

  @Test
  void testDictionaryLookup_InIndexOnPath() {
    // A lookup reads the root, which each element of the path reads the same, so the position is
    // not bound
    assertEquals(
        lines(
            "GLOBALS:",
            "let d index /*/PathNode/TextField by ./normalize-space(text());",
            "TEMPLATES:",
            "let body01() -> { eval((PathNode/RepeatableTextField)[count(key('d', 'x'))]/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let $d index BT-00-Text by BT-00-Text;",
            "{ND-Root} ${BT-00-Repeatable-Text[at count($d['x'])]}")));
  }

  @Test
  void testDictionaryLookup_InFilterInIndexOnList() {
    // The filter reads the root from each of its own elements, not from the values of the list
    assertEquals(
        lines(
            "GLOBALS:",
            "let d index /*/PathNode/TextField by ./normalize-space(text());",
            "TEMPLATES:",
            "let body01() -> { eval(for $__item in for $c in . return (1,2)[count($c/PathNode/RepeatableTextField[count(key('d', 'x')) = 1]/normalize-space(text()))] return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let $d index BT-00-Text by BT-00-Text;",
            "{ND-Root} ${for context:$c in ND-Root return [1, 2][at count($c::BT-00-Repeatable-Text[count($d['x']) == 1])]}")));
  }

  // #endregion Globals --------------------------------------------------------

  @Test
  void testDisplayTemplate() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:t='test'",
            "TEMPLATES:",
            "let body01() -> { text('this is a ')eval($t) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text:$t = 'test';",
            "display this is a ${$t};")));
  }

  // #region Sequence variable declarations ------------------------------------

  @Test
  void testDisplayTemplate_WithTextSequenceVariable() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string*:items=('a','b','c')",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($items) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text*:$items = ['a', 'b', 'c'];",
            "display count: ${count($items)};")));
  }

  @Test
  void testDisplayTemplate_WithNumericSequenceVariable() {
    assertEquals(
        lines(
            "GLOBALS:",
            "decimal*:nums=(1,2,3)",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($nums) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let number*:$nums = [1, 2, 3];",
            "display count: ${count($nums)};")));
  }

  @Test
  void testDisplayTemplate_WithBooleanSequenceVariable() {
    assertEquals(
        lines(
            "GLOBALS:",
            "boolean*:flags=(true(),false())",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($flags) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let indicator*:$flags = [TRUE, FALSE];",
            "display count: ${count($flags)};")));
  }

  @Test
  void testDisplayTemplate_WithDateSequenceVariable() {
    assertEquals(
        lines(
            "GLOBALS:",
            "date*:dates=(xs:date('2024-01-01Z'),xs:date('2024-12-31Z'))",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($dates) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let date*:$dates = [2024-01-01Z, 2024-12-31Z];",
            "display count: ${count($dates)};")));
  }

  @Test
  void testDisplayTemplate_WithTimeSequenceVariable() {
    assertEquals(
        lines(
            "GLOBALS:",
            "time*:times=(xs:time('10:00:00Z'),xs:time('18:00:00Z'))",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($times) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let time*:$times = [10:00:00Z, 18:00:00Z];",
            "display count: ${count($times)};")));
  }

  @Test
  void testDisplayTemplate_WithDurationSequenceVariable() {
    assertEquals(
        lines(
            "GLOBALS:",
            "duration*:durs=(xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M'))",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($durs) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let duration*:$durs = [P1Y, P2M];",
            "display count: ${count($durs)};")));
  }

  // #endregion Sequence variable declarations ---------------------------------

  // #region Function declarations ---------------------------------------------

  @Test
  void testDisplayTemplate_WithNumericFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "decimal:twice(decimal:n) -> { $n * 2 }",
            "TEMPLATES:",
            "let body01() -> { text('twice: ')eval(for $__item in udf:twice(21) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let number:?twice(number:$n) = $n * 2;",
            "display twice: ${?twice(21)};")));
  }

  @Test
  void testDisplayTemplate_WithDateFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "date:nextDay(date:d) -> { ($d + xs:dayTimeDuration('P1D')) }",
            "TEMPLATES:",
            "let body01() -> { text('next day: ')eval(format-date(udf:nextDay(xs:date('2024-01-01Z')), '[D01]/[M01]/[Y0001]')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let date:?nextDay(date:$d) = add-duration($d, P1D);",
            "display next day: ${?nextDay(2024-01-01Z)};")));
  }

  @Test
  void testDisplayTemplate_DatePlusDuration_IsDisplayedAsADate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('deadline: ')eval(format-date((PathNode/StartDateField/xs:date(text()) + xs:dayTimeDuration('P3D')), '[D01]/[M01]/[Y0001]')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("display deadline: ${BT-00-StartDate + P3D};"));
  }

  @Test
  void testDisplayTemplate_WithTimeFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "time:noon() -> { xs:time('12:00:00Z') }",
            "TEMPLATES:",
            "let body01() -> { text('noon: ')eval(format-time(udf:noon(), '[H01]:[m01] [Z]')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let time:?noon() = 12:00:00Z;",
            "display noon: ${?noon()};")));
  }

  @Test
  void testDisplayTemplate_WithDurationFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "duration:week() -> { xs:dayTimeDuration('P7D') }",
            "TEMPLATES:",
            "let body01() -> { text('days: ')eval(for $__item in days-from-duration(udf:week()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let duration:?week() = P1W;",
            "display days: ${days(?week())};")));
  }

  // A function's parameters are declared in its own scope, which closes after its body.

  @Test
  void testFunctionScope_ParameterIsNotDeclaredInTheNextFunction() {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate(lines("let text:?a(text:$x) = $x;", "let text:?b() = $x;",
            "display ${?b()};")));
    assertEquals(InvalidIdentifierException.ErrorCode.UNDECLARED_IDENTIFIER,
        exception.getErrorCode());
    assertEquals("line 2:16 Variable '$x' is not declared.", exception.getMessage());
  }

  @Test
  void testFunctionScope_ParameterIsNotDeclaredInATemplateLine() {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate(lines("let text:?a(text:$x) = $x;", "display ${$x};")));
    assertEquals(InvalidIdentifierException.ErrorCode.UNDECLARED_IDENTIFIER,
        exception.getErrorCode());
    assertEquals("line 2:10 Variable '$x' is not declared.", exception.getMessage());
  }

  @Test
  void testFunctionScope_TwoFunctionsTakeParametersOfTheSameName() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:a(string:x) -> { $x }",
            "string:b(string:x) -> { $x }",
            "TEMPLATES:",
            "let body01() -> { eval(udf:b('c')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines("let text:?a(text:$x) = $x;", "let text:?b(text:$x) = $x;",
            "display ${?b('c')};")));
  }

  @Test
  void testFunctionScope_VariableTakesTheNameOfAParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:a(string:x) -> { $x }",
            "string:x='b'",
            "TEMPLATES:",
            "let body01() -> { eval($x) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines("let text:?a(text:$x) = $x;", "let text:$x = 'b';",
            "display ${$x};")));
  }

  @Test
  void testFunctionScope_FunctionTakesTheNameOfAParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:a(string:x) -> { $x }",
            "string:x() -> { 'd' }",
            "TEMPLATES:",
            "let body01() -> { eval(udf:x()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines("let text:?a(text:$x) = $x;", "let text:?x() = 'd';",
            "display ${?x()};")));
  }

  @Test
  void testFunctionScope_TemplateTakesAParameterOfTheSameName() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:a(string:x) -> { $x }",
            "TEMPLATES:",
            "let t(string:x) -> { eval($x) }",
            "let body01() -> { call(t(string:x='c')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines("let text:?a(text:$x) = $x;",
            "let template:t(text:$x) display ${$x};", "invoke t('c');")));
  }

  @Test
  void testFunctionScope_FunctionCannotTakeTheNameOfItsParameter() {
    assertNameClash("line 1:0 Function '?x' has the same name as Parameter '$x', already"
        + " declared in this scope.", "let text:?x(text:$x) = $x;", "display ${?x('c')};");
  }

  // #endregion Function declarations ------------------------------------------

  // #region Sequence function declarations ------------------------------------

  @Test
  void testDisplayTemplate_WithTextSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string*:getItems() -> { ('a','b','c') }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:getItems()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text*:?getItems() = ['a', 'b', 'c'];",
            "display count: ${count(?getItems())};")));
  }

  @Test
  void testDisplayTemplate_WithNumericSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "decimal*:getNumbers() -> { (1,2,3) }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:getNumbers()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let number*:?getNumbers() = [1, 2, 3];",
            "display count: ${count(?getNumbers())};")));
  }

  @Test
  void testDisplayTemplate_WithBooleanSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "boolean*:getFlags() -> { (true(),false()) }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:getFlags()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let indicator*:?getFlags() = [TRUE, FALSE];",
            "display count: ${count(?getFlags())};")));
  }

  @Test
  void testDisplayTemplate_WithDateSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "date*:getDates() -> { (xs:date('2024-01-01Z'),xs:date('2024-12-31Z')) }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:getDates()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let date*:?getDates() = [2024-01-01Z, 2024-12-31Z];",
            "display count: ${count(?getDates())};")));
  }

  @Test
  void testDisplayTemplate_WithTimeSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "time*:getTimes() -> { (xs:time('10:00:00Z'),xs:time('18:00:00Z')) }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:getTimes()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let time*:?getTimes() = [10:00:00Z, 18:00:00Z];",
            "display count: ${count(?getTimes())};")));
  }

  @Test
  void testDisplayTemplate_WithDurationSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "duration*:getDurations() -> { (xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:getDurations()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let duration*:?getDurations() = [P1Y, P2M];",
            "display count: ${count(?getDurations())};")));
  }

  // #endregion Sequence function declarations ---------------------------------

  // #region Sequence parameter declarations -----------------------------------

  @Test
  void testDisplayTemplate_WithTextSequenceParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string*:processItems(string*:items) -> { $items }",
            "TEMPLATES:",
            "let body01() -> { text('done') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text*:?processItems(text*:$items) = $items;",
            "display done;")));
  }

  @Test
  void testDisplayTemplate_WithNumericSequenceParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "decimal*:processNumbers(decimal*:nums) -> { $nums }",
            "TEMPLATES:",
            "let body01() -> { text('done') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let number*:?processNumbers(number*:$nums) = $nums;",
            "display done;")));
  }

  @Test
  void testDisplayTemplate_WithBooleanSequenceParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "boolean*:processFlags(boolean*:flags) -> { $flags }",
            "TEMPLATES:",
            "let body01() -> { text('done') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let indicator*:?processFlags(indicator*:$flags) = $flags;",
            "display done;")));
  }

  @Test
  void testDisplayTemplate_WithDateSequenceParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "date*:processDates(date*:dates) -> { $dates }",
            "TEMPLATES:",
            "let body01() -> { text('done') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let date*:?processDates(date*:$dates) = $dates;",
            "display done;")));
  }

  @Test
  void testDisplayTemplate_WithTimeSequenceParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "time*:processTimes(time*:times) -> { $times }",
            "TEMPLATES:",
            "let body01() -> { text('done') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let time*:?processTimes(time*:$times) = $times;",
            "display done;")));
  }

  @Test
  void testDisplayTemplate_WithDurationSequenceParameter() {
    assertEquals(
        lines(
            "GLOBALS:",
            "duration*:processDurations(duration*:durs) -> { $durs }",
            "TEMPLATES:",
            "let body01() -> { text('done') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let duration*:?processDurations(duration*:$durs) = $durs;",
            "display done;")));
  }

  // #endregion Sequence parameter declarations --------------------------------

  // #region templateLine ------------------------------------------------------

  @Test
  void testTemplateLine_NoIndentation() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('foo') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text} foo"));
  }

  /**
   * All nodes that contain any children get an auto generated outline number.
   */
  @Test
  void testTemplateLine_AutogeneratedOutline() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('Implicit 1 (shown)')",
            "for-each(../..).call(body0101()) }",
            "let body0101() -> { #1.1: text('Implicit 1.1 (shown)')",
            "for-each(PathNode/NumberField).call(body010101()) }",
            "let body010101() -> { text('Implicit 1.1.1 (hidden)') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "{BT-00-Text} Implicit 1 (shown)",
            "\t{ND-Root} Implicit 1.1 (shown)",
            "\t\t{BT-00-Number} Implicit 1.1.1 (hidden)")));
  }

  @Test
  @ResourceLock(Resources.LOCALE)
  void testTemplateLine_AutogeneratedOutline_AsciiDigitsWhateverTheDefaultLocale() {
    Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
    try {
      Locale.setDefault(Locale.Category.FORMAT, new Locale("ar", "EG"));
      assertEquals(
          lines(
              "TEMPLATES:",
              "let body01() -> { #1: text('Implicit 1 (shown)')",
              "for-each(../..).call(body0101()) }",
              "let body0101() -> { #1.1: text('Implicit 1.1 (shown)')",
              "for-each(PathNode/NumberField).call(body010101()) }",
              "let body010101() -> { text('Implicit 1.1.1 (hidden)') }",
              "MAIN:",
              "for-each(/*/PathNode/TextField).call(body01())"), //
          translateTemplate(lines(
              "{BT-00-Text} Implicit 1 (shown)",
              "\t{ND-Root} Implicit 1.1 (shown)",
              "\t\t{BT-00-Number} Implicit 1.1.1 (hidden)")));
    } finally {
      Locale.setDefault(Locale.Category.FORMAT, formatLocale);
    }
  }

  /**
   * The autogenerated number for a node can be overridden. Leaf nodes don't get
   * an outline number.
   */
  @Test
  void testTemplateLine_ExplicitOutline() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #2: text('foo')",
            "for-each(../..).call(body0101()) }",
            "let body0101() -> { #2.3: text('bar')",
            "for-each(PathNode/NumberField).call(body010101()) }",
            "let body010101() -> { text('foo') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "2 {BT-00-Text} foo",
            "\t3{ND-Root} bar",
            "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for some nodes can be overridden. The other nodes
   * get an auto
   * generated outline number. Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLine_MixedOutline() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #2: text('foo')",
            "for-each(../..).call(body0101()) }",
            "let body0101() -> { #2.1: text('bar')",
            "for-each(PathNode/NumberField).call(body010101()) }",
            "let body010101() -> { text('foo') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "2{BT-00-Text} foo",
            "\t{ND-Root} bar",
            "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value
   * zero. Leaf nodes
   * don't get an outline number.
   */
  @Test
  void testTemplateLine_SuppressedOutline() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #2: text('foo')",
            "for-each(../..).call(body0101()) }",
            "let body0101() -> { text('bar')",
            "for-each(PathNode/NumberField).call(body010101()) }",
            "let body010101() -> { text('foo') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "2{BT-00-Text} foo",
            "\t0{ND-Root} bar",
            "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value
   * zero. Child nodes
   * will still get an outline number. Leaf nodes still won't get an outline
   * number.
   */
  @Test
  void testTemplateLine_SuppressedOutlineAtParent() {
    // Outline is ignored if the line has no children
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('foo')",
            "for-each(../..).call(body0101()) }",
            "let body0101() -> { #1: text('bar')",
            "for-each(PathNode/NumberField).call(body010101()) }",
            "let body010101() -> { text('foo') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "0{BT-00-Text} foo",
            "\t{ND-Root} bar",
            "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLine_IndentationWithTabs() {
    assertEquals(
        lines("TEMPLATES:", //
            "let body01() -> { #1: text('foo')", "for-each(.).call(body0101()) }", //
            "let body0101() -> { text('bar') }", //
            "MAIN:", //
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "{BT-00-Text} foo",
            "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLine_IndentationWithSpaces() {
    assertEquals(
        lines("TEMPLATES:", //
            "let body01() -> { #1: text('foo')", "for-each(.).call(body0101()) }", //
            "let body0101() -> { text('bar') }", //
            "MAIN:", //
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "    {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLine_LowerIndentation() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('foo')", "for-each(.).call(body0101()) }",
            "let body0101() -> { text('bar') }",
            "let body02() -> { text('code') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())",
            "for-each(/*/PathNode/CodeField).call(body02())"),
        translateTemplate(lines(
            "{BT-00-Text} foo",
            "\t{BT-00-Text} bar",
            "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLine_LineJoining() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('foo')", "for-each(.).call(body0101()) }",
            "let body0101() -> { text('bar joined more') }",
            "let body02() -> { text('code') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())",
            "for-each(/*/PathNode/CodeField).call(body02())"),
        translateTemplate(lines(
            "{BT-00-Text} foo",
            "\t{BT-00-Text} bar \\ \n  joined \\\n\\\nmore",
            "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLine_VariableScope() {
    assertEquals(
        lines(
            "TEMPLATES:", //
            "let body01() -> { #1: eval(for $x in ./normalize-space(text()) return $x)", //
            "for-each(.).call(body0101()) }", //
            "let body0101() -> { eval(for $x in ./normalize-space(text()) return $x) }", //
            "MAIN:", //
            "for-each(/*/PathNode/TextField).call(body01())"), //
        translateTemplate(lines(
            "{BT-00-Text} ${for text:$x in BT-00-Text return $x}",
            "    {BT-00-Text} ${for text:$x in BT-00-Text return $x}")));
  }

  @Test
  void testTemplateLine_ContextVariable() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:xyz, string:ctx, string:t) -> { #1: eval(for $x in ./normalize-space(text()) return concat($x, $t))", //
            "for-each(.).call(body0101(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2='test'))", //
            "for-each(.).call(body0102(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2='test3')) }", //
            "let body0101(string:xyz, string:ctx, string:t, string:t2) -> { #1.1: eval(for $y in ./normalize-space(text()) return concat($y, $t, $t2))", //
            "for-each(.).call(body010101(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2=$t2))", //
            "for-each(.).call(body010102(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2=$t2)) }", //
            "let body010101(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t, $ctx/normalize-space(text()))) }", //
            "let body010102(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t, $ctx/normalize-space(text()))) }", //
            "let body0102(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t2, $ctx/normalize-space(text()))) }", //
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:xyz='a', string:ctx=., string:t=./normalize-space(text())))"), //
        translateTemplate(lines(
            "{text:$xyz='a', context:$ctx = BT-00-Text, text:$t = BT-00-Text} ${for text:$x in BT-00-Text return concat($x, $t)}",
            "    {BT-00-Text, text:$t2 = 'test'} ${for text:$y in BT-00-Text return concat($y, $t, $t2)}",
            "        {BT-00-Text} ${for text:$z in BT-00-Text return concat($z, $t, $ctx)}",
            "        {BT-00-Text} ${for text:$z in BT-00-Text return concat($z, $t, $ctx)}",
            "    {BT-00-Text, text:$t2 = 'test3'} ${for text:$z in BT-00-Text return concat($z, $t2, $ctx)}")));

  }

  @Test
  void testTemplateLine_ContextDeclarationShortcuts() {
    assertEquals(
        translateTemplate(lines(
            "{BT-00-Number} 1",
            "    {BT-00-Text} 2",
            "        {ND-Root} 3",
            "        {BT-00-Text} ${BT-00-Number}",
            "        {BT-00-Text} 5",
            "    {BT-00-Text} ${BT-00-Number}", //
            "    {BT-00-Number} ${BT-00-Text}",
            "    {ND-SubNode} N2",
            "        {ND-Root} N3",
            "        {ND-Root} N4",
            "        {ND-SubNode} ${BT-00-Number}")), //
        translateTemplate(lines(
            "{BT-00-Number} 1", //
            "    {BT-00-Text} 2", //
            "        {/} 3", //
            "        {..} ${BT-00-Number}", //
            "        {.} 5", //
            "    {.} ${BT-00-Number}", //
            "    {..} ${BT-00-Text}",
            "    {ND-SubNode} N2",
            "        {ND-Root} N3", //
            "        {.} N4", //
            "        {..} ${BT-00-Number}")));
  }

  @Test
  void testTemplateLine_SecondaryTemplate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))line-break()text('some text') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text} \\n some text"));
  }

  @Test
  void testTemplateLine_LineBreak() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))line-break() }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text} \\n"));
  }

  @Test
  void testTemplateLine_EndOfLineComments() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' blah blah') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} #{name|BT-00-Text} blah blah // comment blah blah"));
  }

  @Test
  void testTemplateLine_Indentation_DeepNesting() {
    assertEquals(
        lines(
            "TEMPLATES:", "let body01() -> { #1: text('Level 1')",
            "for-each(.).call(body0101()) }",
            "let body0101() -> { #1.1: text('Level 2')",
            "for-each(.).call(body010101()) }",
            "let body010101() -> { text('Level 3') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate(lines(
            "{BT-00-Text} Level 1",
            "  {BT-00-Text} Level 2",
            "    {BT-00-Text} Level 3")));
  }

  // #endregion templateLine ---------------------------------------------------


  // #region otherSections -----------------------------------------------------

  @Test
  void testOtherSections_SummarySection() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Summary: ')eval(./normalize-space(text())) }",
            "let summary01() -> { text('Summary: ')eval(./normalize-space(text())) }",
            "let summary02() -> { text('Summary: ')eval(./normalize-space(text())) }",
            "let summary03() -> { text('Summary: ')eval(./normalize-space(text())) }",
            "let nav01() -> { text('Summary: ')eval(./normalize-space(text())) }",
            "let nav02() -> { text('Summary: ')eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())",
            "SUMMARY:",
            "for-each(/*/PathNode/TextField).call(summary01())",
            "for-each(/*/PathNode/TextField).call(summary02())",
            "for-each(/*/PathNode/TextField).call(summary03())",
            "NAV:",
            "for-each(/*/PathNode/TextField).call(nav01())",
            "for-each(/*/PathNode/TextField).call(nav02())"),
        translateTemplate(lines(
            "{BT-00-Text} Summary: ${BT-00-Text}",
            "--- SUMMARY ---",
            "{BT-00-Text} Summary: ${BT-00-Text}",
            "{BT-00-Text} Summary: ${BT-00-Text}",
            "{BT-00-Text} Summary: ${BT-00-Text}",
            "--- NAVIGATION ---",
            "{BT-00-Text} Summary: ${BT-00-Text}",
            "{BT-00-Text} Summary: ${BT-00-Text}")));
  }

  // #endregion otherSections --------------------------------------------------

  // #region Labels ------------------------------------------------------------

  @Test
  void testLabelBlock_StandardLabelReference() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_StandardLabelReferenceWithPluraliser() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'), ../NumberField/number()) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text;${BT-00-Number}}"));
  }

  @Test
  void testLabelBlock_StandardLabelReferenceWithSequenceOfAssetIds() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ('BT-00-Text','BT-00-Number') return concat('field', '|', 'name', '|', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{field|name|${['BT-00-Text', 'BT-00-Number']}}"));
  }

  @Test
  void testLabelBlock_StandardLabelReferenceWithSequenceOfAssetIdsAndPluraliser() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ('BT-00-Text','BT-00-Number') return concat('field', '|', 'name', '|', $__item)), ../NumberField/number()) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{field|name|${['BT-00-Text', 'BT-00-Number']};${BT-00-Number}}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('auxiliary', '|', 'text', '|', 'value')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testLabelBlock_ComputedLabelReference() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(string-join(('field','|','name','|','BT-00-Text'), ', ')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{${string-join(['field', '|', 'name', '|', 'BT-00-Text'], ', ')}}"));
  }

  @Test
  void testLabelBlock_ShorthandBtLabelReference() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('business-term', '|', 'name', '|', 'BT-00')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testLabelBlock_ShorthandFieldLabelReference() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{name|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_ShorthandBtLabelReferenceMissingLabelType() {
    assertSyntaxError(() -> translateTemplate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForIndicator() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../IndicatorField return concat('indicator', '|', 'when', '-', $__item, '|', 'BT-00-Indicator'))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../CodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../InternalCodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCodeAttributeWithSameAttributeInContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField/@attribute).call(body01())"),
        translateTemplate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForText() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Text}"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_CODE_OR_INDICATOR,
        exception.getErrorCode());
  }

  @Test
  void testShorthandIndirectLabelReferenceForAttribute() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Attribute}"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_CODE_OR_INDICATOR,
        exception.getErrorCode());
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForUnit_IsPluralisedByItsNumber() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../DurationField/@unitCode return concat('code', '|', 'name', '|', 'timeperiod', '.', $__item)), ../DurationField/number()) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Duration-Unit}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCurrency_IsPluralisedByItsAmount() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../AmountField/@currencyID return concat('code', '|', 'name', '|', 'currency', '.', $__item)), ../AmountField/number()) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Amount-Currency}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForMeasureUnit_IsPluralisedByItsMeasure() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../MeasureField/@unitCode return concat('code', '|', 'name', '|', 'measurement-unit', '.', $__item)), ../MeasureField/number()) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Measure-Unit}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForUnit_WithPluraliser() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../DurationField/@unitCode return concat('code', '|', 'name', '|', 'timeperiod', '.', $__item)), ../NumberField/number()) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Duration-Unit;${BT-00-Number}}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForUnitOfRepeatableField_IsNotPluralised() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../RepeatableDurationField/@unitCode return concat('code', '|', 'name', '|', 'timeperiod', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Repeatable-Duration-Unit}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndIndicatorField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }",
            "MAIN:",
            "for-each(/*/PathNode/IndicatorField).call(body01())"),
        translateTemplate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndCodeField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndTextField() {
    assertSyntaxError(() -> translateTemplate("{BT-00-Text}  #{value}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithOtherLabelType() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithUnknownLabelType() {
    assertSyntaxError(() -> translateTemplate("{BT-00-Text}  #{whatever}"));
  }

  @Test
  void testLabelBlock_ShorthandLabelReferenceFromContext_WithNodeContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(concat('node', '|', 'name', '|', 'ND-Root')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root}  #{name}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceFromContextField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $__item))) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code} #value"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceFromContextField_WithUnitContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $__item in ../@unitCode return concat('code', '|', 'name', '|', 'timeperiod', '.', $__item)), ../number()) }",
            "MAIN:",
            "for-each(/*/PathNode/DurationField/@unitCode).call(body01())"),
        translateTemplate("{BT-00-Duration-Unit} #value"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{ND-Root} #value"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_FIELD_CONTEXT,
        exception.getErrorCode());
  }

  @Test
  void testLabelBlock_Expression_AssetId() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:assetId) -> { label(concat('field', '|', 'name', '|', $assetId)) }",
            "MAIN:",
            "for-each(/*).call(body01(string:assetId='BT-00-Text'))"),
        translateTemplate("{/, text:$assetId='BT-00-Text'}  #{field|name|${$assetId}}"));
  }

  @Test
  void testLabelBlock_Expression_LabelType() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:labelType) -> { label(concat('field', '|', $labelType, '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*).call(body01(string:labelType='name'))"),
        translateTemplate("{/, text:$labelType='name'}  #{field|${$labelType}|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_Expression_AssetType() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:assetType) -> { label(concat($assetType, '|', 'name', '|', 'BT-00-Text')) }",
            "MAIN:",
            "for-each(/*).call(body01(string:assetType='field'))"),
        translateTemplate("{/, text:$assetType='field'}  #{${$assetType}|name|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_Expression_NestedExpressions() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:assetType, string:labelType, string:assetId) -> { label(concat($assetType, '|', $labelType, '|', $assetId)) }",
            "MAIN:",
            "for-each(/*).call(body01(string:assetType='field', string:labelType='name', string:assetId='BT-00-Text'))"),
        translateTemplate(
            "{/, text:$assetType='field', text:$labelType='name', text:$assetId='BT-00-Text'}  #{${$assetType}|${$labelType}|${$assetId}}"));
  }

  /**
   * A label of several asset ids loops over them with a variable of its own, which the label type
   * reads inside the loop; the generated name cannot hide a variable of the template.
   */
  @Test
  void testLabelBlock_SeveralAssetIds_TemplateVariableNamedItem_IsNotHidden() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:item) -> { label(distinct-values(for $__item in ('BT-00-Text','BT-00-Number') return concat('field', '|', $item, '|', $__item))) }",
            "MAIN:",
            "for-each(/*).call(body01(string:item='name'))"),
        translateTemplate(
            "{/, text:$item='name'}  #{field|${$item}|${['BT-00-Text', 'BT-00-Number']}}"));
  }

  // #endregion Labels ---------------------------------------------------------

  // #region Expression block --------------------------------------------------

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code} $value"));
  }

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField_WithText() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('blah ')label(distinct-values(for $__item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $__item)))text(' blah ')eval(./normalize-space(text()))text(' blah') }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField_WithNodeContext() {
    InvalidUsageException exception = assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{ND-Root} $value"));
    assertEquals(InvalidUsageException.ErrorCode.SHORTHAND_REQUIRES_FIELD_CONTEXT,
        exception.getErrorCode());
  }

  // #endregion Expression block -----------------------------------------------

  // #region Formatting functions ------------------------------------------------

  @Test
  void testFormatShortDate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-date(xs:date('2026-02-15'), '[D01]/[M01]/[Y0001]')) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-short(date('2026-02-15'))}"));
  }

  @Test
  void testFormatShortDate_WithFieldReference() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-date(PathNode/StartDateField/xs:date(text()), '[D01]/[M01]/[Y0001]')) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-short(BT-00-StartDate)}"));
  }

  @Test
  void testFormatMediumDate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-date(xs:date('2026-02-15'), '[D01] [MNn,3-3] [Y0001]', 'en', (), ())) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-medium(date('2026-02-15'))}"));
  }

  @Test
  void testFormatLongDate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-date(xs:date('2026-02-15'), '[D01] [MNn] [Y0001]', 'en', (), ())) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-long(date('2026-02-15'))}"));
  }

  @Test
  void testFormatShortTime() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-time(xs:time('14:30:00Z'), '[H01]:[m01] [Z]')) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-short(time('14:30:00Z'))}"));
  }

  @Test
  void testFormatMediumTime() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-time(xs:time('14:30:00Z'), '[H01]:[m01]:[s01]')) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-medium(time('14:30:00Z'))}"));
  }

  @Test
  void testFormatLongTime() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-time(xs:time('14:30:00Z'), '[H01]:[m01]:[s01] [Z]')) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-long(time('14:30:00Z'))}"));
  }

  @Test
  void testFormatShortDateTime() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(concat(format-date(xs:date('2026-02-15'), '[D01]/[M01]/[Y0001]'), ' ', format-time(xs:time('14:30:00Z'), '[H01]:[m01] [Z]'))) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-short(date('2026-02-15'), time('14:30:00Z'))}"));
  }

  @Test
  void testFormatMediumDateTime() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(concat(format-date(xs:date('2026-02-15'), '[D01] [MNn,3-3] [Y0001]', 'en', (), ()), ' ', format-time(xs:time('14:30:00Z'), '[H01]:[m01]:[s01]'))) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-medium(date('2026-02-15'), time('14:30:00Z'))}"));
  }

  @Test
  void testFormatLongDateTime() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(concat(format-date(xs:date('2026-02-15'), '[D01] [MNn] [Y0001]', 'en', (), ()), ' ', format-time(xs:time('14:30:00Z'), '[H01]:[m01]:[s01] [Z]'))) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${format-long(date('2026-02-15'), time('14:30:00Z'))}"));
  }

  @Test
  void testFormatNumber_WithFieldReference() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(format-number(PathNode/NumberField/number(), '# ##0,00')) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} ${format-number(BT-00-Number, '#,##0.00')}"));
  }

  // #endregion Formatting functions ---------------------------------------------

  // #region Formatted expression blocks -------------------------------------------

  // Without options, a value is formatted according to its type.

  @Test
  void testFormattedExpressionBlock_Number_HasUpToNineDecimals() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/NumberField/number() return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number}"));
  }

  @Test
  void testFormattedExpressionBlock_Date_IsShort() {
    assertEquals(translateTemplate("{/} ${format-short(BT-00-StartDate)}"),
        translateTemplate("{/} ${BT-00-StartDate}"));
  }

  @Test
  void testFormattedExpressionBlock_Time_IsShort() {
    assertEquals(translateTemplate("{/} ${format-short(BT-00-StartTime)}"),
        translateTemplate("{/} ${BT-00-StartTime}"));
  }

  @Test
  void testFormattedExpressionBlock_Text_IsDisplayedAsItIs() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(PathNode/TextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Text}"));
  }

  @Test
  void testFormattedExpressionBlock_NumericExpression_HasUpToNineDecimals() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/NumberField/number() * 2 return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number * 2}"));
  }

  @Test
  void testFormattedExpressionBlock_Duration_DisplaysItsNumberAndUnit() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/DurationField/number() return format-number($__item, '# ##0,#########'))eval(for $code in PathNode/DurationField/@unitCode return ' ')label(for $code in PathNode/DurationField/@unitCode return concat('code', '|', 'name', '|', 'timeperiod.', $code), PathNode/DurationField/number()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Duration}"));
  }

  @Test
  void testFormattedExpressionBlock_CalculatedDuration_DisplaysMonthsOrDays() {
    final String duration = "xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text()))";
    final String months = "(years-from-duration(" + duration + ") * 12 + months-from-duration(" + duration + "))";
    final String number = "(if (" + months + " != 0) then " + months + " else days-from-duration(" + duration + "))";
    final String unitCode = "for $__item in " + duration + " return (if ((years-from-duration($__item) * 12"
        + " + months-from-duration($__item)) != 0) then 'MONTH' else 'DAY')";
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in " + number + " return format-number($__item, '# ##0,#########'))"
                + "eval(for $code in " + unitCode + " return ' ')"
                + "label(for $code in " + unitCode + " return concat('code', '|', 'name', '|', 'timeperiod.', $code), "
                + number + ") }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} ${BT-00-EndDate - BT-00-StartDate}"));
  }

  @Test
  void testFormattedExpressionBlock_ContextFieldValue() {
    assertEquals(translateTemplate("{BT-00-Number} ${BT-00-Number}"),
        translateTemplate("{BT-00-Number} $value"));
  }

  @Test
  void testFormattedExpressionBlock_ContextDurationFieldValue_DisplaysItsNumberAndUnit() {
    assertEquals(translateTemplate("{BT-00-Duration} ${BT-00-Duration}"),
        translateTemplate("{BT-00-Duration} $value"));
  }

  @Test
  void testFormattedExpressionBlock_AttributeField() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(PathNode/CodeField/@attribute) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-CodeAttribute}"));
  }

  @Test
  void testFormattedExpressionBlock_ContextAttributeFieldValue() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(../@attribute) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField/@attribute).call(body01())"),
        translateTemplate("{BT-00-CodeAttribute} $value"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormatting_AttributeField() {
    assertEquals(translateTemplate("{/} ${BT-00-CodeAttribute}"),
        translateTemplate("{/} ${BT-00-CodeAttribute|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_ContextFieldValue_WithNodeContext_IsError() {
    assertThrows(InvalidUsageException.class, () -> translateTemplate("{ND-Root} $value"));
  }

  @Test
  void testFormattedExpressionBlock_WithHyperlink() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { hyperlink(eval(for $__item in PathNode/NumberField/number() return format-number($__item, '# ##0,#########')), 'http://example.com') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number}@{'http://example.com'}"));
  }

  // Dates and times: the options give the same result as the format-short/medium/long functions.

  @Test
  void testFormattedExpressionBlock_DateShort() {
    assertEquals(translateTemplate("{/} ${format-short(BT-00-StartDate)}"),
        translateTemplate("{/} ${BT-00-StartDate|short}"));
  }

  @Test
  void testFormattedExpressionBlock_DateMedium() {
    assertEquals(translateTemplate("{/} ${format-medium(BT-00-StartDate)}"),
        translateTemplate("{/} ${BT-00-StartDate|medium}"));
  }

  @Test
  void testFormattedExpressionBlock_DateLong() {
    assertEquals(translateTemplate("{/} ${format-long(BT-00-StartDate)}"),
        translateTemplate("{/} ${BT-00-StartDate|long}"));
  }

  @Test
  void testFormattedExpressionBlock_TimeLong() {
    assertEquals(translateTemplate("{/} ${format-long(BT-00-StartTime)}"),
        translateTemplate("{/} ${BT-00-StartTime|long}"));
  }

  @Test
  void testFormattedExpressionBlock_WithSurroundingText() {
    assertEquals(translateTemplate("{/} Date: ${format-medium(BT-00-StartDate)} end"),
        translateTemplate("{/} Date: ${BT-00-StartDate|medium} end"));
  }

  @Test
  void testFormattedExpressionBlock_WithOptionsAndHyperlink() {
    assertEquals(translateTemplate("{/} ${format-medium(BT-00-StartDate)}@{'http://example.com'}"),
        translateTemplate("{/} ${BT-00-StartDate|medium}@{'http://example.com'}"));
  }

  // Numbers: the options give the same result as format-number.

  @Test
  void testFormattedExpressionBlock_NumberWithDecimals() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/NumberField/number() return format-number($__item, '# ##0,00')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number|2}"));
  }

  @Test
  void testFormattedExpressionBlock_NumberWithoutDecimals() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/NumberField/number() return format-number($__item, '# ##0')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number|0}"));
  }

  @Test
  void testFormattedExpressionBlock_NumberWithNineDecimals() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/NumberField/number() return format-number($__item, '# ##0,000000000')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number|9}"));
  }

  @Test
  void testFormattedExpressionBlock_MoreThanNineDecimals_IsSyntaxError() {
    assertSyntaxError(() -> translateTemplate("{/} ${BT-00-Number|10}"));
  }

  @Test
  void testFormattedExpressionBlock_WhitespaceAroundThePipe() {
    assertEquals(translateTemplate("{/} ${BT-00-Number|2}"),
        translateTemplate("{/} ${BT-00-Number | 2}"));
  }

  // Units.

  @Test
  void testFormattedExpressionBlock_Amount_DisplaysItsCurrency() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/AmountField/number() return format-number($__item, '# ##0,00'))eval(for $code in PathNode/AmountField/@currencyID return ' ')label(for $code in PathNode/AmountField/@currencyID return concat('code', '|', 'name', '|', 'currency.', $code), PathNode/AmountField/number()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Amount|2}"));
  }

  @Test
  void testFormattedExpressionBlock_AmountWithoutUnit() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/AmountField/number() return format-number($__item, '# ##0,00')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Amount|2 no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_DurationWithoutUnit_DisplaysItsNumber() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/DurationField/number() return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Duration|no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_NoUnitOnNumber_IsError() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.NO_UNIT_NOT_APPLICABLE,
        "line 1:4 Format option 'no-unit' cannot format a value of type 'number': it is for an amount, a measure or a duration.",
        "{/} ${BT-00-Number|2 no-unit}");
  }

  @Test
  void testFormattedExpressionBlock_NoUnitOnDate_IsError() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.NO_UNIT_NOT_APPLICABLE,
        "line 1:4 Format option 'no-unit' cannot format a value of type 'date': it is for an amount, a measure or a duration.",
        "{/} ${BT-00-StartDate|no-unit}");
  }

  @Test
  void testFormattedExpressionBlock_NoUnitAfterStyle_IsSyntaxError() {
    assertSyntaxError(() -> translateTemplate("{/} ${BT-00-StartDate|medium no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_StyleOnDurationField_IsError() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.STYLE_NOT_APPLICABLE,
        "line 1:4 Format option 'short' cannot format a value of type 'duration': it is for a date or a time.",
        "{/} ${BT-00-Duration|short}");
  }

  @Test
  void testFormattedExpressionBlock_StyleOnRepeatableDurationField_IsErrorOfTheStyle() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.STYLE_NOT_APPLICABLE,
        "line 1:4 Format option 'short' cannot format a value of type 'duration*': it is for a date or a time.",
        "{/} ${BT-00-Repeatable-Duration|short}");
  }

  @Test
  void testFormattedExpressionBlock_NoUnitOnText_IsError() {
    assertThrows(InvalidUsageException.class, () -> translateTemplate("{/} ${BT-00-Text|no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_UnitOfRepeatableField_IsError() {
    assertThrows(InvalidUsageException.class, () -> translateTemplate("{/} ${BT-00-Repeatable-Duration}"));
  }

  @Test
  void testFormattedExpressionBlock_RepeatableFieldWithoutUnit_FormatsEachValue() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/RepeatableDurationField/number() return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Repeatable-Duration|no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_CalculatedDurationsWithoutUnit_FormatsEachValue() {
    final String months = "(years-from-duration($__item) * 12 + months-from-duration($__item))";
    final String number = "(if (" + months + " != 0) then " + months + " else days-from-duration($__item))";
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P2M')) return format-number("
                + number + ", '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${[P1D, P2M]|no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_RepeatableDurationFieldWithPredicateWithoutUnit_FormatsEachValue() {
    final String durations = "(for $__F in PathNode/RepeatableDurationField[true()] return (if ($__F/@unitCode='WEEK')"
        + " then xs:dayTimeDuration(concat('P', $__F/number() * 7, 'D'))"
        + " else if ($__F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $__F/number(), 'D'))"
        + " else if ($__F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $__F/number(), 'Y'))"
        + " else if ($__F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $__F/number(), 'M')) else ()))";
    final String months = "(years-from-duration($__item) * 12 + months-from-duration($__item))";
    final String number = "(if (" + months + " != 0) then " + months + " else days-from-duration($__item))";
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in " + durations + " return format-number(" + number
                + ", '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Repeatable-Duration[TRUE]|no-unit}"));
  }

  @Test
  void testFormattedExpressionBlock_UnitOfCalculatedDurations_IsError() {
    assertThrows(InvalidUsageException.class, () -> translateTemplate("{/} ${[P1D, P2M]}"));
  }

  // No formatting: the value is displayed as it is, without its unit.

  @Test
  void testFormattedExpressionBlock_NoFormatting_Number() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(PathNode/NumberField/text()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormatting_Date() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(PathNode/StartDateField/text()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-StartDate|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormatting_AmountWithoutItsCurrency() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(PathNode/AmountField/text()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Amount|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormatting_Duration() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(PathNode/DurationField/text()) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Duration|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormatting_CalculatedDuration() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text()))) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} ${BT-00-EndDate - BT-00-StartDate|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormatting_Text() {
    assertEquals(translateTemplate("{/} ${BT-00-Text}"),
        translateTemplate("{/} ${BT-00-Text|no-formatting}"));
  }

  @Test
  void testFormattedExpressionBlock_NoFormattingWithOtherOptions_IsSyntaxError() {
    assertSyntaxError(() -> translateTemplate("{/} ${BT-00-Number|2 no-formatting}"));
  }

  // Expressions: formatted according to the type of their result.

  @Test
  void testFormattedExpressionBlock_NumericExpression() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/NumberField/number() * 2 return format-number($__item, '# ##0,00')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number * 2|2}"));
  }

  @Test
  void testFormattedExpressionBlock_DateExpression() {
    assertEquals(translateTemplate("{/} ${format-medium(date('2026-02-15'))}"),
        translateTemplate("{/} ${date('2026-02-15')|medium}"));
  }

  @Test
  void testFormattedExpressionBlock_TimeExpression() {
    assertEquals(translateTemplate("{/} ${format-long(time('14:30:00Z'))}"),
        translateTemplate("{/} ${time('14:30:00Z')|long}"));
  }

  @Test
  void testFormattedExpressionBlock_ExpressionWithHyperlink() {
    assertEquals(lines("TEMPLATES:",
            "let body01() -> { hyperlink(eval(for $__item in PathNode/NumberField/number() * 2 return format-number($__item, '# ##0,00')), 'http://example.com') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Number * 2|2}@{'http://example.com'}"));
  }

  @Test
  void testFormattedExpressionBlock_TextExpression_IsError() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.DECIMALS_NOT_APPLICABLE,
        "line 1:4 Format option '2' cannot format a value of type 'text': it is for a number or a duration.",
        "{/} ${'abc'|2}");
  }

  @Test
  void testFormattedExpressionBlock_DateStyleOnNumericExpression_IsError() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.STYLE_NOT_APPLICABLE,
        "line 1:4 Format option 'short' cannot format a value of type 'number': it is for a date or a time.",
        "{/} ${BT-00-Number * 2|short}");
  }

  // Sequences: each value is formatted.

  @Test
  void testFormattedExpressionBlock_RepeatableField_FormatsEachValue() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/RepeatableNumberField/number() return format-number($__item, '# ##0,00')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Repeatable-Number|2}"));
  }

  @Test
  void testFormattedExpressionBlock_RepeatableDate_FormatsEachValue() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/RepeatableDateField/xs:date(text()) return format-date($__item, '[D01] [MNn,3-3] [Y0001]', 'en', (), ())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Repeatable-Date|medium}"));
  }

  @Test
  void testFormattedExpressionBlock_RepeatableTime_FormatsEachValue() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/RepeatableTimeField/xs:time(text()) return format-time($__item, '[H01]:[m01]:[s01] [Z]')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Repeatable-Time|long}"));
  }

  @Test
  void testFormattedExpressionBlock_SequenceExpression_FormatsEachValue() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(for $__item in PathNode/RepeatableNumberField[./number() > 1]/number() return format-number($__item, '# ##0,00')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Repeatable-Number[BT-00-Repeatable-Number > 1]|2}"));
  }

  @Test
  void testFormattedExpressionBlock_TextSequence_IsError() {
    assertThrows(InvalidUsageException.class,
        () -> translateTemplate("{/} ${BT-00-Repeatable-Text|2}"));
  }

  @Test
  void testFormattedExpressionBlock_DecimalsOnDateField_IsError() {
    assertInvalidUsage(InvalidUsageException.ErrorCode.DECIMALS_NOT_APPLICABLE,
        "line 1:4 Format option '2' cannot format a value of type 'date': it is for a number or a duration.",
        "{/} ${BT-00-StartDate|2}");
  }

  @Test
  void testFormattedExpressionBlock_DateStyleOnNumberField_IsError() {
    assertThrows(RuntimeException.class, () -> translateTemplate("{/} ${BT-00-Number|short}"));
  }

  @Test
  void testFormattedExpressionBlock_DecimalsOnTextField_IsError() {
    assertThrows(RuntimeException.class, () -> translateTemplate("{/} ${BT-00-Text|2}"));
  }

  // Labels: the expressions in a label are not formatted, and take no options.

  @Test
  void testFormattedExpressionBlock_OptionsInLabel_IsSyntaxError() {
    assertSyntaxError(() -> translateTemplate("{/} #{${'field|name|BT-00-Text'|2}}"));
  }

  // #endregion Formatted expression blocks ----------------------------------------

  // #region Preferred language functions ----------------------------------------

  @Test
  void testPreferredLanguageFunction() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language(PathNode/TextMultilingualField)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language(BT-00-Text-Multilingual)}"));
  }

  @Test
  void testPreferredLanguageTextFunction() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text(PathNode/TextMultilingualField)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language-text(BT-00-Text-Multilingual)}"));
  }

  @Test
  void testPreferredLanguageFunction_WithPredicate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language(PathNode/TextMultilingualField['a' = 'a'])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language(BT-00-Text-Multilingual['a' == 'a'])}"));
  }

  @Test
  void testPreferredLanguageTextFunction_WithPredicate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text(PathNode/TextMultilingualField['a' = 'a'])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language-text(BT-00-Text-Multilingual['a' == 'a'])}"));
  }

  @Test
  void testPreferredLanguageFunction_RepeatableField() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language(SubNode/RepeatableInSubNode/TextMultilingual)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language(BT-13-TextMultilingual)}"));
  }

  @Test
  void testPreferredLanguageTextFunction_RepeatableField() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text(SubNode/RepeatableInSubNode/TextMultilingual)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language-text(BT-13-TextMultilingual)}"));
  }

  @Test
  void testPreferredLanguageFunction_RepeatableFieldWithIndexer() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language((SubNode/RepeatableInSubNode/TextMultilingual)[1])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language(BT-13-TextMultilingual[at 1])}"));
  }

  @Test
  void testPreferredLanguageTextFunction_RepeatableFieldWithIndexer() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text((SubNode/RepeatableInSubNode/TextMultilingual)[1])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language-text(BT-13-TextMultilingual[at 1])}"));
  }

  @Test
  void testPreferredLanguageTextFunction_RepeatableFieldWithPredicateAndIndexer() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text((SubNode/RepeatableInSubNode/TextMultilingual['a' = 'a'])[1])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${preferred-language-text(BT-13-TextMultilingual['a' == 'a'][at 1])}"));
  }

  @Test
  void testPreferredLanguageFunction_NonMultilingualField_Throws() {
    assertThrows(TypeMismatchException.class,
        () -> translateTemplate("{/} ${preferred-language(BT-00-Text)}"));
  }

  @Test
  void testPreferredLanguageTextFunction_NonMultilingualField_Throws() {
    assertThrows(TypeMismatchException.class,
        () -> translateTemplate("{/} ${preferred-language-text(BT-00-Text)}"));
  }

  // Property syntax: fieldContext:preferredLanguage / fieldContext:preferredLanguageText

  @Test
  void testPreferredLanguageProperty() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language(PathNode/TextMultilingualField)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Text-Multilingual:preferredLanguage}"));
  }

  @Test
  void testPreferredLanguageTextProperty() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text(PathNode/TextMultilingualField)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Text-Multilingual:preferredLanguageText}"));
  }

  @Test
  void testPreferredLanguageProperty_WithPredicate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language(PathNode/TextMultilingualField['a' = 'a'])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Text-Multilingual['a' == 'a']:preferredLanguage}"));
  }

  @Test
  void testPreferredLanguageTextProperty_WithPredicate() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text(PathNode/TextMultilingualField['a' = 'a'])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-00-Text-Multilingual['a' == 'a']:preferredLanguageText}"));
  }

  @Test
  void testPreferredLanguageProperty_RepeatableField() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language(SubNode/RepeatableInSubNode/TextMultilingual)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-13-TextMultilingual:preferredLanguage}"));
  }

  @Test
  void testPreferredLanguageTextProperty_RepeatableField() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text(SubNode/RepeatableInSubNode/TextMultilingual)) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-13-TextMultilingual:preferredLanguageText}"));
  }

  @Test
  void testPreferredLanguageProperty_RepeatableFieldWithIndexer() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language((SubNode/RepeatableInSubNode/TextMultilingual)[1])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-13-TextMultilingual[at 1]:preferredLanguage}"));
  }

  @Test
  void testPreferredLanguageTextProperty_RepeatableFieldWithIndexer() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text((SubNode/RepeatableInSubNode/TextMultilingual)[1])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-13-TextMultilingual[at 1]:preferredLanguageText}"));
  }

  @Test
  void testPreferredLanguageTextProperty_RepeatableFieldWithPredicateAndIndexer() {
    assertEquals(
        lines("TEMPLATES:",
            "let body01() -> { eval(efx:preferred-language-text((SubNode/RepeatableInSubNode/TextMultilingual['a' = 'a'])[1])) }",
            "MAIN:", "for-each(/*).call(body01())"),
        translateTemplate("{/} ${BT-13-TextMultilingual['a' == 'a'][at 1]:preferredLanguageText}"));
  }

  @Test
  void testPreferredLanguageProperty_NonMultilingualField_Throws() {
    assertThrows(TypeMismatchException.class,
        () -> translateTemplate("{/} ${BT-00-Text:preferredLanguage}"));
  }

  @Test
  void testPreferredLanguageTextProperty_NonMultilingualField_Throws() {
    assertThrows(TypeMismatchException.class,
        () -> translateTemplate("{/} ${BT-00-Text:preferredLanguageText}"));
  }

  // #endregion Preferred language functions -------------------------------------

  // #region contextDeclarationBlock -------------------------------------------

  @Test
  void testContextDeclarationBlock_ContextFieldVariable() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:ctx) -> { text('Context: ')eval($ctx/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:ctx=.))"),
        translateTemplate("{context:$ctx = BT-00-Text} Context: ${$ctx}"));
  }

  @Test
  void testContextDeclarationBlock_ContextVariableNameReusedOnALaterLine() {
    // The context variable of the first line is out of scope on the second, where the name is a
    // text variable
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:ctx) -> { text('Context: ')eval($ctx/normalize-space(text())) }",
            "let body02(string:ctx) -> { text('Text: ')eval($ctx) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:ctx=.))",
            "for-each(/*/PathNode/TextField).call(body02(string:ctx='a'))"),
        translateTemplate(lines("{context:$ctx = BT-00-Text} Context: ${$ctx}",
            "{BT-00-Text, text:$ctx = 'a'} Text: ${$ctx}")));
  }

  @Test
  void testContextDeclarationBlock_ContextNodeVariable() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(context:ctx) -> { text('Context: ')eval($ctx/PathNode/TextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01(context:ctx=.))"),
        translateTemplate("{context:$ctx = ND-Root} Context: ${$ctx::BT-00-Text}"));
  }

  @Test
  void testContextDeclarationBlock_MultipleVariables() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:var1, decimal:var2) -> { text('Variables: ')eval($var1)text(', ')eval(for $__item in $var2 return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:var1='hello', decimal:var2=42))"),
        translateTemplate("{text:$var1='hello', number:$var2=42, BT-00-Text} Variables: ${$var1}, ${$var2}"));
  }

  @Test
  void testContextDeclarationBlock_VariableBeforeContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:prefix) -> { text('Prefix: ')eval($prefix)text(' Value: ')eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:prefix='test'))"),
        translateTemplate("{text:$prefix='test', BT-00-Text} Prefix: ${$prefix} Value: ${BT-00-Text}"));
  }

  @Test
  void testContextDeclarationBlock_OnlyVariables() {
    assertSyntaxError(() -> translateTemplate(
        "{text:$var1='test', number:$var2=123, indicator:$var3=TRUE} Only vars: ${$var1}, ${$var2}, ${$var3}"));
  }

  /**
   * Context variables are always scalar even when the context field is repeatable,
   * because the template iterates over values and the variable holds each iteration's value.
   */
  @Test
  void testContextDeclarationBlock_RepeatableFieldContextVariable_UsedAsScalar() {
    String result = translateTemplate("{context:$ctx = BT-13-Number} Value plus one: ${$ctx + 1}");

    assertTrue(result.contains("decimal:ctx"),
        "Context variable should be scalar (decimal:ctx), not sequence. Actual: " + result);
    assertFalse(result.contains("decimal*:ctx"),
        "Context variable should NOT be typed as sequence. Actual: " + result);

    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(decimal:ctx) -> { text('Value plus one: ')eval(for $__item in $ctx/number() + 1 return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*/SubNode/RepeatableInSubNode/Number).call(body01(decimal:ctx=.))"),
        result);
  }

  @Test
  void testWithDisplay_RootContext_ForLoop() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(for $x in PathNode/RepeatableTextField/normalize-space(text()) return $x) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(
            "with ND-Root display ${for text:$x in BT-00-Repeatable-Text return $x};"));
  }

  @Test
  void testWithDisplay_RootContext_ForConcatenatedIterations() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(for $x in PathNode/RepeatableTextField/normalize-space(text()) return PathNode/RepeatableTextField[../TextField/normalize-space(text()) = $x]/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(
            "with ND-Root display ${for text:$x in BT-00-Repeatable-Text return BT-00-Repeatable-Text[BT-00-Text == $x]};"));
  }

  @Test
  void testWithDisplay_PredicateWithForLoop_VarInRepeatableField() {
    // Working variant: $var in repeatable-field inside a sub-predicate.
    // Analogous to: OPT-316-Contract[$tender in BT-3202-Contract]
    translateTemplate(
        "with ND-Root[count(for text:$x in BT-00-Repeatable-Text, text:$y in BT-00-Text[$x in BT-00-Repeatable-Text] return $y) > 0] display foo;");
  }

  @Test
  void testWithDisplay_PredicateWithForLoop_RepeatableFieldEqVar() {
    // Broken variant: repeatable-field == $var inside a sub-predicate.
    // Analogous to: OPT-316-Contract[BT-3202-Contract == $tender]
    // This should produce a meaningful error about scalar/multiple mismatch,
    // not a confusing syntax error like "expected {When, Display, Invoke}".
    assertThrows(TypeMismatchException.class, () -> translateTemplate(
        "with BT-00-Number[count(for text:$x in BT-00-Repeatable-Text, text:$y in BT-00-Text[BT-00-Repeatable-Text == $x] return $y) > 0] display foo;"));
  }

  // #endregion contextDeclarationBlock ----------------------------------------

  // #region chooseTemplate ----------------------------------------------------

  @Test
  void testChooseTemplate_WhenBlock_MultipleConditions() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let multi-when-template(string:status) -> { choose { when $status = 'active': text('Status: Active'), when $status = 'inactive': text('Status: Inactive'), when $status = 'pending': text('Status: Pending'), otherwise: text('Status: Unknown') } }",
            "let body01() -> { call(multi-when-template(string:status='active')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:multi-when-template(text:$status)",
            "when $status == 'active' display Status: Active",
            "when $status == 'inactive' display Status: Inactive",
            "when $status == 'pending' display Status: Pending",
            "otherwise display Status: Unknown;",
            "invoke multi-when-template('active');")));
  }

  @Test
  void testChooseTemplate_WhenBlock_ComplexBooleanExpressions() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let complex-when-template(decimal:value) -> { choose { when $value > 0 and $value < 100: text('In range'), when $value <= 0: text('Too low'), otherwise: text('Too high') } }",
            "let body01() -> { call(complex-when-template(decimal:value=50)) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:complex-when-template(number:$value)",
            "when $value > 0 and $value < 100 display In range",
            "when $value <= 0 display Too low",
            "otherwise display Too high;",
            "invoke complex-when-template(50);")));
  }

  @Test
  void testChooseTemplate_OtherwiseBlock_InvokeTemplate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let fallback-template(string:reason) -> { text('Fallback: ')eval($reason) }",
            "let otherwise-invoke-template(boolean:condition, string:reason) -> { choose { when $condition: text('Condition met'), otherwise: call(fallback-template(string:reason=$reason)) } }",
            "let body01() -> { call(otherwise-invoke-template(boolean:condition=false(), string:reason='default')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:fallback-template(text:$reason) display Fallback: ${$reason};",
            "let template:otherwise-invoke-template(indicator:$condition, text:$reason)",
            "when $condition display Condition met",
            "otherwise invoke fallback-template($reason);",
            "invoke otherwise-invoke-template(FALSE, 'default');")));
  }

  @Test
  void testChooseTemplate_WhenOtherwise() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let some-template(string:txt) -> { text('&#62;')eval($txt)text('&#60;') }",
            "let body01(string:t) -> { choose { when 1 > 2: text('foo'), when 2 < 3: text('bar'), when 3 > 3: call(some-template(string:txt='1')), otherwise: text('foo-bar') } }",
            "let body02(string:t) -> { choose { when 1 > 2: text('foo'), when 2 < 3: text('bar'), when 3 > 3: text('foo-bar'), otherwise: call(some-template(string:txt='2')) } }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:t='test'))",
            "for-each(/*/PathNode/TextField).call(body02(string:t='test'))"),
        translateTemplate(lines(
            "// test",
            "let template:some-template(text:$txt) display >${$txt}<;",
            "with BT-00-Text, text:$t='test'",
            "when 1 > 2 display foo",
            "when 2 < 3 display bar",
            "when 3 > 3 invoke some-template('1')",
            "otherwise display foo-bar;",
            "with BT-00-Text, text:$t='test'",
            "when 1 > 2 display foo",
            "when 2 < 3 display bar",
            "when 3 > 3 display foo-bar",
            "otherwise invoke some-template('2');")));
  }

  @Test
  void testChooseTemplate_WhenNoOtherwise() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:t='text'",
            "TEMPLATES:",
            "let body01() -> { choose { when true(): eval(./normalize-space(text()))text(' is a ')eval($t), otherwise nothing } }",
            "MAIN:",
            "for-each(/*/PathNode/TextField[true()]).call(body01())"),
        translateTemplate(lines(
            "let text:$t = 'text';",
            "with BT-00-Text[TRUE] when TRUE display ${BT-00-Text} is a ${$t};")));
  }

  @Test
  void testChooseTemplate_WhenNoOtherwiseNoContext() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string:t='test'",
            "TEMPLATES:",
            "let body01() -> { choose { when true(): text('this is a ')eval($t), otherwise nothing } }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text:$t = 'test';",
            "when TRUE display this is a ${$t};")));
  }

  // #endregion chooseTemplate -------------------------------------------------

  // #region variableList ------------------------------------------------------

  @Test
  void testVariableList_WithAllDataTypes() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:str, decimal:num, boolean:bool, date:dt, time:tm, duration:dur) -> { text('All types: ')eval($str)text(', ')eval(for $__item in $num return format-number($__item, '# ##0,#########'))text(', ')eval($bool)text(', ')eval(format-date($dt, '[D01]/[M01]/[Y0001]'))text(', ')eval(format-time($tm, '[H01]:[m01] [Z]'))text(', ')eval(for $__item in (if ((years-from-duration($dur) * 12 + months-from-duration($dur)) != 0) then (years-from-duration($dur) * 12 + months-from-duration($dur)) else days-from-duration($dur)) return format-number($__item, '# ##0,#########'))eval(for $code in for $__item in $dur return (if ((years-from-duration($__item) * 12 + months-from-duration($__item)) != 0) then 'MONTH' else 'DAY') return ' ')label(for $code in for $__item in $dur return (if ((years-from-duration($__item) * 12 + months-from-duration($__item)) != 0) then 'MONTH' else 'DAY') return concat('code', '|', 'name', '|', 'timeperiod.', $code), (if ((years-from-duration($dur) * 12 + months-from-duration($dur)) != 0) then (years-from-duration($dur) * 12 + months-from-duration($dur)) else days-from-duration($dur))) }",
            "MAIN:",
            "for-each(/*).call(body01(string:str='text', decimal:num=42, boolean:bool=true(), date:dt=xs:date('2023-01-01'), time:tm=xs:time('12:00:00'), duration:dur=xs:dayTimeDuration('P1D')))"),
        translateTemplate(
            "{/, text:$str='text', number:$num=42, indicator:$bool=TRUE, date:$dt=date('2023-01-01'), time:$tm=time('12:00:00'), duration:$dur=day-time-duration('P1D')} All types: ${$str}, ${$num}, ${$bool}, ${$dt}, ${$tm}, ${$dur}"));
  }

  @Test
  void testVariableList_ExpressionInitializers() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:computed) -> { text('Computed: ')eval($computed) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:computed=concat('prefix-', ./normalize-space(text()))))"),
        translateTemplate("{BT-00-Text, text:$computed=concat('prefix-', BT-00-Text)} Computed: ${$computed}"));
  }

  // #endregion variableList ---------------------------------------------------

  // #region templateLine edge cases -------------------------------------------

  @Test
  void testTemplateLine_OutlineNumber_Only() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('text') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("1 display text;"));
  }

  @Test
  void testTemplateLine_OutlineNumber_WithContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Value: ')eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("1 {BT-00-Text} Value: ${BT-00-Text}"));
  }

  // #endregion templateLine edge cases ----------------------------------------

  // #region InvalidIndentationException ---------------------------------------

  @Test
  void testTemplateLine_InvalidIndentation_FirstIndentation() {
    assertThrows(InvalidIndentationException.class, () -> translateTemplate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLine_InvalidIndentation_MixedIndentation() {
    InvalidIndentationException ex = assertThrows(InvalidIndentationException.class,
        () -> translateTemplate("{BT-00-Text} foo\n\t  {BT-00-Text} bar"));
    assertEquals(InvalidIndentationException.ErrorCode.MIXED_INDENTATION, ex.getErrorCode());
    assertTrue(ex.getMessage().startsWith("line "), "Error message should include source position");
  }

  @Test
  void testTemplateLine_InvalidIndentation_MixedSpaceThenTab() {
    assertThrows(InvalidIndentationException.class,
        () -> translateTemplate("{BT-00-Text} foo\n  \t{BT-00-Text} bar"));
  }

  @Test
  void testTemplateLine_InvalidIndentation_SkippedIndentation() {
    assertThrows(InvalidIndentationException.class,
        () -> translateTemplate("{BT-00-Text} foo\n\t\t{BT-00-Text} bar"));
  }

  @Test
  void testTemplateDeclaration_InvalidNestedDeclaration() {
    var template = lines(
        "let template:tem-plate1(text:$t) display --${$t}--;",
        "  let template:tem-plate2(text:$t) display --${$t}--;",
        "with context:$ctx1 = BT-00-Text, text:$t='++' invoke tem-plate2($t);");
    assertThrows(InvalidIndentationException.class, () -> translateTemplate(template));
  }

  @Test
  void testTemplateLine_InvalidIndentation_InconsistentSpacing() {
    var template = lines(
        "{BT-00-Text} Level 1",
        "\t\t{BT-00-Text} Level 2 with 2 spaces",
        "\t\t\t{BT-00-Text} Level 3 with 4 spaces");
    assertThrows(InvalidIndentationException.class, () -> translateTemplate(template));
  }

  @Test
  void testTemplateDeclaration_InvalidNestedStructure() {
    var template = lines(
        "let template:outer-template(text:$param) display Outer template;",
        "  let template:nested-template(text:$inner) display Nested template;",
        "");
    assertThrows(InvalidIndentationException.class, () -> translateTemplate(template));
  }

  @Test
  void testTemplateLine_InvalidIndentation_InconsistentSpaces() {
    InvalidIndentationException exception = assertThrows(InvalidIndentationException.class,
        () -> translateTemplate(
            lines("{BT-00-Text} foo", "  {BT-00-Text} bar", "   {BT-00-Text} baz")));
    assertEquals(InvalidIndentationException.ErrorCode.INCONSISTENT_INDENTATION_SPACES,
        exception.getErrorCode());
    assertEquals("line 3:0 Inconsistent indentation. Expected a multiple of 2 spaces.",
        exception.getMessage());
  }

  // #endregion InvalidIndentationException ------------------------------------

  // #region TypeMismatchException ---------------------------------------------

  @Test
  void testTemplateDefinition_InvalidParameters() {
    var template = lines(
        "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
        "invoke param-validation-template('test', 'test');");
    assertThrows(TypeMismatchException.class, () -> translateTemplate(template));
  }

  @Test
  void testDictionaryLookup_NumericKey_Throws() {
    var template = lines(
        "let $dic index BT-00-Number by BT-00-Text;",
        "display ${$dic[1]};");
    assertThrows(TypeMismatchException.class, () -> translateTemplate(template));
  }

  @Test
  void testDictionaryName_WithIndex_Throws() {
    // The name of a dictionary has no value that an index could select from
    var template = lines(
        "let $dic index BT-00-Number by BT-00-Text;",
        "display ${$dic[at 1]};");
    InvalidIdentifierException exception =
        assertThrows(InvalidIdentifierException.class, () -> translateTemplate(template));
    assertEquals(InvalidIdentifierException.ErrorCode.DICTIONARY_WITHOUT_KEY,
        exception.getErrorCode());
    assertEquals("line 2:10 Dictionary '$dic' has no value of its own: look up a key in it,"
        + " as in $dic['key'].", exception.getMessage());
  }

  @Test
  void testUndeclaredDictionary_NamesItAsWritten() {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate("display ${$dic['key']};"));
    assertEquals(InvalidIdentifierException.ErrorCode.UNDECLARED_IDENTIFIER,
        exception.getErrorCode());
    assertEquals("line 1:10 Dictionary '$dic' is not declared.", exception.getMessage());
  }

  @Test
  void testUndeclaredTemplate_NamesItAsWritten() {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate("invoke nothing('a');"));
    assertEquals("line 1:0 Template 'nothing' is not declared.", exception.getMessage());
  }

  @Test
  void testNameClash_FunctionThenVariable_NamesBoth() {
    assertNameClash("line 2:0 Variable '$x' has the same name as Function '?x', already declared"
        + " in this scope.", "let text:?x() = 'a';", "let text:$x = 'b';");
  }

  @Test
  void testNameClash_VariableThenFunction_NamesBoth() {
    assertNameClash("line 2:0 Function '?x' has the same name as Variable '$x', already declared"
        + " in this scope.", "let text:$x = 'b';", "let text:?x() = 'a';");
  }

  @Test
  void testNameClash_TwoVariables_NamesOne() {
    assertNameClash("line 2:0 Variable '$x' is already declared in this scope.",
        "let text:$x = 'b';", "let text:$x = 'c';");
  }

  @Test
  void testNameClash_ParameterThenIterator_NamesBoth() {
    assertNameClash("line 1:28 Variable '$p' has the same name as Parameter '$p', already"
        + " declared in this scope.",
        "let text*:?f(text:$p) = for text:$p in BT-00-Text return $p;");
  }

  @Test
  void testParameterReference_Sequence_NamesItAsWritten() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateTemplate("let text:?first(text*:$items) = upper-case($items);"));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SEQUENCE, exception.getErrorCode());
    assertEquals("line 1:43 Type mismatch. Function 'upper-case' cannot take 'text*'."
        + " Its argument must be 'text'. Parameter '$items' is declared as a sequence.",
        exception.getMessage());
  }

  @Test
  void testParameterReference_AsContext_NamesItAsWritten() {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate("let text:?f(text:$p) = $p::BT-00-Text;"));
    assertEquals(InvalidIdentifierException.ErrorCode.NOT_A_CONTEXT_VARIABLE,
        exception.getErrorCode());
    assertEquals("line 1:23 Parameter '$p' is not a context variable.", exception.getMessage());
  }

  @Test
  void testTemplateParameterReference_Sequence_NamesItAsWritten() {
    TypeMismatchException exception = assertThrows(TypeMismatchException.class,
        () -> translateTemplate("let template:t(text*:$items) display ${upper-case($items)};"));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SEQUENCE, exception.getErrorCode());
    assertEquals("line 1:50 Type mismatch. Function 'upper-case' cannot take 'text*'."
        + " Its argument must be 'text'. Parameter '$items' is declared as a sequence.",
        exception.getMessage());
  }

  private void assertNameClash(final String message, final String... template) {
    InvalidIdentifierException exception = assertThrows(InvalidIdentifierException.class,
        () -> translateTemplate(lines(template)));
    assertEquals(InvalidIdentifierException.ErrorCode.IDENTIFIER_ALREADY_DECLARED,
        exception.getErrorCode());
    assertEquals(message, exception.getMessage());
  }

  @Test
  void testDictionaryName_AsValue_Throws() {
    var template = lines(
        "let $dic index BT-00-Number by BT-00-Text;",
        "display ${$dic};");
    InvalidIdentifierException exception =
        assertThrows(InvalidIdentifierException.class, () -> translateTemplate(template));
    assertEquals(InvalidIdentifierException.ErrorCode.DICTIONARY_WITHOUT_KEY,
        exception.getErrorCode());
    assertEquals("line 2:10 Dictionary '$dic' has no value of its own: look up a key in it,"
        + " as in $dic['key'].", exception.getMessage());
  }

  @Test
  void testDictionaryName_AsContext_Throws() {
    var template = lines(
        "let $dic index BT-00-Number by BT-00-Text;",
        "display ${$dic::BT-00-Number};");
    InvalidIdentifierException exception =
        assertThrows(InvalidIdentifierException.class, () -> translateTemplate(template));
    assertEquals(InvalidIdentifierException.ErrorCode.DICTIONARY_WITHOUT_KEY,
        exception.getErrorCode());
  }

  @Test
  void testVariableDeclaration_ReservedName_Throws() {
    var template = lines(
        "let text:$__index = 'a';",
        "display ${$__index};");
    InvalidIdentifierException exception =
        assertThrows(InvalidIdentifierException.class, () -> translateTemplate(template));
    assertEquals(InvalidIdentifierException.ErrorCode.IDENTIFIER_IS_RESERVED,
        exception.getErrorCode());
  }

  @Test
  void testFunctionDeclaration_ReservedName_Throws() {
    var template = lines(
        "let number:?__twice(number:$n) = $n * 2;",
        "display ${?__twice(21)};");
    InvalidIdentifierException exception =
        assertThrows(InvalidIdentifierException.class, () -> translateTemplate(template));
    assertEquals(InvalidIdentifierException.ErrorCode.IDENTIFIER_IS_RESERVED,
        exception.getErrorCode());
  }

  @Test
  void testTemplateDeclaration_ReservedName_Throws() {
    var template = lines(
        "let template:__greeting() display Hello;",
        "invoke __greeting();");
    InvalidIdentifierException exception =
        assertThrows(InvalidIdentifierException.class, () -> translateTemplate(template));
    assertEquals(InvalidIdentifierException.ErrorCode.IDENTIFIER_IS_RESERVED,
        exception.getErrorCode());
  }

  @Test
  void testComputedExpressionBlock_Node_Throws() {
    assertThrows(TypeMismatchException.class,
        () -> translateTemplate("{ND-Root} ${ND-SubNode}"));
  }

  @Test
  void testTemplateInvocation_NodeArgument_Throws() {
    var template = lines(
        "let template:greeting(text:$name) display Hello ${$name};",
        "invoke greeting(ND-SubNode);");
    assertThrows(TypeMismatchException.class, () -> translateTemplate(template));
  }

  @Test
  void testChooseTemplate_NumericCondition_Throws() {
    assertThrows(TypeMismatchException.class,
        () -> translateTemplate("when 1 display this is one;"));
  }

  @Test
  void testVariableDeclaration_ValueOfAnotherType_Throws() {
    var template = lines(
        "let text:$x = 1;",
        "display ${$x};");
    assertThrows(TypeMismatchException.class, () -> translateTemplate(template));
  }

  @Test
  void testVariableDeclaration_IndexOnListReadingAField() {
    // No context is declared for a global variable: the field is read from the root, which the
    // target language would do for each value of the list, so the position is bound first
    var template = lines(
        "let number:$n = [1, 2][at BT-00-Number];",
        "display ${$n};");
    assertEquals(
        lines(
            "GLOBALS:",
            "decimal:n=(for $__index in /*/PathNode/NumberField/number() return (1,2)[$__index])",
            "TEMPLATES:",
            "let body01() -> { eval(for $__item in $n return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(template));
  }

  @Test
  void testVariable_IndexReadingAField() {
    var template = lines(
        "let text*:$list = ['a', 'b'];",
        "{ND-Root} ${$list[at BT-00-Number]}");
    assertEquals(
        lines(
            "GLOBALS:",
            "string*:list=('a','b')",
            "TEMPLATES:",
            "let body01() -> { eval((for $__index in PathNode/NumberField/number() return $list[$__index])) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(template));
  }

  @Test
  void testDictionaryLookup_InIndexOnList() {
    // A lookup starts from the root, which the target language would read for each value, so the
    // position is bound first
    var template = lines(
        "let $d index BT-00-Text by BT-00-Text;",
        "{ND-Root} ${[1, 2][at count($d['x'])]}");
    assertEquals(
        lines(
            "GLOBALS:",
            "let d index /*/PathNode/TextField by ./normalize-space(text());",
            "TEMPLATES:",
            "let body01() -> { eval(for $__item in (for $__index in count(key('d', 'x')) return (1,2)[$__index]) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(template));
  }

  // Each type error says what its construct was given, and what the operand at fault must be. One
  // test for each construct of a template pins its whole message.

  @Test
  void testTypeErrorMessage_ExpressionBlock() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Expression block cannot take 'node'."
            + " Its expression must be a value or a sequence of values.",
        "{ND-Root} ${ND-SubNode}");
  }

  @Test
  void testTypeErrorMessage_Variable() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:14 Type mismatch. Variable 'text:$x' cannot take 'number'."
            + " Its value must be 'text'.",
        lines("let text:$x = 1;", "{ND-Root} ${$x}"));
  }

  @Test
  void testTypeErrorMessage_FunctionDeclaration() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:16 Type mismatch. Function 'text:?f' cannot take 'number'."
            + " Its value must be 'text'.",
        lines("let text:?f() = 1;", "{ND-Root} ${?f()}"));
  }

  @Test
  void testTypeErrorMessage_When() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:5 Type mismatch. Clause 'when' cannot take 'number'."
            + " Its condition must be 'indicator'.",
        "when 1 display this is one;");
  }

  @Test
  void testTypeErrorMessage_Hyperlink() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:24 Type mismatch. Hyperlink cannot take 'number'. Its URL must be 'text'.",
        "DISPLAY here is a Link@{1};");
  }

  @Test
  void testTypeErrorMessage_LabelBlock() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:10 Type mismatch. Label block cannot take 'number'. Its asset id must be 'text'.",
        "{ND-Root} #{field|name|${1}}");
  }

  @Test
  void testTypeErrorMessage_FunctionInvocation() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let text:?greeting(text:$name) = concat('Hello ', $name);",
            "{ND-Root} ${?greeting(1)}")));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, ex.getErrorCode());
    assertEquals("line 2:12 Type mismatch. Function '?greeting' cannot take 'number'."
        + " Its argument must be 'text'.", ex.getMessage());
  }

  @Test
  void testTypeErrorMessage_TemplateInvocation() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let template:greeting(text:$name) display Hello ${$name};",
            "invoke greeting(ND-SubNode);")));
    assertEquals(TypeMismatchException.ErrorCode.CANNOT_CONVERT, ex.getErrorCode());
    assertEquals("line 2:0 Type mismatch. Template 'greeting' cannot take 'node'."
        + " Its argument must be 'text'.", ex.getMessage());
  }

  @Test
  void testTypeErrorMessage_PreferredLanguage_NotAField() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Function 'preferred-language' cannot take 'text'."
            + " Its argument must be a field reference.",
        "{ND-Root} ${preferred-language('a')}");
  }

  @Test
  void testTypeErrorMessage_PreferredLanguage_NotMultilingual() {
    assertTypeError(TypeMismatchException.ErrorCode.CANNOT_CONVERT,
        "line 1:12 Type mismatch. Function 'preferred-language' cannot take 'text'."
            + " Its argument must be a multilingual field.",
        "{ND-Root} ${preferred-language(BT-00-Text)}");
  }

  @Test
  void testPreferredLanguageFunction_FilteredField_AsThePostfixForm() {
    assertEquals(
        translateTemplate("{ND-Root} ${BT-00-Text-Multilingual[BT-00-Text == 'a']:preferredLanguage}"),
        translateTemplate("{ND-Root} ${preferred-language(BT-00-Text-Multilingual[BT-00-Text == 'a'])}"));
  }

  private void assertTypeError(final TypeMismatchException.ErrorCode code, final String message,
      final String template) {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(template));
    assertEquals(code, ex.getErrorCode());
    assertEquals(message, ex.getMessage());
  }

  private void assertInvalidUsage(final InvalidUsageException.ErrorCode code, final String message,
      final String template) {
    InvalidUsageException ex = assertThrows(InvalidUsageException.class,
        () -> translateTemplate(template));
    assertEquals(code, ex.getErrorCode());
    assertEquals(message, ex.getMessage());
  }

  // #endregion TypeMismatchException ------------------------------------------

  // #region Repeatable Fields in Expression Blocks ----------------------------

  @Test
  void testExpressionBlock_RepeatableFieldDirect() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Value: ')eval(PathNode/RepeatableTextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Value: ${BT-00-Repeatable-Text}"));
  }

  @Test
  void testExpressionBlock_RepeatableFieldWithForLoop() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Value: ')eval(for $x in PathNode/RepeatableTextField/normalize-space(text()) return $x) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Value: ${for text:$x in BT-00-Repeatable-Text return $x}"));
  }

  @Test
  void testExpressionBlock_ScalarField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Value: ')eval(PathNode/TextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Value: ${BT-00-Text}"));
  }

  @Test
  void testExpressionBlock_LiteralNumericSequence() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Values: ')eval(for $__item in (1,2,3) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Values: ${[1,2,3]}"));
  }

  @Test
  void testExpressionBlock_MixedSequenceWithField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Values: ')eval(for $__item in (1,2,PathNode/NumberField/number()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Values: ${[1,2,BT-00-Number]}"));
  }

  @Test
  void testExpressionBlock_ConditionalWithRepeatableField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Values: ')eval((if (PathNode/IndicatorField) then PathNode/RepeatableTextField/normalize-space(text()) else 'a')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(
            "{ND-Root} Values: ${if BT-00-Indicator then BT-00-Repeatable-Text else 'a'}"));
  }

  // #endregion Repeatable Fields in Expression Blocks -------------------------

  // #region Errors raised with no position ------------------------------------

  @Test
  void testUnknownAliasInExpressionBlock_IsPlacedAtTheAlias() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateTemplate(
            lines("{ND-Root} a", "{ND-Root} b", "{ND-Root} ${BT-NONEXISTENT}")));
    assertEquals("line 3:12 Unknown symbol 'BT-NONEXISTENT'.", ex.getMessage());
  }

  @Test
  void testUnknownNodeAsLineContext_IsPlacedAtTheContext() {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class,
        () -> translateTemplate(lines("{ND-Root} a", "{ND-NONEXISTENT} b")));
    assertEquals("line 2:1 Unknown symbol 'ND-NONEXISTENT'.", ex.getMessage());
  }

  // #endregion Errors raised with no position ---------------------------------

  // #region IllegalArgumentException ------------------------------------------

  @Test
  void testTemplateDefinition_InvalidTooManyParameters() {
    var template = lines(
        "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
        "invoke param-validation-template('test', 42, 43);");
    assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
  }

  @Test
  void testTemplateDefinition_InvalidTooFewParameters() {
    var template = lines(
        "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
        "invoke param-validation-template('test');");
    assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
  }

  @Test
  void testTemplateDefinition_InvalidNoArguments() {
    var template = lines(
        "let template:greeting(text:$name) display Hello ${$name};",
        "invoke greeting();");
    InvalidArgumentException ex =
        assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
    assertEquals(InvalidArgumentException.ErrorCode.ARGUMENT_NUMBER_MISMATCH, ex.getErrorCode());
    assertEquals(
        "line 2:0 Argument number mismatch. Template 'greeting' takes 1 argument but got 0.",
        ex.getMessage());
  }

  @Test
  @ResourceLock(Resources.LOCALE)
  void testTemplateDefinition_InvalidNoArguments_AsciiDigitsWhateverTheDefaultLocale() {
    var template = lines(
        "let template:greeting(text:$name) display Hello ${$name};",
        "invoke greeting();");
    Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
    try {
      Locale.setDefault(Locale.Category.FORMAT, new Locale("ar", "EG"));
      InvalidArgumentException ex =
          assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
      assertEquals(
          "line 2:0 Argument number mismatch. Template 'greeting' takes 1 argument but got 0.",
          ex.getMessage());
    } finally {
      Locale.setDefault(Locale.Category.FORMAT, formatLocale);
    }
  }

  @Test
  void testFunctionDeclaration_InvalidNoArguments() {
    var template = lines(
        "let text:?greeting(text:$name) = concat('Hello ', $name);",
        "display ${?greeting()};");
    InvalidArgumentException ex =
        assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
    assertEquals(InvalidArgumentException.ErrorCode.ARGUMENT_NUMBER_MISMATCH, ex.getErrorCode());
    assertEquals(
        "line 2:10 Argument number mismatch. Function '?greeting' takes 1 argument but got 0.",
        ex.getMessage());
  }

  @Test
  void testFunctionDeclaration_InvalidTooManyArguments_ReportsTheirNumber() {
    var template = lines(
        "let text:?greeting(text:$name) = concat('Hello ', $name);",
        "display ${?greeting('a', 'b', 'c')};");
    InvalidArgumentException ex =
        assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
    assertEquals(InvalidArgumentException.ErrorCode.ARGUMENT_NUMBER_MISMATCH, ex.getErrorCode());
    assertEquals(
        "line 2:10 Argument number mismatch. Function '?greeting' takes 1 argument but got 3.",
        ex.getMessage());
  }

  // #endregion IllegalArgumentException ---------------------------------------

  @Test
  void testContextualizer_WithPredicate() {
    assertEquals(
        lines(
            "TEMPLATES:", 
            "let body01() -> { #1: text('line1: ')eval(.[1 = 1]/normalize-space(text()))",
            "for-each(.[1 = 2]).call(body0101()) }",
            "let body0101() -> { #1.1: text('line2: ')eval(.[1 = 4]/normalize-space(text()))",
            "for-each(.[1 = 4]).call(body010101()) }",
            "let body010101() -> { text('line3: ')eval(.[1 = 5]/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/SubNode/SubSubNode/SubTextField[0 = 0]).call(body01())"),
        translateTemplate(lines(
            "{BT-01-SubSubNode-Text} line1: ${BT-01-SubSubNode-Text[1==1]}",
            "  {BT-01-SubSubNode-Text[1==2]} line2: ${BT-01-SubSubNode-Text[1==4]}",
            "    {BT-01-SubSubNode-Text[1==4]} line3: ${BT-01-SubSubNode-Text[1==5]}")));
  }

  @Test
  void testContextualizer_WithFieldInPredicate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('line1')",
            "for-each(SubTextField).call(body0101()) }",
            "let body0101() -> { text('line2') }",
            "MAIN:",
            "for-each(/*/SubNode[SubTextField]).call(body01())"),
        translateTemplate(lines(
            "{ND-SubNode[BT-01-SubNode-Text is present]} line1",
            "  {BT-01-SubNode-Text} line2")));
  }

  // #region Include directive ---------------------------------------------------

  @Test
  void testInclude_SingleFile_SameOutputAsInlined() throws Exception {
    String includedContent = "{BT-00-Code} Code: ${BT-00-Code}\n";

    String templateWithInclude = lines(
        "{BT-00-Text} Text: ${BT-00-Text}",
        "#include \"extra-lines.efx\"") + "\n";

    String templateInlined = lines(
        "{BT-00-Text} Text: ${BT-00-Text}",
        "{BT-00-Code} Code: ${BT-00-Code}") + "\n";

    IncludedFileResolver resolver = path -> {
      if ("extra-lines.efx".equals(path)) {
        return includedContent;
      }
      throw new java.io.IOException("Unknown include: " + path);
    };

    EfxTranslatorOptions options = new EfxTranslatorOptions(
        false, null, "udf", DecimalFormat.EFX_DEFAULT, resolver, Locale.ENGLISH);

    String withInclude = EfxTranslator.translateTemplate(
        DependencyFactoryMock.INSTANCE, getSdkVersion(), templateWithInclude, options);
    String inlined = translateTemplate(lines(
        "{BT-00-Text} Text: ${BT-00-Text}",
        "{BT-00-Code} Code: ${BT-00-Code}"));

    assertEquals(inlined, withInclude,
        "Template with #include should produce the same output as the equivalent inlined template");
  }

  @Test
  void testUnknownFieldInIncludedFile_IsPlacedInThatFile() {
    String includedContent = lines(
        "{BT-00-Code} Code: ${BT-00-Code}",
        "{BT-999-Unknown} Text: ${BT-00-Text}") + "\n";

    String templateWithInclude = lines(
        "{BT-00-Text} Text: ${BT-00-Text}",
        "#include \"extra-lines.efx\"",
        "{BT-00-Number} Number: ${BT-00-Number}") + "\n";

    IncludedFileResolver resolver = path -> {
      if ("extra-lines.efx".equals(path)) {
        return includedContent;
      }
      throw new java.io.IOException("Unknown include: " + path);
    };

    EfxTranslatorOptions options = new EfxTranslatorOptions(
        false, null, "udf", DecimalFormat.EFX_DEFAULT, resolver, Locale.ENGLISH);

    SymbolResolutionException exception = assertThrows(SymbolResolutionException.class,
        () -> EfxTranslator.translateTemplate(
            DependencyFactoryMock.INSTANCE, getSdkVersion(), templateWithInclude, options));
    assertEquals("line 2:1 in 'extra-lines.efx' Unknown symbol 'BT-999-Unknown'.",
        exception.getMessage());
  }

  // #endregion Include directive ------------------------------------------------

  // #region: fieldContext structural variations in template context declarations

  @Test
  void testContextDeclaration_WithFieldContextIndexer() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each((/*/PathNode/RepeatableTextField)[1]).call(body01())"),
        translateTemplate("{BT-00-Repeatable-Text[at 1]} ${BT-00-Repeatable-Text}"));
  }

  @Test
  void testContextDeclaration_WithFieldContextIndexReadingAField() {
    // No context is declared above the line, so the field is read from the root, as the target
    // language would read it for each element, and the position is bound first
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each((for $__index in /*/PathNode/NumberField/number() return (/*/PathNode/RepeatableTextField)[$__index])).call(body01())"),
        translateTemplate("{BT-00-Repeatable-Text[at BT-00-Number]} ${BT-00-Repeatable-Text}"));
  }

  @Test
  void testContextDeclaration_Nested_WithFieldContextIndexReadingAField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: eval(PathNode/TextField/normalize-space(text()))",
            "for-each((for $__index in PathNode/NumberField/number() return (PathNode/RepeatableTextField)[$__index])).call(body0101()) }",
            "let body0101() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "{ND-Root} ${BT-00-Text}",
            "  {BT-00-Repeatable-Text[at BT-00-Number]} ${BT-00-Repeatable-Text}")));
  }

  @Test
  void testContextDeclaration_WithFieldContextPredicate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField['a' = 'a']).call(body01())"),
        translateTemplate("{BT-00-Text['a' == 'a']} ${BT-00-Text}"));
  }

  @Test
  void testContextDeclaration_WithFieldContextPredicateAndIndexer() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each((/*/PathNode/RepeatableTextField['a' = 'a'])[1]).call(body01())"),
        translateTemplate("{BT-00-Repeatable-Text['a' == 'a'][at 1]} ${BT-00-Repeatable-Text}"));
  }

  @Test
  void testContextDeclaration_AbsoluteReferenceWithPredicate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField['a' = 'a']).call(body01())"),
        translateTemplate("{/BT-00-Text['a' == 'a']} ${BT-00-Text}"));
  }

  @Test
  void testContextDeclaration_AbsoluteReferenceWithIndexer() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each((/*/PathNode/RepeatableTextField)[1]).call(body01())"),
        translateTemplate("{/BT-00-Repeatable-Text[at 1]} ${BT-00-Repeatable-Text}"));
  }

  // #endregion: fieldContext structural variations in template context declarations

  // #region Context declarations of nested template lines ---------------------

  @Test
  void testContextDeclaration_NestedInRepeatingNode_NodeContextIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((NonRepeatableSubNode)[2]).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-NonRepeatableSubNode[at 2]} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_NodeContextIndexReadingAField() {
    // The position reads the field in each element of the line above
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((for $__index in count(TextField/normalize-space(text())) return (RepeatableInRepeatableNode)[$__index])).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at count(BT-00-Text-In-Repeatable-Node)]} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_FieldContextIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((TextField)[2]).call(body0101()) }",
            "let body0101() -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {BT-00-Text-In-Repeatable-Node[at 2]} ${BT-00-Text-In-Repeatable-Node}")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_FieldContextIndexReadingAField() {
    // The position reads the field in each element of the line above, where it is a single value
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((for $__index in NumberField/number() return (TextField)[$__index])).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {BT-00-Text-In-Repeatable-Node[at BT-00-Number-In-Repeatable-Node]} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_TwoLevels_IndexReadingAField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(NonRepeatableSubNode).call(body0101()) }",
            "let body0101() -> { #1.1: text('b')",
            "for-each((for $__index in count(TextField/normalize-space(text())) return (RepeatableSubSubNode)[$__index])).call(body010101()) }",
            "let body010101() -> { text('c') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-NonRepeatableSubNode} b",
            "    {ND-RepeatableSubSubNode[at count(BT-00-Text-In-NonRepeatableSubNode)]} c")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_TwoLevels_BothIndexed() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[1]).call(body0101()) }",
            "let body0101() -> { #1.1: text('b')",
            "for-each((../TextField)[2]).call(body010101()) }",
            "let body010101() -> { text('c') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at 1]} b",
            "    {BT-00-Text-In-Repeatable-Node[at 2]} c")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_WithClauseIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((NonRepeatableSubNode)[2]).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  WITH ND-NonRepeatableSubNode[at 2] DISPLAY b;")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_ContextVariableIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0101(context:c=.)) }",
            "let body0101(context:c) -> { eval($c/../TextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {context:$c = ND-RepeatableInRepeatableNode[at 2]} ${$c::BT-00-Text-In-Repeatable-Node}")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_ContextOverrideInIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((for $__index in count(../PathNode/RepeatableTextField/normalize-space(text())) return (RepeatableInRepeatableNode)[$__index])).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at count(ND-Root::BT-00-Repeatable-Text)]} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_ContextVariableOverrideInIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(context:r) -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[count($r/TextField/normalize-space(text()))]).call(body0101(context:r=$r)) }",
            "let body0101(context:r) -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01(context:r=.))"),
        translateTemplate(lines(
            "{context:$r = ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at count($r::BT-00-Text-In-Repeatable-Node)]} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_SameContextAfterIndexedLine() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0101())",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0102()) }",
            "let body0101() -> { text('b') }",
            "let body0102() -> { text('c') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at 2]} b",
            "  {.} c")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_AttributeFieldInIndexedLine() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((TextField)[2]).call(body0101()) }",
            "let body0101() -> { eval(./@attribute) }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {BT-00-Text-In-Repeatable-Node[at 2]} ${BT-00-Attribute-In-Repeatable-Node}")));
  }

  @Test
  void testContextDeclaration_NestedInFieldContext_NodeContextIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((../RepeatableInRepeatableNode)[2]).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode/TextField).call(body01())"),
        translateTemplate(lines(
            "{BT-00-Text-In-Repeatable-Node} a",
            "  {ND-RepeatableInRepeatableNode[at 2]} b")));
  }

  @Test
  void testContextDeclaration_NestedInFieldContext_NodeContextIndexReadingAField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((for $__index in ../NumberField/number() return (../RepeatableInRepeatableNode)[$__index])).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode/TextField).call(body01())"),
        translateTemplate(lines(
            "{BT-00-Text-In-Repeatable-Node} a",
            "  {ND-RepeatableInRepeatableNode[at BT-00-Number-In-Repeatable-Node]} b")));
  }

  @Test
  void testContextDeclaration_NestedInFieldContext_WithClauseIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((../RepeatableInRepeatableNode)[2]).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode/TextField).call(body01())"),
        translateTemplate(lines(
            "WITH BT-00-Text-In-Repeatable-Node DISPLAY a;",
            "  WITH ND-RepeatableInRepeatableNode[at 2] DISPLAY b;")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_LineAfterAnIndexedLineAndItsChild() {
    // The last line stands in the element of the first, as the indexed line does
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0101())",
            "for-each((TextField)[1]).call(body0102()) }",
            "let body0101() -> { #1.1: text('b')",
            "for-each(../TextField).call(body010101()) }",
            "let body010101() -> { text('c') }",
            "let body0102() -> { text('d') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at 2]} b",
            "    {BT-00-Text-In-Repeatable-Node} c",
            "  {BT-00-Text-In-Repeatable-Node[at 1]} d")));
  }


  @Test
  void testContextDeclaration_NestedInTemplateDeclaration_NodeContextIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let t() -> { text('x')",
            "for-each(/*/RepeatableNode).call(t01()) }",
            "let t01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[2]).call(t0101()) }",
            "let t0101() -> { text('b') }",
            "let body01() -> { text('c') }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let template:t() display x;",
            "  {ND-RepeatableNode} a",
            "    {ND-RepeatableInRepeatableNode[at 2]} b",
            "{ND-Root} c")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_SameContextAfterADedent() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0101())",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0102()) }",
            "let body0101() -> { #1.1: text('b')",
            "for-each(../TextField).call(body010101()) }",
            "let body010101() -> { text('c') }",
            "let body0102() -> { text('d') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[at 2]} b",
            "    {BT-00-Text-In-Repeatable-Node} c",
            "  {.} d")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_ContextVariableAfterParentContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((RepeatableInRepeatableNode)[2]).call(body0101(context:c=.)) }",
            "let body0101(context:c) -> { #1.1: text('b')",
            "for-each(.).call(body010101(context:c=$c)) }",
            "let body010101(context:c) -> { #1.1.1: text('c')",
            "for-each(.).call(body01010101(context:c=$c)) }",
            "let body01010101(context:c) -> { eval($c/../TextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {context:$c = ND-RepeatableInRepeatableNode[at 2]} b",
            "    {ND-RepeatableInRepeatableNode} c",
            "      {..} ${$c::BT-00-Text-In-Repeatable-Node}")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_ParentContextAfterALineThatLeavesIt() {
    // The line above left the element of the line named, so its field or node is selected again, without its brackets
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(RepeatableInRepeatableNode[../TextField/normalize-space(text()) = 'x']).call(body0101()) }",
            "let body0101() -> { #1.1: text('b')",
            "for-each(../TextField).call(body010101()) }",
            "let body010101() -> { #1.1.1: text('c')",
            "for-each(../RepeatableInRepeatableNode).call(body01010101()) }",
            "let body01010101() -> { text('d') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-RepeatableInRepeatableNode[BT-00-Text-In-Repeatable-Node == 'x']} b",
            "    {BT-00-Text-In-Repeatable-Node} c",
            "      {..} d")));
  }


  @Test
  void testContextDeclaration_NestedInRepeatingNode_Plain() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(NonRepeatableSubNode).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-NonRepeatableSubNode} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_Predicate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(NonRepeatableSubNode[TextField/normalize-space(text()) = 'x']).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-NonRepeatableSubNode[BT-00-Text-In-NonRepeatableSubNode == 'x']} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_SameContextUnderPredicatedLine() {
    // A line after the predicated line repeats its predicate; a line nested in it stands in its element
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(NonRepeatableSubNode[TextField/normalize-space(text()) = 'x']).call(body0101())",
            "for-each(NonRepeatableSubNode[TextField/normalize-space(text()) = 'x']).call(body0102()) }",
            "let body0101() -> { text('b') }",
            "let body0102() -> { #1.2: text('c')",
            "for-each(.).call(body010201()) }",
            "let body010201() -> { text('d') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-NonRepeatableSubNode[BT-00-Text-In-NonRepeatableSubNode == 'x']} b",
            "  {.} c",
            "    {.} d")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_ParentContextUnderPredicatedLine() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(NonRepeatableSubNode[TextField/normalize-space(text()) = 'x']).call(body0101()) }",
            "let body0101() -> { #1.1: text('b')",
            "for-each(RepeatableSubSubNode).call(body010101()) }",
            "let body010101() -> { #1.1.1: text('c')",
            "for-each(..).call(body01010101()) }",
            "let body01010101() -> { text('d') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {ND-NonRepeatableSubNode[BT-00-Text-In-NonRepeatableSubNode == 'x']} b",
            "    {ND-RepeatableSubSubNode} c",
            "      {..} d")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_AbsoluteReference() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(/*/SubNode).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {/ND-SubNode} b")));
  }

  @Test
  void testContextDeclaration_NestedInRepeatingNode_AbsoluteReferenceWithIndex() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each((/*/RepeatableNode)[2]).call(body0101()) }",
            "let body0101() -> { text('b') }",
            "MAIN:",
            "for-each(/*/RepeatableNode).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode} a",
            "  {/ND-RepeatableNode[at 2]} b")));
  }

  @Test
  void testContextDeclaration_AttributeFieldInIndexedLine() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { eval(./@attribute) }",
            "MAIN:",
            "for-each((/*/RepeatableNode/TextField)[2]).call(body01())"),
        translateTemplate("{BT-00-Text-In-Repeatable-Node[at 2]} ${BT-00-Attribute-In-Repeatable-Node}"));
  }

  @Test
  void testContextDeclaration_SameAndParentContextUnderPredicatedLine() {
    // Both lines stand in the element of the line above
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { #1: text('a')",
            "for-each(.).call(body0101())",
            "for-each(.).call(body0102()) }",
            "let body0101() -> { text('b') }",
            "let body0102() -> { text('c') }",
            "MAIN:",
            "for-each(/*/RepeatableNode[TextField/normalize-space(text()) = 'x']).call(body01())"),
        translateTemplate(lines(
            "{ND-RepeatableNode[BT-00-Text-In-Repeatable-Node == 'x']} a",
            "  {.} b",
            "  {..} c")));
  }

  // #endregion Context declarations of nested template lines ------------------
}