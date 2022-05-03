package eu.europa.ted.efx.interfaces;

import java.util.List;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;

public interface ScriptGenerator {

    /**
     * Given a PathExpression and a predicate, this method should return the target language script
     * for matching the subset of nodes in the PathExpression that match the predicate.
     * 
     * Similar to {@link mapFieldReferenceWithPredicate} but for nodes. Quick reminder: the
     * difference between fields and nodes is that fields contain values, while nodes contain other
     * nodes and/or fields.
     */
    public <T extends Expression> T mapNodeReferenceWithPredicate(
            final PathExpression nodeReference, final BooleanExpression predicate, Class<T> type);

    /**
     * Given a PathExpression and a predicate, this method should return the target language script
     * for matching the subset of nodes in the PathExpression that match the predicate.
     * 
     * Similar to {@link mapNodeReferenceWithPredicate} but for fields. Quick reminder: the
     * difference between fields and nodes is that fields contain values, while nodes contain other
     * nodes and/or fields.
     */
    public <T extends Expression> T mapFieldReferenceWithPredicate(
            final PathExpression fieldReference, final BooleanExpression predicate, Class<T> type);

    /**
     * Given a PathExpression, this method should return the target language script for retrieving
     * the value of the field.
     */
    public <T extends Expression> T mapFieldValueReference(final PathExpression fieldReference,
            Class<T> type);

    /**
     * Given a PathExpression and an attribute name, this method should return the target language
     * script for retrieving the value of the attribute.
     */
    public <T extends Expression> T mapFieldAttributeReference(final PathExpression fieldReference,
            String attribute, Class<T> type);

    /**
     * Takes a list of string expressions and returns the target language script that corresponds to
     * a list of string expressions.
     */
    public StringListExpression mapList(final List<StringExpression> list);


    /**
     * Takes a Java Boolean value and returns the corresponding target language script.
     */
    public BooleanExpression mapBoolean(Boolean value);

    /**
     * Returns the target language script for performing a logical AND operation on the two given
     * operands.
     */
    public BooleanExpression mapLogicalAnd(final BooleanExpression leftOperand,
            final BooleanExpression rightOperand);

    /**
     * Returns the target language script for performing a logical OR operation on the two given
     * operands.
     */
    public BooleanExpression mapLogicalOr(final BooleanExpression leftOperand,
            final BooleanExpression rightOperand);

    /**
     * Returns the target language script for performing a logical NOT operation on the given
     * boolean expression.
     */
    public BooleanExpression mapLogicalNot(BooleanExpression condition);

    /**
     * Returns the target language script that checks whether a given list of strings (haystack)
     * contains a given string (needle).
     */
    public BooleanExpression mapInListCondition(final StringExpression needle,
            final StringListExpression haystack);

    /**
     * Returns the target language script that checks whether a given string matches the given RegEx
     * pattern.
     */
    public BooleanExpression mapMatchesPatternCondition(final StringExpression expression,
            final String pattern);

    /**
     * Returns the given expression parenthesized in the target language.
     */
    public <T extends Expression> T mapParenthesizedExpression(T expression, Class<T> type);


    /**
     * TODO: Not properly defined yet.
     * 
     * When we need data from an external source, we need some script that gets that data. Getting
     * the data is a two-step process: a) we need to access the data source, b) we need to get the
     * actual data from the data source. This method should return the target language script that
     * connects to the data source and permits us to subsequently get the data by using a
     * PathExpression.
     */
    public PathExpression mapExternalReference(final StringExpression externalReference);

    /**
     * TODO: Not properly defined yet.
     * 
     * See {@link mapExternalReference} for more details.
     */
    public PathExpression mapFieldInExternalReference(final PathExpression externalReference,
            final PathExpression fieldReference);


    /**
     * Joins two given path expressions into one by placing the second after the first and using the proper delimiter.
     * 
     * @param first The part of the path that goes before the delimiter.
     * @param second The part of the path that goes after the delimiter.
     * @return
     */
    public PathExpression joinPaths(PathExpression first, PathExpression second);

    /**
     * Gets a piece of text and returns it inside quotes as expected by the target language.
     * 
     * @param value
     * @return
     */
    public StringExpression mapString(String value);

    /**
     * Returns the target language script that compares the two operands (for equality etc.).
     * 
     * @param operator The EFX operator that is used to compare the two operands. Do not forget to
     *        translate the operator to the target language equivalent.
     */
    public BooleanExpression mapComparisonOperator(Expression leftOperand, String operator,
            Expression rightOperand);

    /**
     * Given a numeric operation, this method should return the target language script that performs
     * the operation.
     * 
     * @param operator The EFX intended operator. Do not forget to translate the operator to the
     *        target language equivalent.
     */
    public NumericExpression mapNumericOperator(NumericExpression leftOperand, String operator,
            NumericExpression rightOperand);

    /**
     * Returns the numeric literal passed in target language script. The passed literal is in EFX.
     */
    public NumericExpression mapNumericLiteral(final String literal);

    /**
     * Returns the string literal in the target language. Note that the string literal passed as a
     * parameter is already between quotes in EFX.
     */
    public StringExpression mapStringLiteral(final String literal);

    public DateExpression mapDateLiteral(final String literal);

    public TimeExpression mapTimeLiteral(final String literal);

    public DurationExpression mapDurationLiteral(final String literal);

    /*
     * Numeric Functions
     */

    public NumericExpression mapCountFunction(final PathExpression set);

    public NumericExpression mapToNumberFunction(StringExpression text);

    public NumericExpression mapSumFunction(PathExpression setReference);

    public NumericExpression mapStringLengthFunction(StringExpression text);

    /*
     * String Functions
     */

    public StringExpression mapStringConcatenationFunction(List<StringExpression> list);

    public BooleanExpression mapStringEndsWithFunction(StringExpression text,
            StringExpression endsWith);

    public BooleanExpression mapStringStartsWithFunction(StringExpression text,
            StringExpression startsWith);

    public BooleanExpression mapStringContainsFunction(StringExpression haystack,
            StringExpression needle);

    public StringExpression mapSubstringFunction(StringExpression text, NumericExpression start);

    public StringExpression mapSubstringFunction(StringExpression text, NumericExpression start,
            NumericExpression length);

    public StringExpression mapNumberToStringFunction(NumericExpression number);

    /*
     * Boolean Functions
     */

    public BooleanExpression mapExistsExpression(PathExpression reference);

    /*
     * Date Functions
     */

    public DateExpression mapDateFromStringFunction(StringExpression pop);

    /*
     * Time Functions
     */

    public TimeExpression mapTimeFromStringFunction(StringExpression pop);

    /*
     * Duration Functions
     */


    public DurationExpression mapDurationFromDatesFunction(DateExpression startDate,
            DateExpression endDate);

    public StringExpression mapFormatNumberFunction(NumericExpression number,
            StringExpression format);
}
