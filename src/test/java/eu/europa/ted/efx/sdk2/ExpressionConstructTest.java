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

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequencePath;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

/**
 * Proves that an expression construct composes what the overload that takes its arguments gives,
 * with {@code T} bound by them: the class the visitor translates the construct to. A predicate is
 * also composed with the method the script generator has for what its path selects.
 */
class ExpressionConstructTest {

  private static final ScriptGenerator SCRIPT =
      new XPathScriptGenerator(EfxTranslatorOptions.DEFAULT);

  private static final IteratorListExpression ITERATORS =
      new IteratorListExpression("$x in (1, 2)");

  @Test
  void testParentheses_GiveASingleValueForASingleValue() {
    assertComposed(StringExpression.class, ExpressionConstruct.PARENTHESES,
        value(StringExpression.class));
  }

  @Test
  void testParentheses_GiveASequenceForASequence() {
    assertComposed(StringSequenceExpression.class, ExpressionConstruct.PARENTHESES,
        value(StringSequenceExpression.class));
  }

  @Test
  void testConditional_GivesASingleValueForTwoSingleValues() {
    assertComposed(NumericExpression.class, ExpressionConstruct.CONDITIONAL,
        value(BooleanExpression.class), value(NumericExpression.class),
        value(NumericExpression.class));
  }

  @Test
  void testConditional_GivesASequenceIfEitherBranchIsOne() {
    assertComposed(NumericSequenceExpression.class, ExpressionConstruct.CONDITIONAL,
        value(BooleanExpression.class), value(NumericExpression.class),
        value(NumericSequenceExpression.class));
  }

  @Test
  void testList_GivesTheSequenceOfItsItems() {
    assertComposed(DateSequenceExpression.class, ExpressionConstruct.LIST,
        value(DateExpression.class), value(DateExpression.class));
  }

  @Test
  void testIteration_GivesASequenceForASingleValue() {
    assertComposed(NumericSequenceExpression.class, ExpressionConstruct.iteration(ITERATORS),
        value(NumericExpression.class));
  }

  @Test
  void testQuantifier_GivesAnIndicator() {
    assertComposed(BooleanExpression.class, ExpressionConstruct.every("every", ITERATORS),
        value(BooleanExpression.class));
  }

  @Test
  void testIndexInValue_GivesASingleValue() {
    assertComposed(DateExpression.class, ExpressionConstruct.INDEX_IN_VALUE,
        value(DateSequenceExpression.class), value(NumericExpression.class));
  }

  @Test
  void testIndexInPath_GivesAPathToASingleValue() {
    assertComposed(StringPath.class, ExpressionConstruct.INDEX_IN_PATH,
        value(StringSequencePath.class), value(NumericExpression.class));
  }

  @Test
  void testPredicate_GivesAPathOfTheClassItTakes() {
    assertComposed(StringSequencePath.class, ExpressionConstruct.PREDICATE,
        value(StringSequencePath.class), value(BooleanExpression.class));
  }

  @Test
  void testPredicate_OnAField_ComposesThePredicateOnAField() {
    assertEquals("composeFieldReferenceWithPredicate",
        composerOfPredicateOn(value(StringSequencePath.class)));
  }

  @Test
  void testPredicate_OnANode_ComposesThePredicateOnANode() {
    assertEquals("composeNodeReferenceWithPredicate",
        composerOfPredicateOn(value(NodeSequencePath.class)));
  }

  @Test
  void testContextOverride_GivesAPathOfTheClassOfItsRightOperand() {
    assertComposed(StringSequencePath.class, ExpressionConstruct.CONTEXT_OVERRIDE,
        value(NodePath.class), value(StringSequencePath.class));
  }

  /**
   * Gives the name of the script generator method that composes a predicate on a path, since the
   * XPath of the two methods for a predicate is the same. The script generator answers every call
   * with the name of its method; it is not a subclass of the XPath generator, which the component
   * factory would find as a second script generator.
   */
  private static String composerOfPredicateOn(final TypedExpression path) {
    final ScriptGenerator script = (ScriptGenerator) Proxy.newProxyInstance(
        ScriptGenerator.class.getClassLoader(), new Class<?>[] {ScriptGenerator.class},
        (proxy, method, arguments) -> Expression.instantiate(method.getName(),
            PathExpression.class.cast(arguments[0]).getClass()));
    return ExpressionConstruct.PREDICATE.resolve(List.of(path, value(BooleanExpression.class)))
        .orElseThrow().compose(script).getScript();
  }

  private static void assertComposed(final Class<? extends TypedExpression> expected,
      final ExpressionConstruct construct, final TypedExpression... arguments) {
    assertEquals(expected,
        construct.resolve(List.of(arguments)).orElseThrow().compose(SCRIPT).getClass());
  }

  private static TypedExpression value(final Class<? extends TypedExpression> type) {
    return Expression.instantiate(type.getSimpleName(), type);
  }
}
