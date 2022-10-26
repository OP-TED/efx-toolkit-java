package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class CallStack extends Stack<CallStackObjectBase> {

  private static final long serialVersionUID = 1L;

  private Map<String, Class<? extends Expression>> variableTypes = new HashMap<>();
  private Map<String, Expression> parameterValues = new HashMap<>();

  public CallStack() {}

  public void pushParameterDeclaration(String parameterName, Expression parameterDeclaration,
      Expression parameterValue) {
    if (this.variableTypes.containsKey(parameterName)) {
      throw new ParseCancellationException(
          "A parameter with the name " + parameterDeclaration.script + " already exists");
    } else if (parameterDeclaration.getClass() == Expression.class) {
      throw new ParseCancellationException();
    } else {
      this.variableTypes.put(parameterName, parameterDeclaration.getClass());
      this.parameterValues.put(parameterName, parameterValue);
      this.push(parameterDeclaration);
    }
  }

  public void pushVariableDeclaration(String variableName, Expression variableDeclaration) {
    if (parameterValues.containsKey(variableName)) {
      throw new ParseCancellationException("A parameter with the name " + variableDeclaration.script
          + " has already been declared.");
    } else if (this.variableTypes.containsKey(variableName)) {
      throw new ParseCancellationException(
          "A variable with the name " + variableDeclaration.script + " has already been declared.");
    } else if (variableDeclaration.getClass() == Expression.class) {
      throw new ParseCancellationException();
    } else {
      this.variableTypes.put(variableName, variableDeclaration.getClass());
      this.push(variableDeclaration);
    }
  }

  public void pushVariableReference(String variableName, Expression variableReference) {
    if (this.parameterValues.containsKey(variableName)) {
      this.push(parameterValues.get(variableName));
    } else if (this.variableTypes.containsKey(variableName)) {
      this.push(Expression.instantiate(variableReference.script, variableTypes.get(variableName)));
    } else {
      throw new ParseCancellationException(
          "A variable or parameter with the name " + variableName + " has not been declared.");
    }
  }

  public synchronized <T extends CallStackObjectBase> T pop(Class<T> expectedType) {
    Class<? extends CallStackObjectBase> actualType = peek().getClass();
    if (!expectedType.isAssignableFrom(actualType) && !actualType.equals(Expression.class)) {
      throw new ParseCancellationException("Type mismatch. Expected " + expectedType.getSimpleName()
          + " instead of " + this.peek().getClass().getSimpleName());
    }
    return expectedType.cast(this.pop());
  }

  @Override
  public void clear() {
    super.clear();
    this.variableTypes.clear();
    this.parameterValues.clear();
  }
}
