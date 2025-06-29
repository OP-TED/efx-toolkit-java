package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;

public class Dictionary extends Identifier {
  public final StringExpression keyExpression;
  public final PathExpression pathExpression;
  public final Class<? extends TypedExpression> type;

  public Dictionary(String dictionaryName, PathExpression pathExpression, StringExpression keyExpression) {
    
    super(dictionaryName, keyExpression.getDataType());
    this.keyExpression = keyExpression;
    this.pathExpression = pathExpression;
    this.type = pathExpression.getClass();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Dictionary dictionary = (Dictionary) o;
    return java.util.Objects.equals(keyExpression, dictionary.keyExpression);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + java.util.Objects.hashCode(keyExpression);
    return result;
  }
}