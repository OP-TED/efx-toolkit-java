package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import eu.europa.ted.efx.mock.DependencyFactoryMock;

class EfxExpressionTranslatorTest {
  private static final String[] SDK_VERSIONS = new String[] {"eforms-sdk-1.0", "eforms-sdk-2.0"};

  protected static Stream<Arguments> provideSdkVersions() {
    List<Arguments> arguments = new ArrayList<>();

    for (String sdkVersion : SDK_VERSIONS) {
      arguments.add(Arguments.of(sdkVersion));
    }

    return Stream.of(arguments.toArray(new Arguments[0]));
  }

  private void testExpressionTranslationWithContext(final String sdkVersion,
      final String expectedTranslation, final String context, final String expression) {
    assertEquals(expectedTranslation,
        translateExpressionWithContext(sdkVersion, context, expression));
  }

  private void testExpressionTranslation(final String sdkVersion, final String expectedTranslation,
      final String expression, final String... params) {
    assertEquals(expectedTranslation, translateExpression(sdkVersion, expression, params));
  }

  private String translateExpressionWithContext(final String sdkVersion, final String context,
      final String expression) {
    return translateExpression(sdkVersion, String.format("{%s} ${%s}", context, expression));
  }

  private String translateExpression(final String sdkVersion, final String expression,
      final String... params) {
    try {
      return EfxTranslator.translateExpression(DependencyFactoryMock.INSTANCE, sdkVersion,
          expression, params);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  // #region: Boolean expressions ---------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParenthesizedBooleanExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "(true() or true()) and false()", "BT-00-Text",
        "(ALWAYS or TRUE) and NEVER");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testLogicalOrCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "true() or false()", "BT-00-Text",
        "ALWAYS or NEVER");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testLogicalAndCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "true() and 1 + 1 = 2", "BT-00-Text",
        "ALWAYS and 1 + 1 == 2");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testInListCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "not('x' = ('a','b','c'))", "BT-00-Text",
        "'x' not in ('a', 'b', 'c')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testEmptinessCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/TextField/normalize-space(text()) = ''", "ND-Root", "BT-00-Text is empty");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testEmptinessCondition_WithNot(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/TextField/normalize-space(text()) != ''", "ND-Root", "BT-00-Text is not empty");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testPresenceCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "PathNode/TextField", "ND-Root",
        "BT-00-Text is present");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testPresenceCondition_WithNot(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "not(PathNode/TextField)", "ND-Root",
        "BT-00-Text is not present");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUniqueValueCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "count(for $x in PathNode/TextField, $y in /*/PathNode/TextField[. = $x] return $y) = 1",
        "ND-Root", "BT-00-Text is unique in /BT-00-Text");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUniqueValueCondition_WithNot(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "not(count(for $x in PathNode/TextField, $y in /*/PathNode/TextField[. = $x] return $y) = 1)",
        "ND-Root", "BT-00-Text is not unique in /BT-00-Text");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testLikePatternCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "fn:matches(normalize-space('123'), '[0-9]*')",
        "BT-00-Text", "'123' like '[0-9]*'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testLikePatternCondition_WithNot(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "not(fn:matches(normalize-space('123'), '[0-9]*'))", "BT-00-Text",
        "'123' not like '[0-9]*'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_UsingTextFields(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/TextField/normalize-space(text()) = PathNode/TextMultilingualField/normalize-space(text())",
        "Root", "text == textMultilingual");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_UsingNumericFields(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/NumberField/number() <= PathNode/IntegerField/number()", "ND-Root",
        "BT-00-Number <= integer");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_UsingIndicatorFields(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/IndicatorField != PathNode/IndicatorField", "ND-Root",
        "BT-00-Indicator != BT-00-Indicator");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_UsingDateFields(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/StartDateField/xs:date(text()) <= PathNode/EndDateField/xs:date(text())",
        "ND-Root", "BT-00-StartDate <= BT-00-EndDate");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_UsingTimeFields(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/StartTimeField/xs:time(text()) <= PathNode/EndTimeField/xs:time(text())",
        "ND-Root", "BT-00-StartTime <= BT-00-EndTime");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_UsingMeasureFields(final String sdkVersion) {
    assertEquals(
        "boolean(for $T in (current-date()) return ($T + (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) <= $T + (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ()))))",
        translateExpressionWithContext(sdkVersion, "ND-Root", "BT-00-Measure <= BT-00-Measure"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_WithStringLiteral(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/TextField/normalize-space(text()) = 'abc'", "ND-Root", "BT-00-Text == 'abc'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_WithNumericLiteral(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/IntegerField/number() - PathNode/NumberField/number() > 0", "ND-Root",
        "integer - BT-00-Number > 0");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_WithDateLiteral(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') > PathNode/StartDateField/xs:date(text())", "ND-Root",
        "2022-01-01Z > BT-00-StartDate");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_WithTimeLiteral(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('00:01:00Z') > PathNode/EndTimeField/xs:time(text())", "ND-Root",
        "00:01:00Z > BT-00-EndTime");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldValueComparison_TypeMismatch(final String sdkVersion) {
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext(sdkVersion, "ND-Root", "00:01:00 > BT-00-StartDate"));
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanComparison_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "false() != true()", "BT-00-Text",
        "NEVER != ALWAYS");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanComparison_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "../IndicatorField != true()", "BT-00-Text",
        "BT-00-Indicator != ALWAYS");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericComparison(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "2 > 1 and 3 >= 1 and 1 = 1 and 4 < 5 and 5 <= 5 and ../NumberField/number() > ../IntegerField/number()",
        "BT-00-Text", "2 > 1 and 3>=1 and 1==1 and 4<5 and 5<=5 and BT-00-Number > integer");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringComparison(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "'aaa' < 'bbb'", "BT-00-Text",
        "'aaa' < 'bbb'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateComparison_OfTwoDateLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2018-01-01Z') > xs:date('2018-01-01Z')", "BT-00-Text",
        "2018-01-01Z > 2018-01-01Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateComparison_OfTwoDateReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/StartDateField/xs:date(text()) = PathNode/EndDateField/xs:date(text())",
        "ND-Root", "BT-00-StartDate == BT-00-EndDate");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateComparison_OfDateReferenceAndDateFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/StartDateField/xs:date(text()) = xs:date(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "BT-00-StartDate == date(BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeComparison_OfTwoTimeLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "xs:time('13:00:10Z') > xs:time('21:20:30Z')",
        "BT-00-Text", "13:00:10Z > 21:20:30Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testZonedTimeComparison_OfTwoTimeLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('13:00:10+01:00') > xs:time('21:20:30+02:00')", "BT-00-Text",
        "13:00:10+01:00 > 21:20:30+02:00");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeComparison_OfTwoTimeReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/StartTimeField/xs:time(text()) = PathNode/EndTimeField/xs:time(text())",
        "ND-Root", "BT-00-StartTime == BT-00-EndTime");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeComparison_OfTimeReferenceAndTimeFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/StartTimeField/xs:time(text()) = xs:time(PathNode/TextField/normalize-space(text()))",
        "ND-Root", "BT-00-StartTime == time(BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationComparison_UsingYearMOnthDurationLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P1Y') = $T + xs:yearMonthDuration('P12M')))",
        "BT-00-Text", "P1Y == P12M");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationComparison_UsingDayTimeDurationLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P21D') > $T + xs:dayTimeDuration('P7D')))",
        "BT-00-Text", "P3W > P7D");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCalculatedDurationComparison(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P3M') > $T + xs:dayTimeDuration(PathNode/EndDateField/xs:date(text()) - PathNode/StartDateField/xs:date(text()))))",
        "ND-Root", "P3M > (BT-00-EndDate - BT-00-StartDate)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNegativeDuration_Literal(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "xs:yearMonthDuration('-P3M')", "ND-Root",
        "-P3M");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNegativeDuration_ViaMultiplication(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "(-3 * (2 * xs:yearMonthDuration('-P3M')))",
        "ND-Root", "2 * -P3M * -3");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNegativeDuration_ViaMultiplicationWithField(final String sdkVersion) {
    assertEquals(
        "(-3 * (2 * (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ()))))",
        translateExpressionWithContext(sdkVersion, "ND-Root", "2 * measure:BT-00-Measure * -3"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationAddition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(xs:dayTimeDuration('P3D') + xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
        "ND-Root", "P3D + (BT-00-StartDate - BT-00-EndDate)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationSubtraction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(xs:dayTimeDuration('P3D') - xs:dayTimeDuration(PathNode/StartDateField/xs:date(text()) - PathNode/EndDateField/xs:date(text())))",
        "ND-Root", "P3D - (BT-00-StartDate - BT-00-EndDate)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanLiteralExpression_Always(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "true()", "BT-00-Text", "ALWAYS");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanLiteralExpression_Never(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "false()", "BT-00-Text", "NEVER");
  }

  // #endregion: Boolean expressions

  // #region: Quantified expressions ------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringQuantifiedExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in ('a','b','c') satisfies $x <= 'a'", "ND-Root",
        "every text:$x in ('a', 'b', 'c') satisfies $x <= 'a'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringQuantifiedExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/TextField satisfies $x <= 'a'", "ND-Root",
        "every text:$x in BT-00-Text satisfies $x <= 'a'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanQuantifiedExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in (true(),false(),true()) satisfies $x", "ND-Root",
        "every indicator:$x in (TRUE, FALSE, ALWAYS) satisfies $x");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanQuantifiedExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/IndicatorField satisfies $x", "ND-Root",
        "every indicator:$x in BT-00-Indicator satisfies $x");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericQuantifiedExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "every $x in (1,2,3) satisfies $x <= 1",
        "ND-Root", "every number:$x in (1, 2, 3) satisfies $x <= 1");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericQuantifiedExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/NumberField satisfies $x <= 1", "ND-Root",
        "every number:$x in BT-00-Number satisfies $x <= 1");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateQuantifiedExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) satisfies $x <= xs:date('2012-01-01Z')",
        "ND-Root",
        "every date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) satisfies $x <= 2012-01-01Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateQuantifiedExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/StartDateField satisfies $x <= xs:date('2012-01-01Z')", "ND-Root",
        "every date:$x in BT-00-StartDate satisfies $x <= 2012-01-01Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateQuantifiedExpression_UsingMultipleIterators(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/StartDateField, $y in ($x,xs:date('2022-02-02Z')), $i in (true(),true()) satisfies $x <= xs:date('2012-01-01Z')",
        "ND-Root",
        "every date:$x in BT-00-StartDate, date:$y in ($x, 2022-02-02Z), indicator:$i in (ALWAYS, TRUE) satisfies $x <= 2012-01-01Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeQuantifiedExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in (xs:time('00:00:00Z'),xs:time('00:00:01Z'),xs:time('00:00:02Z')) satisfies $x <= xs:time('00:00:00Z')",
        "ND-Root", "every time:$x in (00:00:00Z, 00:00:01Z, 00:00:02Z) satisfies $x <= 00:00:00Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeQuantifiedExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/StartTimeField satisfies $x <= xs:time('00:00:00Z')", "ND-Root",
        "every time:$x in BT-00-StartTime satisfies $x <= 00:00:00Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationQuantifiedExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P3D')) satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "every measure:$x in (P1D, P2D, P3D) satisfies $x <= P1D");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationQuantifiedExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "every $x in PathNode/MeasureField satisfies boolean(for $T in (current-date()) return ($T + $x <= $T + xs:dayTimeDuration('P1D')))",
        "ND-Root", "every measure:$x in BT-00-Measure satisfies $x <= P1D");
  }

  // #endregion: Quantified expressions

  // #region: Conditional expressions -----------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "(if 1 > 2 then 'a' else 'b')", "ND-Root",
        "if 1 > 2 then 'a' else 'b'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalStringExpression_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "(if 'a' > 'b' then 'a' else 'b')", "ND-Root",
        "if 'a' > 'b' then 'a' else 'b'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalStringExpression_UsingFieldReferenceInCondition(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(if 'a' > PathNode/TextField/normalize-space(text()) then 'a' else 'b')", "ND-Root",
        "if 'a' > BT-00-Text then 'a' else 'b'");
    testExpressionTranslationWithContext(sdkVersion,
        "(if PathNode/TextField/normalize-space(text()) >= 'a' then 'a' else 'b')", "ND-Root",
        "if BT-00-Text >= 'a' then 'a' else 'b'");
    testExpressionTranslationWithContext(sdkVersion,
        "(if PathNode/TextField/normalize-space(text()) >= PathNode/TextField/normalize-space(text()) then 'a' else 'b')",
        "ND-Root", "if BT-00-Text >= BT-00-Text then 'a' else 'b'");
    testExpressionTranslationWithContext(sdkVersion,
        "(if PathNode/StartDateField/xs:date(text()) >= PathNode/EndDateField/xs:date(text()) then 'a' else 'b')",
        "ND-Root", "if BT-00-StartDate >= BT-00-EndDate then 'a' else 'b'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalStringExpression_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else 'b')", "ND-Root",
        "if 'a' > 'b' then BT-00-Text else 'b'");
    testExpressionTranslationWithContext(sdkVersion,
        "(if 'a' > 'b' then 'a' else PathNode/TextField/normalize-space(text()))", "ND-Root",
        "if 'a' > 'b' then 'a' else BT-00-Text");
    testExpressionTranslationWithContext(sdkVersion,
        "(if 'a' > 'b' then PathNode/TextField/normalize-space(text()) else PathNode/TextField/normalize-space(text()))",
        "ND-Root", "if 'a' > 'b' then BT-00-Text else BT-00-Text");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalStringExpression_UsingFieldReferences_TypeMismatch(final String sdkVersion) {
    assertThrows(ParseCancellationException.class, () -> translateExpressionWithContext(sdkVersion,
        "ND-Root", "if 'a' > 'b' then BT-00-StartDate else BT-00-Text"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalBooleanExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(if PathNode/IndicatorField then true() else false())", "ND-Root",
        "if BT-00-Indicator then TRUE else FALSE");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalNumericExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(if 1 > 2 then 1 else PathNode/NumberField/number())", "ND-Root",
        "if 1 > 2 then 1 else BT-00-Number");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalDateExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(if xs:date('2012-01-01Z') > PathNode/EndDateField/xs:date(text()) then PathNode/StartDateField/xs:date(text()) else xs:date('2012-01-02Z'))",
        "ND-Root", "if 2012-01-01Z > BT-00-EndDate then BT-00-StartDate else 2012-01-02Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalTimeExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(if PathNode/EndTimeField/xs:time(text()) > xs:time('00:00:01Z') then PathNode/StartTimeField/xs:time(text()) else xs:time('00:00:01Z'))",
        "ND-Root", "if BT-00-EndTime > 00:00:01Z then BT-00-StartTime else 00:00:01Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConditionalDurationExpression(final String sdkVersion) {
    assertEquals(
        "(if boolean(for $T in (current-date()) return ($T + xs:dayTimeDuration('P1D') > $T + (for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())))) then xs:dayTimeDuration('P1D') else xs:dayTimeDuration('P2D'))",
        translateExpressionWithContext(sdkVersion, "ND-Root",
            "if P1D > BT-00-Measure then P1D else P2D"));
  }

  // #endregion: Conditional expressions

  // #region: Iteration expressions -------------------------------------------

  // Strings from iteration ---------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromStringIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in ('a','b','c') return concat($x, 'text'))", "ND-Root",
        "'a' in (for text:$x in ('a', 'b', 'c') return concat($x, 'text'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsSequenceFromIteration_UsingMultipleIterators(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in ('a','b','c'), $y in (1,2), $z in PathNode/IndicatorField return concat($x, format-number($y, '0.##########'), 'text'))",
        "ND-Root",
        "'a' in (for text:$x in ('a', 'b', 'c'), number:$y in (1, 2), indicator:$z in BT-00-Indicator return concat($x, string($y), 'text'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsSequenceFromIteration_UsingObjectVariable(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "for $n in PathNode/TextField[../NumberField], $d in $n/../StartDateField return 'text'",
        "ND-Root",
        "for context:$n in BT-00-Text[BT-00-Number is present], date:$d in $n::BT-00-StartDate return 'text'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsSequenceFromIteration_UsingNodeContextVariable(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "for $n in .[PathNode/TextField/normalize-space(text()) = 'a'] return 'text'", "ND-Root",
        "for context:$n in ND-Root[BT-00-Text == 'a'] return 'text'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromStringIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in PathNode/TextField return concat($x, 'text'))", "ND-Root",
        "'a' in (for text:$x in BT-00-Text return concat($x, 'text'))");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromBooleanIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in (true(),false()) return 'y')", "ND-Root",
        "'a' in (for indicator:$x in (TRUE, FALSE) return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromBooleanIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in PathNode/IndicatorField return 'y')", "ND-Root",
        "'a' in (for indicator:$x in BT-00-Indicator return 'y')");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromNumericIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "'a' = (for $x in (1,2,3) return 'y')",
        "ND-Root", "'a' in (for number:$x in (1, 2, 3) return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromNumericIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in PathNode/NumberField return 'y')", "ND-Root",
        "'a' in (for number:$x in BT-00-Number return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromDateIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return 'y')",
        "ND-Root", "'a' in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromDateIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in PathNode/StartDateField return 'y')", "ND-Root",
        "'a' in (for date:$x in BT-00-StartDate return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromTimeIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return 'y')",
        "ND-Root", "'a' in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromTimeIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in PathNode/StartTimeField return 'y')", "ND-Root",
        "'a' in (for time:$x in BT-00-StartTime return 'y')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromDurationIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 'y')",
        "ND-Root", "'a' in (for measure:$x in (P1D, P1Y, P2M) return 'y')");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringsFromDurationIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "'a' = (for $x in PathNode/MeasureField return 'y')", "ND-Root",
        "'a' in (for measure:$x in BT-00-Measure return 'y')");
  }

  // Numbers from iteration ---------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromStringIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in ('a','b','c') return number($x))", "ND-Root",
        "123 in (for text:$x in ('a', 'b', 'c') return number($x))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromStringIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in PathNode/TextField return number($x))", "ND-Root",
        "123 in (for text:$x in BT-00-Text return number($x))");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromBooleanIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "123 = (for $x in (true(),false()) return 0)",
        "ND-Root", "123 in (for indicator:$x in (TRUE, FALSE) return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromBooleanIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in PathNode/IndicatorField return 0)", "ND-Root",
        "123 in (for indicator:$x in BT-00-Indicator return 0)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromNumericIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "123 = (for $x in (1,2,3) return 0)",
        "ND-Root", "123 in (for number:$x in (1, 2, 3) return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromNumericIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in PathNode/NumberField return 0)", "ND-Root",
        "123 in (for number:$x in BT-00-Number return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromDateIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return 0)",
        "ND-Root", "123 in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromDateIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in PathNode/StartDateField return 0)", "ND-Root",
        "123 in (for date:$x in BT-00-StartDate return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromTimeIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return 0)",
        "ND-Root", "123 in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromTimeIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in PathNode/StartTimeField return 0)", "ND-Root",
        "123 in (for time:$x in BT-00-StartTime return 0)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromDurationIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return 0)",
        "ND-Root", "123 in (for measure:$x in (P1D, P1Y, P2M) return 0)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumbersFromDurationIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "123 = (for $x in PathNode/MeasureField return 0)", "ND-Root",
        "123 in (for measure:$x in BT-00-Measure return 0)");
  }

  // Dates from iteration ---------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromStringIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in ('a','b','c') return xs:date($x))", "ND-Root",
        "2022-01-01Z in (for text:$x in ('a', 'b', 'c') return date($x))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromStringIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in PathNode/TextField return xs:date($x))", "ND-Root",
        "2022-01-01Z in (for text:$x in BT-00-Text return date($x))");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromBooleanIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in (true(),false()) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for indicator:$x in (TRUE, FALSE) return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromBooleanIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in PathNode/IndicatorField return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for indicator:$x in BT-00-Indicator return 2022-01-01Z)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromNumericIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in (1,2,3) return xs:date('2022-01-01Z'))", "ND-Root",
        "2022-01-01Z in (for number:$x in (1, 2, 3) return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromNumericIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in PathNode/NumberField return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for number:$x in BT-00-Number return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromDateIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return xs:date('2022-01-01Z'))",
        "ND-Root",
        "2022-01-01Z in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromDateIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in PathNode/StartDateField return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for date:$x in BT-00-StartDate return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromTimeIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return xs:date('2022-01-01Z'))",
        "ND-Root",
        "2022-01-01Z in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromTimeIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in PathNode/StartTimeField return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for time:$x in BT-00-StartTime return 2022-01-01Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromDurationIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for measure:$x in (P1D, P1Y, P2M) return 2022-01-01Z)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDatesFromDurationIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (for $x in PathNode/MeasureField return xs:date('2022-01-01Z'))",
        "ND-Root", "2022-01-01Z in (for measure:$x in BT-00-Measure return 2022-01-01Z)");
  }

  // Times from iteration ---------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromStringIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in ('a','b','c') return xs:time($x))", "ND-Root",
        "12:00:00Z in (for text:$x in ('a', 'b', 'c') return time($x))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromStringIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in PathNode/TextField return xs:time($x))", "ND-Root",
        "12:00:00Z in (for text:$x in BT-00-Text return time($x))");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromBooleanIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in (true(),false()) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for indicator:$x in (TRUE, FALSE) return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromBooleanIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in PathNode/IndicatorField return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for indicator:$x in BT-00-Indicator return 12:00:00Z)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromNumericIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in (1,2,3) return xs:time('12:00:00Z'))", "ND-Root",
        "12:00:00Z in (for number:$x in (1, 2, 3) return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromNumericIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in PathNode/NumberField return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for number:$x in BT-00-Number return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromDateIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return xs:time('12:00:00Z'))",
        "ND-Root",
        "12:00:00Z in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromDateIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in PathNode/StartDateField return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for date:$x in BT-00-StartDate return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromTimeIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return xs:time('12:00:00Z'))",
        "ND-Root",
        "12:00:00Z in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromTimeIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in PathNode/StartTimeField return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for time:$x in BT-00-StartTime return 12:00:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromDurationIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for measure:$x in (P1D, P1Y, P2M) return 12:00:00Z)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimesFromDurationIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:00:00Z') = (for $x in PathNode/MeasureField return xs:time('12:00:00Z'))",
        "ND-Root", "12:00:00Z in (for measure:$x in BT-00-Measure return 12:00:00Z)");
  }

  // Durations from iteration ---------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromStringIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:dayTimeDuration('P2D'),xs:dayTimeDuration('P7D')) return $x)",
        "ND-Root", "P1D in (for measure:$x in (P1D, P2D, P1W) return $x)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromStringIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/TextField return xs:dayTimeDuration($x))",
        "ND-Root", "P1D in (for text:$x in BT-00-Text return day-time-duration($x))");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromBooleanIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in (true(),false()) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for indicator:$x in (TRUE, FALSE) return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromBooleanIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/IndicatorField return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for indicator:$x in BT-00-Indicator return P1D)");
  }


  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromNumericIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in (1,2,3) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for number:$x in (1, 2, 3) return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromNumericIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/NumberField return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for number:$x in BT-00-Number return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromDateIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in (xs:date('2012-01-01Z'),xs:date('2012-01-02Z'),xs:date('2012-01-03Z')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for date:$x in (2012-01-01Z, 2012-01-02Z, 2012-01-03Z) return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromDateIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartDateField return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for date:$x in BT-00-StartDate return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromTimeIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in (xs:time('12:00:00Z'),xs:time('12:00:01Z'),xs:time('12:00:02Z')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for time:$x in (12:00:00Z, 12:00:01Z, 12:00:02Z) return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromTimeIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/StartTimeField return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for time:$x in BT-00-StartTime return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromDurationIteration_UsingLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in (xs:dayTimeDuration('P1D'),xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2M')) return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for measure:$x in (P1D, P1Y, P2M) return P1D)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationsFromDurationIteration_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:dayTimeDuration('P1D') = (for $x in PathNode/MeasureField return xs:dayTimeDuration('P1D'))",
        "ND-Root", "P1D in (for measure:$x in BT-00-Measure return P1D)");
  }

  // #endregion: Iteration expressions

  // #region: Numeric expressions ---------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testMultiplicationExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "3 * 4", "BT-00-Text", "3 * 4");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testAdditionExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "4 + 4", "BT-00-Text", "4 + 4");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParenthesizedNumericExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "(2 + 2) * 4", "BT-00-Text", "(2 + 2)*4");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericLiteralExpression(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "3.1415", "BT-00-Text", "3.1415");
  }

  // #endregion: Numeric expressions

  // #region: Lists -----------------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringList(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "'a' = ('a','b','c')", "BT-00-Text",
        "'a' in ('a', 'b', 'c')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericList_UsingNumericLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "4 = (1,2,3)", "BT-00-Text", "4 in (1, 2, 3)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericList_UsingNumericField(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "4 = (1,../NumberField/number(),3)",
        "BT-00-Text", "4 in (1, BT-00-Number, 3)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumericList_UsingTextField(final String sdkVersion) {
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext(sdkVersion, "BT-00-Text", "4 in (1, BT-00-Text, 3)"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testBooleanList(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "false() = (true(),PathNode/IndicatorField,true())", "ND-Root",
        "NEVER in (TRUE, BT-00-Indicator, ALWAYS)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateList(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date('2022-01-01Z') = (xs:date('2022-01-02Z'),PathNode/StartDateField/xs:date(text()),xs:date('2022-02-02Z'))",
        "ND-Root", "2022-01-01Z in (2022-01-02Z, BT-00-StartDate, 2022-02-02Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeList(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time('12:20:21Z') = (xs:time('12:30:00Z'),PathNode/StartTimeField/xs:time(text()),xs:time('13:40:00Z'))",
        "ND-Root", "12:20:21Z in (12:30:00Z, BT-00-StartTime, 13:40:00Z)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationList_UsingDurationLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:yearMonthDuration('P3M') = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        "BT-00-Text", "P3M in (P1M, P3M, P6M)");
  }



  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDurationList_UsingDurationField(final String sdkVersion) {
    assertEquals(
        "(for $F in ../MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ())) = (xs:yearMonthDuration('P1M'),xs:yearMonthDuration('P3M'),xs:yearMonthDuration('P6M'))",
        translateExpressionWithContext(sdkVersion, "BT-00-Text",
            "BT-00-Measure in (P1M, P3M, P6M)"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCodeList(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "'a' = ('code1','code2','code3')",
        "BT-00-Text", "'a' in codelist:accessibility");
  }

  // #endregion: Lists

  // #region: References ------------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldAttributeValueReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "PathNode/TextField/@Attribute = 'text'",
        "ND-Root", "BT-00-Attribute == 'text'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldAttributeValueReference_SameElementContext(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "@Attribute = 'text'", "BT-00-Text",
        "BT-00-Attribute == 'text'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testScalarFromAttributeReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "PathNode/CodeField/@listName", "ND-Root",
        "BT-00-Code/@listName");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testScalarFromAttributeReference_SameElementContext(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "./@listName", "BT-00-Code",
        "BT-00-Code/@listName");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceWithPredicate(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "PathNode/IndicatorField['a' = 'a']",
        "ND-Root", "BT-00-Indicator['a' == 'a']");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceWithPredicate_WithFieldReferenceInPredicate(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/IndicatorField[../CodeField/normalize-space(text()) = 'a']", "ND-Root",
        "BT-00-Indicator[BT-00-Code == 'a']");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceInOtherNotice(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "fn:doc(concat($urlPrefix, 'da4d46e9-490b-41ff-a2ae-8166d356a619'))/*/PathNode/TextField/normalize-space(text())",
        "ND-Root", "notice('da4d46e9-490b-41ff-a2ae-8166d356a619')/BT-00-Text");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceWithFieldContextOverride(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "../TextField/normalize-space(text())",
        "BT-00-Code", "BT-01-SubLevel-Text::BT-00-Text");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceWithFieldContextOverride_WithIntegerField(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "../IntegerField/number()", "BT-00-Code",
        "BT-01-SubLevel-Text::integer");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceWithNodeContextOverride(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "../../PathNode/IntegerField/number()",
        "BT-00-Text", "ND-Root::integer");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReferenceWithNodeContextOverride_WithPredicate(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "../../PathNode/IntegerField/number()",
        "BT-00-Text", "ND-Root[BT-00-Indicator == TRUE]::integer");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testAbsoluteFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "/*/PathNode/IndicatorField", "BT-00-Text",
        "/BT-00-Indicator");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSimpleFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "../IndicatorField", "BT-00-Text",
        "BT-00-Indicator");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReference_ForDurationFields(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "(for $F in PathNode/MeasureField return (if ($F/@unitCode='WEEK') then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D')) else if ($F/@unitCode='DAY') then xs:dayTimeDuration(concat('P', $F/number(), 'D')) else if ($F/@unitCode='YEAR') then xs:yearMonthDuration(concat('P', $F/number(), 'Y')) else if ($F/@unitCode='MONTH') then xs:yearMonthDuration(concat('P', $F/number(), 'M')) else ()))",
        "ND-Root", "BT-00-Measure");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFieldReference_WithAxis(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "./preceding::PathNode/IntegerField/number()",
        "ND-Root", "ND-Root::preceding::integer");
  }

  // #endregion: References

  // #region: Boolean functions -----------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNotFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "not(true())", "BT-00-Text", "not(ALWAYS)");
    testExpressionTranslationWithContext(sdkVersion, "not(1 + 1 = 2)", "BT-00-Text",
        "not(1 + 1 == 2)");
    assertThrows(ParseCancellationException.class,
        () -> translateExpressionWithContext(sdkVersion, "BT-00-Text", "not('text')"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testContainsFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "contains(PathNode/TextField/normalize-space(text()), 'xyz')", "ND-Root",
        "contains(BT-00-Text, 'xyz')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStartsWithFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "starts-with(PathNode/TextField/normalize-space(text()), 'abc')", "ND-Root",
        "starts-with(BT-00-Text, 'abc')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testEndsWithFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "ends-with(PathNode/TextField/normalize-space(text()), 'abc')", "ND-Root",
        "ends-with(BT-00-Text, 'abc')");
  }

  // #endregion: Boolean functions

  // #region: Numeric functions -----------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCountFunction_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "count(PathNode/TextField)", "ND-Root",
        "count(BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testCountFunction_UsingSequenceFromIteration(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "count(for $x in PathNode/TextField return concat($x, '-xyz'))", "ND-Root",
        "count(for text:$x in BT-00-Text return concat($x, '-xyz'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testNumberFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "number(PathNode/TextField/normalize-space(text()))", "ND-Root", "number(BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSumFunction_UsingFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "sum(PathNode/NumberField)", "ND-Root",
        "sum(BT-00-Number)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSumFunction_UsingNumericSequenceFromIteration(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "sum(for $v in PathNode/NumberField return $v + 1)", "ND-Root",
        "sum(for number:$v in BT-00-Number return $v +1)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringLengthFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "string-length(PathNode/TextField/normalize-space(text()))", "ND-Root",
        "string-length(BT-00-Text)");
  }

  // #endregion: Numeric functions

  // #region: String functions ------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSubstringFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "substring(PathNode/TextField/normalize-space(text()), 1, 3)", "ND-Root",
        "substring(BT-00-Text, 1, 3)");
    testExpressionTranslationWithContext(sdkVersion,
        "substring(PathNode/TextField/normalize-space(text()), 4)", "ND-Root",
        "substring(BT-00-Text, 4)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testToStringFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "format-number(123, '0.##########')",
        "ND-Root", "string(123)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testConcatFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "concat('abc', 'def')", "ND-Root",
        "concat('abc', 'def')");
  };

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringJoinFunction_withLiterals(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "string-join(('abc','def'), ',')", "ND-Root",
        "string-join(('abc', 'def'), ',')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testStringJoinFunction_withFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "string-join(PathNode/TextField, ',')",
        "ND-Root", "string-join(BT-00-Text, ',')");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testFormatNumberFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "format-number(PathNode/NumberField/number(), '#,##0.00')", "ND-Root",
        "format-number(BT-00-Number, '#,##0.00')");
  }

  // #endregion: String functions

  // #region: Date functions --------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDateFromStringFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:date(PathNode/TextField/normalize-space(text()))", "ND-Root", "date(BT-00-Text)");
  }

  // #endregion: Date functions

  // #region: Time functions --------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testTimeFromStringFunction(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "xs:time(PathNode/TextField/normalize-space(text()))", "ND-Root", "time(BT-00-Text)");
  }

  // #endregion: Time functions

  // #region: Sequence Functions ----------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDistinctValuesFunction_WithStringSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "distinct-values(('one','two','one'))",
        "ND-Root", "distinct-values(('one', 'two', 'one'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDistinctValuesFunction_WithNumberSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "distinct-values((1,2,3,2,3,4))", "ND-Root",
        "distinct-values((1, 2, 3, 2, 3, 4))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDistinctValuesFunction_WithDateSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'),xs:date('2018-01-01Z'),xs:date('2022-01-02Z')))",
        "ND-Root", "distinct-values((2018-01-01Z, 2020-01-01Z, 2018-01-01Z, 2022-01-02Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDistinctValuesFunction_WithTimeSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'),xs:time('12:00:00Z'),xs:time('14:00:00Z')))",
        "ND-Root", "distinct-values((12:00:00Z, 13:00:00Z, 12:00:00Z, 14:00:00Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDistinctValuesFunction_WithBooleanSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((true(),false(),false(),false()))", "ND-Root",
        "distinct-values((TRUE, FALSE, FALSE, NEVER))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testDistinctValuesFunction_WithFieldReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "distinct-values(PathNode/TextField)",
        "ND-Root", "distinct-values(BT-00-Text)");
  }

  // #region: Union

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithStringSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((('one','two'), ('two','three','four')))", "ND-Root",
        "value-union(('one', 'two'), ('two', 'three', 'four'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithNumberSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "distinct-values(((1,2,3), (2,3,4)))",
        "ND-Root", "value-union((1, 2, 3), (2, 3, 4))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithDateSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(((xs:date('2018-01-01Z'),xs:date('2020-01-01Z')), (xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))))",
        "ND-Root", "value-union((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithTimeSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(((xs:time('12:00:00Z'),xs:time('13:00:00Z')), (xs:time('12:00:00Z'),xs:time('14:00:00Z'))))",
        "ND-Root", "value-union((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithBooleanSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(((true(),false()), (false(),false())))", "ND-Root",
        "value-union((TRUE, FALSE), (FALSE, NEVER))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithFieldReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((PathNode/TextField, PathNode/TextField))", "ND-Root",
        "value-union(BT-00-Text, BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testUnionFunction_WithTypeMismatch(final String sdkVersion) {
    assertThrows(ParseCancellationException.class, () -> translateExpressionWithContext(sdkVersion,
        "ND-Root", "value-union(BT-00-Text, BT-00-Number)"));
  }

  // #endregion: Union

  // #region: Intersect

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithStringSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(('one','two')[.= ('two','three','four')])", "ND-Root",
        "value-intersect(('one', 'two'), ('two', 'three', 'four'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithNumberSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "distinct-values((1,2,3)[.= (2,3,4)])",
        "ND-Root", "value-intersect((1, 2, 3), (2, 3, 4))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithDateSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'))[.= (xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))])",
        "ND-Root", "value-intersect((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithTimeSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'))[.= (xs:time('12:00:00Z'),xs:time('14:00:00Z'))])",
        "ND-Root", "value-intersect((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithBooleanSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((true(),false())[.= (false(),false())])", "ND-Root",
        "value-intersect((TRUE, FALSE), (FALSE, NEVER))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithFieldReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(PathNode/TextField[.= PathNode/TextField])", "ND-Root",
        "value-intersect(BT-00-Text, BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIntersectFunction_WithTypeMismatch(final String sdkVersion) {
    assertThrows(ParseCancellationException.class, () -> translateExpressionWithContext(sdkVersion,
        "ND-Root", "value-intersect(BT-00-Text, BT-00-Number)"));
  }

  // #endregion: Intersect

  // #region: Except

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithStringSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(('one','two')[not(. = ('two','three','four'))])", "ND-Root",
        "value-except(('one', 'two'), ('two', 'three', 'four'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithNumberSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "distinct-values((1,2,3)[not(. = (2,3,4))])",
        "ND-Root", "value-except((1, 2, 3), (2, 3, 4))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithDateSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'))[not(. = (xs:date('2018-01-01Z'),xs:date('2022-01-02Z')))])",
        "ND-Root", "value-except((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithTimeSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((xs:time('12:00:00Z'),xs:time('13:00:00Z'))[not(. = (xs:time('12:00:00Z'),xs:time('14:00:00Z')))])",
        "ND-Root", "value-except((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithBooleanSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values((true(),false())[not(. = (false(),false()))])", "ND-Root",
        "value-except((TRUE, FALSE), (FALSE, NEVER))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithFieldReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "distinct-values(PathNode/TextField[not(. = PathNode/TextField)])", "ND-Root",
        "value-except(BT-00-Text, BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testExceptFunction_WithTypeMismatch(final String sdkVersion) {
    assertThrows(ParseCancellationException.class, () -> translateExpressionWithContext(sdkVersion,
        "ND-Root", "value-except(BT-00-Text, BT-00-Number)"));
  }

  // #endregion: Except

  // #region: Compare sequences

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithStringSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "deep-equal(sort(('one','two')), sort(('two','three','four')))", "ND-Root",
        "sequence-equal(('one', 'two'), ('two', 'three', 'four'))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithNumberSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "deep-equal(sort((1,2,3)), sort((2,3,4)))",
        "ND-Root", "sequence-equal((1, 2, 3), (2, 3, 4))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithDateSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "deep-equal(sort((xs:date('2018-01-01Z'),xs:date('2020-01-01Z'))), sort((xs:date('2018-01-01Z'),xs:date('2022-01-02Z'))))",
        "ND-Root", "sequence-equal((2018-01-01Z, 2020-01-01Z), (2018-01-01Z, 2022-01-02Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithTimeSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "deep-equal(sort((xs:time('12:00:00Z'),xs:time('13:00:00Z'))), sort((xs:time('12:00:00Z'),xs:time('14:00:00Z'))))",
        "ND-Root", "sequence-equal((12:00:00Z, 13:00:00Z), (12:00:00Z, 14:00:00Z))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithBooleanSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "deep-equal(sort((true(),false())), sort((false(),false())))", "ND-Root",
        "sequence-equal((TRUE, FALSE), (FALSE, NEVER))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithDurationSequences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "deep-equal(sort((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P2Y'))), sort((xs:yearMonthDuration('P1Y'),xs:yearMonthDuration('P3Y'))))",
        "ND-Root", "sequence-equal((P1Y, P2Y), (P1Y, P3Y))");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testSequenceEqualFunction_WithFieldReferences(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "deep-equal(sort(PathNode/TextField), sort(PathNode/TextField))", "ND-Root",
        "sequence-equal(BT-00-Text, BT-00-Text)");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithStringParameter(final String sdkVersion) {
    testExpressionTranslation(sdkVersion, "'hello' = 'world'",
        "{ND-Root, text:$p1, text:$p2} ${$p1 == $p2}", "'hello'", "'world'");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithUnquotedStringParameter(final String sdkVersion) {
    assertThrows(ParseCancellationException.class, () -> translateExpression(sdkVersion,
        "{ND-Root, text:$p1, text:$p2} ${$p1 == $p2}", "hello", "world"));
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithNumberParameter(final String sdkVersion) {
    testExpressionTranslation(sdkVersion, "1 = 2",
        "{ND-Root, number:$p1, number:$p2} ${$p1 == $p2}", "1", "2");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithDateParameter(final String sdkVersion) {
    testExpressionTranslation(sdkVersion, "xs:date('2018-01-01Z') = xs:date('2020-01-01Z')",
        "{ND-Root, date:$p1, date:$p2} ${$p1 == $p2}", "2018-01-01Z", "2020-01-01Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithTimeParameter(final String sdkVersion) {
    testExpressionTranslation(sdkVersion, "xs:time('12:00:00Z') = xs:time('13:00:00Z')",
        "{ND-Root, time:$p1, time:$p2} ${$p1 == $p2}", "12:00:00Z", "13:00:00Z");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithBooleanParameter(final String sdkVersion) {
    testExpressionTranslation(sdkVersion, "true() = false()",
        "{ND-Root, indicator:$p1, indicator:$p2} ${$p1 == $p2}", "ALWAYS", "FALSE");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testParametrizedExpression_WithDurationParameter(final String sdkVersion) {
    testExpressionTranslation(sdkVersion,
        "boolean(for $T in (current-date()) return ($T + xs:yearMonthDuration('P1Y') = $T + xs:yearMonthDuration('P2Y')))",
        "{ND-Root, measure:$p1, measure:$p2} ${$p1 == $p2}", "P1Y", "P2Y");
  }

  // #endregion: Compare sequences

  // #endregion Sequence Functions

  // #region: Indexers --------------------------------------------------------

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIndexer_WithFieldReference(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "PathNode/TextField[1]", "ND-Root",
        "text:BT-00-Text[1]");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIndexer_WithFieldReferenceAndPredicate(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion,
        "PathNode/TextField[./normalize-space(text()) = 'hello'][1]", "ND-Root",
        "text:BT-00-Text[BT-00-Text == 'hello'][1]");
  }

  @ParameterizedTest
  @MethodSource("provideSdkVersions")
  void testIndexer_WithTextSequence(final String sdkVersion) {
    testExpressionTranslationWithContext(sdkVersion, "('a','b','c')[1]", "ND-Root",
        "('a', 'b','c')[1]");
  }

  // #endregion: Indexers
}
