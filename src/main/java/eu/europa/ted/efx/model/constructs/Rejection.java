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

import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * Where and why a parameter list rejects the arguments of a call: the argument it reports, at its
 * position, and the parameter type that argument does not convert to.
 *
 * <p>A parameter list that takes the arguments gives them {@link Parameters#match converted};
 * one that does not gives its {@link Parameters#rejectionOf rejection}.</p>
 */
public final class Rejection {

  private final Parameters<?> parameters;
  private final int position;
  private final ParameterType<?> required;
  private final TypedExpression argument;

  Rejection(final Parameters<?> parameters, final int position, final ParameterType<?> required,
      final TypedExpression argument) {
    this.parameters = parameters;
    this.position = position;
    this.required = required;
    this.argument = argument;
  }

  /**
   * @return the parameter list that rejects the arguments, as the construct declares it
   */
  public Parameters<?> getParameters() {
    return this.parameters;
  }

  /**
   * @return the position of the rejected argument
   */
  public int getPosition() {
    return this.position;
  }

  /**
   * @return the parameter type the rejected argument had to convert to, with {@code T} bound by
   *         the arguments if they bind it
   */
  public ParameterType<?> getRequired() {
    return this.required;
  }

  /**
   * @return what is wrong with the rejected argument
   */
  public Kind getKind() {
    return this.required.rejectionKindOf(this.argument);
  }

  /**
   * What is wrong with a rejected argument, in the order in which a mismatch reports them at the
   * same position.
   */
  public enum Kind {

    /** A sequence where a single value of its primitive is required. */
    CARDINALITY,

    /** A value of another primitive than the one required. */
    PRIMITIVE,

    /** A value where a path is required, or a node, which has no value, where a value is. */
    PATH
  }
}
