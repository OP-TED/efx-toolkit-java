package eu.europa.ted.efx.interfaces;

import java.util.List;
import eu.europa.ted.efx.model.Expression.PathExpression;

/**
 * EFX expressions contain references to eForms fields and nodes. These references are in the form
 * of field or node identifiers (a.k.a. symbols). The role of the {@link SymbolResolver} is to
 * provide further information on these referenced fields or nodes by looking them up in a
 * repository.
 */
public interface SymbolResolver {

        /**
         * Gets the identifier of the parent node of the given field.
         * 
         * @param fieldId The identifier of the field to look for.
         * @return The identifier of the parent node of the given field.
         */
        public String parentNodeOfField(final String fieldId);

        /**
         * Gets the path that can be used to locate the give field in the data source, relative to
         * another given path.
         * 
         * @param fieldId The identifier of the field to look for.
         * @param contextPath The path relative to which we expect to find the return value.
         * @return The path to the given field relative to the given context path.
         */
        public PathExpression relativePathOfField(final String fieldId,
                        final PathExpression contextPath);

        /**
         * Gets the path that can be used to locate the given node in the data source, relative to
         * another given path.
         * 
         * @param nodeId The identifier of the node to look for.
         * @param contextPath The path relative to which we expect to find the return value.
         * @return The path to the given node relative to the given context path.
         */
        public PathExpression relativePathOfNode(final String nodeId,
                        final PathExpression contextPath);

        /**
         * Gets the absolute path that can be used to locate a field in the data source.
         * 
         * @param fieldId The identifier of the field to look for.
         * @return The absolute path to the field as a PathExpression.
         */
        public PathExpression absolutePathOfField(final String fieldId);

        /**
         * Gets the absolute path the can be used to locate a node in the data source.
         * 
         * @param nodeId The identifier of the node to look for.
         * @return The absolute path to the node as a PathExpression.
         */
        public PathExpression absolutePathOfNode(final String nodeId);

        /**
         * Gets the type of the given field.
         * 
         * @param fieldId The identifier of the field to look for.
         * @return The type of the field as a string.
         */
        public String typeOfField(final String fieldId);

        /**
         * Gets the codelist associated with the given field. If the codelist is a tailored codelist
         * the this method will return the its codelist.
         * 
         * @param fieldId The identifier of the field to look for.
         * @return The "root" codelist associated ith the given field.
         */
        public String rootCodelistOfField(final String fieldId);

        /**
         * Gets the list of all codes in a given codelist as a list of strings.
         * 
         * @param codelistId The identifier of the codelist to expand.
         * @return The list of codes in the given codelist.
         */
        public List<String> expandCodelist(final String codelistId);
}
