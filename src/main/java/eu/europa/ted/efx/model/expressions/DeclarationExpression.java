package eu.europa.ted.efx.model.expressions;

public class DeclarationExpression extends Expression.Impl {

  public DeclarationExpression(final String script) {
    super(script);
  }

  public static DeclarationExpression empty() {
    return new DeclarationExpression("");
  }
}