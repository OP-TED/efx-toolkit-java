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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.TypeMismatchException;

/**
 * Tests for the ExpressionPreprocessor's type resolution logic.
 *
 * Organized by resolve method and CardinalityResolutionContext to clearly show coverage.
 * Each test is named: test[ResolveMethod]_[Context]_[scenario].
 */
class PreprocessorTypeResolutionTest extends EfxTestsBase {

  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  // #region resolveFieldOrAttributeReference -----------------------------------

  @Test
  void testResolveFieldReference_Scalar_NonRepeatableField() {
    translateExpressionWithContext("ND-Root", "BT-00-Text == 'test'");
  }

  @Test
  void testResolveFieldReference_Scalar_RepeatableField_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root", "BT-00-Repeatable-Text == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT, ex.getErrorCode());
  }

  @Test
  void testResolveFieldReference_Scalar_NonRepeatableAttribute() {
    translateExpressionWithContext("ND-Root", "BT-00-CodeAttribute/@attribute == 'test'");
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
    translateExpressionWithContext("ND-RepeatableNode",
        "BT-00-Text-In-Repeatable-Node/@attribute == 'test'");
  }

  @Test
  void testResolveFieldReference_Sequence_RepeatableFieldInIterator() {
    translateExpressionWithContext("ND-Root",
        "for text:$x in BT-00-Repeatable-Text return $x");
  }

  @Test
  void testResolveFieldReference_Sequence_NonRepeatableFieldInIterator() {
    // Silent promotion: non-repeatable field accepted in sequence context.
    translateExpressionWithContext("ND-Root",
        "for text:$x in BT-00-Text return $x");
  }

  @Test
  void testResolveFieldReference_Either_RepeatableFieldInForReturn() {
    translateExpressionWithContext("ND-Root",
        "for text:$x in BT-00-Text return BT-00-Repeatable-Text");
  }

  @Test
  void testResolveFieldReference_Either_NonRepeatableFieldInForReturn() {
    translateExpressionWithContext("ND-Root",
        "for text:$x in BT-00-Text return BT-00-Text");
  }

  @Test
  void testResolveFieldReference_Either_NestedForReturn() {
    translateExpressionWithContext("ND-Root",
        "for text:$x in BT-00-Text return (for text:$y in BT-00-Text return BT-00-Repeatable-Text)");
  }

  // #endregion resolveFieldOrAttributeReference --------------------------------

  // #region resolveFunctionInvocation ------------------------------------------

  @Test
  void testResolveFunctionInvocation_Scalar() {
    translateExpressionWithContext("ND-Root", "number(BT-00-Text)");
  }

  @Test
  void testResolveFunctionInvocation_Sequence() {
    translateExpressionWithContext("ND-Root", "count(BT-00-Text)");
  }

  @Test
  void testResolveFunctionInvocation_Sequence_ScalarFunction_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let text:?f() = 'hi';",
            "display count: ${count(?f())};")));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SCALAR, ex.getErrorCode());
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
    translateTemplate(lines(
        "let text:?f() = 'hi';",
        "display ${?f()};"));
  }

  @Test
  void testResolveFunctionInvocation_Either_SequenceFunction() {
    translateTemplate(lines(
        "let text*:?f() = ['a', 'b'];",
        "display count: ${count(?f())};"));
  }

  // #endregion resolveFunctionInvocation ---------------------------------------

  // #region resolveContextVariableReference ------------------------------------

  @Test
  void testResolveContextVariable_Scalar_NonRepeatableField() {
    translateExpressionWithContext("ND-Root",
        "for context:$f in BT-00-Text return $f == 'test'");
  }

  @Test
  void testResolveContextVariable_Scalar_RepeatableField() {
    translateExpressionWithContext("ND-Root",
        "for context:$f in BT-00-Repeatable-Text return $f == 'test'");
  }

  @Test
  void testResolveContextVariable_Scalar_NodeContextAsValue_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "for context:$n in ND-SubNode return $n == 'test'"));
    assertEquals(TypeMismatchException.ErrorCode.NODE_CONTEXT_AS_VALUE, ex.getErrorCode());
  }

  @Test
  void testResolveContextVariable_Sequence_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateExpressionWithContext("ND-Root",
            "for context:$f in BT-00-Repeatable-Text return 'test' in $f"));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SCALAR, ex.getErrorCode());
  }

  @Test
  void testResolveContextVariable_Either_FieldContextInForReturn() {
    translateExpressionWithContext("ND-Root",
        "for context:$f in BT-00-Text return $f");
  }

  // #endregion resolveContextVariableReference ---------------------------------

  // #region resolveRegularVariableReference ------------------------------------

  @Test
  void testResolveRegularVariable_Scalar_ScalarVariable() {
    translateTemplate(lines(
        "let text:$x = 'a';",
        "with BT-00-Text[$x == 'a'] display foo;"));
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
  void testResolveRegularVariable_Sequence_ScalarVariable_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let text:$x = 'a';",
            "display count: ${count($x)};")));
    assertEquals(TypeMismatchException.ErrorCode.IDENTIFIER_IS_SCALAR, ex.getErrorCode());
  }

  @Test
  void testResolveRegularVariable_Sequence_SequenceVariable() {
    translateTemplate(lines(
        "let text*:$items = ['a', 'b', 'c'];",
        "display count: ${count($items)};"));
  }

  @Test
  void testResolveRegularVariable_Either_ScalarVariable() {
    translateTemplate(lines(
        "let text:$x = 'test';",
        "display value: ${$x};"));
  }

  @Test
  void testResolveRegularVariable_Either_SequenceVariable() {
    translateTemplate(lines(
        "let text*:$items = ['a', 'b'];",
        "display ${for text:$x in $items return $x};"));
  }

  // #endregion resolveRegularVariableReference ---------------------------------

  // #region resolveDictionaryLookup --------------------------------------------

  @Test
  void testResolveDictionaryLookup_Scalar_InPredicate() {
    translateTemplate(lines(
        "let $dic index BT-00-Number by BT-00-Text;",
        "with BT-00-Text[$dic['key'] == 1] display foo;"));
  }

  @Test
  void testResolveDictionaryLookup_Either_InDisplayBlock() {
    translateTemplate(lines(
        "let $dic index BT-00-Number by BT-00-Text;",
        "display ${$dic['key']};"));
  }

  @Test
  void testResolveDictionaryLookup_Sequence_Throws() {
    TypeMismatchException ex = assertThrows(TypeMismatchException.class,
        () -> translateTemplate(lines(
            "let $dic index BT-00-Number by BT-00-Text;",
            "display ${count($dic['key'])};")));
    assertEquals(TypeMismatchException.ErrorCode.DICTIONARY_IS_SCALAR, ex.getErrorCode());
  }

  // #endregion resolveDictionaryLookup -----------------------------------------
}
