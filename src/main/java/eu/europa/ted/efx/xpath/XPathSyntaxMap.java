package eu.europa.ted.efx.xpath;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import eu.europa.ted.efx.interfaces.SyntaxMap;

public class XPathSyntaxMap implements SyntaxMap {

    /**
     * Maps efx operators to xPath operators.
     */
    private static final Map<String, String> operators = Map.ofEntries(entry("+", "+"), entry("-", "-"),
            entry("*", "*"), entry("/", "div"), entry("%", "mod"),
            entry("==", "="), entry("!=", "!="),
            entry("<", "<"), entry("<=", "<="), entry(">", ">"), entry(">=", ">="));

    @Override
    public String mapOperator(String leftOperand, String operator, String rightOperand) {
        return String.format("%s %s %s", leftOperand, operators.get(operator), rightOperand);    
    }

    @Override
    public String mapNodeReferenceWithPredicate(String nodeReference, String predicate) {
        return nodeReference + '[' + predicate + ']';
    }

    @Override
    public String mapFieldReferenceWithPredicate(String fieldReference, String predicate) {
        return fieldReference + '[' + predicate + ']';
    }

    @Override
    public String mapFieldValueReference(String fieldReference) {
        return fieldReference + "/normalize-space(text())";
    }

    @Override
    public String mapFieldAttributeReference(String fieldReference, String attribute) {
        return fieldReference + "/@" + attribute;
    }

    @Override
    public String mapList(final List<String> list) {
        if (list == null || list.isEmpty()) {
            return "()";
        }

        final StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (final String item : list) {
            joiner.add('\'' + item.replaceAll("^'|'$", "") + '\'');
        }
        return joiner.toString();
    }

    @Override
    public String mapLiteral(String literal) {
        return literal;
    }

    @Override
    public String mapBoolean(Boolean value) {
        return value ? "true()" : "false()";
    }



    @Override
    public String mapInListCondition(String expression, String List) {
        return String.format("%s = %s", expression, List);
    }

    @Override
    public String mapMatchesPatternCondition(String expression, String pattern) {
        return String.format("fn:matches(normalize-space(%s), %s)", expression, pattern);
    }

    @Override
    public String mapParenthesizedExpression(String expression) {
        return "(" + expression + ")";
    }

    @Override
    public String mapExternalReference(String reference) {
        // TODO: implement this properly
        return "fn:doc('http://notice.service/" + reference + "')";
    }


    @Override
    public String mapFieldInExternalReference(String externalReference, String fieldReference) {
        return externalReference + "/" + fieldReference;
    }

    /*** BooleanExpressions ***/


    @Override
    public String mapLogicalAnd(String leftOperand, String rightOperand) {
        return String.format("%s and %s", leftOperand, rightOperand);
    }

    @Override
    public String mapLogicalOr(String leftOperand, String rightOperand) {
        return String.format("%s or %s", leftOperand, rightOperand);
    }

    @Override
    public String mapLogicalNot(String condition) {
        return String.format("not(%s)", condition);
    }

    @Override
    public String mapExistsExpression(String reference) {
        return reference;
    }
    
    
    /*** Boolean functions ***/

    @Override
    public String mapStringContainsFunction(String haystack, String needle) {
        return "contains(" + haystack + ", " + needle + ")";
    }

    @Override
    public String mapStringStartsWithFunction(String text, String startsWith) {
        return "starts-with(" + text + ", " + startsWith + ")";
    }

    @Override
    public String mapStringEndsWithFunction(String text, String endsWith) {
        return "ends-with(" + text + ", " + endsWith + ")";
    }


    /*** Numeric functions ***/

    @Override
    public String mapCountFunction(String nodeSet) {
        return "count(" + nodeSet + ")";
    }

    @Override
    public String mapToNumberFunction(String text) {
        return "number(" + text + ")";
    }

    @Override
    public String mapSumFunction(String nodeSet) {
        return "sum(" + nodeSet + ")";
    }

    @Override
    public String mapStringLengthFunction(String text) {
        return "string-length(" + text + ")";
    }


    /*** String functions ***/

    @Override
    public String mapSubstringFunction(String text, String start, String length) {
        return "substring(" + text + ", " + start + ", " + length + ")";
    }

    @Override
    public String mapSubstringFunction(String text, String start) {
        return "substring(" + text + ", " + start + ")";
    }

    @Override
    public String mapNumberToStringFunction(String number) {
        return "string(" + number + ")";
    }

    @Override
    public String mapStringConcatenationFunction(List<String> list) {
        return "concat(" + String.join(", ", list) + ")";
    }

    @Override
    public String mapFormatNumberFunction(String number, String format) {
        return "format-number(" + number + ", " + format + ")";
    }


    /*** Date functions ***/

    @Override
    public String mapDateFromStringFunction(String date) {
        return "xs:date(" + date + ")";
    }


    /*** Time functions ***/

    @Override
    public String mapTimeFromStringFunction(String time) {
        return "xs:time(" + time + ")";
    }


    /*** Duration functions ***/

    @Override
    public String mapDurationFromDatesFunction(String startDate, String endDate) {
        return "(xs:date(" + startDate + ") - xs:date(" + endDate + "))";
    }



    private static String ensureQuotes(String text) {
        if (text.startsWith("'") && text.endsWith("'")) {
            return text;
        }

        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.replaceAll("^\"|\"$", "");
            if (text.contains("'")) {
                text = text.replaceAll("'", "\'");
            }
        }

        return "'" + text + "'";
    }
}
