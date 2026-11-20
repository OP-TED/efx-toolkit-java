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

/**
 * How EFX writes a function, a template or an operator: a function with the token before its
 * arguments in parentheses, a template with its name after {@code INVOKE}, an operator with the
 * token beside its operands. A type error names what was written, and each of its arguments, by
 * its spelling.
 *
 * <p>A built-in is usually spelled by its own name, but a call may spell it otherwise:
 * {@code string} is a spelling of the function {@code text}, and {@code :preferredLanguage} spells
 * the function {@code preferred-language} as an operator after a field. A step that only a field
 * has, such as {@code :rawValue}, is spelled as an operator after the field. A function declared
 * in EFX is spelled with its prefix, as {@code ?greeting}.</p>
 */
public final class Spelling {

  /**
   * What is written: a function or a template, whose arguments are numbered, or an operator,
   * whose operands are named by their side.
   */
  private enum Kind {
    FUNCTION("Function", "argument"), TEMPLATE("Template", "argument"),
    OPERATOR("Operator", "operand");

    private final String noun;
    private final String argument;

    Kind(final String noun, final String argument) {
      this.noun = noun;
      this.argument = argument;
    }
  }

  private final String token;
  private final Kind kind;

  private Spelling(final String token, final Kind kind) {
    this.token = token;
    this.kind = kind;
  }

  /**
   * @param token the function as written, such as {@code substring}, {@code string} or
   *        {@code ?greeting}
   * @return the spelling of a function
   */
  public static Spelling function(final String token) {
    return new Spelling(token, Kind.FUNCTION);
  }

  /**
   * @param name the name of the template, as {@code INVOKE} writes it
   * @return the spelling of a template
   */
  public static Spelling template(final String name) {
    return new Spelling(name, Kind.TEMPLATE);
  }

  /**
   * @param token the operator as written, such as {@code +} or {@code :preferredLanguage}
   * @return the spelling of an operator
   */
  public static Spelling operator(final String token) {
    return new Spelling(token, Kind.OPERATOR);
  }

  /**
   * @return how a type error names what was written: "Function 'string'",
   *         "Template 'greeting'" or "Operator ':preferredLanguage'"
   */
  public String describe() {
    return this.kind.noun + " '" + this.token + "'";
  }

  /**
   * @param position the position of an argument
   * @param count the number of arguments
   * @return how a type error names the argument at that position: the arguments of a function or
   *         a template by their position, "argument", or "1st argument", "2nd argument" and so
   *         on; the operands of an operator, "operand", or "left operand" and "right operand"
   */
  public String roleOf(final int position, final int count) {
    if (this.kind != Kind.OPERATOR) {
      return Parameters.numbered(this.kind.argument, position, count);
    }
    if (count == 1) {
      return this.kind.argument;
    }
    return position == 0 ? "left operand" : "right operand";
  }

  /**
   * @param number a number of arguments
   * @return how an error counts the arguments of what was written: "1 argument" or
   *         "2 arguments", and the operands of an operator as "1 operand" or "2 operands"
   */
  public String count(final int number) {
    return number + " " + this.kind.argument + (number == 1 ? "" : "s");
  }
}
