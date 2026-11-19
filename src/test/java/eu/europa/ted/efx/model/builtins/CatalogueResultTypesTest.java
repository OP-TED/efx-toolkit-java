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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.constructs.Overload;
import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.constructs.Parameters;
import eu.europa.ted.efx.model.constructs.Signature;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

/**
 * Proves that every overload composes what its signature declares: for every binding of
 * {@code T}, the composer, called on arguments of exactly its parameter types with the real
 * {@link XPathScriptGenerator}, returns an expression of the declared result type. A composer that
 * calls the wrong script generator method fails here.
 */
class CatalogueResultTypesTest {

  private static final int VARIADIC_TAIL = 2;

  private static final ScriptGenerator SCRIPT =
      new XPathScriptGenerator(EfxTranslatorOptions.DEFAULT);

  static Stream<org.junit.jupiter.params.provider.Arguments> boundOverloads() {
    return BuiltIns.INSTANCE.getAll().stream()
        .flatMap(builtIn -> builtIn.getOverloads().stream()
            .flatMap(overload -> overload.getSignature().concreteForms().stream()
                .map(bound -> org.junit.jupiter.params.provider.Arguments.of(builtIn.getName(),
                    bound, overload))));
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("boundOverloads")
  void testOverload_ComposesItsResultType(final String name, final Signature bound,
      final Overload overload) {
    List<TypedExpression> arguments = IntStream.range(0, arity(bound))
        .mapToObj(position -> placeholder(bound.getParameters().parameterAt(position).getType()))
        .collect(Collectors.toList());
    TypedExpression result = overload.compose(SCRIPT,
        overload.getSignature().getParameters().match(arguments).orElseThrow());
    assertTrue(bound.getResult().getExpressionType().isInstance(result),
        () -> name + " " + bound + " returned " + result.getClass().getSimpleName());
  }

  private static int arity(final Signature signature) {
    Parameters<?> parameters = signature.getParameters();
    return parameters.size() + (parameters.getVariadic().isPresent() ? VARIADIC_TAIL : 0);
  }

  private static TypedExpression placeholder(final ParameterType<?> type) {
    Class<? extends TypedExpression> expressionType = type.getExpressionType();
    Class<? extends TypedExpression> instantiable =
        expressionType == PathExpression.class ? StringPath.class : expressionType;
    return Expression.instantiate("$" + instantiable.getSimpleName(), instantiable);
  }
}
