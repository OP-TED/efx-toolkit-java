package eu.europa.ted.efx.interfaces;

public interface SymbolMap {

    public String contextPathOfField(final String fieldId);

    public String contextPathOfField(final String fieldId, final String broaderContextPath);

    public String relativeXpathOfField(final String fieldId, final String contextPath);

    public String relativeXpathOfNode(final String nodeId, final String contextPath);

    public String typeOfField(final String fieldId);

    public String rootCodelistOfField(final String fieldId);

    public String expandCodelist(final String codelistId);

    public String mapOperator(final String operator);
}
