package eu.europa.ted.efx.model.variables;

import java.util.Objects;

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
   * Creates an Identifier with the given name and type.
   *
   * @param name     The name of the identifier.
   * @param dataType The data type of the identifier.
   */
  protected Identifier(String name, Class<? extends EfxDataType> dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, dataType);
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
    return Objects.equals(name, other.name) && Objects.equals(dataType, other.dataType);
  }
}