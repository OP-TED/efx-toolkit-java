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
package eu.europa.ted.efx.exceptions;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.types.EfxTypeTokenLookup;

/**
 * What a type error says: which construct was given arguments of which types, which of them does
 * not fit, and what that argument must be instead. The construct gives the words for itself and
 * for the argument, such as "Operator '=='" and "right operand".
 *
 * <p>Every type error starts with the same two sentences, whichever part of the translator finds
 * it: "Operator '==' cannot take 'text' and 'number'. Its right operand must be 'text'." Types are
 * named as a declaration writes them, with a star for a sequence, and a node is 'node'. Where the
 * mistake suggests what was meant, a sentence that says what to write instead follows them, such as
 * "To select by position, write [at n]."</p>
 */
public final class Diagnosis {

  private final String construct;
  private final String role;
  private final List<TypedExpression> arguments;
  private final List<String> requirements;
  private final List<String> suggestions;

  /**
   * @param construct how the error names the construct that rejects an argument, such as
   *        "Operator '=='"
   * @param role how it names the argument it rejects, such as "right operand"
   * @param arguments all its arguments, in order, as it read them
   * @param requirements what the argument it rejects must be, each as a message says it: a type in
   *        quotes, such as 'text', or a phrase, such as "a field reference"
   */
  public Diagnosis(final String construct, final String role,
      final List<? extends TypedExpression> arguments, final List<String> requirements) {
    this(construct, role, arguments, requirements, List.of());
  }

  /**
   * @param construct how the error names the construct that rejects an argument, such as
   *        "Operator '=='"
   * @param role how it names the argument it rejects, such as "right operand"
   * @param arguments all its arguments, in order, as it read them
   * @param requirements what the argument it rejects must be, each as a message says it: a type in
   *        quotes, such as 'text', or a phrase, such as "a field reference"
   * @param suggestions what to write instead, each a sentence, such as "To select by position,
   *        write [at n]."; none where the mistake suggests nothing
   */
  public Diagnosis(final String construct, final String role,
      final List<? extends TypedExpression> arguments, final List<String> requirements,
      final List<String> suggestions) {
    this.construct = construct;
    this.role = role;
    this.arguments = List.copyOf(arguments);
    this.requirements = List.copyOf(requirements);
    this.suggestions = List.copyOf(suggestions);
  }

  /**
   * @return the two sentences: what the construct was given, and what the argument at fault must
   *         be; then what to write instead, if the mistake suggests it
   */
  public String describe() {
    return this.sentences(Stream.empty());
  }

  /**
   * @param explanation why the argument at fault does not fit, as the reference it is written as
   *        tells it, such as "Field 'BT-1' may return multiple values from context 'ND-1'."
   * @return the two sentences, then the explanation, then what to write instead, if the mistake
   *         suggests it
   */
  public String describe(final String explanation) {
    return this.sentences(Stream.of(explanation));
  }

  private String sentences(final Stream<String> explanation) {
    final String reason = String.format("%s cannot take %s. Its %s must be %s.",
        this.construct,
        joined(this.arguments.stream().map(Diagnosis::typeOf).collect(Collectors.toList()), "and"),
        this.role, joined(this.requirements, "or"));
    return Stream.of(Stream.of(reason), explanation, this.suggestions.stream())
        .flatMap(sentences -> sentences)
        .collect(Collectors.joining(" "));
  }

  private static String typeOf(final TypedExpression argument) {
    return "'" + EfxTypeTokenLookup.nameOf(argument.getDataType()) + "'";
  }

  /**
   * Joins words as a sentence lists them: "a", "a and b", "a, b and c".
   */
  private static String joined(final List<String> words, final String conjunction) {
    final int last = words.size() - 1;
    return last == 0 ? words.get(0)
        : String.join(", ", words.subList(0, last)) + " " + conjunction + " " + words.get(last);
  }
}
