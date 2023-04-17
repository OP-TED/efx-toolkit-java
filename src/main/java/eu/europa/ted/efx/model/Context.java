package eu.europa.ted.efx.model;

import eu.europa.ted.efx.model.Expression.PathExpression;

/**
 * Used to store an evaluation context.
 * 
 * The context is stored both as an absolute and a relative path. The object also preserves the
 * symbol that was used to create the context as well as the type of that symbol (field or node).
 * 
 * This class is declared as abstract because it is only meant to be instantiated through its two
 * subclasses {@link FieldContext} and {@link NodeContext}. This makes instantiation more readable
 * and adds type safety at compile-time.
 */
public abstract class Context {

  /**
   * Instantiate this class to create a context from a field identifier.
   */
  public static class FieldContext extends Context {

    public FieldContext(final String fieldId, final PathExpression absolutePath,
        final PathExpression relativePath, final Variable<PathExpression> variable) {
      super(fieldId, absolutePath, relativePath, variable);
    }

    public FieldContext(final String fieldId, final PathExpression absolutePath,
        final PathExpression relativePath) {
      super(fieldId, absolutePath, relativePath);
    }

    public FieldContext(final String fieldId, final PathExpression absolutePath,
        final Variable<PathExpression> variable) {
      super(fieldId, absolutePath, variable);
    }

    public FieldContext(final String fieldId, final PathExpression absolutePath) {
      super(fieldId, absolutePath);
    }
  }

  /**
   * Instantiate this class to create a context from a node identifier.
   */
  public static class NodeContext extends Context {

    public NodeContext(final String nodeId, final PathExpression absolutePath,
        final PathExpression relativePath) {
      super(nodeId, absolutePath, relativePath);
    }

    public NodeContext(final String nodeId, final PathExpression absolutePath) {
      super(nodeId, absolutePath);
    }
  }

  private final String symbol;
  private final PathExpression absolutePath;
  private final PathExpression relativePath;
  private final Variable<PathExpression> variable;

  protected Context(final String symbol, final PathExpression absolutePath,
      final PathExpression relativePath, final Variable<PathExpression> variable) {
    this.variable = variable;
    this.symbol = symbol;
    this.absolutePath = absolutePath;
    this.relativePath = relativePath == null ? absolutePath : relativePath;
  }

  protected Context(final String symbol, final PathExpression absolutePath,
      final PathExpression relativePath) {
    this(symbol, absolutePath, relativePath, null);
  }

  protected Context(final String symbol, final PathExpression absolutePath,
      final Variable<PathExpression> variable) {
    this(symbol, absolutePath, absolutePath, variable);
  }

  protected Context(final String symbol, final PathExpression absolutePath) {
    this(symbol, absolutePath, absolutePath);
  }

  public Boolean isFieldContext() {
    return this.getClass().equals(FieldContext.class);
  }

  public Boolean isNodeContext() {
    return this.getClass().equals(NodeContext.class);
  }

  public Variable<PathExpression> variable() {
    return variable;
  }

  /**
   * Returns the [field or node] identifier that was used to create this context.
   */
  public String symbol() {
    return symbol;
  }

  /**
   * The absolute path of the context is needed when we want to create a new context relative to
   * this one.
   */
  public PathExpression absolutePath() {
    return absolutePath;
  }

  /**
   * Returns the relative path of the context.
   */
  public PathExpression relativePath() {
    return relativePath;
  }
}
