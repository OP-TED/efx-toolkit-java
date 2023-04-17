package eu.europa.ted.efx.model;

public class Identifier<T extends Expression> {
  public String name;
  public T referenceExpression;

  public Identifier(String name, T referenceExpression) {
    this.name = name;
    this.referenceExpression = referenceExpression;
  }
}
