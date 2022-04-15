package eu.europa.ted.efx.interfaces;

import java.util.List;

public interface SymbolMap {

    public String parentNodeOfField(final String fieldId);

    public String relativeXpathOfField(final String fieldId, final String contextPath);

    public String relativeXpathOfNode(final String nodeId, final String contextPath);

    public String absoluteXpathOfField(final String fieldId);

    public String absoluteXpathOfNode(final String nodeId);

    public String typeOfField(final String fieldId);

    public String rootCodelistOfField(final String fieldId);

    public List<String> expandCodelist(final String codelistId);
}
