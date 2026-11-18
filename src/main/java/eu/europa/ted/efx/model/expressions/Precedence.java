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
package eu.europa.ted.efx.model.expressions;

/**
 * The precedence of a script: how tightly the operation at the top of the script binds. The
 * constants are in order, from the loosest to the tightest.
 *
 * <p>A script generator puts the script of an operand in parentheses where the operand binds looser
 * than its place in the operation allows, so that the script keeps the grouping of the EFX
 * expression. For example, the XPath of {@code 1 == 1 == TRUE} is {@code (1 = 1) = true()}, and
 * the XPath of {@code 2 * count-duplicates(BT-00-Number)} puts the subtraction that
 * {@code count-duplicates} is translated to in parentheses.</p>
 */
public enum Precedence {

  /**
   * An expression that starts with a keyword and extends as far to the right as it can, such as
   * {@code for}, {@code some} and {@code every}.
   */
  PREFIX,

  /** A disjunction: {@code or}. */
  OR,

  /** A conjunction: {@code and}. */
  AND,

  /** A comparison, such as {@code =} or {@code <}. */
  COMPARISON,

  /** An addition or a subtraction. */
  ADDITIVE,

  /** A multiplication, a division or a modulus. */
  MULTIPLICATIVE,

  /** A negative number, such as {@code -1}. */
  UNARY,

  /**
   * No operation at the top: a literal, a path, a variable, a call, an expression in parentheses,
   * or one of these followed by a predicate.
   */
  PRIMARY;

  /**
   * @param required the precedence that a place in an operation requires of its operand
   * @return whether a script of this precedence binds at least as tightly as required
   */
  public boolean bindsAtLeast(final Precedence required) {
    return this.compareTo(required) >= 0;
  }
}
