package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.variables.Identifier;
import eu.europa.ted.efx.model.variables.Parameter;

/**
 * The call stack is a stack of stack frames. Each stack frame represents a
 * scope. The top of the
 * stack is the current scope. The bottom of the stack is the global scope.
 */
public class CallStack {

  private static final String TYPE_MISMATCH = "Type mismatch. Expected %s instead of %s.";
  private static final String UNDECLARED_IDENTIFIER = "Identifier not declared: ";
  private static final String IDENTIFIER_ALREADY_DECLARED = "Identifier already declared: ";
  private static final String STACK_UNDERFLOW = "Stack underflow. Return values were available in the dropped frame, but no stack frame is left to consume them.";

  /**
   * Stack frames are means of controlling the scope of variables and parameters.
   * Certain
   * sub-expressions are scoped, meaning that variables and parameters are only
   * available within the
   * scope of the sub-expression.
   */
  class StackFrame extends Stack<CallStackObject> {

    /**
     * Keeps a list of all identifiers declared in the current scope as well as
     * their type.
     */
    Map<String, Identifier> identifierRegistry = new HashMap<String, Identifier>();

    /**
     * Registers an identifier in the current scope. This registration is later used
     * to check if an
     * identifier is declared in the current scope.
     * 
     * @param identifier The identifier to register.
     * @param dataType       The type of the identifier.
     */
    void declareIdentifier(Identifier identifier) {
      this.identifierRegistry.put(identifier.name, identifier);
    }

    /**
     * Returns the object at the top of the stack and removes it from the stack. The
     * object must be
     * of the expected type.
     * 
     * @param expectedType The type that the returned object is expected to have.
     * @return The object removed from the top of the stack.
     */
    synchronized <T extends CallStackObject> T pop(Class<T> expectedType) {
      Class<? extends CallStackObject> actualType = this.peek().getClass();
      if (expectedType.isAssignableFrom(actualType)) {
        return expectedType.cast(this.pop());
      }

      if (TypedExpression.class.isAssignableFrom(actualType) && TypedExpression.class.isAssignableFrom(expectedType)) {
        var actual = actualType.asSubclass(TypedExpression.class);
        var expected = expectedType.asSubclass(TypedExpression.class);
        if (TypedExpression.canConvert(actual, expected)) {
          return expectedType.cast(TypedExpression.from((TypedExpression) this.pop(), expected));
        }
      }

      throw new ParseCancellationException(
          String.format(TYPE_MISMATCH, expectedType.getSimpleName(), actualType.getSimpleName()));
    }

    /**
     * Clears the stack frame and all its registers.
     */
    @Override
    public void clear() {
      super.clear();
      this.identifierRegistry.clear();
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
   * This method is called at the begin boundary of scoped sub-expression to allow
   * for the
   * declaration of local variables.
   */
  public void pushStackFrame() {
    this.frames.push(new StackFrame());
  }

  /**
   * Drops the current stack frame and passes the return values to the previous
   * stack frame.
   * 
   * This method is called at the end boundary of scoped sub-expressions.
   * Variables local to the
   * sub-expression must go out of scope and the return values are passed to the
   * parent expression.
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
   * Declares an identifier. 
   * 
   * @param identifier The identifier to declare.
   */
  public void declareIdentifier(Identifier identifier) {
    if (this.inScope(identifier.name)) {
      throw new ParseCancellationException(IDENTIFIER_ALREADY_DECLARED + identifier.name);
    }
    this.frames.peek().declareIdentifier(identifier);
  }

  /**
   * Checks if an identifier is declared in the current scope.
   * 
   * @param identifier The identifier to check.
   * @return True if the identifier is declared in the current scope.
   */
  boolean inScope(String identifier) {
    return this.frames.stream().anyMatch(
        f -> f.identifierRegistry.containsKey(identifier));
  }

  /**
   * Returns the stack frame containing the given identifier.
   * 
   * @param identifier The identifier to look for.
   * @return The stack frame containing the given identifier or null if no such
   *         stack frame exists.
   */
  StackFrame findFrameContaining(String identifier) {
    return this.frames.stream()
        .filter(
            f -> f.identifierRegistry.containsKey(identifier))
        .findFirst().orElse(null);
  }

  /**
   * Gets the value of a parameter.
   * 
   * @param parameterName The identifier of the parameter.
   * @return The value of the parameter.
   */
  Optional<Expression> getParameter(String parameterName) {
    return this.frames.stream()
        .filter(f -> f.identifierRegistry.containsKey(parameterName)
            && Parameter.class.isAssignableFrom(f.identifierRegistry.get(parameterName).getClass()))
        .findFirst()
        .map(x -> ((Parameter) x.identifierRegistry.get(parameterName)).parameterValue);
  }

  /**
   * Gets the type of a variable.
   * 
   * @param identifier The identifier of the variable.
   * @return The type of the variable.
   */
  Optional<Identifier> getIdentifier(String identifier) {
    return this.frames.stream().filter(f -> f.identifierRegistry.containsKey(identifier)).findFirst()
        .map(x -> x.identifierRegistry.get(identifier));
  }

  /**
   * Gets the type of a variable.
   * 
   * @param identifierName The identifier of the variable.
   * @return The type of the variable.
   */
  public Class<? extends EfxDataType> getTypeOfIdentifier(String identifierName) {
    Optional<Identifier> identifier = this.getIdentifier(identifierName);
    if (!identifier.isPresent()) {
      throw new ParseCancellationException(UNDECLARED_IDENTIFIER + identifierName);
    }
    return identifier.get().dataType;
  }

  /**
   * Pushes a variable reference on the current stack frame. Makes sure there is
   * no name collision
   * with other identifiers already in scope.
   * 
   * @param identifierName      The name of the variable.
   * @throws ParseCancellationException if the variable is not declared in the
   *                                    current scope.
   */
  public void pushIdentifierReference(String identifierName) {
    getParameter(identifierName).ifPresentOrElse(parameterValue -> this.push(parameterValue),
        () -> getIdentifier(identifierName).ifPresentOrElse(
            variable -> this.push(variable.referenceExpression),
            () -> {
              throw new ParseCancellationException(UNDECLARED_IDENTIFIER + identifierName);
            }));
  }

  /**
   * Pushes an object on the current stack frame. No checks, no questions asked.
   * 
   * @param item The object to push on the stack.
   */
  public void push(CallStackObject item) {
    this.frames.peek().push(item);
  }

  public synchronized <T extends CallStackObject> T pop(Class<T> expectedType) {
    return this.frames.peek().pop(expectedType);
  }

  /**
   * Gets the object at the top of the current stack frame without removing it
   * from the stack.
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
