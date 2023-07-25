package eu.europa.ted.efx.xpath;

import static java.util.Map.entry;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.path.NodePathExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

@SdkComponent(versions = {"2"},
    componentType = SdkComponentType.SCRIPT_GENERATOR)
public class XPathScriptGenerator implements ScriptGenerator {

  /**
   * Maps efx operators to xPath operators.
   */
  private static final Map<String, String> operators = Map.ofEntries(entry("+", "+"), //
      entry("-", "-"), //
      entry("*", "*"), //
      entry("/", "div"), //
      entry("%", "mod"), //
      entry("==", "="), //
      entry("!=", "!="), //
      entry("<", "<"), //
      entry("<=", "<="), //
      entry(">", ">"), //
      entry(">=", ">="));

  protected TranslatorOptions translatorOptions;

  public XPathScriptGenerator(TranslatorOptions translatorOptions) {
    this.translatorOptions = translatorOptions;
  }

  @Override
  public PathExpression composeNodeReferenceWithPredicate(PathExpression nodeReference,
      BooleanExpression predicate) {
    return PathExpression.instantiate(nodeReference.getScript() + '[' + predicate.getScript() + ']', EfxDataType.Node.class);
  }

  @Override
  public PathExpression composeFieldReferenceWithPredicate(PathExpression fieldReference,
      BooleanExpression predicate) {
    return PathExpression.instantiate(fieldReference.getScript() + '[' + predicate.getScript() + ']', fieldReference.getDataType());
  }

  @Override
  public PathExpression composeFieldReferenceWithAxis(final PathExpression fieldReference,
      final String axis) {
    return PathExpression.instantiate(XPathContextualizer.addAxis(axis, fieldReference).getScript(), fieldReference.getDataType());
  }

  @Override
  public PathExpression composeFieldValueReference(PathExpression fieldReference) {
    if (fieldReference.is(EfxDataType.String.class)) {
      return PathExpression.instantiate(fieldReference.getScript() + "/normalize-space(text())", fieldReference.getDataType());
    }
    if (fieldReference.is(EfxDataType.Number.class)) {
      return PathExpression.instantiate(fieldReference.getScript() + "/number()", fieldReference.getDataType());
    }
    if (fieldReference.is(EfxDataType.Date.class)) {
      return PathExpression.instantiate(fieldReference.getScript() + "/xs:date(text())", fieldReference.getDataType());
    }
    if (fieldReference.is(EfxDataType.Time.class)) {
      return PathExpression.instantiate(fieldReference.getScript() + "/xs:time(text())", fieldReference.getDataType());
    }
    if (fieldReference.is(EfxDataType.Duration.class)) {
      return PathExpression.instantiate("(for $F in " + fieldReference.getScript() + " return (if ($F/@unitCode='WEEK')" + //
          " then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D'))" + //
          " else if ($F/@unitCode='DAY')" + //
          " then xs:dayTimeDuration(concat('P', $F/number(), 'D'))" + //
          " else if ($F/@unitCode='YEAR')" + //
          " then xs:yearMonthDuration(concat('P', $F/number(), 'Y'))" + //
          " else if ($F/@unitCode='MONTH')" + //
          " then xs:yearMonthDuration(concat('P', $F/number(), 'M'))" + //
          // " else if (" + fieldReference.script + ")" + //
          // " then fn:error('Invalid @unitCode')" + //
          " else ()))", fieldReference.getDataType());
    }

    return PathExpression.instantiate(fieldReference.getScript(), fieldReference.getDataType());
  }

  @Override
  public <T extends PathExpression> T composeFieldAttributeReference(PathExpression fieldReference,
      String attribute, Class<T> type) {
    return Expression.instantiate(
        fieldReference.getScript() + (fieldReference.getScript().isEmpty() ? "" : "/") + "@" + attribute,
        type);
  }

  @Override
  public <T extends TypedExpression> T composeVariableReference(String variableName, Class<T> type) {
    return Expression.instantiate("$" + variableName, type);
  }

  @Override
  public <T extends TypedExpression> T composeVariableDeclaration(String variableName, Class<T> type) {
    return Expression.instantiate("$" + variableName, type);
  }

  @Override
  public <T extends TypedExpression> T composeParameterDeclaration(String parameterName,
      Class<T> type) {
    return Expression.empty(type);
  }

  @Override
  public <T extends SequenceExpression> T composeList(List<? extends ScalarExpression> list,
      Class<T> type) {
    if (list == null || list.isEmpty()) {
      return Expression.instantiate("()", type);
    }

    final StringJoiner joiner = new StringJoiner(",", "(", ")");
    for (final ScalarExpression item : list) {
      joiner.add(item.getScript());
    }
    return Expression.instantiate(joiner.toString(), type);
  }

  @Override
  public NumericExpression getNumericLiteralEquivalent(String literal) {
    return new NumericExpression(literal, true);
  }

  @Override
  public StringExpression getStringLiteralEquivalent(String literal) {
    return new StringExpression(literal, true);
  }

  @Override
  public BooleanExpression getBooleanEquivalent(boolean value) {
    return new BooleanExpression(value ? "true()" : "false()", true);
  }

  @Override
  public DateExpression getDateLiteralEquivalent(String literal) {
    return new DateExpression("xs:date(" + quoted(literal) + ")", true);
  }

  @Override
  public TimeExpression getTimeLiteralEquivalent(String literal) {
    return new TimeExpression("xs:time(" + quoted(literal) + ")", true);
  }

  @Override
  public DurationExpression getDurationLiteralEquivalent(final String literal) {
    if (literal.contains("M") || literal.contains("Y")) {
      return new DurationExpression("xs:yearMonthDuration(" + quoted(literal) + ")", true);
    }
    if (literal.contains("W")) {
      final int weeks = this.getWeeksFromDurationLiteral(literal);
      return new DurationExpression(
          "xs:dayTimeDuration(" + quoted(String.format("P%dD", weeks * 7)) + ")", true);
    }
    return new DurationExpression("xs:dayTimeDuration(" + quoted(literal) + ")", true);
  }

  @Override
  public BooleanExpression composeContainsCondition(
      ScalarExpression needle, SequenceExpression haystack) {
    return new BooleanExpression(String.format("%s = %s", needle.getScript(), haystack.getScript()));
  }

  @Override
  public BooleanExpression composePatternMatchCondition(StringExpression expression,
      String pattern) {
    return new BooleanExpression(
        String.format("fn:matches(normalize-space(%s), %s)", expression.getScript(), pattern));
  }

  @Override
  public BooleanExpression composeAllSatisfy(SequenceExpression list,
      String variableName, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "every " + variableName + " in " + list.getScript() + " satisfies " + booleanExpression.getScript());
  }

  @Override
  public BooleanExpression composeAllSatisfy(
      IteratorListExpression iterators, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "every " + iterators.getScript() + " satisfies " + booleanExpression.getScript());
  }

  @Override
  public BooleanExpression composeAnySatisfies(SequenceExpression list,
      String variableName, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "some " + variableName + " in " + list.getScript() + " satisfies " + booleanExpression.getScript());
  }

  @Override
  public BooleanExpression composeAnySatisfies(
      IteratorListExpression iterators, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "some " + iterators.getScript() + " satisfies " + booleanExpression.getScript());
  }

  @Override
  public <T extends TypedExpression> T composeConditionalExpression(BooleanExpression condition,
      T whenTrue, T whenFalse, Class<T> type) {
    return Expression.instantiate(
        "(if " + condition.getScript() + " then " + whenTrue.getScript() + " else " + whenFalse.getScript() + ")",
        type);
  }

  @Override
  public <T1 extends SequenceExpression, T2 extends SequenceExpression> T2 composeForExpression(
      String variableName, T1 sourceList, ScalarExpression expression, Class<T2> targetListType) {
    return Expression.instantiate(
        "for " + variableName + " in " + sourceList.getScript() + " return " + expression.getScript(),
        targetListType);
  }

  @Override
  public <T extends SequenceExpression> T composeForExpression(
      IteratorListExpression iterators, ScalarExpression expression, Class<T> targetListType) {
    return Expression.instantiate("for " + iterators.getScript() + " return " + expression.getScript(),
        targetListType);
  }

  @Override
  public IteratorExpression composeIteratorExpression(Expression variableDeclarationExpression, SequenceExpression sourceList) {
    return new IteratorExpression(variableDeclarationExpression.getScript() + " in " + sourceList.getScript());
  }

  // @Override
  // public IteratorExpression composeIteratorExpression(
  //     String variableName, EfxPathExpression pathExpression) {
  //   return new IteratorExpression(variableName + " in " + pathExpression.getScript());
  // }

  @Override
  public IteratorListExpression composeIteratorList(List<IteratorExpression> iterators) {
    return new IteratorListExpression(
        iterators.stream().map(i -> i.getScript()).collect(Collectors.joining(", ", "", "")));
  }

  @Override
  public <T extends Expression> T composeParenthesizedExpression(T expression, Class<T> type) {
    try {
      Constructor<T> ctor = type.getConstructor(String.class);
      return ctor.newInstance("(" + expression.getScript() + ")");
    } catch (Exception e) {
      throw new ParseCancellationException(e);
    }
  }

  @Override
  public PathExpression composeExternalReference(StringExpression externalReference) {
    return new NodePathExpression(
        "fn:doc(concat($urlPrefix, " + externalReference.getScript() + "))");
  }


  @Override
  public PathExpression composeFieldInExternalReference(PathExpression externalReference,
      PathExpression fieldReference) {
    return PathExpression.instantiate(externalReference.getScript() + fieldReference.getScript(), fieldReference.getDataType());
  }


  @Override
  public PathExpression joinPaths(final PathExpression first, final PathExpression second) {
    return XPathContextualizer.join(first, second);
  }

  //#region Indexers ----------------------------------------------------------

  @Override
  public <T extends ScalarExpression> T composeIndexer(SequenceExpression list,
      NumericExpression index, Class<T> type) {
    return Expression.instantiate(String.format("%s[%s]", list.getScript(), index.getScript()), type);
  }

  //#endregion Indexers -------------------------------------------------------

  //#region Boolean Expressions -----------------------------------------------


  @Override
  public BooleanExpression composeLogicalAnd(BooleanExpression leftOperand,
      BooleanExpression rightOperand) {
    return new BooleanExpression(
        String.format("%s and %s", leftOperand.getScript(), rightOperand.getScript()));
  }

  @Override
  public BooleanExpression composeLogicalOr(BooleanExpression leftOperand,
      BooleanExpression rightOperand) {
    return new BooleanExpression(
        String.format("%s or %s", leftOperand.getScript(), rightOperand.getScript()));
  }

  @Override
  public BooleanExpression composeLogicalNot(BooleanExpression condition) {
    return new BooleanExpression(String.format("not(%s)", condition.getScript()));
  }

  @Override
  public BooleanExpression composeExistsCondition(PathExpression reference) {
    return new BooleanExpression(reference.getScript());
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(PathExpression needle,
      PathExpression haystack) {
    return new BooleanExpression("count(for $x in " + needle.getScript() + ", $y in " + haystack.getScript()
        + "[. = $x] return $y) = 1");
  }

  //#endregion Boolean Expressions ------------------------------------------

  //#region Boolean functions -----------------------------------------------

  @Override
  public BooleanExpression composeContainsCondition(StringExpression haystack,
      StringExpression needle) {
    return new BooleanExpression("contains(" + haystack.getScript() + ", " + needle.getScript() + ")");
  }

  @Override
  public BooleanExpression composeStartsWithCondition(StringExpression text,
      StringExpression startsWith) {
    return new BooleanExpression("starts-with(" + text.getScript() + ", " + startsWith.getScript() + ")");
  }

  @Override
  public BooleanExpression composeEndsWithCondition(StringExpression text,
      StringExpression endsWith) {
    return new BooleanExpression("ends-with(" + text.getScript() + ", " + endsWith.getScript() + ")");
  }

  @Override
  public BooleanExpression composeComparisonOperation(ScalarExpression leftOperand, String operator,
      ScalarExpression rightOperand) {
    if (leftOperand.is(EfxDataType.Duration.class)) {
      // TODO: Improve this implementation; Check if both are dayTime or yearMonth and compare
      // directly, otherwise, compare by adding to current-date()
      return new BooleanExpression(
          "boolean(for $T in (current-date()) return ($T + " + leftOperand.getScript() + " "
              + operators.get(operator) + " $T + " + rightOperand.getScript() + "))");
    }
    return new BooleanExpression(
        leftOperand.getScript() + " " + operators.get(operator) + " " + rightOperand.getScript());
  }

  @Override
  public BooleanExpression composeSequenceEqualFunction(SequenceExpression one,
      SequenceExpression two) {
    return new BooleanExpression("deep-equal(sort(" + one.getScript() + "), sort(" + two.getScript() + "))");
  }

  //#endregion Boolean functions ----------------------------------------------

  //#region Numeric functions -------------------------------------------------

  @Override
  public NumericExpression composeCountOperation(SequenceExpression list) {
    return new NumericExpression("count(" + list.getScript() + ")");
  }

  @Override
  public NumericExpression composeToNumberConversion(StringExpression text) {
    return new NumericExpression("number(" + text.getScript() + ")");
  }

  @Override
  public NumericExpression composeSumOperation(NumericSequenceExpression nodeSet) {
    return new NumericExpression("sum(" + nodeSet.getScript() + ")");
  }

  @Override
  public NumericExpression composeStringLengthCalculation(StringExpression text) {
    return new NumericExpression("string-length(" + text.getScript() + ")");
  }

  @Override
  public NumericExpression composeNumericOperation(NumericExpression leftOperand, String operator,
      NumericExpression rightOperand) {
    return new NumericExpression(
        leftOperand.getScript() + " " + operators.get(operator) + " " + rightOperand.getScript());
  }

  //#endregion Numeric functions ----------------------------------------------

  //#region String functions --------------------------------------------------

  @Override
  public StringExpression composeSubstringExtraction(StringExpression text, NumericExpression start,
      NumericExpression length) {
    return new StringExpression(
        "substring(" + text.getScript() + ", " + start.getScript() + ", " + length.getScript() + ")");
  }

  @Override
  public StringExpression composeSubstringExtraction(StringExpression text,
      NumericExpression start) {
    return new StringExpression("substring(" + text.getScript() + ", " + start.getScript() + ")");
  }

  @Override
  public StringExpression composeToStringConversion(NumericExpression number) {
    String formatString = this.translatorOptions.getDecimalFormat().adaptFormatString("0.##########");
    return new StringExpression("format-number(" + number.getScript() + ", '" + formatString + "')");
  }

  @Override
  public StringExpression composeToUpperCaseConversion(StringExpression text) {
    return new StringExpression("upper-case(" + text.getScript() + ")");
  }

  @Override
  public StringExpression composeToLowerCaseConversion(StringExpression text) {
    return new StringExpression("lower-case(" + text.getScript() + ")");
  }

  @Override
  public StringExpression composeStringConcatenation(List<StringExpression> list) {
    return new StringExpression(
        "concat(" + list.stream().map(i -> i.getScript()).collect(Collectors.joining(", ")) + ")");
  }

  @Override
  public StringExpression composeStringJoin(StringSequenceExpression list, StringExpression separator) {
    return new StringExpression(
        "string-join(" + list.getScript() + ", " + separator.getScript() + ")");
  }

  @Override
  public StringExpression composeNumberFormatting(NumericExpression number,
      StringExpression format) {
        String formatString = format.isLiteral() ? this.translatorOptions.getDecimalFormat().adaptFormatString(format.getScript()) : format.getScript();
        return new StringExpression("format-number(" + number.getScript() + ", " + formatString + ")");
  }

  @Override
  public StringExpression getStringLiteralFromUnquotedString(String value) {
    return new StringExpression("'" + value + "'", true);
  }

  @Override
  public StringExpression getPreferredLanguage(PathExpression fieldReference) {
    return new StringExpression("efx:preferred-language(" + fieldReference.getScript() + ")");
  }

  @Override
  public StringExpression getTextInPreferredLanguage(PathExpression fieldReference) {
    return new StringExpression("efx:preferred-language-text(" + fieldReference.getScript() + ")");
  }

  //#endregion String functions -----------------------------------------------

  //#region Date functions ----------------------------------------------------

  @Override
  public DateExpression composeToDateConversion(StringExpression date) {
    return new DateExpression("xs:date(" + date.getScript() + ")");
  }

  @Override
  public DateExpression composeAddition(DateExpression date, DurationExpression duration) {
    return new DateExpression("(" + date.getScript() + " + " + duration.getScript() + ")");
  }

  @Override
  public DateExpression composeSubtraction(DateExpression date, DurationExpression duration) {
    return new DateExpression("(" + date.getScript() + " - " + duration.getScript() + ")");
  }

  //#endregion Date functions -------------------------------------------------

  //#region Time functions ----------------------------------------------------

  @Override
  public TimeExpression composeToTimeConversion(StringExpression time) {
    return new TimeExpression("xs:time(" + time.getScript() + ")");
  }

  //#endregion Time functions -------------------------------------------------

  //#region Duration functions ------------------------------------------------

  @Override
  public DurationExpression composeToDayTimeDurationConversion(StringExpression text) {
    return new DurationExpression("xs:dayTimeDuration(" + text.getScript() + ")");
  }

  @Override
  public DurationExpression composeToYearMonthDurationConversion(StringExpression text) {
    return new DurationExpression("xs:yearMonthDuration(" + text.getScript() + ")");
  }

  @Override
  public DurationExpression composeSubtraction(DateExpression startDate, DateExpression endDate) {
    return new DurationExpression("xs:dayTimeDuration(" + endDate.getScript() + " " + operators.get("-")
        + " " + startDate.getScript() + ")");
  }

  @Override
  public DurationExpression composeMultiplication(NumericExpression number,
      DurationExpression duration) {
    return new DurationExpression("(" + number.getScript() + " * " + duration.getScript() + ")");
  }

  @Override
  public DurationExpression composeAddition(DurationExpression left, DurationExpression right) {
    return new DurationExpression("(" + left.getScript() + " + " + right.getScript() + ")");
  }

  @Override
  public DurationExpression composeSubtraction(DurationExpression left, DurationExpression right) {
    return new DurationExpression("(" + left.getScript() + " - " + right.getScript() + ")");
  }


  @Override
  public <T extends SequenceExpression> T composeDistinctValuesFunction(
      T list, Class<T> listType) {
    return Expression.instantiate("distinct-values(" + list.getScript() + ")", listType);
  }

  @Override
  public <T extends SequenceExpression> T composeUnionFunction(T listOne,
      T listTwo, Class<T> listType) {
    return Expression
        .instantiate("distinct-values((" + listOne.getScript() + ", " + listTwo.getScript() + "))", listType);
  }

  @Override
  public <T extends SequenceExpression> T composeIntersectFunction(T listOne, T listTwo, Class<T> listType) {
    return Expression.instantiate("distinct-values(for $L1 in " + listOne.getScript() + " return if (some $L2 in " + listTwo.getScript() + " satisfies $L1 = $L2) then $L1 else ())", listType);
  }

  @Override
  public <T extends SequenceExpression> T composeExceptFunction(T listOne, T listTwo, Class<T> listType) {
    return Expression.instantiate("distinct-values(for $L1 in " + listOne.getScript() + " return if (every $L2 in " + listTwo.getScript() + " satisfies $L1 != $L2) then $L1 else ())", listType);
  }

  //#endregion Duration functions ---------------------------------------------

  //#region Helpers -----------------------------------------------------------


  private String quoted(final String text) {
    return "'" + text.replaceAll("\"", "").replaceAll("'", "") + "'";
  }

  private int getWeeksFromDurationLiteral(final String literal) {
    Matcher weeksMatcher = Pattern.compile("(?<=[^0-9])[0-9]+(?=W)").matcher(literal);
    return weeksMatcher.find() ? Integer.parseInt(weeksMatcher.group()) : 0;
  }

  //#endregion Helpers --------------------------------------------------------
}
