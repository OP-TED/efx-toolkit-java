package eu.europa.ted.efx.model.variables;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxDataType;

/**
 * Represents an identifier that is declared and used in an EFX expression.
 * Identifiers typically point to functions, variables and parameters.
 * 
 * The Identifier class keeps track not only of the name and data type of the identifier,
 * but also of the expressions used to declare and reference it.
 * 
 * @see Expression
 * @see TypedExpression
 * @see ParsedEntity
 * @see EfxDataType
 */
public abstract class Identifier implements ParsedEntity {
  public final String name;
  public final Class<? extends EfxDataType> dataType;

  /**
   * Creates an Identifier with the given name and expressions.
   * The Identifier's data type is inferred from the reference expression.
   *
   * @param name                  The name of the identifier.
   * @param declarationExpression The expression that should be used to declare the Identifier at runtime.
   * @param referenceExpression   The expression that should be used to reference the identifier.
   */
  protected Identifier(String name, Class<? extends EfxDataType> dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Identifier other = (Identifier) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (dataType == null) {
      if (other.dataType != null)
        return false;
    } else if (!dataType.equals(other.dataType))
      return false;
    return true;
  }
}