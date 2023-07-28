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
 * the Lic
 */
package eu.europa.ted.efx.interfaces;

import java.util.List;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
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

/**
 * A ScriptGenerator is used by the EFX expression translator to translate specific computations to
 * the target language script.
 * 
 * Each method defined by this interface corresponds to a specific computation that needs to be
 * translated. The parameters necessary for each computation are passed to the method already
 * translated to the target language. Each method should appropriately combine the given parameters
 * to form the target language script and return it as an {@link Expression}.
 * 
 * As a reference implementation you can use the XPathScriptGenerator class.
 */
public interface ScriptGenerator {

  /**
   * Given a PathExpression and a predicate, this method should return the target language script
   * for matching the subset of nodes in the PathExpression that match the predicate.
   * 
   * Similar to {@link #composeFieldReferenceWithPredicate} but for nodes. Quick reminder: the
   * difference between fields and nodes is that fields contain values, while nodes contain other
   * nodes and/or fields.
   * 
   * @param nodeReference The PathExpression that points to the node.
   * @param predicate The predicate that should be used to match the subset of nodes.
   * @return The target language script that matches the subset of nodes.
   */
  public PathExpression composeNodeReferenceWithPredicate(
      final PathExpression nodeReference, final BooleanExpression predicate);

  /**
   * Given a PathExpression and a predicate, this method should return the target language script
   * for matching the subset of nodes in the PathExpression that match the predicate.
   * 
   * Similar to {@link #composeNodeReferenceWithPredicate} but for fields. Quick reminder: the
   * difference between fields and nodes is that fields contain values, while nodes contain other
   * nodes and/or fields.
   * 
   * @param fieldReference The PathExpression that points to the field.
   * @param predicate The predicate that should be used to match the subset of fields.
   * @return The target language script that matches the subset of fields.
   */
  public PathExpression composeFieldReferenceWithPredicate(
      final PathExpression fieldReference, final BooleanExpression predicate);

  public PathExpression composeFieldReferenceWithAxis(final PathExpression fieldReference,
      final String axis);

  /**
   * Given a PathExpression, this method should return the target language script for retrieving the
   * value of the field.
   * 
   * @param fieldReference The PathExpression that points to the field.
   * @return The target language script that retrieves the value of the field.
   */
  public PathExpression composeFieldValueReference(final PathExpression fieldReference);

  /**
   * Given a PathExpression and an attribute name, this method should return the target language
   * script for retrieving the value of the attribute.
   * 
   * @param <T> The type of the returned Expression.
   * @param fieldReference The PathExpression that points to the field.
   * @param attribute The name of the attribute.
   * @param type The type of the returned Expression.
   * @return The target language script that retrieves the value of the attribute.
   */
  public <T extends PathExpression> T composeFieldAttributeReference(
      final PathExpression fieldReference, String attribute, Class<T> type);

  /**
   * Given a variable name this method should return script to dereference the variable. The
   * returned Expression should be of the indicated type.
   * 
   * @param <T> The type of the returned Expression.
   * @param variableName The name of the variable.
   * @param type The type of the returned Expression.
   * @return The target language script that dereferences the variable.
   */
  public <T extends TypedExpression> T composeVariableReference(String variableName, Class<T> type);

  public <T extends TypedExpression> T composeVariableDeclaration(String variableName, Class<T> type);

  public <T extends TypedExpression> T composeParameterDeclaration(String parameterName, Class<T> type);

  /**
   * Takes a list of expressions and returns the target language script that corresponds to a
   * list of expressions.
   * 
   * @param <T> The type of the returned {@link SequenceExpression}.
   * @param list The list of {@link ScalarExpression}.
   * @param type The type of the returned {@link SequenceExpression}.
   * @return The target language script that corresponds to a list of expressions.
   */
  public <T extends SequenceExpression> T composeList(List<? extends ScalarExpression> list,
      Class<T> type);

  /**
   * Takes a Java Boolean value and returns the corresponding target language script.
   * 
   * @param value The Java Boolean value.
   * @return The target language script that corresponds to the given Java Boolean value.
   */
  public BooleanExpression getBooleanEquivalent(boolean value);

  /**
   * Returns the target language script for performing a logical AND operation on the two given
   * operands.
   * 
   * @param leftOperand The left operand of the logical AND operation.
   * @param rightOperand The right operand of the logical AND operation.
   * @return The target language script for performing a logical AND operation on the two given
   */
  public BooleanExpression composeLogicalAnd(final BooleanExpression leftOperand,
      final BooleanExpression rightOperand);

  /**
   * Returns the target language script for performing a logical OR operation on the two given
   * operands.
   * 
   * @param leftOperand The left operand of the logical OR operation.
   * @param rightOperand The right operand of the logical OR operation.
   * @return The target language script for performing a logical OR operation on the two given
   */
  public BooleanExpression composeLogicalOr(final BooleanExpression leftOperand,
      final BooleanExpression rightOperand);

  /**
   * Returns the target language script for performing a logical NOT operation on the given boolean
   * expression.
   * 
   * @param condition The boolean expression to be negated.
   * @return The target language script for performing a logical NOT operation on the given boolean
   */
  public BooleanExpression composeLogicalNot(BooleanExpression condition);

  /**
   * Returns the target language script that checks whether a given list of values (haystack)
   * contains a given value (needle).
   * 
   * @param needle The value to be searched for.
   * @param haystack The list of values to be searched.
   * @return The target language script that checks whether a given list of values (haystack)
   */
  public BooleanExpression composeContainsCondition(
      final ScalarExpression needle, final SequenceExpression haystack);

  /**
   * Returns the target language script that checks whether a given string matches the given RegEx
   * pattern.
   * 
   * @param expression The string expression to be matched.
   * @param regexPattern The RegEx pattern to be used for matching.
   * @return The target language script that checks whether a given string matches the given RegEx
   */
  public BooleanExpression composePatternMatchCondition(final StringExpression expression,
      final String regexPattern);

  /**
   * Returns the given expression parenthesized in the target language.
   * 
   * @param <T> The type of the returned Expression.
   * @param expression The expression to be parenthesized.
   * @param type The type of the returned Expression.
   * @return The given expression parenthesized in the target language.
   */
  public <T extends Expression> T composeParenthesizedExpression(T expression, Class<T> type);


  public BooleanExpression composeAllSatisfy(
      IteratorListExpression iterators, BooleanExpression booleanExpression);

  public BooleanExpression composeAnySatisfies(
      IteratorListExpression iterators, BooleanExpression booleanExpression);

  public <T extends TypedExpression> T composeConditionalExpression(BooleanExpression condition,
      T whenTrue, T whenFalse, Class<T> type);

  public <T extends SequenceExpression> T composeForExpression(
      IteratorListExpression iterators, ScalarExpression expression, Class<T> targetListType);

  public  IteratorExpression composeIteratorExpression(Expression variableDeclarationExpression, SequenceExpression sourceList);

  public IteratorListExpression composeIteratorList(List<IteratorExpression> iterators);

  /**
   * When we need data from an external source, we need some script that gets that data. Getting the
   * data is a two-step process: a) we need to access the data source, b) we need to get the actual
   * data from the data source. This method should return the target language script that connects
   * to the data source and permits us to subsequently get the data by using a PathExpression.
   * 
   * @param externalReference The PathExpression that points to the external data source.
   * @return a PathExpression with the target language script that retrieves the external data source.
   */
  public PathExpression composeExternalReference(final StringExpression externalReference);

  /**
   * See {@link #composeExternalReference} for more details.
   * 
   * @param externalReference The PathExpression that points to the external data source.
   * @param fieldReference The PathExpression that points to the field in the external data source.
   * @return a PathExpression with the target language script that retrieves the external data. 
   */
  public PathExpression composeFieldInExternalReference(final PathExpression externalReference,
      final PathExpression fieldReference);


  /**
   * Joins two given path expressions into one by placing the second after the first and using the
   * proper delimiter.
   * 
   * @param first The part of the path that goes before the delimiter.
   * @param second The part of the path that goes after the delimiter.
   * @return The joined path expression.
   */
  public PathExpression joinPaths(PathExpression first, PathExpression second);

  /**
   * Gets a piece of text and returns it inside quotes as expected by the target language.
   * 
   * @param value The text to be quoted.
   * @return The quoted text.
   */
  public StringExpression getStringLiteralFromUnquotedString(String value);

  /**
   * Returns the target language script that compares the two operands (for equality etc.).
   * 
   * @param leftOperand The left operand of the comparison.
   * @param operator The EFX operator that is used to compare the two operands. Do not forget to
   *        translate the operator to the target language equivalent.
   * @param rightOperand The right operand of the comparison.
   * @return The target language script that performs the comparison.
   */
  public BooleanExpression composeComparisonOperation(ScalarExpression leftOperand, String operator,
      ScalarExpression rightOperand);

  /**
   * Given a numeric operation, this method should return the target language script that performs
   * the operation.
   * 
   * @param leftOperand The left operand of the numeric operation.
   * @param operator The EFX intended operator. Do not forget to translate the operator to the
   *        target language equivalent.
   * @param rightOperand The right operand of the numeric operation.
   * @return The target language script that performs the numeric operation.
   */
  public NumericExpression composeNumericOperation(NumericExpression leftOperand, String operator,
      NumericExpression rightOperand);

  /**
   * Returns the numeric literal passed in target language script. The passed literal is in EFX.
   * 
   * @param efxLiteral The numeric literal in EFX.
   * @return The numeric literal in the target language.
   */
  public NumericExpression getNumericLiteralEquivalent(final String efxLiteral);

  /**
   * Returns the string literal in the target language. Note that the string literal passed as a
   * parameter is already between quotes in EFX.
   * 
   * @param efxLiteral The string literal in EFX.
   * @return The string literal in the target language.
   */
  public StringExpression getStringLiteralEquivalent(final String efxLiteral);

  public DateExpression getDateLiteralEquivalent(final String efxLiteral);

  public TimeExpression getTimeLiteralEquivalent(final String efxLiteral);

  public DurationExpression getDurationLiteralEquivalent(final String efxLiteral);

  /*
   * Numeric Functions
   */

  public NumericExpression composeCountOperation(final SequenceExpression list);

  public NumericExpression composeToNumberConversion(StringExpression text);

  public NumericExpression composeSumOperation(NumericSequenceExpression list);

  public NumericExpression composeStringLengthCalculation(StringExpression text);

  /*
   * String Functions
   */

  public StringExpression composeStringConcatenation(List<StringExpression> list);

  public StringExpression composeStringJoin(StringSequenceExpression list, StringExpression separator);

  public BooleanExpression composeEndsWithCondition(StringExpression text,
      StringExpression endsWith);

  public BooleanExpression composeStartsWithCondition(StringExpression text,
      StringExpression startsWith);

  public BooleanExpression composeContainsCondition(StringExpression haystack,
      StringExpression needle);

  public StringExpression composeSubstringExtraction(StringExpression text,
      NumericExpression start);

  public StringExpression composeSubstringExtraction(StringExpression text, NumericExpression start,
      NumericExpression length);

  public StringExpression composeToStringConversion(NumericExpression number);

  /**
   * Returns the target language script that converts the given text to upper case.
   * 
   * @since SDK 2.0.0
   * @see #composeToLowerCaseConversion(StringExpression)
   * 
   * @param text The text to be converted to upper case.
   * @return     The target language script that converts the text to upper case.
   */
  public StringExpression composeToUpperCaseConversion(StringExpression text);

  /**
   * Returns the target language script that converts the given text to lower case.
   * 
   * @since SDK 2.0.0
   * @see #composeToUpperCaseConversion(StringExpression)
   * 
   * @param text   The text to be converted to lower case.
   * @return       The target language script that converts the text to lower case.
   */
  public StringExpression composeToLowerCaseConversion(StringExpression text);

  /**
   * Gets the target language script that retrieves the preferred language ID
   * out of the languages available in the given field.
   * 
   * The function is intended to be used in a predicate to select the text in the preferred
   * language. The function's implementation will typically have to depend on a runtime call 
   * to a runtime library function that retrieves the language identifiers that are preferred
   * for the current visualisation.
   * 
   * @since SDK 2.0.0 
   * @see #getTextInPreferredLanguage(PathExpression)
   * 
   * @param fieldReference  The multilingual text field.
   * @return The target language script that retrieves the preferred language ID.
   */
  public StringExpression getPreferredLanguage(final PathExpression fieldReference);

  /**
   * Given a reference to a multilingual field, this function should generate the target language script
   * that returns the text value of the field in the preferred language.
   * 
   * Calling the function in EFX 2
   * 
   * @since SDK 2.0.0
   * @see #getPreferredLanguage(PathExpression)
   * 
   * @param fieldReference  The multilingual text field.
   * @return The target language script that retrieves the field's text in the preferred language.
   */
  public StringExpression getTextInPreferredLanguage(final PathExpression fieldReference);

  /*
   * Boolean Functions
   */

  public BooleanExpression composeExistsCondition(PathExpression reference);

  public BooleanExpression composeUniqueValueCondition(PathExpression needle,
      PathExpression haystack);

  public BooleanExpression composeSequenceEqualFunction(SequenceExpression one,
      SequenceExpression two);

  /*
   * Date Functions
   */

  public DateExpression composeToDateConversion(StringExpression pop);

  public DateExpression composeAddition(final DateExpression date,
      final DurationExpression duration);

  public DateExpression composeSubtraction(final DateExpression date,
      final DurationExpression duration);

  /*
   * Time Functions
   */

  public TimeExpression composeToTimeConversion(StringExpression pop);



  /*
   * Duration Functions
   */

  public DurationExpression composeToDayTimeDurationConversion(StringExpression text);

  public DurationExpression composeToYearMonthDurationConversion(StringExpression text);

  public DurationExpression composeSubtraction(DateExpression startDate, DateExpression endDate);

  public StringExpression composeNumberFormatting(NumericExpression number,
      StringExpression format);

  public DurationExpression composeMultiplication(final NumericExpression number,
      final DurationExpression duration);

  public DurationExpression composeAddition(final DurationExpression left,
      final DurationExpression right);

  public DurationExpression composeSubtraction(final DurationExpression left,
      final DurationExpression right);

  /*
   * Sequence Functions
   */

  public <T extends SequenceExpression> T composeDistinctValuesFunction(
      T list, Class<T> listType);

  public <T extends SequenceExpression> T composeUnionFunction(T listOne,
      T listTwo, Class<T> listType);

  public <T extends SequenceExpression> T composeIntersectFunction(T listOne,
      T listTwo, Class<T> listType);

  public <T extends SequenceExpression> T composeExceptFunction(T listOne,
      T listTwo, Class<T> listType);

  public <T extends ScalarExpression> T composeIndexer(SequenceExpression list,
      NumericExpression index, Class<T> type);
}
