package eu.europa.ted.efx;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import eu.europa.ted.efx.interfaces.SyntaxMap;

public class XPathSyntaxMap implements SyntaxMap {

    /**
     * Maps efx operators to xPath operators.
     */
    static final Map<String, String> operators = Map.ofEntries(entry("+", "+"), entry("-", "-"),
            entry("*", "*"), entry("/", "div"), entry("%", "mod"),
            entry("==", "="), entry("!=", "!="),
            entry("<", "<"), entry("<=", "<="), entry(">", ">"), entry(">=", ">="));

    @Override
    public String mapOperator(String leftOperand, String operator, String rightOperand) {
        return String.format("%s %s %s", leftOperand, XPathSyntaxMap.operators.get(operator), rightOperand);    
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
    public String mapFieldTextReference(String fieldReference) {
        return fieldReference + "/normalize-space(text())";
    }

    @Override
    public String mapAttributeReference(String fieldReference, String attribute) {
        return fieldReference + "/@" + attribute;
    }

    @Override
    public String mapFunctionCall(String functionName, List<String> arguments) {
        final StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (final String argument : arguments) {
            joiner.add(argument);
        }        
        return functionName + joiner.toString();
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
    public String mapInListCondition(String expression, String List) {
        return String.format("%s = %s", expression, List);
    }

    @Override
    public String mapMatchesPatternCondition(String expression, String pattern) {
        return "fn:matches(normalize-space(" + expression + "), " + pattern + ")";
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
}
