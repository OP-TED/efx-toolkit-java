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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;

import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.identifiers.Variable;

/**
 * Used to keep track of the current evaluation context. Extends Stack&lt;Context&gt; to provide
 * helper methods for pushing directly using a fieldId or a nodeId. The point is to make it easier
 * to use a context stack and reduce the possibility of coding mistakes.
 */
public class ContextStack extends Stack<Context> {

  private final SymbolResolver symbols;

  /**
   * The context of each context variable, by its declaration. The call stack finds the declaration
   * that a name refers to where it is read, and that declaration is a context variable if it has a
   * context here. A name declared again is another declaration, which has a context only if it is
   * given one.
   */
  private final Map<Variable, Context> contextsOfVariables = new IdentityHashMap<>();

  /**
   * The reads made where no context is declared.
   */
  private final Reads readsWithoutContext = new Reads();

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

  /**
   * Declares a context variable: gives a variable the context that it holds one element of.
   *
   * @param variable the declaration of the variable
   * @param context the context
   */
  public void declareContextVariable(final Variable variable, final Context context) {
    this.contextsOfVariables.put(variable, context);
  }

  /**
   * @param variable the declaration of a variable
   * @return the context that the variable holds one element of, or {@code null} if it is not a
   *         context variable
   */
  public Context getContextFromVariable(final Variable variable) {
    return this.contextsOfVariables.get(variable);
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
   * @return the reads made while the context at the top of the stack is the current one, or, if the
   *         stack is empty or its top is {@code null}, as EFX-1 pushes for an absolute reference,
   *         the reads made where no context is declared
   */
  public Reads readsInCurrentContext() {
    if (this.isEmpty() || this.peek() == null) {
      return this.readsWithoutContext;
    }

    return this.peek().reads();
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
