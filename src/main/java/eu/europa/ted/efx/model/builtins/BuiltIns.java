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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The catalogue of the standard EFX built-ins: every operator and function of the enums of this
 * package, looked up by name. A {@link PatternMatch} is not in it, since one is made for each use of
 * {@code like}.
 */
public final class BuiltIns {

  /** The catalogue. */
  public static final BuiltIns INSTANCE = new BuiltIns();

  private final List<BuiltIn> all;
  private final Map<String, BuiltIn> byName;

  private BuiltIns() {
    this.all = Stream.<BuiltIn[]>of(
        LogicalOperators.values(),
        ComparisonOperators.values(),
        ArithmeticOperators.values(),
        ConditionOperators.values(),
        StringFunctions.values(),
        NumericFunctions.values(),
        DateTimeFunctions.values(),
        ConversionFunctions.values(),
        SequenceFunctions.values(),
        TemplateFunctions.values())
        .flatMap(Arrays::stream)
        .collect(Collectors.toUnmodifiableList());
    this.byName = this.all.stream()
        .collect(Collectors.toUnmodifiableMap(BuiltIn::getName, Function.identity()));
  }

  /**
   * Finds a built-in.
   *
   * @param name the operator as written, or the function token
   * @return the built-in, if the catalogue has one of that name
   */
  public Optional<BuiltIn> find(final String name) {
    return Optional.ofNullable(this.byName.get(name));
  }

  /**
   * @return every built-in of the catalogue, enum by enum
   */
  public List<BuiltIn> getAll() {
    return this.all;
  }
}
