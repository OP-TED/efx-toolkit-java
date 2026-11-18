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

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.sdk2.EfxLexer;

/**
 * Proves that the catalogue has a built-in for every function of the EFX-2 lexer and for every
 * operator, and nothing else, and that the built-ins that say they are functions are those of the
 * lexer's functions.
 *
 * <p>The functions are the tokens named {@code ...Function}, and the keywords that are also
 * function names. A token the lexer retypes, such as {@code string}, which it reads as
 * {@code text}, is the function of the token it becomes.</p>
 */
class CatalogueCompletenessTest {

  private static final Set<Integer> KEYWORD_FUNCTIONS = Set.of(EfxLexer.Not, EfxLexer.Text,
      EfxLexer.Number, EfxLexer.Indicator, EfxLexer.Date, EfxLexer.Time, EfxLexer.Empty);

  private static final Set<String> OPERATORS = Set.of(
      "and", "or",
      "==", "!=", "<", "<=", ">", ">=",
      "+", "-", "*", "/", "%",
      "in", "not in", "is empty", "is not empty", "is present", "is not present",
      "is unique in", "is not unique in", "has duplicates", "has no duplicates");

  @Test
  void testCatalogue_HasEveryFunctionAndOperator() {
    Set<String> expected = Stream.concat(functions(), OPERATORS.stream())
        .collect(Collectors.toCollection(TreeSet::new));
    Set<String> actual = BuiltIns.INSTANCE.getAll().stream().map(BuiltIn::getName)
        .collect(Collectors.toCollection(TreeSet::new));
    assertEquals(expected, actual);
  }

  @Test
  void testFunctions_AreTheFunctionTokens() {
    Set<String> expected = functions().collect(Collectors.toCollection(TreeSet::new));
    Set<String> actual = BuiltIns.INSTANCE.getAll().stream().filter(BuiltIn::isFunction)
        .map(BuiltIn::getName).collect(Collectors.toCollection(TreeSet::new));
    assertEquals(expected, actual);
  }

  @Test
  void testComparisonOperators_AreTheComparisonToken() {
    Set<Integer> tokens = Stream.of(ComparisonOperators.values()).map(BuiltIn::getName)
        .map(CatalogueCompletenessTest::tokenOf).collect(Collectors.toSet());
    assertEquals(Set.of(EfxLexer.Comparison), tokens);
  }

  private static Stream<String> functions() {
    return IntStream.rangeClosed(1, EfxLexer.VOCABULARY.getMaxTokenType())
        .filter(token -> KEYWORD_FUNCTIONS.contains(token)
            || String.valueOf(EfxLexer.VOCABULARY.getSymbolicName(token)).endsWith("Function"))
        .filter(token -> EfxLexer.VOCABULARY.getLiteralName(token) != null)
        .map(token -> tokenOf(spelling(token)))
        .mapToObj(CatalogueCompletenessTest::spelling);
  }

  private static String spelling(final int token) {
    String literal = EfxLexer.VOCABULARY.getLiteralName(token);
    return literal.substring(1, literal.length() - 1);
  }

  private static int tokenOf(final String text) {
    EfxLexer lexer = new EfxLexer(CharStreams.fromString(text));
    lexer.pushMode(EfxLexer.EXPRESSION);
    return lexer.nextToken().getType();
  }
}
