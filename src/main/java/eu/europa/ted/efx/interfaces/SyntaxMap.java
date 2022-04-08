package eu.europa.ted.efx.interfaces;

import java.util.List;

public interface SyntaxMap {
    public String mapOperator(String leftOperand, String operator, String rightOperand);
    
    public String mapNodeReferenceWithPredicate(final String nodeReference, final String predicate);

    public String mapFieldReferenceWithPredicate(final String fieldReference, final String predicate);

    public String mapFieldTextReference(final String fieldReference);

    public String mapAttributeReference(final String fieldReference, String attribute);

    public String mapFunctionCall(final String functionName, final List<String> arguments);

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

    public String mapFieldInExternalReference(final String externalReference, final String fieldReference);
}
