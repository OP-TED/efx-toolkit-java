package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIndentationException;

class EfxTemplateTranslatorV2Test extends EfxTestsBase {
  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  // #region Core Template Structure ------------------------------------------

  // #region templateDefinition -----------------------------------------------

  @Test
  void testTemplateDefinition_InvokeTemplate() {
    assertEquals(
        lines(
            "let some-template(string:content) -> { text('Content: ')eval($content) }",
            "let block02() -> { call(some-template(string:content='test')) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:some-template(text:$content) display Content: ${$content};",
            "invoke some-template('test');")));
  }

  @Test
  void testTemplateDefinition_NestedInvokeTemplate() {
    assertEquals(
        lines(
            "let other-template(string:value) -> { text('Value: ')eval($value) }",
            "let invoke-template(string:param) -> { call(other-template(string:value=$param)) }",
            "let block03() -> { call(invoke-template(string:param='test')) }",
            "for-each(/*).call(block03())"),
        translateTemplate(lines(
            "let template:other-template(text:$value) display Value: ${$value};",
            "let template:invoke-template(text:$param) invoke other-template($param);",
            "invoke invoke-template('test');")));
  }

  @Test
  void testTemplateDefinition_ChooseTemplate() {
    assertEquals(
        lines(
            "let conditional-template(boolean:condition, string:value) -> { choose { when $condition: text('Condition met: ')eval($value), when $condition = false(): text('Condition not met'), otherwise: text('Unknown condition: ')eval($condition) } }",
            "let block02() -> { call(conditional-template(boolean:condition=true(), string:value='test')) }",
            "for-each(/*).call(block02())"),
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
            "let param-validation-template(string:param1, decimal:param2) -> { text('Valid parameters') }",
            "let block02() -> { call(param-validation-template(string:param1='test', decimal:param2=42)) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
            "invoke param-validation-template('test', 42);")));
  }

  // #endregion templateDefinition --------------------------------------------

  // #region templateDeclaration ----------------------------------------------

  @Test
  void testTemplateDeclaration_NoParameters() {
    assertEquals(
        lines(
            "let simple-template() -> { text('Hello World') }",
            "let block02() -> { call(simple-template()) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:simple-template() display Hello World;",
            "invoke simple-template();")));
  }

  @Test
  void testTemplateDeclaration_SingleParameter() {
    assertEquals(
        lines(
            "let greeting-template(string:name) -> { text('Hello ')eval($name) }",
            "let block02() -> { call(greeting-template(string:name='World')) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:greeting-template(text:$name) display Hello ${$name};",
            "invoke greeting-template('World');")));
  }

  @Test
  void testTemplateDeclaration_MultipleParameters() {
    assertEquals(
        lines(
            "let complex-template(string:first, decimal:second, boolean:third) -> { text('Values: ')eval($first)text(', ')eval($second)text(', ')eval($third) }",
            "let block02() -> { call(complex-template(string:first='test', decimal:second=42, boolean:third=true())) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:complex-template(text:$first, number:$second, indicator:$third) display Values: ${$first}, ${$second}, ${$third};",
            "invoke complex-template('test', 42, TRUE);")));
  }

  @Test
  void testTemplateDeclaration_AllParameterTypes() {
    assertEquals(
        lines(
            "let all-types-template(string:str, decimal:num, boolean:bool, date:dt, time:tm, duration:dur) -> { text('Params: ')eval($str)text(', ')eval($num)text(', ')eval($bool)text(', ')eval(for $item in $dt return format-date($item, '[D01]/[M01]/[Y0001]'))text(', ')eval(for $item in $tm return format-time($item, '[H01]:[m01] [Z]'))text(', ')eval($dur) }",
            "let block02() -> { call(all-types-template(string:str='text', decimal:num=123, boolean:bool=true(), date:dt=xs:date('2023-01-01'), time:tm=xs:time('12:00:00'), duration:dur=xs:dayTimeDuration('P1D'))) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:all-types-template(text:$str, number:$num, indicator:$bool, date:$dt, time:$tm, measure:$dur) display Params: ${$str}, ${$num}, ${$bool}, ${$dt}, ${$tm}, ${$dur};",
            "invoke all-types-template('text', 123, TRUE, date('2023-01-01'), time('12:00:00'), day-time-duration('P1D'));")));
  }

  @Test
  void testTemplateDeclaration_SpecialCharactersInName() {
    assertEquals(
        lines(
            "let template-with-dashes(string:param) -> { text('Template: ')eval($param) }",
            "let block02() -> { call(template-with-dashes(string:param='test')) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:template-with-dashes(text:$param) display Template: ${$param};",
            "invoke template-with-dashes('test');")));
  }

  // #endregion templateDeclaration -------------------------------------------

  // #region templateFragment -------------------------------------------------

  @Test
  void testTemplateFragment_TextAndExpression() {
    assertEquals(
        "let block01() -> { text('Value is: ')eval(./normalize-space(text())) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} Value is: ${BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_TextAndLabel() {
    assertEquals(
        "let block01() -> { text('Field: ')label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} Field: #{field|name|BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_MixedContent() {
    assertEquals(
        "let block01() -> { text('Label: ')label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' Value: ')eval(./normalize-space(text()))text(' End') }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} Label: #{field|name|BT-00-Text} Value: ${BT-00-Text} End"));
  }

  @Test
  void testTemplateFragment_OnlyExpression() {
    assertEquals(
        "let block01() -> { eval(./normalize-space(text())) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} ${BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_OnlyLabel() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} #{field|name|BT-00-Text}"));
  }

  @Test
  void testTemplateFragment_MultipleExpressions() {
    assertEquals(
        "let block01() -> { eval(./number())text(' + ')eval(./number())text(' = ')eval(./number() + ./number()) }\nfor-each(/*/PathNode/NumberField).call(block01())",
        translateTemplate("{BT-00-Number} ${BT-00-Number} + ${BT-00-Number} = ${BT-00-Number + BT-00-Number}"));
  }

  // #region textBlock -------------------------------------------------------

  @Test
  void testTextBlock_SimpleText() {
    assertEquals(
        "let block01() -> { text('Simple text content') }\nfor-each(/*).call(block01())",
        translateTemplate("display Simple text content;"));
  }

  @Test
  void testTextBlock_WithWhitespace() {
    assertEquals(
        "let block01() -> { text('Text with   spaces') }\nfor-each(/*).call(block01())",
        translateTemplate("display   Text with   spaces  ;"));
  }

  @Test
  void testTextBlock_WithSpecialCharacters() {
    assertEquals(
        "let block01() -> { text('Text with special chars: &#60;&#62;&#38;&#34;&#39;') }\nfor-each(/*).call(block01())",
        translateTemplate("display Text with special chars: <>&#38;\"';"));
  }

  @Test
  void testTextBlock_MultipleTextBlocks() {
    assertEquals(
        "let block01() -> { text('First block ')eval('')text(' Second block') }\nfor-each(/*).call(block01())",
        translateTemplate("display First block ${''} Second block;"));
  }

  @Test
  void testTextBlock_WithNewlines() {
    assertEquals(
        "let block01() -> { text('Line 1')line-break()text('Line 2')line-break()text('Line 3') }\nfor-each(/*).call(block01())",
        translateTemplate("display Line 1 \\nLine 2\\n  Line 3;"));
  }

  @Test
  void testTextBlock_WithEscapedCharacters() {
    assertEquals(
        "let block01() -> { text('Text with quotes: &#34;&#39; and backslash: \\\\') }\nfor-each(/*).call(block01())",
        translateTemplate("display Text with quotes: \"' and backslash: \\\\;"));
  }

  // #endregion textBlock -----------------------------------------------------

  // #endregion templateFragment ----------------------------------------------

  @Test
  void testTemplate_ComplexNesting() {
    assertEquals(
        lines(
            "let complex-template(string:field) -> { text('name ')eval($field)text(' label ')label(concat('field', '|', 'name', '|', $field)) }",
            "let block02() -> { call(complex-template(string:field='BT-00-Text')) }",
            "for-each(/*).call(block02())"),
        translateTemplate(lines(
            "let template:complex-template(text:$field) display name ${$field} label #{field|name|${$field}};",
            "invoke complex-template('BT-00-Text');")));
  }

  // #endregion Core Template Structure ---------------------------------------

  // #region Globals ----------------------------------------------------------

  @Test
  void testGlobals_VariableDeclaration() {
    assertEquals(
        lines(
            "string:t3='a'",
            "decimal:n1=12",
            "string:t1=$t3",
            "string:test(string:p1, decimal:p2) -> { concat($p1, $t1) }",
            "decimal:n2=$n1 + 1",
            "string:t4=udf:test($t3, 22)",
            "let block01(string:t2) -> { eval(PathNode/TextField/normalize-space(text())) }",
            "for-each(/*).call(block01(string:t2=udf:test($t3, 99)))"), //
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
            "string:t3='a'",
            "decimal:n1=12",
            "string:t1=$t3",
            "string:test(string:p1, decimal:p2) -> { concat($p1, $t1) }",
            "decimal:n2=$n1 + 1",
            "string:t4=udf:test($t3, 22)",
            "let some-template(string:text1, string:text2) -> { eval(/*/PathNode/TextField/normalize-space(text()))text(' dokimi&#59; ')eval($text2)text(' &#59;')",
            "for-each(/*/PathNode/NumberField).call(some-template01(string:text1=$text1, string:text2=$text2)) }",
            "let some-template01(string:text1, string:text2) -> { eval(../TextField/normalize-space(text())) }",
            "let block02(string:ctx2) -> { #2: eval(./normalize-space(text()))text(' lala')",
            "for-each(../NumberField).call(block0201(string:ctx2=$ctx2, decimal:ctx3=.))",
            "for-each(.).call(block0202(string:ctx2=$ctx2, string:ctx=., string:t2=udf:test($t3, 99)))",
            "for-each(.).call(block0203(string:ctx2=$ctx2, string:ctx4=., string:t5=udf:test($t3, 99))) }",
            "let block0201(string:ctx2, decimal:ctx3) -> { eval(../TextField/normalize-space(text())) }",
            "let block0202(string:ctx2, string:ctx, string:t2) -> { call(some-template(string:text1=$ctx, string:text2=$t2)) }",
            "let block0203(string:ctx2, string:ctx4, string:t5) -> { eval(./normalize-space(text())) }",
            "for-each(/*/PathNode/TextField).call(block02(string:ctx2=.))"),
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
            "let some-template(string:t) -> { text('--')eval($t)text('--') }",
            "let block02(string:ctx1, string:tx) -> { #1: call(some-template(string:t=$tx))",
            "for-each(../StartDateField).call(block0201(string:ctx1=$ctx1, string:tx=$tx)) }",
            "let block0201(string:ctx1, string:tx) -> { text('Nested content allowed') }",
            "for-each(/*/PathNode/TextField).call(block02(string:ctx1=., string:tx='++'))"), //
        translateTemplate(lines(
            "let template:some-template(text:$t) display --${$t}--;",
            "with context:$ctx1 = BT-00-Text, text:$tx='++' invoke some-template($tx);",
            "  {BT-00-StartDate} Nested content allowed")));
  }

  @Test
  void testGlobals_NoTemplateLines() {
    assertEquals(
        lines(
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
            "let dic index /*/PathNode/NumberField by /*/PathNode/TextField/normalize-space(text());",
            "let block01() -> { eval(key('dic', 'key')) }",
            "for-each(/*).call(block01())"),
        translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "display ${$dic['key']};")));
  }

  // #endregion Globals -------------------------------------------------------

  @Test
  void testDisplayTemplate() {
    assertEquals(
        lines("string:t='test'",
            "let block01() -> { text('this is a ')eval($t) }",
            "for-each(/*).call(block01())"),
        translateTemplate(lines(
            "let text:$t = 'test';",
            "display this is a ${$t};")));
  }

  // #region templateLine -----------------------------------------------------

  @Test
  void testTemplateLine_NoIndentation() {
    assertEquals(
        "let block01() -> { text('foo') }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text} foo"));
  }

  /**
   * All nodes that contain any children get an auto generated outline number.
   */
  @Test
  void testTemplateLine_AutogeneratedOutline() {
    assertEquals(lines("let block01() -> { #1: text('Implicit 1 (shown)')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #1.1: text('Implicit 1.1 (shown)')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('Implicit 1.1.1 (hidden)') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} Implicit 1 (shown)", "\t{ND-Root} Implicit 1.1 (shown)", "\t\t{BT-00-Number} Implicit 1.1.1 (hidden)")));
  }

  /**
   * The autogenerated number for a node can be overridden. Leaf nodes don't get
   * an outline number.
   */
  @Test
  void testTemplateLine_ExplicitOutline() {
    assertEquals(lines("let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #2.3: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(
            lines("2 {BT-00-Text} foo", "\t3{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The autogenerated number for some nodes can be overridden. The other nodes
   * get an auto
   * generated outline number. Leaf nodes don't get an outline number.
   */
  @Test
  void testTemplateLine_MixedOutline() {
    assertEquals(lines("let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #2.1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("2{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  /**
   * The outline number can be suppressed for a line if overridden with the value
   * zero. Leaf nodes
   * don't get an outline number.
   */
  @Test
  void testTemplateLine_SuppressedOutline() {
    assertEquals(lines("let block01() -> { #2: text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(
            lines("2{BT-00-Text} foo", "\t0{ND-Root} bar", "\t\t{BT-00-Number} foo")));
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
    assertEquals(lines("let block01() -> { text('foo')",
        "for-each(../..).call(block0101()) }",
        "let block0101() -> { #1: text('bar')",
        "for-each(PathNode/NumberField).call(block010101()) }",
        "let block010101() -> { text('foo') }",
        "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("0{BT-00-Text} foo", "\t{ND-Root} bar", "\t\t{BT-00-Number} foo")));
  }

  @Test
  void testTemplateLine_IndentationWithTabs() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar")));
  }

  @Test
  void testTemplateLine_IndentationWithSpaces() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }", //
            "let block0101() -> { text('bar') }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} foo", "    {BT-00-Text} bar")));
  }

  @Test
  void testTemplateLine_LowerIndentation() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }",
            "let block0101() -> { text('bar') }",
            "let block02() -> { text('code') }",
            "for-each(/*/PathNode/TextField).call(block01())",
            "for-each(/*/PathNode/CodeField).call(block02())"),
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar", "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLine_LineJoining() {
    assertEquals(
        lines("let block01() -> { #1: text('foo')", "for-each(.).call(block0101()) }",
            "let block0101() -> { text('bar joined more') }",
            "let block02() -> { text('code') }",
            "for-each(/*/PathNode/TextField).call(block01())",
            "for-each(/*/PathNode/CodeField).call(block02())"),
        translateTemplate(lines("{BT-00-Text} foo", "\t{BT-00-Text} bar \\ \n  joined \\\n\\\nmore",
            "{BT-00-Code} code")));
  }

  @Test
  void testTemplateLine_VariableScope() {
    assertEquals(
        lines("let block01() -> { #1: eval(for $x in ./normalize-space(text()) return $x)", //
            "for-each(.).call(block0101()) }", //
            "let block0101() -> { eval(for $x in ./normalize-space(text()) return $x) }", //
            "for-each(/*/PathNode/TextField).call(block01())"), //
        translateTemplate(lines("{BT-00-Text} ${for text:$x in BT-00-Text return $x}",
            "    {BT-00-Text} ${for text:$x in BT-00-Text return $x}")));

  }

  @Test
  void testTemplateLine_ContextVariable() {
    assertEquals(
        lines(
            "let block01(string:xyz, string:ctx, string:t) -> { #1: eval(for $x in ./normalize-space(text()) return concat($x, $t))", //
            "for-each(.).call(block0101(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2='test'))", //
            "for-each(.).call(block0102(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2='test3')) }", //
            "let block0101(string:xyz, string:ctx, string:t, string:t2) -> { #1.1: eval(for $y in ./normalize-space(text()) return concat($y, $t, $t2))", //
            "for-each(.).call(block010101(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2=$t2))", //
            "for-each(.).call(block010102(string:xyz=$xyz, string:ctx=$ctx, string:t=$t, string:t2=$t2)) }", //
            "let block010101(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t, $ctx)) }", //
            "let block010102(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t, $ctx)) }", //
            "let block0102(string:xyz, string:ctx, string:t, string:t2) -> { eval(for $z in ./normalize-space(text()) return concat($z, $t2, $ctx)) }", //
            "for-each(/*/PathNode/TextField).call(block01(string:xyz='a', string:ctx=., string:t=./normalize-space(text())))"), //
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
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))line-break()text('some text') }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text} \\n some text"));
  }

  @Test
  void testTemplateLine_LineBreak() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))line-break() }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text} \\n"));
  }

  @Test
  void testTemplateLine_EndOfLineComments() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'))text(' blah blah') }\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root} #{name|BT-00-Text} blah blah // comment blah blah"));
  }

  @Test
  void testTemplateLine_Indentation_DeepNesting() {
    assertEquals(
        lines(
            "let block01() -> { #1: text('Level 1')",
            "for-each(.).call(block0101()) }",
            "let block0101() -> { #1.1: text('Level 2')",
            "for-each(.).call(block010101()) }",
            "let block010101() -> { text('Level 3') }",
            "for-each(/*/PathNode/TextField).call(block01())"),
        translateTemplate(lines(
            "{BT-00-Text} Level 1",
            "  {BT-00-Text} Level 2",
            "    {BT-00-Text} Level 3")));
  }

  // #endregion templateLine -------------------------------------------------

  // #region Labels -----------------------------------------------------------

  @Test
  void testLabelBlock_StandardLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_StandardLabelReferenceWithPluraliser() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text'), ../NumberField/number()) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{field|name|BT-00-Text;${BT-00-Number}}"));
  }

  @Test
  void testStandardLabelReference_UsingLabelTypeAsAssetId() {
    assertEquals(
        "let block01() -> { label(concat('auxiliary', '|', 'text', '|', 'value')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{auxiliary|text|value}"));
  }

  @Test
  void testLabelBlock_ComputedLabelReference() {
    assertEquals(
        "let block01() -> { label(string-join(('field','|','name','|','BT-00-Text'), ', ')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{${string-join(('field', '|', 'name', '|', 'BT-00-Text'), ', ')}}"));
  }

  @Test
  void testLabelBlock_ShorthandBtLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('business-term', '|', 'name', '|', 'BT-00')) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{name|BT-00}"));
  }

  @Test
  void testLabelBlock_ShorthandFieldLabelReference() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
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
        "let block01() -> { label(distinct-values(for $item in ../IndicatorField return concat('indicator', '|', 'when', '-', $item, '|', 'BT-00-Indicator'))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Indicator}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCode() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../CodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Code}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForInternalCode() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../InternalCodeField/normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-Internal-Code}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCodeAttribute() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../CodeField/@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("{BT-00-Text}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceForCodeAttributeWithSameAttributeInContext() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ../@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/CodeField/@attribute).call(block01())",
        translateTemplate("{BT-00-CodeAttribute}  #{BT-00-CodeAttribute}"));
  }

  @Test
  void testShorthandIndirectLabelReferenceForCodeAttribute_WithSameElementInContext() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ./@attribute return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/CodeField).call(block01())",
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
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Indicator')) }\nfor-each(/*/PathNode/IndicatorField).call(block01())",
        translateTemplate("{BT-00-Indicator}  #{name}"));
  }

  @Test
  void testShorthandLabelReferenceFromContext_WithValueLabelTypeAndCodeField() {
    assertEquals(
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Code')) }\nfor-each(/*/PathNode/CodeField).call(block01())",
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
        "let block01() -> { label(concat('field', '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*/PathNode/TextField).call(block01())",
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
        "let block01() -> { label(concat('node', '|', 'name', '|', 'ND-Root')) }\nfor-each(/*).call(block01())",
        translateTemplate("{ND-Root}  #{name}"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceFromContextField() {
    assertEquals(
        "let block01() -> { label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item))) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} #value"));
  }

  @Test
  void testLabelBlock_ShorthandIndirectLabelReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("{ND-Root} #value"));
  }

  @Test
  void testLabelBlock_Expression_AssetId() {
    assertEquals(
        "let block01(string:assetId) -> { label(concat('field', '|', 'name', '|', $assetId)) }\nfor-each(/*).call(block01(string:assetId='BT-00-Text'))",
        translateTemplate("{/, text:$assetId='BT-00-Text'}  #{field|name|${$assetId}}"));
  }

  @Test
  void testLabelBlock_Expression_LabelType() {
    assertEquals(
        "let block01(string:labelType) -> { label(concat('field', '|', $labelType, '|', 'BT-00-Text')) }\nfor-each(/*).call(block01(string:labelType='name'))",
        translateTemplate("{/, text:$labelType='name'}  #{field|${$labelType}|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_Expression_AssetType() {
    assertEquals(
        "let block01(string:assetType) -> { label(concat($assetType, '|', 'name', '|', 'BT-00-Text')) }\nfor-each(/*).call(block01(string:assetType='field'))",
        translateTemplate("{/, text:$assetType='field'}  #{${$assetType}|name|BT-00-Text}"));
  }

  @Test
  void testLabelBlock_Expression_NestedExpressions() {
    assertEquals(
        "let block01(string:assetType, string:labelType, string:assetId) -> { label(concat($assetType, '|', $labelType, '|', $assetId)) }\nfor-each(/*).call(block01(string:assetType='field', string:labelType='name', string:assetId='BT-00-Text'))",
        translateTemplate(
            "{/, text:$assetType='field', text:$labelType='name', text:$assetId='BT-00-Text'}  #{${$assetType}|${$labelType}|${$assetId}}"));
  }

  // #endregion Labels --------------------------------------------------------

  // #region Expression block -------------------------------------------------

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField() {
    assertEquals(
        "let block01() -> { eval(./normalize-space(text())) }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} $value"));
  }

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField_WithText() {
    assertEquals(
        "let block01() -> { text('blah ')label(distinct-values(for $item in ./normalize-space(text()) return concat('code', '|', 'name', '|', 'main-activity', '.', $item)))text(' blah ')eval(./normalize-space(text()))text(' blah') }\nfor-each(/*/PathNode/CodeField).call(block01())",
        translateTemplate("{BT-00-Code} blah #value blah $value blah"));
  }

  @Test
  void testExpressionBlock_ShorthandFieldValueReferenceFromContextField_WithNodeContext() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate("{ND-Root} $value"));
  }

  // #endregion Expression block ----------------------------------------------

  // #region contextDeclarationBlock ------------------------------------------

  @Test
  void testContextDeclarationBlock_MultipleVariables() {
    assertEquals(
        "let block01(string:var1, decimal:var2) -> { text('Variables: ')eval($var1)text(', ')eval($var2) }\nfor-each(/*/PathNode/TextField).call(block01(string:var1='hello', decimal:var2=42))",
        translateTemplate("{text:$var1='hello', number:$var2=42, BT-00-Text} Variables: ${$var1}, ${$var2}"));
  }

  @Test
  void testContextDeclarationBlock_VariableBeforeContext() {
    assertEquals(
        "let block01(string:prefix) -> { text('Prefix: ')eval($prefix)text(' Value: ')eval(./normalize-space(text())) }\nfor-each(/*/PathNode/TextField).call(block01(string:prefix='test'))",
        translateTemplate("{text:$prefix='test', BT-00-Text} Prefix: ${$prefix} Value: ${BT-00-Text}"));
  }

  @Test
  void testContextDeclarationBlock_OnlyVariables() {
    assertThrows(ParseCancellationException.class, () -> translateTemplate(
        "{text:$var1='test', number:$var2=123, indicator:$var3=TRUE} Only vars: ${$var1}, ${$var2}, ${$var3}"));
  }

  // #endregion contextDeclarationBlock ---------------------------------------

  // #region chooseTemplate ------------------------------------

  @Test
  void testChooseTemplate_WhenBlock_MultipleConditions() {
    assertEquals(
        lines(
            "let multi-when-template(string:status) -> { choose { when $status = 'active': text('Status: Active'), when $status = 'inactive': text('Status: Inactive'), when $status = 'pending': text('Status: Pending'), otherwise: text('Status: Unknown') } }",
            "let block02() -> { call(multi-when-template(string:status='active')) }",
            "for-each(/*).call(block02())"),
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
            "let complex-when-template(decimal:value) -> { choose { when $value > 0 and $value < 100: text('In range'), when $value <= 0: text('Too low'), otherwise: text('Too high') } }",
            "let block02() -> { call(complex-when-template(decimal:value=50)) }",
            "for-each(/*).call(block02())"),
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
            "let fallback-template(string:reason) -> { text('Fallback: ')eval($reason) }",
            "let otherwise-invoke-template(boolean:condition, string:reason) -> { choose { when $condition: text('Condition met'), otherwise: call(fallback-template(string:reason=$reason)) } }",
            "let block03() -> { call(otherwise-invoke-template(boolean:condition=false(), string:reason='default')) }",
            "for-each(/*).call(block03())"),
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
        lines("let some-template(string:txt) -> { text('&#62;')eval($txt)text('&#60;') }",
            "let block02(string:t) -> { choose { when 1 > 2: text('foo'), when 2 < 3: text('bar'), when 3 > 3: call(some-template(string:txt='1')), otherwise: text('foo-bar') } }",
            "let block03(string:t) -> { choose { when 1 > 2: text('foo'), when 2 < 3: text('bar'), when 3 > 3: text('foo-bar'), otherwise: call(some-template(string:txt='2')) } }",
            "for-each(/*/PathNode/TextField).call(block02(string:t='test'))",
            "for-each(/*/PathNode/TextField).call(block03(string:t='test'))"),
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
        lines("string:t='text'",
            "let block01() -> { choose { when true(): eval(./normalize-space(text()))text(' is a ')eval($t), otherwise nothing } }",
            "for-each(/*/PathNode/TextField[true()]).call(block01())"),
        translateTemplate(lines(
            "let text:$t = 'text';",
            "with BT-00-Text[TRUE] when TRUE display ${BT-00-Text} is a ${$t};")));
  }

  @Test
  void testChooseTemplate_WhenNoOtherwiseNoContext() {
    assertEquals(
        lines("string:t='test'",
            "let block01() -> { choose { when true(): text('this is a ')eval($t), otherwise nothing } }",
            "for-each(/*).call(block01())"),
        translateTemplate(lines(
            "let text:$t = 'test';",
            "when TRUE display this is a ${$t};")));
  }

  // #endregion chooseTemplate ---------------------------------

  // #region templateVariableList ---------------------------------------------

  @Test
  void testTemplateVariableList_WithAllDataTypes() {
    assertEquals(
        "let block01(string:str, decimal:num, boolean:bool, date:dt, time:tm, duration:dur) -> { text('All types: ')eval($str)text(', ')eval($num)text(', ')eval($bool)text(', ')eval(for $item in $dt return format-date($item, '[D01]/[M01]/[Y0001]'))text(', ')eval(for $item in $tm return format-time($item, '[H01]:[m01] [Z]'))text(', ')eval($dur) }\nfor-each(/*).call(block01(string:str='text', decimal:num=42, boolean:bool=true(), date:dt=xs:date('2023-01-01'), time:tm=xs:time('12:00:00'), duration:dur=xs:dayTimeDuration('P1D')))",
        translateTemplate(
            "{/, text:$str='text', number:$num=42, indicator:$bool=TRUE, date:$dt=date('2023-01-01'), time:$tm=time('12:00:00'), measure:$dur=day-time-duration('P1D')} All types: ${$str}, ${$num}, ${$bool}, ${$dt}, ${$tm}, ${$dur}"));
  }

  @Test
  void testTemplateVariableList_ExpressionInitializers() {
    assertEquals(
        "let block01(string:computed) -> { text('Computed: ')eval($computed) }\nfor-each(/*/PathNode/TextField).call(block01(string:computed=concat('prefix-', ./normalize-space(text()))))",
        translateTemplate("{BT-00-Text, text:$computed=concat('prefix-', BT-00-Text)} Computed: ${$computed}"));
  }

  // #endregion templateVariableList ------------------------------------------

  // #region templateLine edge cases ------------------------------------------

  @Test
  void testTemplateLine_OutlineNumber_Only() {
    assertEquals(
        "let block01() -> { text('text') }\nfor-each(/*).call(block01())",
        translateTemplate("1 display text;"));
  }

  @Test
  void testTemplateLine_OutlineNumber_WithContext() {
    assertEquals(
        "let block01() -> { text('Value: ')eval(./normalize-space(text())) }\nfor-each(/*/PathNode/TextField).call(block01())",
        translateTemplate("1 {BT-00-Text} Value: ${BT-00-Text}"));
  }

  // #endregion templateLine edge cases ---------------------------------------

  // #region InvalidIndentationException --------------------------------------

  @Test
  void testTemplateLine_InvalidIndentation_FirstIndentation() {
    assertThrows(InvalidIndentationException.class, () -> translateTemplate("  {BT-00-Text} foo"));
  }

  @Test
  void testTemplateLine_InvalidIndentation_MixedIndentation() {
    assertThrows(InvalidIndentationException.class,
        () -> translateTemplate("{BT-00-Text} foo\n\t  {BT-00-Text} bar"));
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

  // #endregion InvalidIndentationException -----------------------------------

  // #region TypeMismatchException --------------------------------------------

  @Test
  void testTemplateDefinition_InvalidParameters() {
    var template = lines(
        "let template:param-validation-template(text:$param1, number:$param2) display Valid parameters;",
        "invoke param-validation-template('test', 'test');");
    assertThrows(InvalidArgumentException.class, () -> translateTemplate(template));
  }

  // #endregion TypeMismatchException -----------------------------------------

  // #region IllegalArgumentException -----------------------------------------

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

  // #endregion IllegalArgumentException --------------------------------------

  @Test
  void testContextulizer_WithPredicate() {
    assertEquals(
        lines("let block01() -> { #1: text('line1: ')eval(.[1 = 1]/normalize-space(text()))",
            "for-each(.[1 = 2]).call(block0101()) }",
            "let block0101() -> { #1.1: text('line2: ')eval(.[1 = 4]/normalize-space(text()))",
            "for-each(.[1 = 4]).call(block010101()) }",
            "let block010101() -> { text('line3: ')eval(.[1 = 5]/normalize-space(text())) }",
            "for-each(/*/SubNode/SubSubNode/SubTextField[0 = 0]).call(block01())"),
        translateTemplate(lines("{BT-01-SubSubNode-Text} line1: ${BT-01-SubSubNode-Text[1==1]}",
            "  {BT-01-SubSubNode-Text[1==2]} line2: ${BT-01-SubSubNode-Text[1==4]}",
            "    {BT-01-SubSubNode-Text[1==4]} line3: ${BT-01-SubSubNode-Text[1==5]}")));
  }

  @Test
  void testContextualizer_WithFieldInPredicate() {
    assertEquals(
        lines("let block01() -> { #1: text('line1')",
        "for-each(SubTextField).call(block0101()) }",
        "let block0101() -> { text('line2') }",
        "for-each(/*/SubNode[SubTextField]).call(block01())"),
        translateTemplate(lines("{ND-SubNode[BT-01-SubNode-Text is present]} line1",
            "  {BT-01-SubNode-Text} line2")));
  }


}