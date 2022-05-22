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
     * Similar to {@link #composeFieldReferenceWithPredicate} but for nodes. Quick reminder: the
     * difference between fields and nodes is that fields contain values, while nodes contain other
     * nodes and/or fields.
     */
    public <T extends Expression> T composeNodeReferenceWithPredicate(
            final PathExpression nodeReference, final BooleanExpression predicate, Class<T> type);

    /**
     * Given a PathExpression and a predicate, this method should return the target language script
     * for matching the subset of nodes in the PathExpression that match the predicate.
     * 
     * Similar to {@link #composeNodeReferenceWithPredicate} but for fields. Quick reminder: the
     * difference between fields and nodes is that fields contain values, while nodes contain other
     * nodes and/or fields.
     */
    public <T extends Expression> T composeFieldReferenceWithPredicate(
            final PathExpression fieldReference, final BooleanExpression predicate, Class<T> type);

    /**
     * Given a PathExpression, this method should return the target language script for retrieving
     * the value of the field.
     */
    public <T extends Expression> T composeFieldValueReference(final PathExpression fieldReference,
            Class<T> type);

    /**
     * Given a PathExpression and an attribute name, this method should return the target language
     * script for retrieving the value of the attribute.
     */
    public <T extends Expression> T composeFieldAttributeReference(final PathExpression fieldReference,
            String attribute, Class<T> type);

    /**
     * Given a variable name this method should return script to dereference the variable.
     * The returned Expression should be of the indicated type. 
     */
    public <T extends Expression> T composeVariableReference(String variableName, Class<T> type);

    /**
     * Takes a list of string expressions and returns the target language script that corresponds to
     * a list of string expressions.
     */
    public StringListExpression composeListOfStrings(final List<StringExpression> list);


    /**
     * Takes a Java Boolean value and returns the corresponding target language script.
     */
    public BooleanExpression getBooleanEquivalent(boolean value);

    /**
     * Returns the target language script for performing a logical AND operation on the two given
     * operands.
     */
    public BooleanExpression composeLogicalAnd(final BooleanExpression leftOperand,
            final BooleanExpression rightOperand);

    /**
     * Returns the target language script for performing a logical OR operation on the two given
     * operands.
     */
    public BooleanExpression composeLogicalOr(final BooleanExpression leftOperand,
            final BooleanExpression rightOperand);

    /**
     * Returns the target language script for performing a logical NOT operation on the given
     * boolean expression.
     */
    public BooleanExpression composeLogicalNot(BooleanExpression condition);

    /**
     * Returns the target language script that checks whether a given list of strings (haystack)
     * contains a given string (needle).
     */
    public BooleanExpression composeContainsCondition(final StringExpression needle,
            final StringListExpression haystack);

    /**
     * Returns the target language script that checks whether a given string matches the given RegEx
     * pattern.
     */
    public BooleanExpression composePatternMatchCondition(final StringExpression expression,
            final String regexPattern);

    /**
     * Returns the given expression parenthesized in the target language.
     */
    public <T extends Expression> T composeParenthesizedExpression(T expression, Class<T> type);


    public BooleanExpression composeAllSatisfy(StringListExpression list, String variableName,
            BooleanExpression booleanExpression);

    public BooleanExpression composeAnySatisfies(StringListExpression list, String variableName,
            BooleanExpression booleanExpression);

            
    public <T extends Expression> T composeConditionalExpression(BooleanExpression condition,
    T whenTrue, T whenFalse, Class<T> type);

    /**
     * TODO: Not properly defined yet.
     * 
     * When we need data from an external source, we need some script that gets that data. Getting
     * the data is a two-step process: a) we need to access the data source, b) we need to get the
     * actual data from the data source. This method should return the target language script that
     * connects to the data source and permits us to subsequently get the data by using a
     * PathExpression.
     */
    public PathExpression composeExternalReference(final StringExpression externalReference);

    /**
     * TODO: Not properly defined yet.
     * 
     * See {@link #composeExternalReference} for more details.
     */
    public PathExpression composeFieldInExternalReference(final PathExpression externalReference,
            final PathExpression fieldReference);


    /**
     * Joins two given path expressions into one by placing the second after the first and using the
     * proper delimiter.
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
    public StringExpression getStringLiteralFromUnquotedString(String value);

    /**
     * Returns the target language script that compares the two operands (for equality etc.).
     * 
     * @param operator The EFX operator that is used to compare the two operands. Do not forget to
     *        translate the operator to the target language equivalent.
     */
    public BooleanExpression composeComparisonOperation(Expression leftOperand, String operator,
            Expression rightOperand);

    /**
     * Given a numeric operation, this method should return the target language script that performs
     * the operation.
     * 
     * @param operator The EFX intended operator. Do not forget to translate the operator to the
     *        target language equivalent.
     */
    public NumericExpression composeNumericOperation(NumericExpression leftOperand, String operator,
            NumericExpression rightOperand);

    /**
     * Returns the numeric literal passed in target language script. The passed literal is in EFX.
     */
    public NumericExpression getNumericLiteralEquivalent(final String efxLiteral);

    /**
     * Returns the string literal in the target language. Note that the string literal passed as a
     * parameter is already between quotes in EFX.
     */
    public StringExpression getStringLiteralEquivalent(final String efxLiteral);

    public DateExpression getDateLiteralEquivalent(final String efxLiteral);

    public TimeExpression getTimeLiteralEquivalent(final String efxLiteral);

    public DurationExpression getDurationLiteralEquivalent(final String efxLiteral);

    /*
     * Numeric Functions
     */

    public NumericExpression composeCountOperation(final PathExpression set);

    public NumericExpression composeToNumberConversion(StringExpression text);

    public NumericExpression composeSumOperation(PathExpression setReference);

    public NumericExpression composeStringLengthCalculation(StringExpression text);

    /*
     * String Functions
     */

    public StringExpression composeStringConcatenation(List<StringExpression> list);

    public BooleanExpression composeEndsWithCondition(StringExpression text,
            StringExpression endsWith);

    public BooleanExpression composeStartsWithCondition(StringExpression text,
            StringExpression startsWith);

    public BooleanExpression composeContainsCondition(StringExpression haystack,
            StringExpression needle);

    public StringExpression composeSubstringExtraction(StringExpression text, NumericExpression start);

    public StringExpression composeSubstringExtraction(StringExpression text, NumericExpression start,
            NumericExpression length);

    public StringExpression composeToStringConversion(NumericExpression number);

    /*
     * Boolean Functions
     */

    public BooleanExpression composeExistsCondition(PathExpression reference);

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

    public DurationExpression composeSubtraction(DateExpression startDate,
            DateExpression endDate);

    public StringExpression composeNumberFormatting(NumericExpression number,
            StringExpression format);

    public DurationExpression composeMultiplication(final NumericExpression number,
            final DurationExpression duration);

    public DurationExpression composeAddition(final DurationExpression left,
            final DurationExpression right);

    public DurationExpression composeSubtraction(final DurationExpression left,
            final DurationExpression right);
}
