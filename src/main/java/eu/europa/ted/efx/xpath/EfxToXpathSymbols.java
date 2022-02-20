package eu.europa.ted.efx.xpath;


public class EfxToXpathSymbols {
	
	Object Fields;	// TODO: to be populated from the SDK or MDD
	Object Nodes;	// TODO: to be populated from the SDK or MDD

	/**
	 * Gets the id of the parent node of a given field.
	 * 
	 * @param fieldId 	The id of the field who's parent node we are looking for.
	 * @return			The id of the parent node of the given field.
	 */
	String getParentNodeOf(String fieldId) {
		return fieldId + "/..";		// dummy implementation
	}

	/**
	 * Dummy method added here just to make other dummy implementations more readable.
	 * 
	 * @param id	The id of a node or a field.
	 * @return		The xPath of the given node or field.
	 */
	String getXpathOf(String id) {
		return id;	// dummy implementation
	}

	/**
	 * Find the context of a rule that applies to a given field.
	 * The context of the rule applied to a field, is typically 
	 * the xPathAbsolute of that field's parent node.
	 * 
	 * @param fieldId The id of the field of which we want to find the context.
	 * @return The absolute xPath of the parent node of the passed field
	 */
	String getContextPathOfField(String fieldId) {
		//
		// Algorithm to be implemented.
		// Find the field with the given id,
		// and return the xPathAbsolute of its parent node.
		//
		return getXpathOf(getParentNodeOf(fieldId));	// dummy implementation
	}
	

	/**
	 * Find the context for nested predicate that applies to a given field 
	 * taking into account the pre-existing context.
	 * 
	 * @param fieldId
	 * @param broaderContextPath
	 * @return
	 */
	String getNarrowerContextPathOf(String fieldId, String broaderContextPath) {
		// 
		// Algorithm to be implemented:
		// 
		// First find the parent node of the given field
		// Then find the xPath of that parent node, relative to the given context
		// and return it.
		//
		return getRelativeXpathOfNode(getParentNodeOf(fieldId), broaderContextPath);
	}
	
	/**
	 * Gets the xPath of the given field relative to the given context.
	 * 
	 * @param fieldId	The id of the field for which we want to find the relative xPath
	 * @param contextPath	xPath indicating the context.
	 * @return The xPath of the given field relative to the given context.
	 */
	String getRelativeXpathOfField(String fieldId, String contextPath) {
		return contextPath + "//" + getXpathOf(fieldId);
	}

	/**
	 * Gets the xPath of a node relative to the given context.
	 * 
	 * @param nodeId		The id of the node for which we want the relative xPath
	 * @param contextPath	The xPath of the context.
	 * @return				The xPath of the given node relative to the given context.
	 */
	String getRelativeXpathOfNode(String nodeId, String contextPath) {
		return contextPath + "//" + getXpathOf(nodeId);
	}
}
