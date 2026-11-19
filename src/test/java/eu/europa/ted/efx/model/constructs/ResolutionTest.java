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
package eu.europa.ted.efx.model.constructs;

import static eu.europa.ted.efx.model.constructs.Overload.overload;
import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.Signature.signature;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.builtins.AmbiguousBuiltIn;
import eu.europa.ted.efx.model.builtins.ArithmeticOperators;
import eu.europa.ted.efx.model.builtins.BuiltIn;
import eu.europa.ted.efx.model.builtins.BuiltIns;
import eu.europa.ted.efx.model.builtins.ComparisonOperators;
import eu.europa.ted.efx.model.builtins.ConditionOperators;
import eu.europa.ted.efx.model.builtins.NumericFunctions;
import eu.europa.ted.efx.model.builtins.PatternMatch;
import eu.europa.ted.efx.model.builtins.SequenceFunctions;
import eu.europa.ted.efx.model.builtins.StringFunctions;
import eu.europa.ted.efx.model.builtins.TwoOverloadsBuiltIn;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanLiteral;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DatePath;
import eu.europa.ted.efx.model.expressions.scalar.DurationLiteral;
import eu.europa.ted.efx.model.expressions.scalar.DynamicExpression;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringPath;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericLiteral;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringLiteral;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequencePath;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

/**
 * Tests how a call resolves: arity, the binding of {@code T}, conversion by widening and by
 * promotion, path parameters, and each kind of mismatch.
 */
class ResolutionTest {

  private static final ScriptGenerator SCRIPT =
      new XPathScriptGenerator(EfxTranslatorOptions.DEFAULT);

  private static final DatePath DATE_FIELD =
      Expression.instantiate("$date", DatePath.class);
  private static final DurationLiteral DURATION =
      Expression.instantiate("$duration", DurationLiteral.class);
  private static final StringLiteral TEXT =
      Expression.instantiate("$text", StringLiteral.class);
  private static final StringLiteral OTHER_TEXT =
      Expression.instantiate("$other", StringLiteral.class);
  private static final NumericLiteral NUMBER =
      Expression.instantiate("$number", NumericLiteral.class);
  private static final BooleanLiteral INDICATOR =
      Expression.instantiate("$indicator", BooleanLiteral.class);
  private static final StringPath TEXT_FIELD =
      Expression.instantiate("$field", StringPath.class);
  private static final MultilingualStringPath MULTILINGUAL_FIELD =
      Expression.instantiate("$multilingual", MultilingualStringPath.class);
  private static final StringSequencePath REPEATABLE_TEXT_FIELD =
      Expression.instantiate("$repeatable", StringSequencePath.class);
  private static final MultilingualStringSequencePath REPEATABLE_MULTILINGUAL_FIELD =
      Expression.instantiate("$repeatableMultilingual", MultilingualStringSequencePath.class);
  private static final StringSequenceExpression TEXTS =
      Expression.instantiate("$texts", StringSequenceExpression.class);
  private static final NodePath NODE =
      Expression.instantiate("$node", NodePath.class);
  private static final NodeSequencePath NODES =
      Expression.instantiate("$nodes", NodeSequencePath.class);
  private static final MultilingualStringSequenceExpression MULTILINGUAL_TEXTS =
      Expression.instantiate("$multilingualTexts", MultilingualStringSequenceExpression.class);
  private static final DynamicExpression DYNAMIC =
      Expression.instantiate("$dynamic", DynamicExpression.class);

  private static final Overload TAKES_T = taking(ParameterType.T);
  private static final Overload TAKES_T_SEQUENCE = taking(ParameterType.T_SEQUENCE);
  private static final Overload TAKES_PATH = taking(ParameterType.PATH);
  private static final Overload TAKES_NODES = taking(ParameterType.NODES);
  private static final Overload TAKES_MULTILINGUAL_FIELD =
      taking(ParameterType.MULTILINGUAL_FIELD);
  private static final Overload TAKES_TEXT_SEQUENCE = taking(ParameterType.TEXT_SEQUENCE);

  @Test
  void testDatePlusDuration_IsADateAddition() {
    TypedExpression result = compose(ArithmeticOperators.PLUS, DATE_FIELD, DURATION);
    assertEquals(DateExpression.class, result.getClass());
    assertEquals("($date + $duration)", result.getScript());
  }

  @Test
  void testDurationPlusDate_IsTheSameDateAddition() {
    TypedExpression result = compose(ArithmeticOperators.PLUS, DURATION, DATE_FIELD);
    assertEquals(DateExpression.class, result.getClass());
    assertEquals("($date + $duration)", result.getScript());
  }

  @Test
  void testDateMinusDate_SubtractsTheSecondFromTheFirst() {
    DatePath later = Expression.instantiate("$later", DatePath.class);
    TypedExpression result = compose(ArithmeticOperators.MINUS, later, DATE_FIELD);
    assertEquals(SCRIPT.composeSubtraction(TypedExpression.from(DATE_FIELD, DateExpression.class),
        TypedExpression.from(later, DateExpression.class)).getScript(), result.getScript());
  }

  @Test
  void testMultilingualTextEqualsText_WidensToText() {
    Resolution resolution = resolve(ComparisonOperators.EQUAL, MULTILINGUAL_FIELD, TEXT);
    assertEquals(StringExpression.class, resolution.getArguments().typeAt(0));
    assertEquals(StringExpression.class, resolution.getArguments().typeAt(1));
  }

  @Test
  void testDynamicEqualsIndicator_BindsTToIndicator() {
    Resolution resolution = resolve(ComparisonOperators.EQUAL, DYNAMIC, INDICATOR);
    assertEquals(BooleanExpression.class, resolution.getArguments().typeAt(0));
  }

  @Test
  void testCountOfAField_PromotesItToASequence() {
    Resolution resolution = resolve(NumericFunctions.COUNT, TEXT_FIELD);
    assertEquals(StringSequenceExpression.class, resolution.getArguments().typeAt(0));
  }

  @Test
  void testTextInText_PromotesTheHaystack() {
    Resolution resolution = resolve(ConditionOperators.IN, TEXT, OTHER_TEXT);
    assertEquals(StringExpression.class, resolution.getArguments().typeAt(0));
    assertEquals(StringSequenceExpression.class, resolution.getArguments().typeAt(1));
  }

  @Test
  void testCountOfANode_CountsTheNodesItSelects() {
    Resolution resolution = resolve(NumericFunctions.COUNT, NODE);
    assertEquals(NodeSequencePath.class, resolution.getArguments().typeAt(0));
  }

  @Test
  void testIsPresentOfAField_KeepsThePath() {
    Resolution resolution = resolve(ConditionOperators.IS_PRESENT, TEXT_FIELD);
    assertSame(TEXT_FIELD, resolution.getArguments().at(0, PathExpression.class));
  }

  @Test
  void testUnionOfTextAndMultilingualSequences_IsATextSequence() {
    TypedExpression result = compose(SequenceFunctions.VALUE_UNION, REPEATABLE_TEXT_FIELD,
        REPEATABLE_MULTILINGUAL_FIELD);
    assertEquals(StringSequenceExpression.class, result.getClass());
  }

  @Test
  void testConcatOfThreeTexts_ConcatenatesThemInOrder() {
    TypedExpression result = compose(StringFunctions.CONCAT, TEXT, OTHER_TEXT, TEXT_FIELD);
    assertEquals(SCRIPT.composeStringConcatenation(List.of(TEXT, OTHER_TEXT,
        TypedExpression.from(TEXT_FIELD, StringExpression.class)))
        .getScript(), result.getScript());
  }

  @Test
  void testTextPlusNumber_IsRejectedAtTheTextByEveryOverload() {
    assertEquals(List.of(0, 0, 0, 0), positions(mismatch(ArithmeticOperators.PLUS, TEXT, NUMBER)));
  }

  @Test
  void testTextEqualsNumber_IsRejectedAtTheNumber() {
    Mismatch mismatch = mismatch(ComparisonOperators.EQUAL, TEXT, NUMBER);
    assertEquals(List.of(1), positions(mismatch));
    assertEquals(StringExpression.class,
        mismatch.getRejections().get(0).getRequired().getExpressionType());
  }

  @Test
  void testRepeatableFieldEqualsText_IsRejectedAtTheSequence() {
    assertEquals(List.of(0), positions(mismatch(ComparisonOperators.EQUAL, REPEATABLE_TEXT_FIELD, TEXT)));
  }

  @Test
  void testNodeEqualsText_IsRejectedAtTheNode() {
    assertEquals(List.of(0), positions(mismatch(ComparisonOperators.EQUAL, NODE, TEXT)));
  }

  @Test
  void testIndexOfANumberInTexts_IsRejectedAtTheNumber() {
    assertEquals(List.of(1), positions(mismatch(SequenceFunctions.INDEX_OF, TEXTS, NUMBER)));
  }

  @Test
  void testIsPresentOfAValue_IsRejected() {
    assertEquals(List.of(0), positions(mismatch(ConditionOperators.IS_PRESENT, TEXT)));
  }

  @Test
  void testSubstringOfOneArgument_IsAnArityMismatch() {
    assertFalse(StringFunctions.SUBSTRING.takes(1));
  }

  @Test
  void testConcatOfNoArgument_IsAnArityMismatch() {
    assertFalse(StringFunctions.CONCAT.takes(0));
  }

  @Test
  void testNotIn_NegatesIn() {
    assertEquals(SCRIPT.composeLogicalNot(composed(ConditionOperators.IN, TEXT, TEXTS)).getScript(),
        compose(ConditionOperators.NOT_IN, TEXT, TEXTS).getScript());
  }

  @Test
  void testHasDuplicates_NegatesHasNoDuplicates() {
    assertEquals(SCRIPT.composeLogicalNot(composed(ConditionOperators.HAS_NO_DUPLICATES, TEXTS))
        .getScript(), compose(ConditionOperators.HAS_DUPLICATES, TEXTS).getScript());
  }

  @Test
  void testLike_MatchesThePattern() {
    TypedExpression result = compose(PatternMatch.like("'[0-9]+'"), TEXT);
    assertEquals(SCRIPT.composePatternMatchCondition(TEXT, "'[0-9]+'").getScript(),
        result.getScript());
  }

  @Test
  void testNotLike_NegatesLike() {
    assertEquals(SCRIPT.composeLogicalNot(composed(PatternMatch.like("'[0-9]+'"), TEXT)).getScript(),
        compose(PatternMatch.notLike("'[0-9]+'"), TEXT).getScript());
  }

  @Test
  void testLike_RejectsAnUnsupportedPattern() {
    assertThrows(InvalidUsageException.class, () -> PatternMatch.like("'(?=a)'"));
  }

  @Test
  void testTextPlusNumber_HasNoResolution() {
    assertTrue(ArithmeticOperators.PLUS.resolve(List.of(TEXT, NUMBER)).isEmpty());
  }

  @Test
  void testNumberPlusNumber_HasNoMismatch() {
    assertThrows(IllegalArgumentException.class,
        () -> ArithmeticOperators.PLUS.mismatchOf(List.of(NUMBER, NUMBER)));
  }

  @Test
  void testAmbiguousBuiltIn_IsAConfigurationError() {
    TranslatorConfigurationException exception = assertThrows(TranslatorConfigurationException.class,
        () -> AmbiguousBuiltIn.INSTANCE.resolve(List.of(TEXT)));
    assertEquals(TranslatorConfigurationException.ErrorCode.AMBIGUOUS_OVERLOADS, exception.getErrorCode());
  }

  @Test
  void testSingleValueOverload_IsChosenOverASequenceForASingleValue() {
    assertChosenInEitherOrder(TAKES_T, TAKES_T_SEQUENCE,
        Expression.instantiate("$value", StringExpression.class));
  }

  @Test
  void testSequenceOverload_IsChosenForASequence() {
    assertChosenInEitherOrder(TAKES_T_SEQUENCE, TAKES_T, TEXTS);
  }

  @Test
  void testNodesOverload_IsChosenOverAnyPathForNodes() {
    assertChosenInEitherOrder(TAKES_NODES, TAKES_PATH, NODE, NODES);
  }

  @Test
  void testAnyPathOverload_IsChosenForAFieldOverNodes() {
    assertChosenInEitherOrder(TAKES_PATH, TAKES_NODES, TEXT_FIELD);
  }

  @Test
  void testMultilingualFieldOverload_IsChosenOverAnyPath() {
    assertChosenInEitherOrder(TAKES_MULTILINGUAL_FIELD, TAKES_PATH,
        REPEATABLE_MULTILINGUAL_FIELD);
  }

  @Test
  void testMultilingualFieldOverload_IsChosenOverATextSequenceForAMultilingualField() {
    assertChosenInEitherOrder(TAKES_MULTILINGUAL_FIELD, TAKES_TEXT_SEQUENCE, MULTILINGUAL_FIELD,
        REPEATABLE_MULTILINGUAL_FIELD);
  }

  @Test
  void testTextSequenceOverload_IsChosenForTextsThatAreNotAMultilingualField() {
    assertChosenInEitherOrder(TAKES_TEXT_SEQUENCE, TAKES_MULTILINGUAL_FIELD, REPEATABLE_TEXT_FIELD,
        MULTILINGUAL_TEXTS);
  }

  @Test
  void testNodes_MatchNeitherAMultilingualFieldNorATextSequence() {
    for (final TwoOverloadsBuiltIn builtIn :
        TwoOverloadsBuiltIn.inBothOrders(TAKES_MULTILINGUAL_FIELD, TAKES_TEXT_SEQUENCE)) {
      assertTrue(builtIn.resolve(List.of(NODES)).isEmpty(), builtIn.getOverloads().toString());
    }
  }

  @Test
  void testAnyPathAndATextSequence_AreAmbiguousForARepeatableTextField() {
    for (final TwoOverloadsBuiltIn builtIn :
        TwoOverloadsBuiltIn.inBothOrders(TAKES_PATH, TAKES_TEXT_SEQUENCE)) {
      final TranslatorConfigurationException exception = assertThrows(
          TranslatorConfigurationException.class,
          () -> builtIn.resolve(List.of(REPEATABLE_TEXT_FIELD)), builtIn.getOverloads().toString());
      assertEquals(TranslatorConfigurationException.ErrorCode.AMBIGUOUS_OVERLOADS,
          exception.getErrorCode());
    }
  }

  @Test
  void testArgumentsOfTwoEqualCalls_AreFoundInASet() {
    final Arguments<?> first = resolve(NumericFunctions.COUNT, NUMBER).getArguments();
    final Arguments<?> second = resolve(NumericFunctions.COUNT, NUMBER).getArguments();
    final Set<Arguments<?>> found = new HashSet<>(List.of(first));
    assertEquals(first, second);
    assertTrue(found.contains(second));
  }

  @Test
  void testCatalogue_FindsABuiltInByName() {
    assertSame(ConditionOperators.IS_NOT_EMPTY, BuiltIns.INSTANCE.find("is not empty").orElseThrow());
    assertTrue(BuiltIns.INSTANCE.find("like").isEmpty());
  }

  /**
   * Checks that a call of each argument alone chooses the expected of two overloads, whichever is
   * declared first.
   */
  private static void assertChosenInEitherOrder(final Overload expected, final Overload other,
      final TypedExpression... arguments) {
    for (final TwoOverloadsBuiltIn builtIn : TwoOverloadsBuiltIn.inBothOrders(expected, other)) {
      for (final TypedExpression argument : arguments) {
        assertSame(expected, resolve(builtIn, argument).getOverload(),
            builtIn.getOverloads() + " for " + argument.getClass().getSimpleName());
      }
    }
  }

  /**
   * Declares an overload of one parameter of a type, which returns its argument.
   */
  private static <E extends TypedExpression> Overload taking(final ParameterType<E> type) {
    return overload(signature(parameter("argument", type)).returning(type),
        (script, argument) -> argument);
  }

  private static Resolution resolve(final BuiltIn builtIn, final TypedExpression... arguments) {
    return builtIn.resolve(List.of(arguments))
        .orElseThrow(() -> new AssertionError(builtIn.mismatchOf(List.of(arguments))));
  }

  private static BooleanExpression composed(final BuiltIn builtIn,
      final TypedExpression... arguments) {
    return TypedExpression.from(compose(builtIn, arguments), BooleanExpression.class);
  }

  private static TypedExpression compose(final BuiltIn builtIn,
      final TypedExpression... arguments) {
    return resolve(builtIn, arguments).compose(SCRIPT);
  }

  private static Mismatch mismatch(final BuiltIn builtIn, final TypedExpression... arguments) {
    assertTrue(builtIn.resolve(List.of(arguments)).isEmpty());
    return builtIn.mismatchOf(List.of(arguments));
  }

  private static List<Integer> positions(final Mismatch mismatch) {
    return mismatch.getRejections().stream().map(Rejection::getPosition)
        .collect(Collectors.toList());
  }
}
