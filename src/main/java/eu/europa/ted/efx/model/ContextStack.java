package eu.europa.ted.efx.model;

import java.util.Stack;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.Expression.PathExpression;

/**
 * Used to keep trak of the current evaluation context. Extends Stack&lt;Context&gt; to provide
 * helper methods for pushing directly using a fieldId or a nodeId. The point is to make it easier
 * to use a context stack and reduce the possibility of coding mistakes.
 */
public class ContextStack extends Stack<Context> {

  private final SymbolResolver symbols;

  /**
   * Creates a new ContextStack.
   * 
   * @param symbols the SymbolMap is used to resolve fieldIds and nodeIds.
   */
  public ContextStack(final SymbolResolver symbols) {
    this.symbols = symbols;
  }

  /**
   * Creates a new Context for the given field and places it at the top of the stack. The new
   * Context is determined by the field itself, and it is made to be relative to the one currently
   * at the top of the stack (or absolute if the stack is empty).
   */
  public FieldContext pushFieldContext(final String fieldId) {
    PathExpression absolutePath = symbols.getAbsolutePathOfField(fieldId);
    if (this.isEmpty()) {
      FieldContext context = new FieldContext(fieldId, absolutePath);
      this.push(context);
      return context;
    }
    PathExpression relativePath = symbols.getRelativePathOfField(fieldId, this.absolutePath());
    FieldContext context = new FieldContext(fieldId, absolutePath, relativePath);
    this.push(context);
    return context;
  }

  /**
   * Creates a new Context for the given node and places it at the top of the stack. The new Context
   * is relative to the one currently at the top of the stack (or absolute if the stack is empty).
   */
  public NodeContext pushNodeContext(final String nodeId) {
    PathExpression absolutePath = symbols.getAbsolutePathOfNode(nodeId);
    if (this.isEmpty()) {
      NodeContext context = new NodeContext(nodeId, absolutePath);
      this.push(context);
      return context;
    }
    PathExpression relativePath = symbols.getRelativePathOfNode(nodeId, this.absolutePath());
    NodeContext context = new NodeContext(nodeId, absolutePath, relativePath);
    this.push(context);
    return context;
  }

  /**
   * Returns true if the context at the top of the stack is a {@link FieldContext}. Does not remove
   * the context from the stack.
   */
  public Boolean isFieldContext() {
    if (this.isEmpty() || this.peek() == null) {
      return null;
    }

    return this.peek().isFieldContext();
  }

  /**
   * Returns true if the context at the top of the stack is a {@link NodeContext}. Does not remove
   * the context from the stack.
   */
  public Boolean isNodeContext() {
    if (this.isEmpty() || this.peek() == null) {
      return null;
    }

    return this.peek().isNodeContext();
  }

  /**
   * Returns the [field or node] identifier that was used to create the context that is currently at
   * the top of the stack. Does not remove the context from the stack.
   */
  public String symbol() {
    if (this.isEmpty() || this.peek() == null) {
      return null;
    }

    return this.peek().symbol();
  }

  /**
   * Returns the absolute path of the context that is currently at the top of the stack. Does not
   * remove the context from the stack.
   */
  public PathExpression absolutePath() {
    if (this.isEmpty() || this.peek() == null) {
      return null;
    }

    return this.peek().absolutePath();
  }

  /**
   * Returns the relative path of the context that is currently at the top of the stack. Does not
   * remove the context from the stack.
   */
  public PathExpression relativePath() {
    if (this.isEmpty() || this.peek() == null) {
      return null;
    }

    return this.peek().relativePath();
  }
}
