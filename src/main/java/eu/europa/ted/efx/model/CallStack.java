package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.antlr.v4.runtime.misc.ParseCancellationException;

public class CallStack  {

  class StackFrame extends Stack<CallStackObject> {

    Map<String, Class<? extends Expression>> typeRegister = new HashMap<String, Class<? extends Expression>>();

    Map<String, Expression> valueRegister = new HashMap<String, Expression>();

    public void pushParameterDeclaration(String parameterName, Expression parameterDeclarationExpression,
        Expression parameterValue) {
      this.declareIdentifier(parameterName, parameterDeclarationExpression.getClass());
      this.storeValue(parameterName, parameterValue);
      this.push(parameterDeclarationExpression);
    }

    public void pushVariableDeclaration(String variableName, Expression variableDeclarationExpression) {
      this.declareIdentifier(variableName, variableDeclarationExpression.getClass());
      this.push(variableDeclarationExpression);
    }

    public void declareIdentifier(String identifier, Class<? extends Expression> type) {
      this.typeRegister.put(identifier, type);
    }

    private void storeValue(String identifier, Expression value) {
      this.valueRegister.put(identifier, value);
    }

    public synchronized <T extends CallStackObject> T pop(Class<T> expectedType) {
      Class<? extends CallStackObject> actualType = peek().getClass();
      if (!expectedType.isAssignableFrom(actualType) && !actualType.equals(Expression.class)) {
        throw new ParseCancellationException("Type mismatch. Expected " + expectedType.getSimpleName()
            + " instead of " + this.peek().getClass().getSimpleName());
      }
      return expectedType.cast(this.pop());
    }

    @Override
    public void clear() {
        super.clear();
        this.typeRegister.clear();
        this.valueRegister.clear();  
    }
  }

  Stack<StackFrame> frames;

  public CallStack() {
    this.frames = new Stack<>();
    this.frames.push(new StackFrame());
  }

  public void pushStackFrame() {
    this.frames.push(new StackFrame());
  }

  public void popStackFrame() {
    StackFrame droppedFrame = this.frames.pop();
    this.frames.peek().addAll(droppedFrame);  // pass return values to the current stack frame
  }

  public void pushParameterDeclaration(String parameterName, Expression parameterDeclaration, Expression parameterValue) {
    if (this.inScope(parameterName)) {
      throw new ParseCancellationException("Identifier " + parameterDeclaration.script + " already declared.");
    } else if (parameterDeclaration.getClass() == Expression.class) {
      throw new ParseCancellationException();
    } else {
      this.frames.peek().pushParameterDeclaration(parameterName, parameterDeclaration, parameterValue);
    }
  }

  public void pushVariableDeclaration(String variableName, Expression variableDeclaration) {
    if (this.inScope(variableName)) {
      throw new ParseCancellationException("Identifier " + variableDeclaration.script + " already declared.");
    } else if (variableDeclaration.getClass() == Expression.class) {
      throw new ParseCancellationException();
    } else {
      this.frames.peek().pushVariableDeclaration(variableName, variableDeclaration);
    }
  }

  public void declareTemplateVariable(String variableName, Class<? extends Expression> variableType) {
    if (this.inScope(variableName)) {
      throw new ParseCancellationException("Identifier " + variableName + " already declared.");
    } else if (variableType == Expression.class) {
      throw new ParseCancellationException();
    } else {
      this.frames.peek().declareIdentifier(variableName, variableType);
    }
  }

  boolean inScope(String identifier) {
    return this.frames.stream().anyMatch(f -> f.typeRegister.containsKey(identifier) || f.valueRegister.containsKey(identifier) );      
  }

  StackFrame findFrameContaining(String identifier) {
    return this.frames.stream().filter(f -> f.typeRegister.containsKey(identifier) || f.valueRegister.containsKey(identifier)).findFirst().orElse(null);
  }

  Optional<Expression> getParameter(String identifier) {
    return this.frames.stream().filter(f -> f.valueRegister.containsKey(identifier)).findFirst().map(x -> x.valueRegister.get(identifier));
  }

  Optional<Class<? extends Expression>> getVariable(String identifier) {
    return this.frames.stream().filter(f -> f.typeRegister.containsKey(identifier)).findFirst().map(x -> x.typeRegister.get(identifier));
  }

  public void pushVariableReference(String variableName, Expression variableReference) {
    getParameter(variableName).ifPresentOrElse(parameterValue -> this.push(parameterValue),
        () -> getVariable(variableName).ifPresentOrElse(
            variableType -> this.pushVariableReference(variableReference, variableType),
            () -> {
              throw new ParseCancellationException("Identifier " + variableName + " not declared.");
            }));
  }

  private void pushVariableReference(Expression variableReference, Class<? extends Expression> variableType) {
    this.frames.peek().push(Expression.instantiate(variableReference.script, variableType));
  }

  public void push(CallStackObject item) {
    this.frames.peek().push(item);
  }

  public synchronized <T extends CallStackObject> T pop(Class<T> expectedType) {
    return this.frames.peek().pop(expectedType);
  }

  public synchronized CallStackObject peek() {
    return this.frames.peek().peek();
  }

  public int size() {
    return this.frames.peek().size();
  }
  
  public boolean empty() {
    return this.frames.peek().empty();
  }

  public void clear() {
    this.frames.peek().clear();
  }
}
