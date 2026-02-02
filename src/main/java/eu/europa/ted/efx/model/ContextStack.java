/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.PathExpression;

/**
 * Used to keep track of the current evaluation context. Extends Stack&lt;Context&gt; to provide
 * helper methods for pushing directly using a fieldId or a nodeId. The point is to make it easier
 * to use a context stack and reduce the possibility of coding mistakes.
 */
public class ContextStack extends Stack<Context> {

  private final SymbolResolver symbols;

  private final Map<String, Context> variables = new HashMap<>();

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
   * 
   * @param fieldId the field to create a context for.
   * @return the new FieldContext.
   */
  public FieldContext pushFieldContext(final String fieldId) {
    PathExpression absolutePath = symbols.getAbsolutePathOfField(fieldId);
    if (this.isEmpty()) {
      FieldContext context = new FieldContext(fieldId, absolutePath);
      this.push(context);
      return context;
    }
    PathExpression relativePath = symbols.getRelativePathOfField(fieldId, this.symbol());
    FieldContext context = new FieldContext(fieldId, absolutePath, relativePath);
    this.push(context);
    return context;
  }

  /**
   * Creates a new Context for the given node and places it at the top of the stack. The new Context
   * is relative to the one currently at the top of the stack (or absolute if the stack is empty).
   * 
   * @param nodeId the id node to create a context for.
   * @return the new NodeContext.
   */
  public NodeContext pushNodeContext(final String nodeId) {
    PathExpression absolutePath = symbols.getAbsolutePathOfNode(nodeId);
    if (this.isEmpty()) {
      NodeContext context = new NodeContext(nodeId, absolutePath);
      this.push(context);
      return context;
    }
    PathExpression relativePath = symbols.getRelativePathOfNode(nodeId, this.symbol());
    NodeContext context = new NodeContext(nodeId, absolutePath, relativePath);
    this.push(context);
    return context;
  }

  public void declareContextVariable(final String variableName, final Context variableValue) {
    this.variables.put(variableName, variableValue);
  }

  public Context getContextFromVariable(final String variableName) {
    return this.variables.get(variableName);
  }

  /**
   * Returns true if the context at the top of the stack is a {@link FieldContext}. Does not remove
   * the context from the stack.
   * 
   * @return true if the context at the top of the stack is a {@link FieldContext}.
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
   * 
   * @return true if the context at the top of the stack is a {@link NodeContext}.
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
   * 
   * @return the [field or node] identifier that was used to create the context that is currently at
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
   * 
   * @return the absolute path of the context that is currently at the top of the stack.
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
   *
   * @return the relative path of the context that is currently at the top of the stack.
   */
  public PathExpression relativePath() {
    if (this.isEmpty() || this.peek() == null) {
      return null;
    }

    return this.peek().relativePath();
  }

  /**
   * Returns the parent context (second from top of the stack) without removing it.
   * This is useful for the ".." context shortcut which refers to the grandparent context.
   *
   * @return the parent context, or null if there are fewer than 2 contexts on the stack.
   */
  public Context peekParentContext() {
    if (this.size() < 2) {
      return null;
    }
    return this.get(this.size() - 2);
  }
}
