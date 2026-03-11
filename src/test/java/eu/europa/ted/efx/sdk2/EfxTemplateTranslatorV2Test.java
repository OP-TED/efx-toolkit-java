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
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIndentationException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;

class EfxTemplateTranslatorV2Test extends EfxTestsBase {
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  // #region Core Template Structure -------------------------------------------

  // #region templateDefinition ------------------------------------------------

  @Test
  void testTemplateDefinition_InvokeTemplate() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let some-template(string:content) -> { text('Content: ')eval($content) }",
            "let body02() -> { call(some-template(string:content='test')) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body03() -> { call(invoke-template(string:param='test')) }",
            "MAIN:",
            "for-each(/*).call(body03())"),
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
            "let body02() -> { call(conditional-template(boolean:condition=true(), string:value='test')) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body02() -> { call(param-validation-template(string:param1='test', decimal:param2=42)) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body02() -> { call(simple-template()) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body02() -> { call(greeting-template(string:name='World')) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
        translateTemplate(lines(
            "let template:greeting-template(text:$name) display Hello ${$name};",
            "invoke greeting-template('World');")));
  }

  @Test
  void testTemplateDeclaration_MultipleParameters() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let complex-template(string:first, decimal:second, boolean:third) -> { text('Values: ')eval($first)text(', ')eval($second)text(', ')eval($third) }",
            "let body02() -> { call(complex-template(string:first='test', decimal:second=42, boolean:third=true())) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
        translateTemplate(lines(
            "let template:complex-template(text:$first, number:$second, indicator:$third) display Values: ${$first}, ${$second}, ${$third};",
            "invoke complex-template('test', 42, TRUE);")));
  }

  @Test
  void testTemplateDeclaration_AllParameterTypes() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let all-types-template(string:str, decimal:num, boolean:bool, date:dt, time:tm, duration:dur) -> { text('Params: ')eval($str)text(', ')eval($num)text(', ')eval($bool)text(', ')eval(for $item in $dt return format-date($item, '[D01]/[M01]/[Y0001]'))text(', ')eval(for $item in $tm return format-time($item, '[H01]:[m01] [Z]'))text(', ')eval($dur) }",
            "let body02() -> { call(all-types-template(string:str='text', decimal:num=123, boolean:bool=true(), date:dt=xs:date('2023-01-01'), time:tm=xs:time('12:00:00'), duration:dur=xs:dayTimeDuration('P1D'))) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body02() -> { call(template-with-dashes(string:param='test')) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body01() -> { eval(./number())text(' + ')eval(./number())text(' = ')eval(./number() + ./number()) }",
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
            "let body02() -> { call(complex-template(string:field='BT-00-Text')) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body02(string:ctx2) -> { #2: eval(./normalize-space(text()))text(' lala')",
            "for-each(../NumberField).call(body0201(string:ctx2=$ctx2, decimal:ctx3=.))",
            "for-each(.).call(body0202(string:ctx2=$ctx2, string:ctx=., string:t2=udf:test($t3, 99)))",
            "for-each(.).call(body0203(string:ctx2=$ctx2, string:ctx4=., string:t5=udf:test($t3, 99))) }",
            "let body0201(string:ctx2, decimal:ctx3) -> { eval(../TextField/normalize-space(text())) }",
            "let body0202(string:ctx2, string:ctx, string:t2) -> { call(some-template(string:text1=$ctx, string:text2=$t2)) }",
            "let body0203(string:ctx2, string:ctx4, string:t5) -> { eval(./normalize-space(text())) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body02(string:ctx2=.))"),
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
            "let body02(string:ctx1, string:tx) -> { #1: call(some-template(string:t=$tx))",
            "for-each(../StartDateField).call(body0201(string:ctx1=$ctx1, string:tx=$tx)) }",
            "let body0201(string:ctx1, string:tx) -> { text('Nested content allowed') }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body02(string:ctx1=., string:tx='++'))"), //
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
            "let body01() -> { eval(key('dic', 'key')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "display ${$dic['key']};")));
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
            "let body01() -> { text('count: ')eval(count($items)) }",
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
            "let body01() -> { text('count: ')eval(count($nums)) }",
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
            "let body01() -> { text('count: ')eval(count($flags)) }",
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
            "let body01() -> { text('count: ')eval(count($dates)) }",
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
            "let body01() -> { text('count: ')eval(count($times)) }",
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
            "let body01() -> { text('count: ')eval(count($durs)) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let duration*:$durs = [P1Y, P2M];",
            "display count: ${count($durs)};")));
  }

  // #endregion Sequence variable declarations ---------------------------------

  // #region Sequence function declarations ------------------------------------

  @Test
  void testDisplayTemplate_WithTextSequenceFunction() {
    assertEquals(
        lines(
            "GLOBALS:",
            "string*:getItems() -> { ('a','b','c') }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(count(udf:getItems())) }",
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
            "let body01() -> { text('count: ')eval(count(udf:getNumbers())) }",
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
            "let body01() -> { text('count: ')eval(count(udf:getFlags())) }",
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
            "let body01() -> { text('count: ')eval(count(udf:getDates())) }",
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
            "let body01() -> { text('count: ')eval(count(udf:getTimes())) }",
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
            "let body01() -> { text('count: ')eval(count(udf:getDurations())) }",
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
            "let body010101(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t, $ctx)) }", //
            "let body010102(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t, $ctx)) }", //
            "let body0102(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t2, $ctx)) }", //
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
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-01}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForIndicator() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $item in ../IndicatorField return concat('indicator', '|', 'when', '-', $item, '|', 'BT-00-Indicator'))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $item in ../CodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $item in ../InternalCodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01())"),
        translateTemplate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCodeAttributeWithSameAttributeInContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField/@attribute).call(body01())"),
        translateTemplate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { label(distinct-values(for $item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForText() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Text}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForAttribute() {
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{BT-00-Attribute}"));
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
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{value}"));
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
    assertThrows(ParseCancellationException.class,
        () -> translateTemplate("{BT-00-Text}  #{whatever}"));
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
            "let body01() -> { label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code} #value"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("{ND-Root} #value"));
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
            "let body01() -> { text('blah ')label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item)))text(' blah ')eval(./normalize-space(text()))text(' blah') }",
            "MAIN:",
            "for-each(/*/PathNode/CodeField).call(body01())"),
        translateTemplate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("{ND-Root} $value"));
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

  // #endregion Preferred language functions -------------------------------------

  // #region contextDeclarationBlock -------------------------------------------

  @Test
  void testContextDeclarationBlock_ContextFieldVariable() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01(string:ctx) -> { text('Context: ')eval($ctx) }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body01(string:ctx=.))"),
        translateTemplate("{context:$ctx = BT-00-Text} Context: ${$ctx}"));
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
            "let body01(string:var1, decimal:var2) -> { text('Variables: ')eval($var1)text(', ')eval($var2) }",
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
    assertThrows(ParseCancellationException.class, () -> translateTemplate(
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
            "let body01(decimal:ctx) -> { text('Value plus one: ')eval($ctx + 1) }",
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
            "let body02() -> { call(multi-when-template(string:status='active')) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body02() -> { call(complex-when-template(decimal:value=50)) }",
            "MAIN:",
            "for-each(/*).call(body02())"),
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
            "let body03() -> { call(otherwise-invoke-template(boolean:condition=false(), string:reason='default')) }",
            "MAIN:",
            "for-each(/*).call(body03())"),
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
            "let body02(string:t) -> { choose { when 1 > 2: text('foo'), when 2 < 3: text('bar'), when 3 > 3: call(some-template(string:txt='1')), otherwise: text('foo-bar') } }",
            "let body03(string:t) -> { choose { when 1 > 2: text('foo'), when 2 < 3: text('bar'), when 3 > 3: text('foo-bar'), otherwise: call(some-template(string:txt='2')) } }",
            "MAIN:",
            "for-each(/*/PathNode/TextField).call(body02(string:t='test'))",
            "for-each(/*/PathNode/TextField).call(body03(string:t='test'))"),
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
            "let body01(string:str, decimal:num, boolean:bool, date:dt, time:tm, duration:dur) -> { text('All types: ')eval($str)text(', ')eval($num)text(', ')eval($bool)text(', ')eval(for $item in $dt return format-date($item, '[D01]/[M01]/[Y0001]'))text(', ')eval(for $item in $tm return format-time($item, '[H01]:[m01] [Z]'))text(', ')eval($dur) }",
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

  // #endregion InvalidIndentationException ------------------------------------

  // #region TypeMismatchException ---------------------------------------------

  @Test
  void testTemplateDefinition_InvalidParameters() {
    var template = lines(
        "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
        "invoke param-validation-template('test', 'test');");
    assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
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
  void testExpressionBlock_RepeatableFieldWithExplicitCast() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Value: ')eval(PathNode/RepeatableTextField/normalize-space(text())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Value: ${(text*)BT-00-Repeatable-Text}"));
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
            "let body01() -> { text('Values: ')eval((1,2,3)) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Values: ${[1,2,3]}"));
  }

  @Test
  void testExpressionBlock_MixedSequenceWithField() {
    assertEquals(
        lines(
            "TEMPLATES:",
            "let body01() -> { text('Values: ')eval((1,2,PathNode/NumberField/number())) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate("{ND-Root} Values: ${[1,2,BT-00-Number]}"));
  }

  // #endregion Repeatable Fields in Expression Blocks -------------------------

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

  // #endregion Include directive ------------------------------------------------
}