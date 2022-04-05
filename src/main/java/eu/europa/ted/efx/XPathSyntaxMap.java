package eu.europa.ted.efx;

import static java.util.Map.entry;
import java.util.Map;
import eu.europa.ted.efx.interfaces.SyntaxMap;

public class XPathSyntaxMap implements SyntaxMap {

    /**
     * Maps efx operators to xPath operators.
     */
    static final Map<String, String> operators = Map.ofEntries(entry("+", "+"), entry("-", "-"),
            entry("*", "*"), entry("/", "div"), entry("%", "mod"), entry("and", "and"),
            entry("or", "or"), entry("not", "not"), entry("==", "="), entry("!=", "!="),
            entry("<", "<"), entry("<=", "<="), entry(">", ">"), entry(">=", ">="));

    @Override
    public String mapOperator(String operator) {
        return XPathSyntaxMap.operators.get(operator);
    }
}
