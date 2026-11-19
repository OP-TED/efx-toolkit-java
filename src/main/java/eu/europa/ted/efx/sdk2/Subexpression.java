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

import java.util.Optional;

import org.antlr.v4.runtime.ParserRuleContext;

import eu.europa.ted.efx.exceptions.Diagnosis;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.constructs.Rejection;
import eu.europa.ted.efx.model.constructs.Spelling;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.sdk2.EfxParser.ExpressionContext;

/**
 * A sub-expression as the visitor translates it: the translated expression, and the
 * {@link Origin} of the sub-expression, which is what it is written as.
 *
 * <p>The construct that contains a sub-expression decides how it reads it: as a value, which is
 * what an operator, a built-in, a declaration or a clause takes; as a path, which is what
 * {@code is present} takes and what a step such as {@code /@attribute} continues; or as the context
 * that the reference after {@code ::} is read in. The origin answers each reading, so that no
 * reading needs the parse tree of the sub-expression again.</p>
 *
 * @param <E> the class of the translated expression: a path for a field or a node
 */
final class Subexpression<E extends TypedExpression> {

  private final E expression;
  private final Origin<? super E> origin;

  Subexpression(final E expression, final Origin<? super E> origin) {
    this.expression = expression;
    this.origin = origin;
  }

  /**
   * @return the translated expression, as it is written: the path of a field or a node
   */
  E expression() {
    return this.expression;
  }

  Origin<? super E> origin() {
    return this.origin;
  }

  /**
   * @return the value of the sub-expression, where a value is required; for a sub-expression that
   *         has no value, such as a node, its path, which the construct that requires a value
   *         rejects
   */
  TypedExpression value() {
    return this.origin.valueOf(this.expression);
  }

  /**
   * @return the sub-expression as a path, where a path is required
   */
  TypedExpression path() {
    return this.origin.pathOf(this.expression);
  }

  /**
   * @return the context that the reference after {@code ::} is read in, if the sub-expression names
   *         one
   */
  Optional<Context> context() {
    return this.origin.contextOf(this.expression);
  }

  /**
   * Translates a predicate after the sub-expression, or a lookup after the name of a dictionary,
   * which is written as one.
   *
   * @param condition the condition of the predicate, or the key of the lookup
   * @return the sub-expression selected by the predicate, or the values the key indexes
   */
  Subexpression<?> filteredBy(final ExpressionContext condition) {
    return this.origin.filteredBy(this.expression, condition);
  }

  /**
   * Translates an index after the sub-expression.
   *
   * @param position the position the index selects
   * @return the sub-expression selected by the index
   */
  Subexpression<?> indexedBy(final ExpressionContext position) {
    return this.origin.indexedBy(this.expression, position);
  }

  /**
   * Translates a step that only a field has, such as an attribute or the raw value, after the
   * sub-expression.
   *
   * @param step the step
   * @param spelling the step as written, such as the operator {@code :rawValue}
   * @param reader what the step reads from the field
   * @return what the step gives
   */
  Subexpression<?> readFromField(final ParserRuleContext step, final Spelling spelling,
      final Origin.FieldReader reader) {
    return this.origin.readFromField(this.expression, step, spelling, reader);
  }

  /**
   * Gives the error of the reference the sub-expression is written as, where the reference explains
   * why a construct rejects the sub-expression: a sequence where a single value is required, or a
   * node, which has no value, where a value is.
   *
   * @param kind what is wrong with the sub-expression
   * @param diagnosis what the construct was given, and what it requires
   * @return the error of the reference, or nothing if the construct gives its own
   */
  Optional<TypeMismatchException> errorOf(final Rejection.Kind kind, final Diagnosis diagnosis) {
    return this.origin.errorOf(kind, diagnosis);
  }
}
