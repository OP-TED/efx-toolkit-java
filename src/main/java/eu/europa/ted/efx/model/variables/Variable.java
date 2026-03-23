/*
 * Copyright 2023 European Union
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

import java.util.ArrayList;
import java.util.List;

import eu.europa.ted.efx.model.expressions.DeclarationExpression;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

/**
 * A variable declared in EFX source code.
 *
 * Tracks three expressions: the initialization expression (determines the variable's type),
 * the reference expression (target-language code for accessing the variable), and the
 * declaration expression (target-language code for declaring the variable).
 *
 * For iterator variables, the initialization expression may be a sequence while the reference
 * expression is scalar (since the iterator yields one element at a time).
 */
public class Variable extends Identifier {
  public final Expression declarationExpression;
  public final TypedExpression initializationExpression;
  public final TypedExpression referenceExpression;
  private final List<DynamicVariable> dynamicDependencies = new ArrayList<>();

  public Variable(String variableName, TypedExpression initializationExpression, TypedExpression referenceExpression) {
    this(variableName, DeclarationExpression.empty(), initializationExpression, referenceExpression);
  }

  public Variable(String variableName, Expression declarationExpression, TypedExpression initializationExpression, TypedExpression referenceExpression) {
    super(variableName, initializationExpression.getDataType());
    this.declarationExpression = declarationExpression;
    this.initializationExpression = initializationExpression;
    this.referenceExpression = referenceExpression;
    // Compare primitive types (without cardinality) since iterator variables have sequence initializers but scalar references
    // Use isAssignableFrom to allow compatible types (e.g., MultilingualString is assignable to String)
    assert EfxTypeLattice.toPrimitive(referenceExpression.getDataType())
        .isAssignableFrom(EfxTypeLattice.toPrimitive(initializationExpression.getDataType()));
  }

  public void addDynamicDependencies(final List<DynamicVariable> dependencies) {
    this.dynamicDependencies.addAll(dependencies);
  }

  public List<DynamicVariable> getDynamicDependencies() {
    return this.dynamicDependencies;
  }

  public boolean hasDynamicDependencies() {
    return !this.dynamicDependencies.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Variable variable = (Variable) o;
    return java.util.Objects.equals(declarationExpression, variable.declarationExpression) &&
           java.util.Objects.equals(initializationExpression, variable.initializationExpression) &&
           java.util.Objects.equals(referenceExpression, variable.referenceExpression);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + java.util.Objects.hashCode(declarationExpression);
    result = 31 * result + java.util.Objects.hashCode(initializationExpression);
    result = 31 * result + java.util.Objects.hashCode(referenceExpression);
    return result;
  }
}