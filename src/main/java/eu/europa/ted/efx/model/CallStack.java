package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * The call stack is a stack of stack frames. Each stack frame represents a scope. The top of the
 * stack is the current scope. The bottom of the stack is the global scope.
 */
public class CallStack {

  private static final String TYPE_MISMATCH = "Type mismatch. Expected %s instead of %s.";
  private static final String UNDECLARED_IDENTIFIER = "Identifier not declared: ";
  private static final String IDENTIFIER_ALREADY_DECLARED = "Identifier already declared: ";
  private static final String STACK_UNDERFLOW =
      "Stack underflow. Return values were available in the dropped frame, but no stack frame is left to consume them.";

  /**
   * Stack frames are means of controlling the scope of variables and parameters. Certain
   * sub-expressions are scoped, meaning that variables and parameters are only available within the
   * scope of the sub-expression.
   */
  class StackFrame extends Stack<CallStackObject> {

    /**
     * Keeps a list of all identifiers declared in the current scope as well as their type.
     */
    Map<String, Class<? extends Expression>> typeRegister =
        new HashMap<String, Class<? extends Expression>>();

    /**
     * Keeps a list of all parameter values declared in the current scope.
     */
    Map<String, Expression> valueRegister = new HashMap<String, Expression>();

    /**
     * Registers a parameter identifier and pushes a parameter declaration on the current stack
     * frame. Also stores the parameter value.
     * @param parameterName The name of the parameter.
     * @param parameterDeclarationExpression The expression used to declare the parameter.
     * @param parameterValue The value passed to the parameter.
     */
    void pushParameterDeclaration(String parameterName, Expression parameterDeclarationExpression,
        Expression parameterValue) {
      this.declareIdentifier(parameterName, parameterDeclarationExpression.getClass());
      this.storeValue(parameterName, parameterValue);
      this.push(parameterDeclarationExpression);
    }

    /**
     * Registers a variable identifier and pushes a variable declaration on the current stack frame.
     * @param variableName The name of the variable.
     * @param variableDeclarationExpression The expression used to declare the variable.
     */
    void pushVariableDeclaration(String variableName, Expression variableDeclarationExpression) {
      this.declareIdentifier(variableName, variableDeclarationExpression.getClass());
      this.push(variableDeclarationExpression);
    }

    /**
     * Registers an identifier in the current scope. This registration is later used to check if an
     * identifier is declared in the current scope.
     * @param identifier The identifier to register.
     * @param type The type of the identifier.
     */
    void declareIdentifier(String identifier, Class<? extends Expression> type) {
      this.typeRegister.put(identifier, type);
    }

    /**
     * Used to store parameter values.
     * @param identifier The identifier of the parameter.
     * @param value The value of the parameter.
     */
    void storeValue(String identifier, Expression value) {
      this.valueRegister.put(identifier, value);
    }

    /**
     * Returns the object at the top of the stack and removes it from the stack. The object must be
     * of the expected type.
     * @param expectedType The type that the returned object is expected to have.
     * @return The object removed from the top of the stack.
     */
    synchronized <T extends CallStackObject> T pop(Class<T> expectedType) {
      Class<? extends CallStackObject> actualType = this.peek().getClass();
      if (!expectedType.isAssignableFrom(actualType) && !actualType.equals(Expression.class)) {
        throw new ParseCancellationException(String.format(TYPE_MISMATCH,
            expectedType.getSimpleName(), actualType.getSimpleName()));
      }
      return expectedType.cast(this.pop());
    }

    /**
     * Clears the stack frame and all its registers.
     */
    @Override
    public void clear() {
      super.clear();
      this.typeRegister.clear();
      this.valueRegister.clear();
    }
  }

  /**
   * The stack of stack frames.
   */
  Stack<StackFrame> frames;

  /**
   * Default and only constructor. Adds a global scope to the stack.
   */
  public CallStack() {
    this.frames = new Stack<>();
    this.frames.push(new StackFrame()); // The global scope
  }

  /**
   * Creates a new stack frame and pushes it on top of the call stack.
   * 
   * This method is called at the begin boundary of scoped sub-expression to allow for the
   * declaration of local variables.
   */
  public void pushStackFrame() {
    this.frames.push(new StackFrame());
  }

  /**
   * Drops the current stack frame and passes the return values to the previous stack frame.
   * 
   * This method is called at the end boundary of scoped sub-expressions. Variables local to the
   * sub-expression must go out of scope and the return values are passed to the parent expression.
   */
  public void popStackFrame() {
    StackFrame droppedFrame = this.frames.pop();

    // If the dropped frame is not empty, then it contains return values that should
    // be passed to the next frame on the stack.
    if (droppedFrame.size() > 0) {
      if (this.frames.empty()) {
        throw new ParseCancellationException(STACK_UNDERFLOW);
      }
      this.frames.peek().addAll(droppedFrame);
    }
  }

  /**
   * Pushes a parameter declaration on the current stack frame. Checks if another identifier with
   * the same name is already declared in the current scope.
   * 
   * @param parameterName The name of the parameter.
   * @param parameterDeclaration The expression used to declare the parameter.
   * @param parameterValue The value passed to the parameter.
   * @throws ParseCancellationException if another identifier with the same name is already
   *        declared in the current scope.
   */
  public void pushParameterDeclaration(String parameterName, Expression parameterDeclaration,
      Expression parameterValue) {
    if (this.inScope(parameterName)) {
      throw new ParseCancellationException(
          IDENTIFIER_ALREADY_DECLARED + parameterDeclaration.script);
    }
    this.frames.peek().pushParameterDeclaration(parameterName, parameterDeclaration,
        parameterValue);
  }

  /**
   * Pushes a variable declaration on the current stack frame. Checks if another identifier with the
   * same name is already declared in the current scope.
   * 
   * @param variableName The name of the variable.
   * @param variableDeclaration The expression used to declare the variable.
   */
  public void pushVariableDeclaration(String variableName, Expression variableDeclaration) {
    if (this.inScope(variableName)) {
      throw new ParseCancellationException(
          IDENTIFIER_ALREADY_DECLARED + variableDeclaration.script);
    }
    this.frames.peek().pushVariableDeclaration(variableName, variableDeclaration);
  }

  /**
   * Declares a template variable. Template variables are tracked to ensure proper scoping. However,
   * their declaration is not pushed on the stack as they are declared at the template level (in
   * Markup) and not at the expression level (not in the target language script).
   * 
   * @param variableName The name of the variable.
   * @param variableType The type of the variable.
   */
  public void declareTemplateVariable(String variableName,
      Class<? extends Expression> variableType) {
    if (this.inScope(variableName)) {
      throw new ParseCancellationException(IDENTIFIER_ALREADY_DECLARED + variableName);
    }
    this.frames.peek().declareIdentifier(variableName, variableType);
  }

  /**
   * Checks if an identifier is declared in the current scope.
   * 
   * @param identifier The identifier to check.
   * @return True if the identifier is declared in the current scope.
   */
  boolean inScope(String identifier) {
    return this.frames.stream().anyMatch(
        f -> f.typeRegister.containsKey(identifier) || f.valueRegister.containsKey(identifier));
  }

  /**
   * Returns the stack frame containing the given identifier.
   * 
   * @param identifier The identifier to look for.
   * @return The stack frame containing the given identifier or null if no such stack frame exists.
   */
  StackFrame findFrameContaining(String identifier) {
    return this.frames.stream()
        .filter(
            f -> f.typeRegister.containsKey(identifier) || f.valueRegister.containsKey(identifier))
        .findFirst().orElse(null);
  }

  /**
   * Gets the value of a parameter.
   * 
   * @param identifier The identifier of the parameter.
   * @return The value of the parameter.
   */
  Optional<Expression> getParameter(String identifier) {
    return this.frames.stream().filter(f -> f.valueRegister.containsKey(identifier)).findFirst()
        .map(x -> x.valueRegister.get(identifier));
  }

  /**
   * Gets the type of a variable.
   * 
   * @param identifier The identifier of the variable.
   * @return The type of the variable.
   */
  Optional<Class<? extends Expression>> getVariable(String identifier) {
    return this.frames.stream().filter(f -> f.typeRegister.containsKey(identifier)).findFirst()
        .map(x -> x.typeRegister.get(identifier));
  }

  /**
   * Gets the type of a variable.
   * 
   * @param identifier The identifier of the variable.
   * @return The type of the variable.
   */
  public Class<? extends Expression> getTypeOfIdentifier(String identifier) {
    Optional<Class<? extends Expression>> type = this.getVariable(identifier);
    if (!type.isPresent()) {
      throw new ParseCancellationException(UNDECLARED_IDENTIFIER + identifier);
    }
    return type.get();
  }

  /**
   * Pushes a variable reference on the current stack frame. Makes sure there is no name collision
   * with other identifiers already in scope.
   * 
   * @param variableName The name of the variable.
   * @param variableReference The variable to push on the stack.
   * @throws ParseCancellationException if the variable is not declared in the current scope.
   */
  public void pushVariableReference(String variableName, Expression variableReference) {
    getParameter(variableName).ifPresentOrElse(parameterValue -> this.push(parameterValue),
        () -> getVariable(variableName).ifPresentOrElse(
            variableType -> this.pushVariableReference(variableReference, variableType),
            () -> {
              throw new ParseCancellationException(UNDECLARED_IDENTIFIER + variableName);
            }));
  }

  /**
   * Pushes a variable reference on the current stack frame. This method is private because it is
   * only used for to improve the readability of its public counterpart.
   * @param variableReference The variable to push on the stack.
   * @param variableType The type of the variable.
   */
  private void pushVariableReference(Expression variableReference,
      Class<? extends Expression> variableType) {
    this.frames.peek().push(Expression.instantiate(variableReference.script, variableType));
  }

  /**
   * Pushes an object on the current stack frame. No checks, no questions asked.
   * 
   * @param item The object to push on the stack.
   */
  public void push(CallStackObject item) {
    this.frames.peek().push(item);
  }

  /**
   * Gets the object at the top of the current stack frame and removes it from the stack.
   * 
   * @param <T> The type of the object at the top of the current stack frame.
   * @param expectedType The that the returned object is expected to have.
   * @return The object at the top of the current stack frame.
   */
  public synchronized <T extends CallStackObject> T pop(Class<T> expectedType) {
    return this.frames.peek().pop(expectedType);
  }

  /**
   * Gets the object at the top of the current stack frame without removing it from the stack.
   * 
   * @return The object at the top of the current stack frame.
   */
  public synchronized CallStackObject peek() {
    return this.frames.peek().peek();
  }

  /**
   * Gets the number of elements in the current stack frame.
   * 
   * @return The number of elements in the current stack frame.
   */
  public int size() {
    return this.frames.peek().size();
  }

  /**
   * Checks if the current stack frame is empty.
   * 
   * @return True if the current stack frame is empty.
   */
  public boolean empty() {
    return this.frames.peek().empty();
  }

  /**
   * Clears the current stack frame.
   */
  public void clear() {
    this.frames.peek().clear();
  }
}
