package eu.europa.ted.efx.interfaces;

public interface SymbolMap {

    public String parentNodeOfField(final String fieldId);

    public String contextPathOfField(final String fieldId);

    public String contextPathOfField(final String fieldId, final String contextPath);

    public String relativeXpathOfField(final String fieldId, final String contextPath);

    public String relativeXpathOfNode(final String nodeId, final String contextPath);

    public String absoluteXpathOfField(final String fieldId);

    public String absoluteXpathOfNode(final String nodeId);

    public String typeOfField(final String fieldId);

    public String rootCodelistOfField(final String fieldId);

    public String expandCodelist(final String codelistId);
}
