package eu.europa.ted.efx.interfaces;

import java.util.List;

public interface SyntaxMap {

  /**
   * Returns the quote character used by the target language.
   */
  public Character mapStringQuote();

  /**
   * Given an operator and two operands, the method should return the operation expression in the
   * targte language.
   */
  public String mapOperator(String leftOperand, String operator, String rightOperand);

  public String mapNodeReferenceWithPredicate(final String nodeReference, final String predicate);

  public String mapFieldReferenceWithPredicate(final String fieldReference, final String predicate);

  public String mapFieldValueReference(final String fieldReference);

  public String mapFieldAttributeReference(final String fieldReference, String attribute);

  public String mapList(final List<String> list);

  public String mapLiteral(final String literal);

  public String mapBoolean(Boolean value);

  public String mapLogicalAnd(final String leftOperand, final String rightOperand);

  public String mapLogicalOr(final String leftOperand, final String rightOperand);

  public String mapLogicalNot(String condition);

  public String mapInListCondition(final String expression, final String List);

  public String mapMatchesPatternCondition(final String expression, final String pattern);

  public String mapParenthesizedExpression(final String expression);

  public String mapExternalReference(final String externalReference);

  public String mapFieldInExternalReference(final String externalReference,
      final String fieldReference);

  /*
   * Numeric Functions
   */

  public String mapCountFunction(final String setReference);

  public String mapToNumberFunction(String text);

  public String mapSumFunction(String setReference);

  public String mapStringLengthFunction(String text);

  /*
   * String Functions
   */

  public String mapStringConcatenationFunction(List<String> list);

  public String mapStringEndsWithFunction(String text, String endsWith);

  public String mapStringStartsWithFunction(String text, String startsWith);

  public String mapStringContainsFunction(String haystack, String needle);

  public String mapSubstringFunction(String text, String start);

  public String mapSubstringFunction(String text, String start, String length);

  public String mapNumberToStringFunction(String pop);

  /*
   * Boolean Functions
   */

  public String mapExistsExpression(String reference);

  /*
   * Date Functions
   */

  public String mapDateFromStringFunction(String pop);

  /*
   * Time Functions
   */

  public String mapTimeFromStringFunction(String pop);

  /*
   * Duration Functions
   */


  public String mapDurationFromDatesFunction(String startDate, String endDate);

  public String mapFormatNumberFunction(String number, String format);
}
