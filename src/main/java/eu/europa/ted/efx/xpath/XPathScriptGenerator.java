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
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.LiteralExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanLiteral;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateLiteral;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationLiteral;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericLiteral;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringLiteral;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeLiteral;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
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
    return Expression.instantiate(nodeReference.getScript() + '[' + predicate.getScript() + ']', nodeReference.getClass());
  }

  @Override
  public PathExpression composeFieldReferenceWithPredicate(PathExpression fieldReference,
      BooleanExpression predicate) {
    return Expression.instantiate(fieldReference.getScript() + '[' + predicate.getScript() + ']', fieldReference.getClass());
  }

  @Override
  public PathExpression composeFieldValueReference(PathExpression fieldReference) {
    if (fieldReference.is(EfxDataType.String.class)) {
      return Expression.instantiate(fieldReference.getScript() + "/normalize-space(text())", fieldReference.getClass());
    }
    if (fieldReference.is(EfxDataType.Number.class)) {
      return Expression.instantiate(fieldReference.getScript() + "/number()", fieldReference.getClass());
    }
    if (fieldReference.is(EfxDataType.Date.class)) {
      return Expression.instantiate(fieldReference.getScript() + "/xs:date(text())", fieldReference.getClass());
    }
    if (fieldReference.is(EfxDataType.Time.class)) {
      return Expression.instantiate(fieldReference.getScript() + "/xs:time(text())", fieldReference.getClass());
    }
    if (fieldReference.is(EfxDataType.Duration.class)) {
      return Expression.instantiate("(for $F in " + fieldReference.getScript() + " return (if ($F/@unitCode='WEEK')" + //
          " then xs:dayTimeDuration(concat('P', $F/number() * 7, 'D'))" + //
          " else if ($F/@unitCode='DAY')" + //
          " then xs:dayTimeDuration(concat('P', $F/number(), 'D'))" + //
          " else if ($F/@unitCode='YEAR')" + //
          " then xs:yearMonthDuration(concat('P', $F/number(), 'Y'))" + //
          " else if ($F/@unitCode='MONTH')" + //
          " then xs:yearMonthDuration(concat('P', $F/number(), 'M'))" + //
          // " else if (" + fieldReference.script + ")" + //
          // " then fn:error('Invalid @unitCode')" + //
          " else ()))", fieldReference.getClass());
    }

    return Expression.instantiate(fieldReference.getScript(), fieldReference.getClass());
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
  public <T extends TypedExpression> T composeParameterReference(String parameterName, Class<T> type) {
    return Expression.instantiate("$" + parameterName, type);
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
  public <T extends TypedExpression> T composeDictionaryLookup(String dictionaryName, StringExpression keyExpression,
      Class<T> type) {
        return Expression.instantiate(
            String.format("key(%s, %s)", quoted(dictionaryName), keyExpression.getScript()), type);
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
  public NumericLiteral getNumericLiteralEquivalent(String literal) {
    return new NumericLiteral(literal);
  }

  @Override
  public StringLiteral getStringLiteralEquivalent(String literal) {
    return new StringLiteral(literal);
  }

  @Override
  public BooleanLiteral getBooleanEquivalent(boolean value) {
    return new BooleanLiteral(value ? "true()" : "false()");
  }

  @Override
  public DateLiteral getDateLiteralEquivalent(String literal) {
    return new DateLiteral("xs:date(" + quoted(literal) + ")");
  }

  @Override
  public TimeLiteral getTimeLiteralEquivalent(String literal) {
    return new TimeLiteral("xs:time(" + quoted(literal) + ")");
  }

  @Override
  public DurationLiteral getDurationLiteralEquivalent(final String literal) {
    if (literal.contains("M") || literal.contains("Y")) {
      return new DurationLiteral("xs:yearMonthDuration(" + quoted(literal) + ")");
    }
    if (literal.contains("W")) {
      final int weeks = this.getWeeksFromDurationLiteral(literal);
      return new DurationLiteral(
          "xs:dayTimeDuration(" + quoted(String.format("P%dD", weeks * 7)) + ")");
    }
    return new DurationLiteral("xs:dayTimeDuration(" + quoted(literal) + ")");
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
  public BooleanExpression composeAllSatisfy(
      IteratorListExpression iterators, BooleanExpression booleanExpression) {
    return new BooleanExpression(
        "every " + iterators.getScript() + " satisfies " + booleanExpression.getScript());
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
  public <T extends SequenceExpression> T composeForExpression(
      IteratorListExpression iterators, ScalarExpression expression, Class<T> targetListType) {
    return Expression.instantiate("for " + iterators.getScript() + " return " + expression.getScript(),
        targetListType);
  }

  @Override
  public IteratorExpression composeIteratorExpression(Expression variableDeclarationExpression, SequenceExpression sourceList) {
    return new IteratorExpression(variableDeclarationExpression.getScript() + " in " + sourceList.getScript());
  }

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
    return new NodePath(
        "fn:doc(concat($urlPrefix, " + externalReference.getScript() + "))");
  }


  @Override
  public PathExpression composeFieldInExternalReference(PathExpression externalReference,
      PathExpression fieldReference) {
    return Expression.instantiate(externalReference.getScript() + fieldReference.getScript(), fieldReference.getClass());
  }


  @Override
  public PathExpression joinPaths(final PathExpression first, final PathExpression second) {
    return XPathContextualizer.join(first, second);
  }

  @Override
  public PathExpression contextualizePath(final PathExpression absolutePath,
      final PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, absolutePath);
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

  /**
   * EFX 1 uniqueness check - kept for backward compatibility.
   * EFX 2 uses the typed overloads below.
   */
  @Override
  public BooleanExpression composeUniqueValueCondition(PathExpression needle,
      PathExpression haystack) {
    return new BooleanExpression("count(for $x in " + needle.getScript() + ", $y in " + haystack.getScript()
        + "[. = $x] return $y) = 1");
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(StringExpression needle,
      StringSequenceExpression haystack) {
    return composeTypedUniqueValueCondition(needle.getScript(), haystack.getScript());
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(NumericExpression needle,
      NumericSequenceExpression haystack) {
    return composeTypedUniqueValueCondition(needle.getScript(), haystack.getScript());
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(BooleanExpression needle,
      BooleanSequenceExpression haystack) {
    return composeTypedUniqueValueCondition(needle.getScript(), haystack.getScript());
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(DateExpression needle,
      DateSequenceExpression haystack) {
    return composeTypedUniqueValueCondition(needle.getScript(), haystack.getScript());
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(TimeExpression needle,
      TimeSequenceExpression haystack) {
    return composeTypedUniqueValueCondition(needle.getScript(), haystack.getScript());
  }

  @Override
  public BooleanExpression composeUniqueValueCondition(DurationExpression needle,
      DurationSequenceExpression haystack) {
    return composeTypedUniqueValueCondition(needle.getScript(), haystack.getScript());
  }

  private BooleanExpression composeTypedUniqueValueCondition(String needle, String haystack) {
    return new BooleanExpression(
        "count(for $n in " + needle + ", $x in " + haystack + "[. = $n] return $x) = 1");
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
        String formatString = format instanceof LiteralExpression ? this.translatorOptions.getDecimalFormat().adaptFormatString(format.getScript()) : format.getScript();
        return new StringExpression("format-number(" + number.getScript() + ", " + formatString + ")");
  }

  @Override
  public StringLiteral getStringLiteralFromUnquotedString(String value) {
    return new StringLiteral("'" + value + "'");
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

  @Override
  public DateExpression getCurrentDate() {
    return new DateExpression("current-date()");
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

  @Override
  public <T extends TypedExpression> T composeFunctionInvocation(String functionName, List<? extends TypedExpression> parameters,
      Class<T> type) {
    String namespace = translatorOptions.getUserDefinedFunctionNamespace();
    String qualifiedFunctionName = (namespace != null && !namespace.isEmpty())
        ? namespace + ":" + functionName
        : functionName;
    return Expression.instantiate(qualifiedFunctionName + "(" + parameters.stream().map(p -> p.getScript()).collect(Collectors.joining(", ")) + ")", type);
  }

  //#region Helpers -----------------------------------------------------------


  private String quoted(final String text) {
    return "'" + text.replaceAll("\"", "").replaceAll("'", "") + "'";
  }

  private int getWeeksFromDurationLiteral(final String literal) {
    Matcher weeksMatcher = Pattern.compile("(?<=\\D)\\d+(?=W)").matcher(literal);
    return weeksMatcher.find() ? Integer.parseInt(weeksMatcher.group()) : 0;
  }

  //#endregion Helpers --------------------------------------------------------
}
