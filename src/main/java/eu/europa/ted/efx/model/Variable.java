package eu.europa.ted.efx.model;

public class Variable<T extends Expression> extends Identifier<T> {
  public T initializationExpression;

  public Variable(String variableName, T initializationExpression, T referenceExpression) {
    super(variableName, referenceExpression);
    this.name = variableName;
    this.initializationExpression = initializationExpression;
  }

  public Variable(Identifier<T> identifier, T initializationExpression) {
    super(identifier.name, identifier.referenceExpression);
    this.initializationExpression = initializationExpression;
  }
}
