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
package eu.europa.ted.efx.sdk2;

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.ParameterType.T;
import static eu.europa.ted.efx.model.constructs.ParameterType.T_SEQUENCE;

import java.util.List;
import java.util.stream.Collectors;

import eu.europa.ted.efx.model.constructs.Construct;
import eu.europa.ted.efx.model.constructs.Parameter;
import eu.europa.ted.efx.model.constructs.Parameters;

/**
 * A part of a statement or a declaration that takes an expression of a given type: the condition
 * of a rule, such as {@code WHEN}; a part of a template, such as its hyperlink or a part of a
 * label; the expression of {@code COMPUTE}; the value of a variable or the body of a function; and
 * the source of an iterator.
 *
 * <p>A clause only checks its expression: what it belongs to translates the expression, converted
 * to the parameter of the clause. A type error gives the name of the clause, and names its
 * expression by the name of its parameter. The fixed clauses are constants where they are
 * written; those named after what a declaration writes are made for each one.</p>
 */
final class Clause implements Construct {

  private final String name;
  private final List<Parameters<?>> parameterLists;

  private Clause(final String name, final Parameter.Impl<?>... parameters) {
    this.name = name;
    this.parameterLists = List.of(parameters).stream()
        .<Parameters<?>>map(parameter -> Parameters.of(List.of(parameter)))
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * @param name the clause as a type error names it, such as "Clause 'WHEN'" or
   *        "Variable 'text:$x'"
   * @param parameter its parameter, such as {@code condition: INDICATOR}
   * @return the clause
   */
  static Clause of(final String name, final Parameter.Impl<?> parameter) {
    return new Clause(name, parameter);
  }

  /**
   * Names a clause by the keyword that introduces it, as a type error does.
   *
   * @param keyword the keyword, such as {@code WHEN} or {@code BY}
   * @return the name of the clause, such as "Clause 'WHEN'"
   */
  static String describe(final String keyword) {
    return "Clause '" + keyword + "'";
  }

  /**
   * Gives a clause that takes one value of any type and gives it on as it is: a single value stays
   * one, and a sequence stays a sequence. It has a parameter list for each, {@code (T)} and
   * {@code (T*)}, and a single value takes the first, the more specific.
   *
   * @param name the clause as a type error names it, such as "Clause 'COMPUTE'"
   * @param parameter the name of its parameter, such as "expression"
   * @return the clause
   */
  static Clause ofValueAsGiven(final String name, final String parameter) {
    return new Clause(name, parameter(parameter, T), parameter(parameter, T_SEQUENCE));
  }

  @Override
  public String describe() {
    return this.name;
  }

  /**
   * @return the name of its parameter
   */
  @Override
  public String roleOf(final int position, final int count) {
    return this.parameterLists.get(0).roleOf(position, count);
  }

  @Override
  public List<Parameters<?>> getParameterLists() {
    return this.parameterLists;
  }
}
