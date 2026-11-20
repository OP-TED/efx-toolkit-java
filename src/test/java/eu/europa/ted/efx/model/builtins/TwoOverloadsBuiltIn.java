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
package eu.europa.ted.efx.model.builtins;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Overload;

/**
 * A built-in with two overloads, declared in a given order. Declared in both orders, it proves
 * that the resolution chooses the more specific of two overloads that apply, whichever comes
 * first.
 */
public final class TwoOverloadsBuiltIn implements BuiltIn {

  private final List<Overload> overloads;

  private TwoOverloadsBuiltIn(final Overload first, final Overload second) {
    this.overloads = List.of(first, second);
  }

  /**
   * Declares two overloads in both orders.
   *
   * @param first an overload
   * @param second another overload
   * @return the built-in that declares them in the given order, and the one that declares them
   *         in the other
   */
  public static List<TwoOverloadsBuiltIn> inBothOrders(final Overload first,
      final Overload second) {
    return List.of(new TwoOverloadsBuiltIn(first, second), new TwoOverloadsBuiltIn(second, first));
  }

  @Override
  public String getName() {
    return "two overloads";
  }

  @Override
  public List<Overload> getOverloads() {
    return this.overloads;
  }
}
