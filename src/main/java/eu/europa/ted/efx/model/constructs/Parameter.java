/*
 * Copyright 2026 European Union
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
package eu.europa.ted.efx.model.constructs;

import java.util.Objects;

import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * A parameter: a name, and the type an argument given for it must have. A function or template
 * declared in EFX has the parameters parsed with it; a built-in, an expression construct or a
 * clause has those its signature declares, each a {@link Impl}.
 */
public interface Parameter {

  /**
   * @return the name of the parameter
   */
  String getName();

  /**
   * @return the type an argument given for this parameter must have
   */
  ParameterType<?> getType();

  /**
   * A parameter that a signature declares in code: a name and a {@link ParameterType}, which may
   * mention {@code T}. The names of the parameters of a built-in function are those of the
   * autocomplete catalogue, so that the catalogue can be derived from the signatures.
   *
   * @param <E> the expression class an argument given for it is converted to
   */
  final class Impl<E extends TypedExpression> implements Parameter {

    private final String name;
    private final ParameterType<E> type;

    private Impl(final String name, final ParameterType<E> type) {
      this.name = name;
      this.type = type;
    }

    /**
     * Declares a parameter.
     *
     * @param <E> the expression class of its type
     * @param name the name of the parameter
     * @param type the type of the parameter
     * @return the parameter
     */
    public static <E extends TypedExpression> Impl<E> parameter(final String name,
        final ParameterType<E> type) {
      return new Impl<>(name, type);
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public ParameterType<E> getType() {
      return this.type;
    }

    /**
     * Two parameters are equal when their names and their types are.
     */
    @Override
    public boolean equals(final Object other) {
      return other instanceof Impl && Objects.equals(this.name, ((Impl<?>) other).name)
          && Objects.equals(this.type, ((Impl<?>) other).type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.type);
    }

    @Override
    public String toString() {
      return this.name + ": " + this.type;
    }
  }
}
