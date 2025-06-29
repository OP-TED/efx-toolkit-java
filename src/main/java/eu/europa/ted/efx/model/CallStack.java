package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.variables.ParsedParameter;
import eu.europa.ted.efx.model.variables.Dictionary;
import eu.europa.ted.efx.model.variables.Function;
import eu.europa.ted.efx.model.variables.Identifier;
import eu.europa.ted.efx.model.variables.Identifiers;
import eu.europa.ted.efx.model.variables.ParsedParameters;
import eu.europa.ted.efx.model.variables.Template;
import eu.europa.ted.efx.model.variables.Variable;

/**
 * The call stack is a stack of stack frames. Each stack frame represents a
 * scope. The top of the
 * stack is the current scope. The bottom of the stack is the global scope.
 */
public class CallStack {

  private static final String STACK_UNDERFLOW = "Stack underflow. Return values were available in the dropped frame, but no stack frame is left to consume them.";

  /**
   * Stack frames are means of controlling the scope of variables and parameters.
   * Certain
   * sub-expressions are scoped, meaning that variables and parameters are only
   * available within the
   * scope of the sub-expression.
   */
  class StackFrame extends Stack<ParsedEntity> {

    /**
     * Keeps a list of all identifiers declared in the current scope as well as
     * their type.
     */
    transient Map<String, Identifier> identifierRegistry = new HashMap<>();

    /**
     * Registers an identifier in the current scope. This registration is later used
     * to check if an
     * identifier is declared in the current scope.
     * 
     * @param identifier The identifier to register.
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
    synchronized <T extends ParsedEntity> T pop(Class<T> expectedType) {
      Class<? extends ParsedEntity> actualType = this.peek().getClass();
      if (expectedType.isAssignableFrom(actualType)) {
        return expectedType.cast(this.pop());
      }

      if (TypedExpression.class.isAssignableFrom(actualType) && TypedExpression.class.isAssignableFrom(expectedType)) {
        var actual = actualType.asSubclass(TypedExpression.class);
        var expected = expectedType.asSubclass(TypedExpression.class);
        if (TypedExpression.canConvert(actual, expected)) {
          return expectedType.cast(TypedExpression.from((TypedExpression) this.pop(), expected));
        }
        throw TypeMismatchException.cannotConvert(expected, actual);
      }

      throw TypeMismatchException.cannotConvert(expectedType, actualType);
    }

    synchronized <T extends ParsedEntity> T peek(Class<T> expectedType) {
      Class<? extends ParsedEntity> actualType = this.peek().getClass();
      if (expectedType.isAssignableFrom(actualType)) {
        return expectedType.cast(this.peek());
      }

      if (TypedExpression.class.isAssignableFrom(actualType) && TypedExpression.class.isAssignableFrom(expectedType)) {
        var actual = actualType.asSubclass(TypedExpression.class);
        var expected = expectedType.asSubclass(TypedExpression.class);
        if (TypedExpression.canConvert(actual, expected)) {
          return expectedType.cast(TypedExpression.from((TypedExpression) this.peek(), expected));
        }
      }
      throw TypeMismatchException.cannotConvert(expectedType, actualType);
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
   * Keeps a list of all identifiers declared in the global scope as well as
   * their type.
   */
  Map<String, Identifier> globalIdentifierRegistry = new LinkedHashMap<>();

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
    if (!droppedFrame.isEmpty()) {
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
      throw InvalidIdentifierException.alreadyDeclared(identifier.name);
    }
    this.frames.peek().declareIdentifier(identifier);
  }

  /**
   * Declares a global identifier. 
   * 
   * @param identifier The identifier to declare.
   */
  public void declareGlobalIdentifier(Identifier identifier) {
    if (this.inScope(identifier.name)) {
      throw InvalidIdentifierException.alreadyDeclared(identifier.name);
    }
    this.globalIdentifierRegistry.put(identifier.name, identifier);
  }

  public void declareFunction(Function function) {
    if (this.inScope(function.name)) {
      throw InvalidIdentifierException.alreadyDeclared(function.name);
    }
    this.globalIdentifierRegistry.put(function.name, function);
  }

  public void declareTemplate(Template template) {
    if (this.inScope(template.name)) {
      throw InvalidIdentifierException.alreadyDeclared(template.name);
    }
    this.globalIdentifierRegistry.put(template.name, template);
  }

  /**
   * Checks if an identifier is declared in the current scope.
   * 
   * @param identifier The identifier to check.
   * @return True if the identifier is declared in the current scope.
   */
  boolean inScope(String identifier) {
    return this.globalIdentifierRegistry.containsKey(identifier)
        || this.frames.stream().anyMatch(f -> f.identifierRegistry.containsKey(identifier));
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

  public Identifiers getGlobals() {
    Identifiers globals = new Identifiers();
    for (Identifier identifier : globalIdentifierRegistry.values()) {
        globals.add(identifier);
    }
    return globals;
  }


  /**
   * Gets the value of a parameter.
   * 
   * @param parameterName The identifier of the parameter.
   * @return The value of the parameter.
   */
  Optional<TypedExpression> getParameter(String parameterName) {
    return this.frames.stream()
        .filter(f -> f.identifierRegistry.containsKey(parameterName)
            && ParsedParameter.class.isAssignableFrom(f.identifierRegistry.get(parameterName).getClass()))
        .findFirst()
        .map(x -> ((ParsedParameter) x.identifierRegistry.get(parameterName)).referenceExpression);
  }

  /**
   * Retrieves an {@link Identifier} associated with the given identifier string.
   * The method searches through the identifier registries of all frames and the global
   * identifier registry. It returns the first matching {@link Identifier} found.
   *
   * @param identifier the string identifier to search for in the registries.
   * @return an {@link Optional} containing the {@link Identifier} if found, or an empty {@link Optional} if not found.
   */
  Optional<Identifier> getIdentifier(String identifier) {
    return Stream.concat(
            this.frames.stream().map(f -> f.identifierRegistry),
            Stream.of(this.globalIdentifierRegistry))
        .filter(registry -> registry.containsKey(identifier))
        .findFirst()
        .map(registry -> registry.get(identifier));
  }

  Optional<Variable> getVariable(String identifier) {
    return Stream.concat(
            this.frames.stream().map(f -> f.identifierRegistry),
            Stream.of(this.globalIdentifierRegistry))
        .filter(registry -> registry.containsKey(identifier))
        .findFirst()
        .map(registry -> registry.get(identifier))
        .filter(Variable.class::isInstance)
        .map(Variable.class::cast);
  }

  /**
   * Retrieves a function from the global identifier registry by its name.
   *
   * @param functionName the name of the function to retrieve
   * @return the {@link Function} associated with the given name
   * @throws InvalidIdentifierException if the function name is not found in the registry,
   *         with a message indicating the undeclared identifier
   */
  public Function getFunction(String functionName) {
    return Optional.ofNullable(this.globalIdentifierRegistry.get(functionName))
        .filter(Function.class::isInstance)
        .map(Function.class::cast)
        .orElseThrow(() -> InvalidIdentifierException.undeclaredIdentifier(functionName));
  }

  /**
   * Retrieves a template from the global identifier registry by its name.
   *
   * @param templateName the name of the template to retrieve
   * @return the {@link Template} associated with the given name
   * @throws InvalidIdentifierException if the template name is not found in the registry,
   *         with a message indicating the undeclared identifier
   */
  public Template getTemplate(String templateName) {
    return Optional.ofNullable(this.globalIdentifierRegistry.get(templateName))
        .filter(Template.class::isInstance)
        .map(Template.class::cast)
        .orElseThrow(() -> InvalidIdentifierException.undeclaredIdentifier(templateName));
  }

  /**
   * Retrieves a dictionary from the global identifier registry by its name.
   *
   * @param dictionaryName the name of the dictionary to retrieve
   * @return the {@link Dictionary} associated with the given name
   * @throws InvalidIdentifierException if the dictionary name is not found in the registry,
   *         with a message indicating the undeclared identifier
   */
  public Dictionary getDictionary(String dictionaryName) {
    return Optional.ofNullable(this.globalIdentifierRegistry.get(dictionaryName))
        .filter(Dictionary.class::isInstance)
        .map(Dictionary.class::cast)
        .orElseThrow(() -> InvalidIdentifierException.undeclaredIdentifier(dictionaryName));
  }

  /**
   * Retrieves the list of parameters for a specified function name.
   *
   * @param functionName the name of the function whose parameters are to be retrieved
   * @return the list of parameters associated with the specified function
   * @throws ParseCancellationException if the function name is not declared in the registry
   */
  public ParsedParameters getFunctionParameters(String functionName) {
    return Optional.ofNullable(this.globalIdentifierRegistry.get(functionName))
      .filter(Function.class::isInstance)
      .map(identifier -> ((Function) identifier).parameters)
      .orElseThrow(() -> InvalidIdentifierException.undeclaredIdentifier(functionName));
  }

  /**
   * Retrieves the data type of a given identifier by its name.
   * <p>
   * This method first attempts to find the identifier using the {@code getIdentifier} method.
   * If not found, it tries to resolve it as a function using the {@code getFunction} method.
   * If the identifier is still not found, a {@link ParseCancellationException} is thrown.
   * </p>
   *
   * @param identifierName the name of the identifier to look up
   * @return the class type of the identifier's data type
   * @throws ParseCancellationException if the identifier is not declared
   */
  public Class<? extends EfxDataType> getTypeOfIdentifier(String identifierName) {
    Optional<Identifier> identifier = this.getIdentifier(identifierName)
        .or(() -> Optional.ofNullable((Identifier) this.getFunction(identifierName)));
    if (!identifier.isPresent()) {
      throw InvalidIdentifierException.undeclaredIdentifier(identifierName);
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
    getParameter(identifierName).ifPresentOrElse(this::push,
        () -> getVariable(identifierName).ifPresentOrElse(
            variable -> this.push(variable.referenceExpression),
            () -> {
              throw InvalidIdentifierException.undeclaredIdentifier(identifierName);
            }));
  }

  /**
   * Pushes an object on the current stack frame. No checks, no questions asked.
   * 
   * @param item The object to push on the stack.
   */
  public void push(ParsedEntity item) {
    this.frames.peek().push(item);
  }

  /**
   * Removes and returns the top element of the call stack, ensuring it matches
   * the expected type.
   * This method is thread-safe.
   *
   * @param <T>          The type of the element expected to be returned, which
   *                     must extend {@code ParsedEntity}.
   * @param expectedType The {@code Class} object representing the expected type
   *                     of the element.
   * @return The top element of the call stack, cast to the specified type.
   */
  public synchronized <T extends ParsedEntity> T pop(Class<T> expectedType) {
    return this.frames.peek().pop(expectedType);
  }

  public synchronized <T extends ParsedEntity> T peek(Class<T> expectedType) {
    return this.frames.peek().peek(expectedType);
  }

  /**
   * Retrieves, but does not remove, the top ParsedEntity from the call stack.
   * This method is thread-safe as it is synchronized.
   *
   * @return the top ParsedEntity from the call stack, or {@code null} if the
   *         stack is empty.
   */
  public synchronized ParsedEntity peek() {
    return this.frames.peek().peek();
  }

  /**
   * Returns the size of the current call stack frame.
   *
   * @return the number of elements in the top frame of the call stack.
   */
  public int size() {
    return this.frames.peek().size();
  }

  /**
   * Checks if the call stack is empty.
   *
   * @return {@code true} if the top frame of the call stack is empty, 
   *         {@code false} otherwise.
   */
  public boolean empty() {
    return this.frames.peek().empty();
  }

  /**
   * Clears the current call stack frame by removing all elements from it.
   * This operation affects only the top frame of the stack.
   */
  public void clear() {
    this.frames.peek().clear();
  }
}
