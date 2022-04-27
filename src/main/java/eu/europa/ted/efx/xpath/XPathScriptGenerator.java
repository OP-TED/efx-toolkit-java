package eu.europa.ted.efx.xpath;

import static java.util.Map.entry;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;

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
    public <T extends Expression> T mapNodeReferenceWithPredicate(PathExpression nodeReference,
            BooleanExpression predicate, Class<T> type) {
        return instantiate(nodeReference.script + '[' + predicate.script + ']', type);
    }

    @Override
    public <T extends Expression> T mapFieldReferenceWithPredicate(PathExpression fieldReference,
            BooleanExpression predicate, Class<T> type) {
        return instantiate(fieldReference.script + '[' + predicate.script + ']', type);
    }

    @Override
    public <T extends Expression> T mapFieldValueReference(PathExpression fieldReference,
            Class<T> type) {

        if (StringExpression.class.isAssignableFrom(type)) {
            return this.instantiate(fieldReference.script + "/normalize-space(text())", type);
        }
        if (DateExpression.class.isAssignableFrom(type)) {
            return this.instantiate(fieldReference.script + "/xs:date(text())", type);
        }
        if (TimeExpression.class.isAssignableFrom(type)) {
            return this.instantiate(fieldReference.script + "/xs:time(text())", type);
        }

        return instantiate(fieldReference.script, type);
    }

    @Override
    public <T extends Expression> T mapFieldAttributeReference(PathExpression fieldReference,
            String attribute, Class<T> type) {
        return instantiate(fieldReference.script + "/@" + attribute, type);
    }

    @Override
    public StringListExpression mapList(List<StringExpression> list) {
        if (list == null || list.isEmpty()) {
            return new StringListExpression("()");
        }

        final StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (final StringExpression item : list) {
            joiner.add(item.script);
        }
        return new StringListExpression(joiner.toString());
    }

    @Override
    public NumericExpression mapNumericLiteral(String literal) {
        return new NumericExpression(literal);
    }

    @Override
    public StringExpression mapStringLiteral(String literal) {
        return new StringExpression(literal);
    }

    @Override
    public BooleanExpression mapBoolean(Boolean value) {
        return new BooleanExpression(value ? "true()" : "false()");
    }

    @Override
    public DateExpression mapDateLiteral(String literal) {
        return new DateExpression("xs:date(" + quoted(literal) + ")");
    }

    @Override
    public TimeExpression mapTimeLiteral(String literal) {
        return new TimeExpression("xs:time(" + quoted(literal) + ")");
    }

    @Override
    public DurationExpression mapDurationLiteral(String literal) {
        return new DurationExpression("xs:duration(" + quoted(literal) + ")");
    }

    @Override
    public BooleanExpression mapInListCondition(StringExpression expression,
            StringListExpression list) {
        return new BooleanExpression(String.format("%s = %s", expression.script, list.script));
    }

    @Override
    public BooleanExpression mapMatchesPatternCondition(StringExpression expression,
            String pattern) {
        return new BooleanExpression(
                String.format("fn:matches(normalize-space(%s), %s)", expression.script, pattern));
    }

    @Override
    public <T extends Expression> T mapParenthesizedExpression(T expression, Class<T> type) {
        try {
            Constructor<T> ctor = type.getConstructor(String.class);
            return ctor.newInstance("(" + expression.script + ")");
        } catch (Exception e) {
            throw new ParseCancellationException(e);
        }
    }

    @Override
    public Expression mapExternalReference(Expression externalReference) {
        // TODO: implement this properly.
        return new Expression("fn:doc('http://notice.service/" + externalReference.script + "')");
    }


    @Override
    public Expression mapFieldInExternalReference(Expression externalReference,
            PathExpression fieldReference) {
        return new Expression(externalReference.script + "/" + fieldReference.script);
    }


    /*** BooleanExpressions ***/


    @Override
    public BooleanExpression mapLogicalAnd(BooleanExpression leftOperand,
            BooleanExpression rightOperand) {
        return new BooleanExpression(
                String.format("%s and %s", leftOperand.script, rightOperand.script));
    }

    @Override
    public BooleanExpression mapLogicalOr(BooleanExpression leftOperand,
            BooleanExpression rightOperand) {
        return new BooleanExpression(
                String.format("%s or %s", leftOperand.script, rightOperand.script));
    }

    @Override
    public BooleanExpression mapLogicalNot(BooleanExpression condition) {
        return new BooleanExpression(String.format("not(%s)", condition.script));
    }

    @Override
    public BooleanExpression mapExistsExpression(PathExpression reference) {
        return new BooleanExpression(reference.script);
    }


    /*** Boolean functions ***/

    @Override
    public BooleanExpression mapStringContainsFunction(StringExpression haystack,
            StringExpression needle) {
        return new BooleanExpression("contains(" + haystack.script + ", " + needle.script + ")");
    }

    @Override
    public BooleanExpression mapStringStartsWithFunction(StringExpression text,
            StringExpression startsWith) {
        return new BooleanExpression("starts-with(" + text.script + ", " + startsWith.script + ")");
    }

    @Override
    public BooleanExpression mapStringEndsWithFunction(StringExpression text,
            StringExpression endsWith) {
        return new BooleanExpression("ends-with(" + text.script + ", " + endsWith.script + ")");
    }

    @Override
    public BooleanExpression mapComparisonOperator(Expression leftOperand, String operator,
            Expression rightOperand) {
        return new BooleanExpression(
                leftOperand.script + " " + operators.get(operator) + " " + rightOperand.script);
    }

    @Override
    public BooleanExpression mapDateSpanToDurationComparison(DateExpression leftDate, DateExpression rightDate, String operator,
            DurationExpression duration) {
        return new BooleanExpression(leftDate.script + " " + operators.get(operator) + " (" + rightDate.script + " " + operators.get("+") + " " + duration.script + ")");
    }

    /*** Numeric functions ***/

    @Override
    public NumericExpression mapCountFunction(PathExpression nodeSet) {
        return new NumericExpression("count(" + nodeSet.script + ")");
    }

    @Override
    public NumericExpression mapToNumberFunction(StringExpression text) {
        return new NumericExpression("number(" + text.script + ")");
    }

    @Override
    public NumericExpression mapSumFunction(PathExpression nodeSet) {
        return new NumericExpression("sum(" + nodeSet.script + ")");
    }

    @Override
    public NumericExpression mapStringLengthFunction(StringExpression text) {
        return new NumericExpression("string-length(" + text.script + ")");
    }

    @Override
    public NumericExpression mapNumericOperator(NumericExpression leftOperand, String operator,
            NumericExpression rightOperand) {
        return new NumericExpression(
                leftOperand.script + " " + operators.get(operator) + " " + rightOperand.script);
    }


    /*** String functions ***/

    @Override
    public StringExpression mapSubstringFunction(StringExpression text, NumericExpression start,
            NumericExpression length) {
        return new StringExpression(
                "substring(" + text.script + ", " + start.script + ", " + length.script + ")");
    }

    @Override
    public StringExpression mapSubstringFunction(StringExpression text, NumericExpression start) {
        return new StringExpression("substring(" + text.script + ", " + start.script + ")");
    }

    @Override
    public StringExpression mapNumberToStringFunction(NumericExpression number) {
        return new StringExpression("string(" + number.script + ")");
    }

    @Override
    public StringExpression mapStringConcatenationFunction(List<StringExpression> list) {
        return new StringExpression("concat("
                + list.stream().map(i -> i.script).collect(Collectors.joining(", ")) + ")");
    }

    @Override
    public StringExpression mapFormatNumberFunction(NumericExpression number,
            StringExpression format) {
        return new StringExpression("format-number(" + number.script + ", " + format.script + ")");
    }

    @Override
    public StringExpression mapString(String value) {
        return new StringExpression("'" + value + "'");
    }


    /*** Date functions ***/

    @Override
    public DateExpression mapDateFromStringFunction(StringExpression date) {
        return new DateExpression("xs:date(" + date.script + ")");
    }


    /*** Time functions ***/

    @Override
    public TimeExpression mapTimeFromStringFunction(StringExpression time) {
        return new TimeExpression("xs:time(" + time.script + ")");
    }


    /*** Duration functions ***/

    @Override
    public DurationExpression mapDurationFromDatesFunction(DateExpression startDate,
            DateExpression endDate) {
        return new DurationExpression(startDate.script + " - " + endDate.script);
    }

    private <T extends Expression> T instantiate(String value, Class<T> type) {
        try {
            Constructor<T> constructor = type.getConstructor(String.class);
            return constructor.newInstance(value);
        } catch (Exception e) {
            throw new ParseCancellationException(e);
        }
    }

    private String quoted(final String text) {
        return "'" + text.replaceAll("\"", "").replaceAll("'", "") + "'";
    }
}
