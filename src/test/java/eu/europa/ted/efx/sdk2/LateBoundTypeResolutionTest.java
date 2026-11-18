/*
 * Copyright 2026 European Union
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.exceptions.TypeMismatchException.ErrorCode;

/**
 * Tests for late-bound type resolution logic.
 *
 * Organized by the kind of reference and by what the place it is written in requires: a single
 * value (Scalar), a sequence (Sequence) or either (Either). Each test is named:
 * test[Reference]_[Requirement]_[scenario].
 */
class LateBoundTypeResolutionTest extends EfxTestsBase {

  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  // #region resolveFieldOrAttributeReference -----------------------------------

  @Test
  void testResolveFieldReference_Scalar_NonRepeatableField() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text == 'test'"));
  }

  @Test
  void testResolveFieldReference_Scalar_RepeatableField_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testResolveFieldReference_Scalar_NonRepeatableAttribute() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root", "BT-00-CodeAttribute/@attribute == 'test'"));
  }

  @Test
  void testResolveFieldReference_Scalar_AttributeOnRepeatableNode_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text-In-Repeatable-Node/@attribute == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testResolveFieldReference_Scalar_AttributeOnRepeatableNode_OkFromSameContext() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-RepeatableNode",
            "BT-00-Text-In-Repeatable-Node/@attribute == 'test'"));
  }

  @Test
  void testResolveFieldReference_Sequence_RepeatableFieldInIterator() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Repeatable-Text return $x"));
  }

  @Test
  void testResolveFieldReference_Sequence_NonRepeatableFieldInIterator() {
    // Silent promotion: non-repeatable field accepted in sequence context.
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Text return $x"));
  }

  @Test
  void testResolveFieldReference_Either_RepeatableFieldInForReturn() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Text return BT-00-Repeatable-Text"));
  }

  @Test
  void testResolveFieldReference_Either_NonRepeatableFieldInForReturn() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Text return BT-00-Text"));
  }

  @Test
  void testResolveFieldReference_Either_NestedForReturn() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Text return (for text:$y in BT-00-Text return BT-00-Repeatable-Text)"));
  }

  // #endregion resolveFieldOrAttributeReference --------------------------------

  // #region resolveFunctionInvocation ------------------------------------------

  @Test
  void testResolveFunctionInvocation_Scalar() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root", "number(BT-00-Text)"));
  }

  @Test
  void testResolveFunctionInvocation_Sequence() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root", "count(BT-00-Text)"));
  }

  @Test
  void testResolveFunctionInvocation_Sequence_ScalarFunction_IsPromoted() {
    // Promotion: a scalar function where a sequence is required gives a sequence of one value.
    assertEquals(
        lines(
            "GLOBALS:",
            "string:f() -> { 'hi' }",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count(udf:f()) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text:?f() = 'hi';",
            "display count: ${count(?f())};")));
  }

  @Test
  void testResolveFunctionInvocation_Scalar_SequenceFunction_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let text*:?f() = ['a', 'b'];",
            "with BT-00-Text[?f() == 'a'] display foo;")));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SEQUENCE, ex.getErrorCode());
  }

  @Test
  void testResolveFunctionInvocation_Either_ScalarFunction() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let text:?f() = 'hi';",
            "display ${?f()};")));
  }

  @Test
  void testResolveFunctionInvocation_Either_SequenceFunction() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let text*:?f() = ['a', 'b'];",
            "display count: ${count(?f())};")));
  }

  // #endregion resolveFunctionInvocation ---------------------------------------

  // #region resolveContextVariableReference ------------------------------------

  @Test
  void testResolveContextVariable_Scalar_NonRepeatableField() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for context:$f in BT-00-Text return $f == 'test'"));
  }

  @Test
  void testResolveContextVariable_Scalar_RepeatableField() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for context:$f in BT-00-Repeatable-Text return $f == 'test'"));
  }

  @Test
  void testResolveContextVariable_Scalar_NodeContextAsValue_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "for context:$n in ND-SubNode return $n == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.NODE_CONTEXT_AS_VALUE, ex.getErrorCode());
  }

  @Test
  void testResolveContextVariable_Sequence_IsPromoted() {
    // Promotion: a context variable where a sequence is required gives a sequence of one value.
    testExpressionTranslationWithContext("for $f in PathNode/RepeatableTextField return 'test' = $f/normalize-space(text())", "ND-Root",
        "for context:$f in BT-00-Repeatable-Text return 'test' in $f");
  }

  @Test
  void testResolveContextVariable_Either_FieldContextInForReturn() {
    assertDoesNotThrow(
        () -> translateExpressionWithContext("ND-Root",
            "for context:$f in BT-00-Text return $f"));
  }

  // #endregion resolveContextVariableReference ---------------------------------

  // #region resolveRegularVariableReference ------------------------------------

  @Test
  void testResolveRegularVariable_Scalar_ScalarVariable() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let text:$x = 'a';",
            "with BT-00-Text[$x == 'a'] display foo;")));
  }

  @Test
  void testResolveRegularVariable_Scalar_SequenceVariable_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let text*:$items = ['a', 'b'];",
            "with BT-00-Text[$items == 'a'] display foo;")));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SEQUENCE, ex.getErrorCode());
  }

  @Test
  void testResolveRegularVariable_Sequence_ScalarVariable_IsPromoted() {
    // Promotion: a scalar variable where a sequence is required gives a sequence of one value.
    assertEquals(
        lines(
            "GLOBALS:",
            "string:x='a'",
            "TEMPLATES:",
            "let body01() -> { text('count: ')eval(for $__item in count($x) return format-number($__item, '# ##0,#########')) }",
            "MAIN:",
            "for-each(/*).call(body01())"),
        translateTemplate(lines(
            "let text:$x = 'a';",
            "display count: ${count($x)};")));
  }

  @Test
  void testResolveRegularVariable_Sequence_SequenceVariable() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let text*:$items = ['a', 'b', 'c'];",
            "display count: ${count($items)};")));
  }

  @Test
  void testResolveRegularVariable_Either_ScalarVariable() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let text:$x = 'test';",
            "display value: ${$x};")));
  }

  @Test
  void testResolveRegularVariable_Either_SequenceVariable() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let text*:$items = ['a', 'b'];",
            "display ${for text:$x in $items return $x};")));
  }

  @Test
  void testResolveRegularVariable_UndeclaredVariable_Throws() {
    assertThrows(Exception.class,
        () -> translateExpressionWithContext("ND-Root",
            "for text:$x in BT-00-Text return $undeclared"));
  }

  // #endregion resolveRegularVariableReference ---------------------------------

  // #region resolveDictionaryLookup --------------------------------------------

  @Test
  void testResolveDictionaryLookup_Scalar_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "with BT-00-Text[$dic['key'] == 1] display foo;")));
    assertEquals(TypeMismatchException.ErrorCode.DICTIONARY_IS_SEQUENCE, ex.getErrorCode());
  }

  @Test
  void testResolveDictionaryLookup_Either_InDisplayBlock() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "display ${$dic['key']};")));
  }

  @Test
  void testResolveDictionaryLookup_Sequence_InCountFunction() {
    assertDoesNotThrow(
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "display ${count($dic['key'])};")));
  }

  // #endregion resolveDictionaryLookup -----------------------------------------

  // #region verifyMatchingTypes ------------------------------------------------

  @Test
  void testLateBoundComparison_MismatchedFieldTypes_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text == BT-00-Number"));
    assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, ex.getErrorCode());
  }

  @Test
  void testLateBoundComparison_OneSideMismatched_Throws() {
    // Late-bound text field compared against numeric literal — the typed pop should catch this.
    assertThrows(Exception.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Text == 42"));
  }

  // #endregion verifyMatchingTypes ---------------------------------------------

  // #region typeConversions ------------------------------------------------------

  @Test
  void testLateBoundToNumber_FromTextField() {
    testExpressionTranslationWithContext(
        "number(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "number(BT-00-Text)");
  }

  @Test
  void testLateBoundToString_FromNumberField() {
    testExpressionTranslationWithContext(
        "string(PathNode/NumberField/number())",
        "ND-Root", "text(BT-00-Number)");
  }

  @Test
  void testLateBoundBooleanFromNumber_FromNumberField() {
    testExpressionTranslationWithContext(
        "boolean(PathNode/NumberField/number())",
        "ND-Root", "indicator(BT-00-Number)");
  }

  @Test
  void testLateBoundDateFromString_FromTextField() {
    testExpressionTranslationWithContext(
        "xs:date(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "date(BT-00-Text)");
  }

  @Test
  void testLateBoundTimeFromString_FromTextField() {
    testExpressionTranslationWithContext(
        "xs:time(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "time(BT-00-Text)");
  }

  // #endregion typeConversions ---------------------------------------------------

  // #region parenthesizedLateBound ----------------------------------------------

  @Test
  void testParenthesizedLateBoundScalar_PreservesParentheses() {
    testExpressionTranslationWithContext(
        "(PathNode/NumberField/number()) + 1",
        "ND-Root", "(BT-00-Number) + 1");
  }

  @Test
  void testParenthesizedLateBoundSequence_PreservesParentheses() {
    testExpressionTranslationWithContext(
        "count((PathNode/RepeatableTextField/normalize-space(text())))",
        "ND-Root", "count((BT-00-Repeatable-Text))");
  }

  @Test
  void testParenthesizedLateBoundSequence_BareReference() {
    testExpressionTranslationWithContext(
        "(PathNode/RepeatableTextField/normalize-space(text()))",
        "ND-Root", "(BT-00-Repeatable-Text)");
  }

  // #endregion parenthesizedLateBound -------------------------------------------

  // #region incompatibleOperands -------------------------------------------------

  @Test
  void testLateBoundAddition_IncompatibleTypes_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Duration + BT-00-Text"));
    assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, ex.getErrorCode());
  }

  @Test
  void testLateBoundSubtraction_IncompatibleTypes_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Duration - BT-00-Text"));
    assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, ex.getErrorCode());
  }

  @Test
  void testLateBoundMultiplication_IncompatibleTypes_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Duration / BT-00-Duration"));
    assertEquals(TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS, ex.getErrorCode());
  }

  // #endregion incompatibleOperands -----------------------------------------------

  // #region operandOrigin ----------------------------------------------------------

  // An operand whose value is a sequence where a single value is required names the reference it
  // is written as, at the position of that reference, through any parentheses around it.

  @Test
  void testOperandOrigin_FieldReference() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:12", "BT-00-Repeatable-Text",
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text == 'test'"));
  }

  @Test
  void testOperandOrigin_ParenthesizedFieldReference() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:13", "BT-00-Repeatable-Text",
        () -> translateExpressionWithContext("ND-Root", "(BT-00-Repeatable-Text) == 'test'"));
  }

  @Test
  void testOperandOrigin_NestedParentheses() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:14", "BT-00-Repeatable-Text",
        () -> translateExpressionWithContext("ND-Root", "((BT-00-Repeatable-Text)) == 'test'"));
  }

  @Test
  void testOperandOrigin_ParenthesizedMultilingualField() {
    assertSequenceError(ErrorCode.FIELD_IS_MULTILINGUAL, "1:13", "BT-00-Text-Multilingual",
        () -> translateExpressionWithContext("ND-Root", "(BT-00-Text-Multilingual) == 'test'"));
  }

  @Test
  void testOperandOrigin_AttributeReference() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:12", "BT-00-Repeatable-Text",
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Repeatable-Text/@attribute == 'test'"));
  }

  @Test
  void testOperandOrigin_ContextOverride() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:12", "BT-00-Text-In-Repeatable-Node",
        () -> translateExpressionWithContext("ND-Root",
            "ND-RepeatableNode::BT-00-Text-In-Repeatable-Node == 'test'"));
  }

  @Test
  void testOperandOrigin_ParenthesizedFunctionArgument() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:24", "BT-00-Repeatable-Text",
        () -> translateExpressionWithContext("ND-Root", "upper-case((BT-00-Repeatable-Text))"));
  }

  @Test
  void testOperandOrigin_SequenceFunction() {
    assertSequenceError(ErrorCode.IDENTIFIER_IS_SEQUENCE, "2:16", "?f",
        () -> translateTemplate(lines(
            "let text*:?f() = ['a', 'b'];",
            "with BT-00-Text[?f() == 'a'] display foo;")));
  }

  @Test
  void testOperandOrigin_ParenthesizedSequenceFunction() {
    assertSequenceError(ErrorCode.IDENTIFIER_IS_SEQUENCE, "2:17", "?f",
        () -> translateTemplate(lines(
            "let text*:?f() = ['a', 'b'];",
            "with BT-00-Text[(?f()) == 'a'] display foo;")));
  }

  @Test
  void testOperandOrigin_ParenthesizedSequenceVariable() {
    assertSequenceError(ErrorCode.IDENTIFIER_IS_SEQUENCE, "2:17", "$items",
        () -> translateTemplate(lines(
            "let text*:$items = ['a', 'b'];",
            "with BT-00-Text[($items) == 'a'] display foo;")));
  }

  @Test
  void testOperandOrigin_DictionaryLookup() {
    assertSequenceError(ErrorCode.DICTIONARY_IS_SEQUENCE, "2:16", "$dic",
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "with BT-00-Text[$dic['key'] == 1] display foo;")));
  }

  @Test
  void testOperandOrigin_ParenthesizedDictionaryLookup() {
    assertSequenceError(ErrorCode.DICTIONARY_IS_SEQUENCE, "2:17", "$dic",
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "with BT-00-Text[($dic['key']) == 1] display foo;")));
  }

  @Test
  void testOperandOrigin_ListItem() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:24", "BT-00-Repeatable-Text",
        () -> translateExpressionWithContext("ND-Root", "count(['a', BT-00-Repeatable-Text])"));
  }

  @Test
  void testOperandOrigin_ConditionalBranch_SequenceJoins() {
    // A branch takes a sequence: the conditional takes the join of its branches.
    testExpressionTranslationWithContext(
        "(if (PathNode/IndicatorField/(normalize-space(text()) = ('true', '1'))) then PathNode/RepeatableTextField/normalize-space(text()) else 'x')",
        "ND-Root", "if BT-00-Indicator then BT-00-Repeatable-Text else 'x'");
  }

  @Test
  void testOperandOrigin_ConditionalCondition() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:15", "BT-00-Repeatable-Indicator",
        () -> translateExpressionWithContext("ND-Root",
            "if BT-00-Repeatable-Indicator then 'a' else 'x'"));
  }

  @Test
  void testOperandOrigin_Predicate_NamesThePredicateContext() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "BT-00-Text[(BT-00-Repeatable-Indicator)] is present"));
    assertEquals(ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
    assertEquals("line 1:24 Type mismatch. Predicate cannot take 'text' and 'indicator*'. Its"
        + " condition must be 'indicator'. Field 'BT-00-Repeatable-Indicator' may return multiple"
        + " values from context 'BT-00-Text': use a sequence expression or change the context.",
        ex.getMessage());
  }

  @Test
  void testOperandOrigin_Indexer() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:26", "BT-00-Repeatable-Number",
        () -> translateExpressionWithContext("ND-Root", "['a', 'b'][at BT-00-Repeatable-Number]"));
  }

  @Test
  void testOperandOrigin_DictionaryKey_NamesTheIndexedField() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Repeatable-Text;",
            "display ${$dic['a']};")));
    assertEquals(ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
    assertEquals("line 1:31 Type mismatch. Clause 'by' cannot take 'text*'."
        + " Its key must be 'text'. Field 'BT-00-Repeatable-Text' may return multiple values from"
        + " context 'BT-00-Number': use a sequence expression or change the context.",
        ex.getMessage());
  }

  @Test
  void testOperandOrigin_FunctionBody() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:17", "BT-00-Repeatable-Text",
        () -> translateTemplate(lines(
            "let text:?f() = (BT-00-Repeatable-Text);",
            "display ${?f()};")));
  }

  @Test
  void testOperandOrigin_VariableInitializer() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:15", "BT-00-Repeatable-Text",
        () -> translateTemplate(lines(
            "let text:$x = (BT-00-Repeatable-Text);",
            "display ${$x};")));
  }

  @Test
  void testOperandOrigin_TemplateCondition() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:6", "BT-00-Repeatable-Indicator",
        () -> translateTemplate(lines(
            "when (BT-00-Repeatable-Indicator) display foo",
            "otherwise display bar;")));
  }

  @Test
  void testOperandOrigin_LinkTarget() {
    assertSequenceError(ErrorCode.FIELD_MAY_REPEAT, "1:25", "BT-00-Repeatable-Text",
        () -> translateTemplate("{ND-Root} link ${'x'} @{(BT-00-Repeatable-Text)}"));
  }

  /**
   * Asserts that the translation fails with the given code, at the given line and column, naming
   * the given identifier.
   */
  private static void assertSequenceError(final ErrorCode code, final String position,
      final String identifier, final Executable translation) {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class, translation);
    assertEquals(code, ex.getErrorCode());
    assertTrue(ex.getMessage().startsWith("line " + position + " "), ex::getMessage);
    assertTrue(ex.getMessage().contains("'" + identifier + "'"), ex::getMessage);
  }

  // #endregion operandOrigin -------------------------------------------------------
}
