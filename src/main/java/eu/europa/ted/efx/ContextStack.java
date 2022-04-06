package eu.europa.ted.efx;

import java.util.Stack;
import eu.europa.ted.efx.interfaces.SymbolMap;

/**
 * Used to keep trak of the current evaluation context. Extends Stack<Context> to provide helper
 * methods for pushing directly using a fieldId or a nodeId. The point is to make it easier to use a
 * context stack and reduce the possibility of coding mistakes.
 */
public class ContextStack extends Stack<Context> {

    private final SymbolMap symbols;

    /**
     * Creates a new ContextStack.
     * 
     * @param symbols the SymbolMap is used to resolve fieldIds and nodeIds.
     */
    public ContextStack(SymbolMap symbols) {
        this.symbols = symbols;
    }

    /**
     * Creates a new Context for the given field and places it at the top of the stack. The new
     * Context is relative to the one currently at the top of the stack (or absolute if the stack is
     * empty).
     */
    public Context pushFieldContext(String fieldId) {
        String absolutePath = symbols.contextPathOfField(fieldId);
        if (this.isEmpty()) {
            return this.push(new Context(absolutePath));
        }
        String relativePath = symbols.contextPathOfField(fieldId, this.peek().absolutePath());
        return this.push(new Context(absolutePath, relativePath));
    }

    /**
     * Creates a new Context for the given node and places it at the top of the stack. The new
     * Context is relative to the one currently at the top of the stack (or absolute if the stack is
     * empty).
     */
    public Context pushNodeContext(String nodeId) {
        String absolutePath = symbols.absoluteXpathOfNode(nodeId);
        if (this.isEmpty()) {
            return this.push(new Context(absolutePath));
        }
        String relativePath = symbols.relativeXpathOfNode(nodeId, this.peek().absolutePath());
        return this.push(new Context(absolutePath, relativePath));
    }

    public String absolutePath() {
        if (this.isEmpty() || this.peek() == null) {
            return null;
        }

        return this.peek().absolutePath();
    }

    public String relativePath() {
        if (this.isEmpty() || this.peek() == null) {
            return null;
        }

        return this.peek().relativePath();
    }
}