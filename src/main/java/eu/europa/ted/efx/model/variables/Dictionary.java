/*
 * Copyright 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model.variables;

import java.util.Objects;

import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;

/**
 * A dictionary variable declared in EFX source code.
 *
 * Maps string keys to values retrieved from a path expression. The dictionary stores the key
 * expression, the path expression for values, and the expression type of the path.
 */
public class Dictionary extends Identifier {
  public final StringExpression keyExpression;
  public final PathExpression pathExpression;
  public final Class<? extends TypedExpression> type;

  public Dictionary(String dictionaryName, PathExpression pathExpression, StringExpression keyExpression) {
    super(dictionaryName, pathExpression.getDataType());
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
    return Objects.equals(keyExpression, dictionary.keyExpression)
        && Objects.equals(pathExpression, dictionary.pathExpression)
        && Objects.equals(type, dictionary.type);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (keyExpression != null ? keyExpression.hashCode() : 0);
    result = 31 * result + (pathExpression != null ? pathExpression.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }
}