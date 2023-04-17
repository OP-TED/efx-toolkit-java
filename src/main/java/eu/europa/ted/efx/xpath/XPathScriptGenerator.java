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
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.IteratorExpression;
import eu.europa.ted.efx.model.Expression.IteratorListExpression;
import eu.europa.ted.efx.model.Expression.ListExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.NumericListExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;

@SdkComponent(versions = {"0.6", "0.7", "1", "2"},
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


  @Override
  public <T extends Expression> T composeNodeReferenceWithPredicate(PathExpression nodeReference,
      BooleanExpression predicate, Class<T> type) {
    return Expression.instantiate(nodeReference.script + '[' + predicate.script + ']', type);
  }

  @Override
  public <T extends Expression> T composeFieldReferenceWithPredicate(PathExpression fieldReference,
      BooleanExpression predicate, Class<T> type) {
    return Expression.instantiate(fieldReference.script + '[' + predicate.script + ']', type);
  }

  @Override
  public <T extends Expression> T composeFieldReferenceWithAxis(final PathExpression fieldReference,
      final String axis, Class<T> type) {
    return Expression.instantiate(XPathContextualizer.addAxis(axis, fieldReference).script, type);
  }

  @Override
  public <T extends Expression> T composeFieldValueReference(PathExpression fieldReference,
      Class<T> type) {

    if (StringExpression.class.isAssignableFrom(type)) {
      return Expression.instantiate(fieldReference.script + "/normalize-space(text())", type);
    }
    if (NumericExpression.class.isAssignableFrom(type)) {
      return Expression.instantiate(fieldReference.script + "/number()", type);
    }
    if (DateExpression.class.isAssignableFrom(type)) {
      return Expression.instantiate(fieldReference.script + "/xs:date(text())", type);
    }
    if (TimeExpression.class.isAssignableFrom(type)) {
      return Expression.instantiate(fieldReference.script + "/xs:time(text())", type);
    }
    if (DurationExpression.class.isAssignableFrom(type)) {
      return Expression
          .instantiate("(for $F in " + fieldReference.script + " return (if ($F/@unitCode='WEEK')" + //
              " then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D'))" + //
              " else if ($F/@unitCode='DAY')" + //
              " then xs:dayTimeDuration(concat('P', $F/number(), 'D'))" + //
              " else if ($F/@unitCode='YEAR')" + //
              " then xs:yearMonthDuration(concat('P', $F/number(), 'Y'))" + //
              " else if ($F/@unitCode='MONTH')" + //
              " then xs:yearMonthDuration(concat('P', $F/number(), 'M'))" + //
              // " else if (" + fieldReference.script + ")" + //
              // " then fn:error('Invalid @unitCode')" + //
              " else ()))", type);
    }

    return Expression.instantiate(fieldReference.script, type);
  }

  @Override
  public <T extends Expression> T composeFieldAttributeReference(PathExpression fieldReference,
      String attribute, Class<T> type) {
    return Expression.instantiate(
        fieldReference.script + (fieldReference.script.isEmpty() ? "" : "/") + "@" + attribute,
        type);
  }

  @Override
  public <T extends Expression> T composeVariableReference(String variableName, Class<T> type) {
    return Expression.instantiate("$" + variableName, type);
  }

  @Override
  public <T extends Expression> T composeVariableDeclaration(String variableName, Class<T> type) {
    return Expression.instantiate("$" + variableName, type);
  }

  @Override
  public <T extends Expression> T composeParameterDeclaration(String parameterName,
      Class<T> type) {
    return Expression.empty(type);
  }

  @Override
  public <T extends Expression, L extends ListExpression<T>> L composeList(List<T> list,
      Class<L> type) {
    if (list == null || list.isEmpty()) {
      return Expression.instantiate("()", type);
    }

    final StringJoiner joiner = new StringJoiner(",", "(", ")");
    for (final T item : list) {
      joiner.add(item.script);
    }
    return Expression.instantiate(joiner.toString(), type);
  }

  @Override
  public NumericExpression getNumericLiteralEquivalent(String literal) {
    return new NumericExpression(literal);
  }

  @Override
  public StringExpression getStringLiteralEquivalent(String literal) {
    return new StringExpression(literal);
  }

  @Override
  public BooleanExpression getBooleanEquivalent(boolean value) {
    return new BooleanExpression(value ? "true()" : "false()");
  }

  @Override
  public DateExpression getDateLiteralEquivalent(String literal) {
    return new DateExpression("xs:date(" + quoted(literal) + ")");
  }

  @Override
  public TimeExpression getTimeLiteralEquivalent(String literal) {
    return new TimeExpression("xs:time(" + quoted(literal) + ")");
  }

  @Override
  public DurationExpression getDurationLiteralEquivalent(final String literal) {
    if (literal.contains("M") || literal.contains("Y")) {
      return new DurationExpression("xs:yearMonthDuration(" + quoted(literal) + ")");
    }
    if (literal.contains("W")) {
      final int weeks = this.getWeeksFromDurationLiteral(literal);
      return new DurationExpression(
          "xs:dayTimeDuration(" + quoted(String.format("P%dD", weeks * 7)) + ")");
    }
    return new DurationExpression("xs:dayTimeDuration(" + quoted(literal) + ")");
  }

  @Override
  public <T extends Expression, L extends ListExpression<T>> BooleanExpression composeContainsCondition(
      T needle, L haystack) {
    return new BooleanExpression(String.format("%s = %s", needle.script, haystack.script));
  }

  @Override
  public BooleanExpression composePatternMatchCondition(StringExpression expression,
      String pattern) {
    return new BooleanExpression(
        String.format("fn:matches(normalize-space(%s), %s)", expression.script, pattern));
  }

  @Override
  public <T extends Expression> BooleanExpression composeAllSatisfy(ListExpression<T> list,
      String variableName, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "every " + variableName + " in " + list.script + " satisfies " + booleanExpression.script);
  }

  @Override
  public <T extends Expression> BooleanExpression composeAllSatisfy(
      IteratorListExpression iterators, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "every " + iterators.script + " satisfies " + booleanExpression.script);
  }

  @Override
  public <T extends Expression> BooleanExpression composeAnySatisfies(ListExpression<T> list,
      String variableName, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "some " + variableName + " in " + list.script + " satisfies " + booleanExpression.script);
  }

  @Override
  public <T extends Expression> BooleanExpression composeAnySatisfies(
      IteratorListExpression iterators, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "some " + iterators.script + " satisfies " + booleanExpression.script);
  }

  @Override
  public <T extends Expression> T composeConditionalExpression(BooleanExpression condition,
      T whenTrue, T whenFalse, Class<T> type) {
    return Expression.instantiate(
        "(if " + condition.script + " then " + whenTrue.script + " else " + whenFalse.script + ")",
        type);
  }

  @Override
  public <T1 extends Expression, L1 extends ListExpression<T1>, T2 extends Expression, L2 extends ListExpression<T2>> L2 composeForExpression(
      String variableName, L1 sourceList, T2 expression, Class<L2> targetListType) {
    return Expression.instantiate(
        "for " + variableName + " in " + sourceList.script + " return " + expression.script,
        targetListType);
  }

  @Override
  public <T2 extends Expression, L2 extends ListExpression<T2>> L2 composeForExpression(
      IteratorListExpression iterators, T2 expression, Class<L2> targetListType) {
    return Expression.instantiate("for " + iterators.script + " return " + expression.script,
        targetListType);
  }

  @Override
  public <T extends Expression, L extends ListExpression<T>> IteratorExpression composeIteratorExpression(
      String variableName, L sourceList) {
    return new IteratorExpression(variableName + " in " + sourceList.script);
  }

  @Override
  public IteratorExpression composeIteratorExpression(
      String variableName, PathExpression pathExpression) {
    return new IteratorExpression(variableName + " in " + pathExpression.script);
  }

  @Override
  public IteratorListExpression composeIteratorList(List<IteratorExpression> iterators) {
    return new IteratorListExpression(
        iterators.stream().map(i -> i.script).collect(Collectors.joining(", ", "", "")));
  }

  @Override
  public <T extends Expression> T composeParenthesizedExpression(T expression, Class<T> type) {
    try {
      Constructor<T> ctor = type.getConstructor(String.class);
      return ctor.newInstance("(" + expression.script + ")");
    } catch (Exception e) {
      throw new ParseCancellationException(e);
    }
  }

  @Override
  public PathExpression composeExternalReference(StringExpression externalReference) {
    return new PathExpression(
        "fn:doc(concat($urlPrefix, " + externalReference.script + "))");
  }


  @Override
  public PathExpression composeFieldInExternalReference(PathExpression externalReference,
      PathExpression fieldReference) {
    return new PathExpression(externalReference.script + fieldReference.script);
  }


  @Override
  public PathExpression joinPaths(final PathExpression first, final PathExpression second) {
    return XPathContextualizer.join(first, second);
  }

  /** Indexers ***/

  @Override
  public <T extends Expression, L extends ListExpression<T>> T composeIndexer(L list,
      NumericExpression index,
      Class<T> type) {
    return Expression.instantiate(String.format("%s[%s]", list.script, index.script), type);
  }

  /*** BooleanExpressions ***/


  @Override
  public BooleanExpression composeLogicalAnd(BooleanExpression leftOperand,
      BooleanExpression rightOperand) {
    return new BooleanExpression(
        String.format("%s and %s", leftOperand.script, rightOperand.script));
  }

  @Override
  public BooleanExpression composeLogicalOr(BooleanExpression leftOperand,
      BooleanExpression rightOperand) {
    return new BooleanExpression(
        String.format("%s or %s", leftOperand.script, rightOperand.script));
  }

  @Override
  public BooleanExpression composeLogicalNot(BooleanExpression condition) {
    return new BooleanExpression(String.format("not(%s)", condition.script));
  }

  @Override
  public BooleanExpression composeExistsCondition(PathExpression reference) {
    return new BooleanExpression(reference.script);
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(PathExpression needle,
      PathExpression haystack) {
    return new BooleanExpression("count(for $x in " + needle.script + ", $y in " + haystack.script
        + "[. = $x] return $y) = 1");
  }

  /*** Boolean functions ***/

  @Override
  public BooleanExpression composeContainsCondition(StringExpression haystack,
      StringExpression needle) {
    return new BooleanExpression("contains(" + haystack.script + ", " + needle.script + ")");
  }

  @Override
  public BooleanExpression composeStartsWithCondition(StringExpression text,
      StringExpression startsWith) {
    return new BooleanExpression("starts-with(" + text.script + ", " + startsWith.script + ")");
  }

  @Override
  public BooleanExpression composeEndsWithCondition(StringExpression text,
      StringExpression endsWith) {
    return new BooleanExpression("ends-with(" + text.script + ", " + endsWith.script + ")");
  }

  @Override
  public BooleanExpression composeComparisonOperation(Expression leftOperand, String operator,
      Expression rightOperand) {
    if (DurationExpression.class.isAssignableFrom(leftOperand.getClass())) {
      // TODO: Improve this implementation; Check if both are dayTime or yearMonth and compare
      // directly, otherwise, compare by adding to current-date()
      return new BooleanExpression(
          "boolean(for $T in (current-date()) return ($T + " + leftOperand.script + " "
              + operators.get(operator) + " $T + " + rightOperand.script + "))");
    }
    return new BooleanExpression(
        leftOperand.script + " " + operators.get(operator) + " " + rightOperand.script);
  }

  @Override
  public BooleanExpression composeSequenceEqualFunction(ListExpression<? extends Expression> one,
      ListExpression<? extends Expression> two) {
    return new BooleanExpression("deep-equal(sort(" + one.script + "), sort(" + two.script + "))");
  }

  /*** Numeric functions ***/

  @Override
  public NumericExpression composeCountOperation(PathExpression nodeSet) {
    return new NumericExpression("count(" + nodeSet.script + ")");
  }

  @Override
  public NumericExpression composeCountOperation(ListExpression<? extends Expression> list) {
    return new NumericExpression("count(" + list.script + ")");
  }

  @Override
  public NumericExpression composeToNumberConversion(StringExpression text) {
    return new NumericExpression("number(" + text.script + ")");
  }

  @Override
  public NumericExpression composeSumOperation(PathExpression nodeSet) {
    return new NumericExpression("sum(" + nodeSet.script + ")");
  }

  @Override
  public NumericExpression composeSumOperation(NumericListExpression nodeSet) {
    return new NumericExpression("sum(" + nodeSet.script + ")");
  }

  @Override
  public NumericExpression composeStringLengthCalculation(StringExpression text) {
    return new NumericExpression("string-length(" + text.script + ")");
  }

  @Override
  public NumericExpression composeNumericOperation(NumericExpression leftOperand, String operator,
      NumericExpression rightOperand) {
    return new NumericExpression(
        leftOperand.script + " " + operators.get(operator) + " " + rightOperand.script);
  }


  /*** String functions ***/

  @Override
  public StringExpression composeSubstringExtraction(StringExpression text, NumericExpression start,
      NumericExpression length) {
    return new StringExpression(
        "substring(" + text.script + ", " + start.script + ", " + length.script + ")");
  }

  @Override
  public StringExpression composeSubstringExtraction(StringExpression text,
      NumericExpression start) {
    return new StringExpression("substring(" + text.script + ", " + start.script + ")");
  }

  @Override
  public StringExpression composeToStringConversion(NumericExpression number) {
    return new StringExpression("format-number(" + number.script + ", '0.##########')");
  }

  @Override
  public StringExpression composeStringConcatenation(List<StringExpression> list) {
    return new StringExpression(
        "concat(" + list.stream().map(i -> i.script).collect(Collectors.joining(", ")) + ")");
  }

  @Override
  public StringExpression composeStringJoin(StringListExpression list, StringExpression separator) {
    return new StringExpression(
        "string-join(" + list.script + ", " + separator.script + ")");
  }

  @Override
  public StringExpression composeNumberFormatting(NumericExpression number,
      StringExpression format) {
    return new StringExpression("format-number(" + number.script + ", " + format.script + ")");
  }

  @Override
  public StringExpression getStringLiteralFromUnquotedString(String value) {
    return new StringExpression("'" + value + "'");
  }


  /*** Date functions ***/

  @Override
  public DateExpression composeToDateConversion(StringExpression date) {
    return new DateExpression("xs:date(" + date.script + ")");
  }

  @Override
  public DateExpression composeAddition(DateExpression date, DurationExpression duration) {
    return new DateExpression("(" + date.script + " + " + duration.script + ")");
  }

  @Override
  public DateExpression composeSubtraction(DateExpression date, DurationExpression duration) {
    return new DateExpression("(" + date.script + " - " + duration.script + ")");
  }

  /*** Time functions ***/

  @Override
  public TimeExpression composeToTimeConversion(StringExpression time) {
    return new TimeExpression("xs:time(" + time.script + ")");
  }


  /*** Duration functions ***/

  @Override
  public DurationExpression composeToDayTimeDurationConversion(StringExpression text) {
    return new DurationExpression("xs:dayTimeDuration(" + text.script + ")");
  }

  @Override
  public DurationExpression composeToYearMonthDurationConversion(StringExpression text) {
    return new DurationExpression("xs:yearMonthDuration(" + text.script + ")");
  }

  @Override
  public DurationExpression composeSubtraction(DateExpression startDate, DateExpression endDate) {
    return new DurationExpression("xs:dayTimeDuration(" + endDate.script + " " + operators.get("-")
        + " " + startDate.script + ")");
  }

  @Override
  public DurationExpression composeMultiplication(NumericExpression number,
      DurationExpression duration) {
    return new DurationExpression("(" + number.script + " * " + duration.script + ")");
  }

  @Override
  public DurationExpression composeAddition(DurationExpression left, DurationExpression right) {
    return new DurationExpression("(" + left.script + " + " + right.script + ")");
  }

  @Override
  public DurationExpression composeSubtraction(DurationExpression left, DurationExpression right) {
    return new DurationExpression("(" + left.script + " - " + right.script + ")");
  }


  @Override
  public <T extends Expression, L extends ListExpression<T>> L composeDistinctValuesFunction(
      L list, Class<L> listType) {
    return Expression.instantiate("distinct-values(" + list.script + ")", listType);
  }

  @Override
  public <T extends Expression, L extends ListExpression<T>> L composeUnionFunction(L listOne,
      L listTwo, Class<L> listType) {
    return Expression
        .instantiate("distinct-values((" + listOne.script + ", " + listTwo.script + "))", listType);
  }

  @Override
  public <T extends Expression, L extends ListExpression<T>> L composeIntersectFunction(L listOne,
      L listTwo, Class<L> listType) {
    return Expression.instantiate(
        "distinct-values(" + listOne.script + "[.= " + listTwo.script + "])", listType);
  }

  @Override
  public <T extends Expression, L extends ListExpression<T>> L composeExceptFunction(L listOne,
      L listTwo, Class<L> listType) {
    return Expression.instantiate(
        "distinct-values(" + listOne.script + "[not(. = " + listTwo.script + ")])", listType);
  }


  /*** Helpers ***/


  private String quoted(final String text) {
    return "'" + text.replaceAll("\"", "").replaceAll("'", "") + "'";
  }

  private int getWeeksFromDurationLiteral(final String literal) {
    Matcher weeksMatcher = Pattern.compile("(?<=[^0-9])[0-9]+(?=W)").matcher(literal);
    return weeksMatcher.find() ? Integer.parseInt(weeksMatcher.group()) : 0;
  }
}
