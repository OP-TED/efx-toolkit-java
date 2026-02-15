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
package eu.europa.ted.efx.interfaces;

import java.util.List;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;

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

  public <T extends TypedExpression> T composeParameterReference(String parameterName, Class<T> type);

  public <T extends TypedExpression> T composeVariableDeclaration(String variableName, Class<T> type);

  public <T extends TypedExpression> T composeParameterDeclaration(String parameterName, Class<T> type);

  public <T extends TypedExpression> T composeDictionaryLookup(String dictionaryName, StringExpression keyExpression, Class<T> type);

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

  public IteratorExpression composeIteratorExpression(Expression variableDeclarationExpression, SequenceExpression sourceList);

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
   * Makes the given absolute path relative to the given context path.
   *
   * This is target-language-specific because different target languages (XPath, JavaScript, etc.)
   * may have different path contextualization semantics.
   *
   * @param absolutePath The absolute path to contextualize.
   * @param contextPath The context path to make the result relative to.
   * @return The path relative to the given context.
   */
  public PathExpression contextualizePath(PathExpression absolutePath, PathExpression contextPath);

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

  // #region Numeric Functions ------------------------------------------------

  /**
   * Returns the target language script that counts the number of elements in a sequence.
   *
   * @param list The sequence whose elements are to be counted.
   * @return A numeric expression representing the count.
   */
  public NumericExpression composeCountOperation(final SequenceExpression list);

  /**
   * Returns the target language script that converts a string to a number.
   *
   * @param text The string expression to convert.
   * @return A numeric expression representing the converted value.
   */
  public NumericExpression composeToNumberConversion(StringExpression text);

  /**
   * Returns the target language script that converts a boolean to a number.
   * Typically {@code TRUE} maps to {@code 1} and {@code FALSE} maps to {@code 0}.
   *
   * @param bool The boolean expression to convert.
   * @return A numeric expression representing the converted value.
   */
  public NumericExpression composeToNumberConversion(BooleanExpression bool);

  public NumericExpression composeSumOperation(NumericSequenceExpression list);

  /**
   * Returns the target language script that computes the minimum value in a numeric sequence.
   *
   * @param list The numeric sequence to find the minimum of.
   * @return A numeric expression representing the minimum value.
   */
  public NumericExpression composeMinFunction(NumericSequenceExpression list);

  /**
   * Returns the target language script that computes the maximum value in a numeric sequence.
   *
   * @param list The numeric sequence to find the maximum of.
   * @return A numeric expression representing the maximum value.
   */
  public NumericExpression composeMaxFunction(NumericSequenceExpression list);

  /**
   * Returns the target language script that computes the average of a numeric sequence.
   *
   * @param list The numeric sequence to average.
   * @return A numeric expression representing the average value.
   */
  public NumericExpression composeAvgFunction(NumericSequenceExpression list);

  public NumericExpression composeStringLengthCalculation(StringExpression text);

  /**
   * Returns the target language script that extracts the year component from a date.
   *
   * @param date The date expression to extract the year from.
   * @return A numeric expression representing the year.
   */
  public NumericExpression composeYearFunction(DateExpression date);

  /**
   * Returns the target language script that extracts the month component from a date.
   *
   * @param date The date expression to extract the month from.
   * @return A numeric expression representing the month (1-12).
   */
  public NumericExpression composeMonthFunction(DateExpression date);

  /**
   * Returns the target language script that extracts the day component from a date.
   *
   * @param date The date expression to extract the day from.
   * @return A numeric expression representing the day of the month (1-31).
   */
  public NumericExpression composeDayFunction(DateExpression date);

  /**
   * Returns the target language script that computes the absolute value of a number.
   *
   * @param number The numeric expression whose absolute value is to be computed.
   * @return A numeric expression representing the absolute value.
   */
  public NumericExpression composeAbsFunction(NumericExpression number);

  /**
   * Returns the target language script that rounds a number to the nearest integer.
   *
   * @param number The numeric expression to round.
   * @return A numeric expression representing the rounded value.
   */
  public NumericExpression composeRoundFunction(NumericExpression number);

  /**
   * Returns the target language script that rounds a number down (towards negative infinity).
   *
   * @param number The numeric expression to round down.
   * @return A numeric expression representing the rounded-down value.
   */
  public NumericExpression composeFloorFunction(NumericExpression number);

  /**
   * Returns the target language script that rounds a number up (towards positive infinity).
   *
   * @param number The numeric expression to round up.
   * @return A numeric expression representing the rounded-up value.
   */
  public NumericExpression composeCeilingFunction(NumericExpression number);

  // #endregion Numeric Functions -------------------------------------------

  // #region String Functions -----------------------------------------------

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

  /**
   * Returns the target language script that extracts the part of the text before the first
   * occurrence of the delimiter. Returns an empty string if the delimiter is not found.
   *
   * @param text The text to search in.
   * @param delimiter The delimiter to search for.
   * @return The target language script for the substring before the delimiter.
   */
  public StringExpression composeSubstringBeforeFunction(StringExpression text,
      StringExpression delimiter);

  /**
   * Returns the target language script that extracts the part of the text after the first
   * occurrence of the delimiter. Returns an empty string if the delimiter is not found.
   *
   * @param text The text to search in.
   * @param delimiter The delimiter to search for.
   * @return The target language script for the substring after the delimiter.
   */
  public StringExpression composeSubstringAfterFunction(StringExpression text,
      StringExpression delimiter);

  /**
   * Returns the target language script that converts a number to its string representation.
   *
   * @param number The numeric expression to convert.
   * @return A string expression representing the converted value.
   */
  public StringExpression composeToStringConversion(NumericExpression number);

  /**
   * Returns the target language script that converts a boolean to its string representation.
   *
   * @param bool The boolean expression to convert.
   * @return A string expression representing the converted value.
   */
  public StringExpression composeToStringConversion(BooleanExpression bool);

  /**
   * Returns the target language script that converts a date to its string representation.
   *
   * @param date The date expression to convert.
   * @return A string expression representing the converted value.
   */
  public StringExpression composeToStringConversion(DateExpression date);

  /**
   * Returns the target language script that converts a time to its string representation.
   *
   * @param time The time expression to convert.
   * @return A string expression representing the converted value.
   */
  public StringExpression composeToStringConversion(TimeExpression time);

  /**
   * Returns the target language script that converts a duration to its string representation.
   *
   * @param duration The duration expression to convert.
   * @return A string expression representing the converted value.
   */
  public StringExpression composeToStringConversion(DurationExpression duration);

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
   * Returns the target language script that strips leading/trailing whitespace and collapses
   * internal whitespace sequences to a single space.
   *
   * @param text The text to normalize.
   * @return The target language script that normalizes whitespace in the text.
   */
  public StringExpression composeNormalizeSpaceFunction(StringExpression text);

  /**
   * Returns the target language script that removes leading and trailing whitespace from the text.
   *
   * @param text The text to trim.
   * @return The target language script that trims whitespace from both ends.
   */
  public StringExpression composeTrimFunction(StringExpression text);

  /**
   * Returns the target language script that removes leading whitespace from the text.
   *
   * @param text The text to trim.
   * @return The target language script that trims leading whitespace.
   */
  public StringExpression composeTrimLeftFunction(StringExpression text);

  /**
   * Returns the target language script that removes trailing whitespace from the text.
   *
   * @param text The text to trim.
   * @return The target language script that trims trailing whitespace.
   */
  public StringExpression composeTrimRightFunction(StringExpression text);

  /**
   * Returns the target language script that pads the text on the left with the given character
   * until it reaches the specified length. If the text is already at least the specified length,
   * it is returned unchanged.
   *
   * @param text The text to pad.
   * @param length The desired minimum length.
   * @param padChar The character to pad with.
   * @return The target language script that left-pads the text.
   */
  public StringExpression composePadLeftFunction(StringExpression text, NumericExpression length,
      StringExpression padChar);

  /**
   * Returns the target language script that pads the text on the right with the given character
   * until it reaches the specified length. If the text is already at least the specified length,
   * it is returned unchanged.
   *
   * @param text The text to pad.
   * @param length The desired minimum length.
   * @param padChar The character to pad with.
   * @return The target language script that right-pads the text.
   */
  public StringExpression composePadRightFunction(StringExpression text, NumericExpression length,
      StringExpression padChar);

  /**
   * Returns the target language script that repeats the text the specified number of times.
   *
   * @param text The text to repeat.
   * @param count The number of repetitions.
   * @return The target language script that repeats the text.
   */
  public StringExpression composeRepeatFunction(StringExpression text, NumericExpression count);

  /**
   * Returns the target language script that replaces all occurrences of a literal search string
   * with the replacement string.
   *
   * @param text The text to search in.
   * @param search The literal string to search for.
   * @param replacement The replacement string.
   * @return The target language script that performs literal replacement.
   */
  public StringExpression composeReplaceFunction(StringExpression text, StringExpression search,
      StringExpression replacement);

  /**
   * Returns the target language script that replaces all matches of a regular expression pattern
   * with the replacement string. The pattern uses the EFX regex profile.
   *
   * @param text The text to search in.
   * @param pattern The regex pattern to match.
   * @param replacement The replacement string (may use capture group references).
   * @return The target language script that performs regex replacement.
   */
  public StringExpression composeReplaceRegexFunction(StringExpression text,
      StringExpression pattern, StringExpression replacement);

  /**
   * Composes a URL-encoding function call in the target language.
   *
   * @param text The string to URL-encode.
   * @return The target language script that URL-encodes the string.
   */
  public StringExpression composeUrlEncodeFunction(StringExpression text);

  /**
   * Composes a capitalize-first function call in the target language.
   * Converts the first character of the string to upper case.
   *
   * @param text The string whose first character to capitalize.
   * @return The target language script that capitalizes the first character.
   */
  public StringExpression composeCapitalizeFirstFunction(StringExpression text);

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

  // #endregion String Functions ----------------------------------------------

  // #region Boolean Functions ------------------------------------------------

  public BooleanExpression composeExistsCondition(PathExpression reference);

  /**
   * Returns the target language script that converts a number to a boolean.
   * Typically {@code 0} maps to {@code FALSE} and any non-zero value maps to {@code TRUE}.
   *
   * @param number The numeric expression to convert.
   * @return A boolean expression representing the converted value.
   */
  public BooleanExpression composeToBooleanConversion(NumericExpression number);

  /**
   * Uniqueness check for EFX 1 syntax.
   * <p>
   * This method supports the limited uniqueness syntax available in EFX 1.
   * It is used exclusively by the EFX 1 translator and is kept for backward
   * compatibility with EFX 1.
   * <p>
   * <b>EFX 2 does not use this method.</b> EFX 2's stricter type checking enables
   * more powerful uniqueness syntax, supported by the typed overloads below.
   *
   * @param needle The value to check for uniqueness
   * @param haystack The collection to search within
   * @return A boolean expression evaluating to true if needle appears exactly once in haystack
   */
  public BooleanExpression composeUniqueValueCondition(PathExpression needle,
      PathExpression haystack);

  // Typed uniqueness conditions (EFX 2)
  public BooleanExpression composeUniqueValueCondition(StringExpression needle,
      StringSequenceExpression haystack);

  public BooleanExpression composeUniqueValueCondition(NumericExpression needle,
      NumericSequenceExpression haystack);

  public BooleanExpression composeUniqueValueCondition(BooleanExpression needle,
      BooleanSequenceExpression haystack);

  public BooleanExpression composeUniqueValueCondition(DateExpression needle,
      DateSequenceExpression haystack);

  public BooleanExpression composeUniqueValueCondition(TimeExpression needle,
      TimeSequenceExpression haystack);

  public BooleanExpression composeUniqueValueCondition(DurationExpression needle,
      DurationSequenceExpression haystack);

  /**
   * Returns the target language script that checks whether two sequences contain the same
   * elements, regardless of order.
   *
   * @param one The first sequence.
   * @param two The second sequence.
   * @return A boolean expression that is true when the two sequences are equal.
   */
  public BooleanExpression composeSequenceEqualFunction(SequenceExpression one,
      SequenceExpression two);

  /**
   * Returns the target language script that checks whether a sequence is empty
   * (contains no elements).
   *
   * @param sequence The sequence to check.
   * @return A boolean expression that is true when the sequence is empty.
   */
  public BooleanExpression composeEmptySequenceCondition(SequenceExpression sequence);

  /**
   * Returns the target language script that checks whether all values in a sequence are
   * distinct (i.e. the sequence has no duplicate values).
   *
   * @param sequence The sequence to check.
   * @return A boolean expression that is true when all values are distinct.
   */
  public BooleanExpression composeIsDistinctCondition(SequenceExpression sequence);

  // #endregion Boolean Functions --------------------------------------------

  // #region Date Functions ---------------------------------------------------

  /**
   * Returns the target language script that converts a string to a date.
   *
   * @param pop The string expression to convert.
   * @return A date expression representing the converted value.
   */
  public DateExpression composeToDateConversion(StringExpression pop);

  public DateExpression composeAddition(final DateExpression date,
      final DurationExpression duration);

  public DateExpression composeSubtraction(final DateExpression date,
      final DurationExpression duration);

  /**
   * Returns the current date as a date expression in the target language.
   *
   * @return A date expression representing today's date.
   */
  public DateExpression getCurrentDate();

  //#endregion Date Functions -------------------------------------------------

  // #region Time Functions ---------------------------------------------------

  /**
   * Returns the target language script that converts a string to a time.
   *
   * @param pop The string expression to convert.
   * @return A time expression representing the converted value.
   */
  public TimeExpression composeToTimeConversion(StringExpression pop);

  // #endregion Time Functions ------------------------------------------------

  // #region Duration Functions -----------------------------------------------

  /**
   * Returns the target language script that converts a string to a day-time duration
   * (e.g. {@code "P3DT4H"} for 3 days and 4 hours).
   *
   * @param text The string expression to convert.
   * @return A duration expression representing the converted value.
   */
  public DurationExpression composeToDayTimeDurationConversion(StringExpression text);

  /**
   * Returns the target language script that converts a string to a year-month duration
   * (e.g. {@code "P2Y3M"} for 2 years and 3 months).
   *
   * @param text The string expression to convert.
   * @return A duration expression representing the converted value.
   */
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

  // #endregion Duration Functions --------------------------------------------

  // #region Sequence Functions --------------------------------------------

  /**
   * Returns the target language script that removes duplicate values from a sequence,
   * preserving only distinct values.
   *
   * @param <T>      The type of the sequence expression.
   * @param list     The sequence to remove duplicates from.
   * @param listType The class of the sequence expression type.
   * @return A sequence containing only the distinct values.
   */
  public <T extends SequenceExpression> T composeDistinctValuesFunction(
      T list, Class<T> listType);

  /**
   * Returns the target language script that computes the union of two sequences
   * (all values from both, with duplicates removed).
   *
   * @param <T>      The type of the sequence expression.
   * @param listOne  The first sequence.
   * @param listTwo  The second sequence.
   * @param listType The class of the sequence expression type.
   * @return A sequence containing the union of both sequences.
   */
  public <T extends SequenceExpression> T composeUnionFunction(T listOne,
      T listTwo, Class<T> listType);

  /**
   * Returns the target language script that computes the intersection of two sequences
   * (only values present in both).
   *
   * @param <T>      The type of the sequence expression.
   * @param listOne  The first sequence.
   * @param listTwo  The second sequence.
   * @param listType The class of the sequence expression type.
   * @return A sequence containing only the values present in both sequences.
   */
  public <T extends SequenceExpression> T composeIntersectFunction(T listOne,
      T listTwo, Class<T> listType);

  /**
   * Returns the target language script that computes the difference of two sequences
   * (values in the first sequence that are not in the second).
   *
   * @param <T>      The type of the sequence expression.
   * @param listOne  The first sequence.
   * @param listTwo  The second sequence.
   * @param listType The class of the sequence expression type.
   * @return A sequence containing values from the first sequence not present in the second.
   */
  public <T extends SequenceExpression> T composeExceptFunction(T listOne,
      T listTwo, Class<T> listType);

  /**
   * Returns the target language script that sorts a sequence in ascending order.
   *
   * @param <T>      The type of the sequence expression.
   * @param list     The sequence to sort.
   * @param listType The class of the sequence expression type.
   * @return A sorted sequence.
   */
  public <T extends SequenceExpression> T composeSortFunction(T list, Class<T> listType);

  /**
   * Returns the target language script that reverses the order of elements in a sequence.
   *
   * @param <T>      The type of the sequence expression.
   * @param list     The sequence to reverse.
   * @param listType The class of the sequence expression type.
   * @return A reversed sequence.
   */
  public <T extends SequenceExpression> T composeReverseFunction(T list, Class<T> listType);

  /**
   * Returns the target language script that extracts a contiguous subsequence starting
   * at the given position, through the end of the sequence.
   *
   * @param <T>      The type of the sequence expression.
   * @param list     The source sequence.
   * @param start    The 1-based starting position.
   * @param listType The class of the sequence expression type.
   * @return A subsequence from the starting position to the end.
   */
  public <T extends SequenceExpression> T composeSubsequenceFunction(T list,
      NumericExpression start, Class<T> listType);

  /**
   * Returns the target language script that extracts a contiguous subsequence of the
   * given length, starting at the given position.
   *
   * @param <T>      The type of the sequence expression.
   * @param list     The source sequence.
   * @param start    The 1-based starting position.
   * @param length   The maximum number of elements to extract.
   * @param listType The class of the sequence expression type.
   * @return A subsequence of the given length starting at the given position.
   */
  public <T extends SequenceExpression> T composeSubsequenceFunction(T list,
      NumericExpression start, NumericExpression length, Class<T> listType);

  /**
   * Returns the target language script that finds the 1-based position of the first occurrence of a
   * value within a sequence. Returns 0 if the value is not found.
   *
   * @param list  The sequence to search in.
   * @param value The value to search for.
   * @return A numeric expression with the 1-based position of the first occurrence, or 0 if not
   *         found.
   */
  public NumericExpression composeIndexOfFunction(SequenceExpression list,
      ScalarExpression value);

  /**
   * Returns the target language script that splits a string into a sequence of substrings
   * using the given literal delimiter.
   *
   * @param text The text to split.
   * @param delimiter The literal delimiter to split on.
   * @return A string sequence expression with the split parts.
   */
  public StringSequenceExpression composeSplitFunction(StringExpression text,
      StringExpression delimiter);

  /**
   * Returns the target language script that finds the 1-based position of the first occurrence
   * of a substring within a string. Returns 0 if the substring is not found.
   *
   * @param text The text to search in.
   * @param substring The substring to search for.
   * @return A numeric expression with the 1-based position, or 0 if not found.
   */
  public NumericExpression composeIndexOfSubstringFunction(StringExpression text,
      StringExpression substring);

  /**
   * Returns the target language script that retrieves the element at a given position
   * in a sequence.
   *
   * @param <T>   The scalar type of the elements in the sequence.
   * @param list  The sequence to index into.
   * @param index The 1-based position of the element to retrieve.
   * @param type  The class of the scalar expression type.
   * @return The element at the given position.
   */
  public <T extends ScalarExpression> T composeIndexer(SequenceExpression list,
      NumericExpression index, Class<T> type);

  // #endregion Sequence Functions -----------------------------------------

  // #region Function Invocation ------------------------------------------

  /**
   * Composes a function invocation expression with the specified function name, parameters, 
   * and return type.
   *
   * @param <T> The type of the resulting expression, which must extend {@link TypedExpression}.
   * @param functionName The name of the function to be invoked.
   * @param parameters A list of parameters to be passed to the function, each of which must 
   *                   extend {@link TypedExpression}.
   * @param type The class object representing the expected return type of the function invocation.
   * @return An expression that will invoke the function at runtime.
   */
  public <T extends TypedExpression> T composeFunctionInvocation(String functionName,
          List<? extends TypedExpression> parameters, Class<T> type);

  // #endregion Function Invocation -----------------------------------------
}
