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

import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.ParserRuleContext;

import eu.europa.ted.efx.exceptions.Diagnosis;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.constructs.Rejection;
import eu.europa.ted.efx.model.constructs.Spelling;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.identifiers.Variable;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.sdk2.EfxParser.ExpressionContext;

/**
 * What a sub-expression is written as: a field, a node, a value read from a field, a variable, a
 * context variable, a function invocation, a dictionary or a lookup in one, or anything else, such
 * as a literal, a list or the result of an operator.
 *
 * <p>Each kind answers, for the translated expression of the sub-expression, the questions its
 * parent asks: what its value is, what context it names for the reference after {@code ::}, what a
 * predicate or an index after it means, whether a step that reads a field may follow it, and which
 * error explains why a construct rejects it: a sequence where a single value is required, or a
 * node, which has no value, where a value is. The defaults are those of a sub-expression written as
 * anything but a reference.</p>
 *
 * @param <E> the class of the expressions of this kind: a path for a field or a node
 */
abstract class Origin<E extends TypedExpression> {

  /**
   * Reads what a step after a field gives, such as its attribute or its raw value.
   */
  interface FieldReader {

    /**
     * @param field the path of the field
     * @param origin the field
     * @return what the step gives
     */
    Subexpression<?> read(PathExpression field, FieldOrigin origin);
  }

  /**
   * What a step that only a field has takes, as a type error says it.
   */
  private static final String FIELD_REFERENCE = "a field reference";

  protected final EfxExpressionVisitor translator;

  protected Origin(final EfxExpressionVisitor translator) {
    this.translator = translator;
  }

  /**
   * @param expression the translated sub-expression
   * @return its value, where an operator, a built-in, a declaration or a clause takes it; for an
   *         sub-expression that has no value, such as a node, its path, which the construct rejects
   */
  TypedExpression valueOf(final E expression) {
    return expression;
  }

  /**
   * @param expression the translated sub-expression
   * @return the sub-expression as a path, where {@code is present} takes it
   */
  TypedExpression pathOf(final E expression) {
    return expression;
  }

  /**
   * @param expression the translated sub-expression
   * @return the context the reference after {@code ::} is read in, if the sub-expression names one
   */
  Optional<Context> contextOf(final E expression) {
    return Optional.empty();
  }

  /**
   * Translates a predicate after the sub-expression. A value has no elements that the condition
   * could be read from, so a predicate after it is rejected.
   *
   * @param expression the translated sub-expression
   * @param condition the condition of the predicate
   * @return the sub-expression the predicate selects
   * @throws TypeMismatchException unless the sub-expression is a field or a node
   */
  Subexpression<?> filteredBy(final E expression, final ExpressionContext condition) {
    throw this.translator.predicateOnValue(new Subexpression<>(expression, this), condition);
  }

  /**
   * Translates an index after the sub-expression: the value at a position in the sequence of its
   * values. A single value is a sequence of one.
   *
   * @param expression the translated sub-expression
   * @param position the position the index selects
   * @return the value at that position
   */
  Subexpression<?> indexedBy(final E expression, final ExpressionContext position) {
    return this.translator.positionInValue(new Subexpression<>(expression, this), position);
  }

  /**
   * Translates a step that only a field has, such as an attribute or the raw value, after the
   * sub-expression.
   *
   * @param expression the translated sub-expression
   * @param step the step
   * @param spelling the step as written, such as the operator {@code :rawValue}
   * @param reader what the step reads from the field
   * @return what the step gives
   * @throws TypeMismatchException if the sub-expression is not a field
   */
  Subexpression<?> readFromField(final E expression, final ParserRuleContext step,
      final Spelling spelling, final FieldReader reader) {
    throw TypeMismatchException.cannotConvert(step,
        new Diagnosis(spelling.describe(), spelling.roleOf(0, 1), List.of(expression),
            List.of(FIELD_REFERENCE)));
  }

  /**
   * @return the origin of the sub-expression's value, as parentheses give it: no longer a
   *         reference, but with the error of the reference
   */
  Origin<TypedExpression> asValue() {
    return new ValueOrigin(this.translator, this);
  }

  /**
   * Gives the error of the reference the sub-expression is written as, where the reference explains
   * why a construct rejects the sub-expression.
   *
   * @param kind what is wrong with the sub-expression
   * @param diagnosis what the construct was given, and what it requires
   * @return the error of the reference, or nothing if the construct gives its own
   */
  final Optional<TypeMismatchException> errorOf(final Rejection.Kind kind,
      final Diagnosis diagnosis) {
    switch (kind) {
      case CARDINALITY:
        return this.sequenceError(diagnosis);
      case PATH:
        return this.noValueError(diagnosis);
      default:
        return Optional.empty();
    }
  }

  /**
   * @param diagnosis what the construct taking the sub-expression was given, and what it requires
   * @return the error where the sub-expression gives a sequence and a single value is required, if
   *         the sub-expression is written as a reference that it names
   */
  Optional<TypeMismatchException> sequenceError(final Diagnosis diagnosis) {
    return Optional.empty();
  }

  /**
   * @param diagnosis what the construct taking the sub-expression was given, and what it requires
   * @return the error where the sub-expression has no value and a value is required, if the
   *         sub-expression is written as a reference that it names
   */
  Optional<TypeMismatchException> noValueError(final Diagnosis diagnosis) {
    return Optional.empty();
  }

  // #region Values -----------------------------------------------------------

  /**
   * A sub-expression written as anything but a reference: a literal, a list, the result of an
   * operator or of a built-in.
   */
  static final class None extends Origin<TypedExpression> {

    None(final EfxExpressionVisitor translator) {
      super(translator);
    }
  }

  /**
   * The value of a sub-expression that is no longer the reference it is read from, such as a field
   * in parentheses or the attribute of a field. It keeps the error of the reference.
   */
  static final class ValueOrigin extends Origin<TypedExpression> {

    private final Origin<?> reference;

    ValueOrigin(final EfxExpressionVisitor translator, final Origin<?> reference) {
      super(translator);
      this.reference = reference;
    }

    @Override
    Optional<TypeMismatchException> sequenceError(final Diagnosis diagnosis) {
      return this.reference.sequenceError(diagnosis);
    }

    @Override
    Optional<TypeMismatchException> noValueError(final Diagnosis diagnosis) {
      return this.reference.noValueError(diagnosis);
    }
  }

  // #endregion Values --------------------------------------------------------

  // #region Fields and nodes -------------------------------------------------

  /**
   * A field or a node: a path, whose elements a predicate after it filters and an index after it
   * selects one of.
   */
  abstract static class PathOrigin extends Origin<PathExpression> {

    protected PathOrigin(final EfxExpressionVisitor translator) {
      super(translator);
    }

    @Override
    Subexpression<?> filteredBy(final PathExpression path, final ExpressionContext condition) {
      return this.translator.filtered(path, this, condition);
    }

    @Override
    Subexpression<?> indexedBy(final PathExpression path, final ExpressionContext position) {
      return this.translator.positionInPath(path, this, position);
    }

    /**
     * Pushes the context that the condition of a predicate after the path is read in: the field
     * or the node, as each element it selects.
     */
    abstract void pushElementContext();
  }

  /**
   * A field. Its value is the value step on its path. It records the context its error names.
   */
  static final class FieldOrigin extends PathOrigin {

    private final ParserRuleContext reference;
    private final String fieldId;
    private final String contextSymbol;

    /**
     * @param translator the visitor
     * @param reference the reference to the field
     * @param fieldId the field
     * @param contextSymbol the context its error names: the context of the expression the field
     *        is written in, also after {@code ::}, or {@code null} for an absolute reference
     */
    FieldOrigin(final EfxExpressionVisitor translator, final ParserRuleContext reference,
        final String fieldId, final String contextSymbol) {
      super(translator);
      this.reference = reference;
      this.fieldId = fieldId;
      this.contextSymbol = contextSymbol;
    }

    @Override
    TypedExpression valueOf(final PathExpression path) {
      return this.translator.fieldValueOf(this.fieldId, path);
    }

    @Override
    Optional<Context> contextOf(final PathExpression path) {
      return Optional.of(new FieldContext(this.fieldId,
          this.translator.translation().symbols().getAbsolutePathOfField(this.fieldId), path));
    }

    @Override
    Subexpression<?> readFromField(final PathExpression path, final ParserRuleContext step,
        final Spelling spelling, final FieldReader reader) {
      return reader.read(path, this);
    }

    @Override
    void pushElementContext() {
      this.translator.translation().efxContext().pushFieldContext(this.fieldId);
    }

    @Override
    Optional<TypeMismatchException> sequenceError(final Diagnosis diagnosis) {
      if (FieldTypes.TEXT_MULTILINGUAL.getName()
          .equals(this.translator.translation().symbols().getTypeOfField(this.fieldId))) {
        return Optional.of(
            TypeMismatchException.fieldIsMultilingual(this.reference, diagnosis, this.fieldId));
      }
      return Optional.of(TypeMismatchException.fieldMayRepeat(this.reference, diagnosis,
          this.fieldId, this.contextSymbol));
    }
  }

  /**
   * A node. It has no value: where a value is required, its path is given as it is, and the
   * built-in, the composite or the translation that requires the value rejects it.
   */
  static final class NodeOrigin extends PathOrigin {

    private final String nodeId;

    NodeOrigin(final EfxExpressionVisitor translator, final String nodeId) {
      super(translator);
      this.nodeId = nodeId;
    }

    @Override
    Optional<Context> contextOf(final PathExpression path) {
      return Optional.of(new NodeContext(this.nodeId,
          this.translator.translation().symbols().getAbsolutePathOfNode(this.nodeId), path));
    }

    @Override
    void pushElementContext() {
      this.translator.translation().efxContext().pushNodeContext(this.nodeId);
    }
  }

  // #endregion Fields and nodes ----------------------------------------------

  // #region Identifiers ------------------------------------------------------

  /**
   * A declared identifier that gives a value: a function invocation, a variable or a parameter, or
   * a context variable.
   *
   * @param <E> the class of the reference to the identifier: a path for a context variable
   */
  static class IdentifierOrigin<E extends TypedExpression> extends Origin<E> {

    protected final ParserRuleContext reference;

    /**
     * The identifier as an error names it, such as "Variable '$x'", "Parameter '$x'" or
     * "Function '?f'".
     */
    protected final String identifier;

    IdentifierOrigin(final EfxExpressionVisitor translator, final ParserRuleContext reference,
        final String identifier) {
      super(translator);
      this.reference = reference;
      this.identifier = identifier;
    }

    @Override
    Optional<TypeMismatchException> sequenceError(final Diagnosis diagnosis) {
      return Optional.of(
          TypeMismatchException.identifierIsSequence(this.reference, diagnosis, this.identifier));
    }
  }

  /**
   * A variable or a parameter that holds a value. It names no context.
   */
  static final class VariableOrigin extends IdentifierOrigin<TypedExpression> {

    VariableOrigin(final EfxExpressionVisitor translator, final ParserRuleContext reference,
        final String identifier) {
      super(translator, reference, identifier);
    }

    @Override
    Optional<Context> contextOf(final TypedExpression expression) {
      throw InvalidIdentifierException.notAContextVariable(this.reference, this.identifier);
    }
  }

  /**
   * A context variable: it holds one element of the field or the node it iterates, so it names
   * that field or node as a context. A field context variable is a value of its field. A node
   * context variable has no value: where a value is required, its path is given as it is, the
   * construct that requires the value rejects it, and the error says that the variable holds a
   * node. Where a node is taken, as {@code count} takes one, the variable gives its node.
   */
  static final class ContextVariableOrigin extends IdentifierOrigin<PathExpression> {

    private final Context context;

    ContextVariableOrigin(final EfxExpressionVisitor translator,
        final ParserRuleContext reference, final String identifier, final Context context) {
      super(translator, reference, identifier);
      this.context = context;
    }

    /**
     * The value of a field context variable is the value of its field, read from the element it
     * holds. The element of an attribute field is its attribute, which is its own value. A node
     * context variable gives its node.
     */
    @Override
    TypedExpression valueOf(final PathExpression variable) {
      final String symbol = this.context.symbol();
      if (this.context.isNodeContext()
          || this.translator.translation().symbols().isAttributeField(symbol)) {
        return variable;
      }
      return this.translator.translation().script().composeFieldValueReference(variable);
    }

    @Override
    Optional<TypeMismatchException> noValueError(final Diagnosis diagnosis) {
      if (this.context.isNodeContext()) {
        return Optional.of(TypeMismatchException.nodeContextUsedAsValue(this.reference, diagnosis,
            this.identifier, this.context.symbol()));
      }
      return Optional.empty();
    }

    /**
     * The reference after {@code ::} is read from the element the variable holds.
     */
    @Override
    Optional<Context> contextOf(final PathExpression variable) {
      final String symbol = this.context.symbol();
      if (this.context.isNodeContext()) {
        return Optional.of(new NodeContext(symbol,
            this.translator.translation().symbols().getAbsolutePathOfNode(symbol), variable));
      }
      return Optional.of(new FieldContext(symbol,
          this.translator.translation().symbols().getAbsolutePathOfField(symbol), variable));
    }
  }

  /**
   * A name that is not declared as a variable, a parameter or a context variable. It is not a
   * value: it may only name a dictionary, which a key in brackets after it is looked up in. Any
   * other use of it, an index after it included, is rejected: a dictionary has no value of its
   * own, and a name that nothing declares is undeclared.
   */
  static final class DictionaryNameOrigin extends Origin<TypedExpression> {

    private final ParserRuleContext reference;
    private final String name;

    DictionaryNameOrigin(final EfxExpressionVisitor translator, final ParserRuleContext reference,
        final String name) {
      super(translator);
      this.reference = reference;
      this.name = name;
    }

    @Override
    TypedExpression valueOf(final TypedExpression expression) {
      throw this.noValue();
    }

    @Override
    TypedExpression pathOf(final TypedExpression expression) {
      throw this.noValue();
    }

    @Override
    Optional<Context> contextOf(final TypedExpression expression) {
      throw this.namesADictionary()
          ? InvalidIdentifierException.dictionaryWithoutKey(this.reference, this.name)
          : InvalidIdentifierException.notAContextVariable(this.reference,
              Variable.describe(this.name));
    }

    @Override
    Subexpression<?> filteredBy(final TypedExpression expression, final ExpressionContext key) {
      return this.translator.lookUp(this.name, this.reference, key);
    }

    @Override
    Subexpression<?> readFromField(final TypedExpression expression, final ParserRuleContext step,
        final Spelling spelling, final FieldReader reader) {
      throw this.noValue();
    }

    /**
     * @return the error of a use of the name that reads its value
     */
    private InvalidIdentifierException noValue() {
      return this.namesADictionary()
          ? InvalidIdentifierException.dictionaryWithoutKey(this.reference, this.name)
          : InvalidIdentifierException.undeclaredIdentifier(this.reference,
              Variable.describe(this.name));
    }

    private boolean namesADictionary() {
      return this.translator.translation().stack().getDictionary(this.name).isPresent();
    }
  }

  /**
   * A lookup in a dictionary, which gives the sequence of the values the key indexes.
   */
  static final class DictionaryOrigin extends Origin<TypedExpression> {

    private final ParserRuleContext reference;
    private final String name;

    DictionaryOrigin(final EfxExpressionVisitor translator, final ParserRuleContext reference,
        final String name) {
      super(translator);
      this.reference = reference;
      this.name = name;
    }

    @Override
    Optional<TypeMismatchException> sequenceError(final Diagnosis diagnosis) {
      return Optional.of(
          TypeMismatchException.dictionaryIsSequence(this.reference, diagnosis, this.name));
    }
  }

  // #endregion Identifiers ---------------------------------------------------
}
