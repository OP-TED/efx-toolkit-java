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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.model.constructs.Overload;
import eu.europa.ted.efx.model.constructs.Parameters;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanPath;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DatePath;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationPath;
import eu.europa.ted.efx.model.expressions.scalar.DynamicExpression;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringExpression;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringPath;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericPath;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimePath;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequencePath;

/**
 * Proves that the catalogue is unambiguous: for every built-in, every number of arguments it takes
 * (a variadic tail up to three arguments) and every combination of argument types resolve to one
 * overload, the only one that applies or the most specific of those that do.
 *
 * <p>The argument types are every expression class a translated argument can have: each value
 * primitive (with multilingual text and dynamic) as a single value and as a sequence, each as a
 * value and as a path, and the node paths. A deliberately ambiguous built-in shows that the check
 * finds what it looks for, and a built-in with a pair {@code (T)} and {@code (T*)} that the check
 * allows what the resolution allows.</p>
 */
class CatalogueUnambiguityTest {

  private static final int LONGEST_VARIADIC_TAIL = 3;

  private static final List<TypedExpression> ARGUMENTS = Stream.of(
      StringExpression.class, MultilingualStringExpression.class, BooleanExpression.class,
      DynamicExpression.class, NumericExpression.class, DateExpression.class, TimeExpression.class,
      DurationExpression.class,
      StringSequenceExpression.class, MultilingualStringSequenceExpression.class,
      BooleanSequenceExpression.class, NumericSequenceExpression.class,
      DateSequenceExpression.class, TimeSequenceExpression.class, DurationSequenceExpression.class,
      StringPath.class, MultilingualStringPath.class, BooleanPath.class, NumericPath.class,
      DatePath.class, TimePath.class, DurationPath.class, NodePath.class,
      StringSequencePath.class, MultilingualStringSequencePath.class, BooleanSequencePath.class,
      NumericSequencePath.class, DateSequencePath.class, TimeSequencePath.class,
      DurationSequencePath.class, NodeSequencePath.class)
      .map(type -> Expression.instantiate(type.getSimpleName(), type))
      .collect(Collectors.toList());

  static Stream<BuiltIn> builtIns() {
    return BuiltIns.INSTANCE.getAll().stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("builtIns")
  void testBuiltIn_IsUnambiguous(final BuiltIn builtIn) {
    assertEquals(List.of(), ambiguities(builtIn)
        .map(arguments -> describe(arguments) + " -> " + applicable(builtIn, arguments))
        .collect(Collectors.toList()));
  }

  @Test
  void testAmbiguousBuiltIn_IsCaught() {
    assertEquals(
        List.of("(StringExpression)", "(MultilingualStringExpression)", "(StringPath)",
            "(MultilingualStringPath)"),
        ambiguities(AmbiguousBuiltIn.INSTANCE).map(CatalogueUnambiguityTest::describe)
            .collect(Collectors.toList()));
  }

  @Test
  void testSingleOrSequenceBuiltIn_IsUnambiguous() {
    assertEquals(List.of(), ambiguities(SingleOrSequenceBuiltIn.INSTANCE)
        .map(CatalogueUnambiguityTest::describe).collect(Collectors.toList()));
  }

  /**
   * Lists every combination of arguments that a built-in cannot resolve to one overload: more
   * than one applies and none of them is more specific than the others.
   */
  private static Stream<List<TypedExpression>> ambiguities(final BuiltIn builtIn) {
    return arities(builtIn)
        .flatMap(CatalogueUnambiguityTest::combinations)
        .filter(arguments -> isAmbiguous(builtIn, arguments));
  }

  private static boolean isAmbiguous(final BuiltIn builtIn, final List<TypedExpression> arguments) {
    try {
      builtIn.resolve(arguments);
      return false;
    } catch (TranslatorConfigurationException e) {
      assertEquals(TranslatorConfigurationException.ErrorCode.AMBIGUOUS_OVERLOADS, e.getErrorCode());
      return true;
    }
  }

  private static Stream<Integer> arities(final BuiltIn builtIn) {
    return IntStream.rangeClosed(0, longestSignature(builtIn) + LONGEST_VARIADIC_TAIL)
        .filter(count -> builtIn.getOverloads().stream()
            .anyMatch(overload -> overload.getSignature().getParameters().takes(count)))
        .boxed();
  }

  private static int longestSignature(final BuiltIn builtIn) {
    return builtIn.getOverloads().stream()
        .mapToInt(overload -> overload.getSignature().getParameters().size())
        .max().orElse(0);
  }

  private static Stream<List<TypedExpression>> combinations(final int count) {
    if (count == 0) {
      return Stream.of(List.of());
    }
    return combinations(count - 1).flatMap(prefix -> ARGUMENTS.stream().map(argument -> {
      List<TypedExpression> combination = new ArrayList<>(prefix);
      combination.add(argument);
      return combination;
    }));
  }

  private static String describe(final List<TypedExpression> arguments) {
    return arguments.stream().map(argument -> argument.getClass().getSimpleName())
        .collect(Collectors.joining(", ", "(", ")"));
  }

  private static List<Overload> applicable(final BuiltIn builtIn,
      final List<TypedExpression> arguments) {
    return builtIn.getOverloads().stream()
        .filter(overload -> applies(overload.getSignature().getParameters(), arguments))
        .collect(Collectors.toList());
  }

  private static boolean applies(final Parameters<?> parameters,
      final List<TypedExpression> arguments) {
    return parameters.takes(arguments.size()) && parameters.match(arguments).isPresent();
  }
}
