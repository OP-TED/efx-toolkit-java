package eu.europa.ted.efx.model;

import eu.europa.ted.efx.interfaces.SymbolMap;

/**
 * Used to store an evaluation context. The context is stored both as an absolute and a relative
 * path.
 */
public class Context {
    private final String absolutePath;
    private final String relativePath;

    public Context(String absolutePath, String relativePath) {
        this.absolutePath = absolutePath;
        this.relativePath = relativePath == null ? absolutePath : relativePath;
    }

    public Context(String absolutePath) {
        this(absolutePath, absolutePath);
    }


    /**
     * The absolute path of the context is needed when we want to create a new context relative to
     * this one.
     */
    public String absolutePath() {
        return absolutePath;
    }

    /**
     * Returns the relative path of the context.
     */
    public String relativePath() {
        return relativePath;
    }

    /**
     * Creates a new Context for the given field, relative to this.
     */
    public Context createNestedContextForField(String fieldId, SymbolMap symbols) {
        return new Context(symbols.contextPathOfField(fieldId),
                symbols.relativeXpathOfNode(symbols.parentNodeOfField(fieldId), this.absolutePath()));
    }

    /**
     * Creates a new Context for the given node, relative to this.
     */
    public Context createNestedContextForNode(String nodeId, SymbolMap symbols) {
        return new Context(symbols.absoluteXpathOfNode(nodeId),
                symbols.relativeXpathOfNode(nodeId, this.absolutePath()));
    }

    /**
     * Creates a new Context for the given field. No parent context is specified so the relative
     * path is the same as the absolute path.
     */
    public static Context fromFieldId(String fieldId, SymbolMap symbols) {
        return new Context(symbols.contextPathOfField(fieldId));
    }

    /**
     * Creates a new Context for the given node. No parent context is specified so the relative path
     * is the same as the absolute path.
     */
    public static Context fromNodeId(String nodeId, SymbolMap symbols) {
        return new Context(symbols.absoluteXpathOfNode(nodeId));
    }

    /**
     * Creates a new Context for the given field, relative to the given parent context.
     */
    public static Context fromFieldId(String fieldId, Context parentContext, SymbolMap symbols) {
        if (parentContext == null) {
            return fromFieldId(fieldId, symbols);
        }
        return parentContext.createNestedContextForField(fieldId, symbols);
    }

    /**
     * Creates a new Context for the given node, relative to the given parent context.
     */
    public static Context fromNodeId(String nodeId, Context parentContext, SymbolMap symbols) {
        if (parentContext == null) {
            return fromNodeId(nodeId, symbols);
        }
        return parentContext.createNestedContextForNode(nodeId, symbols);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Context other = (Context) obj;
        return this.absolutePath.equals(other.absolutePath);
    }
}
