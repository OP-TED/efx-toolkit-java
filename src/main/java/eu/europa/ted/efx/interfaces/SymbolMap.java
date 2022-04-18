package eu.europa.ted.efx.interfaces;

import java.util.List;
import eu.europa.ted.efx.model.Expression.PathExpression;

public interface SymbolMap {

    public String parentNodeOfField(final String fieldId);

    public PathExpression relativeXpathOfField(final String fieldId, final PathExpression contextPath);

    public PathExpression relativeXpathOfNode(final String nodeId, final PathExpression contextPath);

    public PathExpression absoluteXpathOfField(final String fieldId);

    public PathExpression absoluteXpathOfNode(final String nodeId);

    public String typeOfField(final String fieldId);

    public String rootCodelistOfField(final String fieldId);

    public List<String> expandCodelist(final String codelistId);
}
