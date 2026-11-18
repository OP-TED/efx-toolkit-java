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

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import eu.europa.ted.efx.exceptions.Diagnosis;
import eu.europa.ted.efx.exceptions.EfxCompilationException;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.SdkInconsistencyException;
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.PrivacySetting;
import eu.europa.ted.efx.model.Reads;
import eu.europa.ted.efx.model.builtins.BuiltIn;
import eu.europa.ted.efx.model.builtins.BuiltIns;
import eu.europa.ted.efx.model.builtins.ConditionOperators;
import eu.europa.ted.efx.model.builtins.ConversionFunctions;
import eu.europa.ted.efx.model.builtins.DateTimeFunctions;
import eu.europa.ted.efx.model.builtins.LogicalOperators;
import eu.europa.ted.efx.model.builtins.NumericFunctions;
import eu.europa.ted.efx.model.builtins.PatternMatch;
import eu.europa.ted.efx.model.builtins.SequenceFunctions;
import eu.europa.ted.efx.model.builtins.StringFunctions;
import eu.europa.ted.efx.model.builtins.TemplateFunctions;
import eu.europa.ted.efx.model.constructs.Arguments;
import eu.europa.ted.efx.model.constructs.Construct;
import eu.europa.ted.efx.model.constructs.Mismatch;
import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.constructs.Rejection;
import eu.europa.ted.efx.model.constructs.Resolution;
import eu.europa.ted.efx.model.constructs.Spelling;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.identifiers.Dictionary;
import eu.europa.ted.efx.model.identifiers.Function;
import eu.europa.ted.efx.model.identifiers.Invocable;
import eu.europa.ted.efx.model.identifiers.ParsedParameter;
import eu.europa.ted.efx.model.identifiers.Variable;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeTokenLookup;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.sdk2.EfxParser.*;
import eu.europa.ted.efx.sdk2.Origin.ContextVariableOrigin;
import eu.europa.ted.efx.sdk2.Origin.DictionaryNameOrigin;
import eu.europa.ted.efx.sdk2.Origin.DictionaryOrigin;
import eu.europa.ted.efx.sdk2.Origin.FieldOrigin;
import eu.europa.ted.efx.sdk2.Origin.IdentifierOrigin;
import eu.europa.ted.efx.sdk2.Origin.NodeOrigin;
import eu.europa.ted.efx.sdk2.Origin.PathOrigin;
import eu.europa.ted.efx.sdk2.Origin.VariableOrigin;
import eu.europa.ted.efx.util.EfxRegexValidator;

/**
 * Translates the expressions of EFX-2 to the target language of a {@link ScriptGenerator}.
 *
 * <p>The grammar does not type expressions: each visit translates its subtree and returns an
 * {@link Subexpression}, the translated expression with what it is written as. The parent reads
 * each sub-expression as a value, as a path or as a context. Every construct is checked the same
 * way, against the parameter lists of its {@link Construct}: the catalogue's built-ins then
 * translate themselves, and the constructs the catalogue does not hold, such as lists,
 * conditionals, iterations, predicates and indexes, are translated here.</p>
 *
 * <p>The template and rules translators hold a visitor for the expressions their files contain.
 * Their behaviour in expressions is given by subclasses: the template's allows the built-ins that
 * only templates may call, and the rules' translates calls of API functions and records the
 * dynamic dependencies of variables.</p>
 */
class EfxExpressionVisitor extends EfxBaseVisitor<Subexpression<?>> {

  /**
   * The context an absolute reference is resolved from: none, so that its path starts at the root.
   */
  private static final String NO_CONTEXT = null;

  /**
   * The variable that an index binds its position to, where the position is read once before the
   * element is selected. Like every name the translator generates, it starts with a prefix that no
   * declaration may give.
   */
  private static final String INDEX_VARIABLE = Translation.RESERVED_PREFIX + "index";

  private final Translation translation;
  private final Origin<TypedExpression> none;
  private final IteratorDeclarations iteratorDeclarations = new IteratorDeclarations();

  EfxExpressionVisitor(final Translation translation) {
    this.translation = translation;
    this.none = new Origin.None(this);
  }

  /**
   * @return the state of the translation whose expressions the visitor translates
   */
  Translation translation() {
    return this.translation;
  }

  @Override
  public Subexpression<?> visit(final ParseTree tree) {
    return this.translate((ParserRuleContext) tree, () -> super.visit(tree));
  }

  /**
   * Translates a node: keeps it on the call stack while it is translated, which names it in the
   * errors it gives. An error raised under the node with no position of its own, such as the
   * symbol resolver's, is placed at the node.
   *
   * <p>A node that is translated as part of another, not visited on its own, such as the right
   * side of a context override or an iterator, is translated here so that its errors are placed
   * at it rather than at the node it is part of.
   *
   * <p>A parent reads a sub-expression, as a value, a path or a context, after the sub-expression
   * is translated, so an error that the symbol resolver or the script generator raises with no
   * position during that reading is placed at the parent.
   *
   * @param node the node
   * @param translation its translation
   * @return what the translation gives
   */
  private <T> T translate(final ParserRuleContext node, final Supplier<T> translation) {
    this.translation.stack().pushParserContext(node);
    try {
      return translation.get();
    } catch (final EfxCompilationException e) {
      throw e.at(node);
    } finally {
      this.translation.stack().popParserContext();
    }
  }

  // #region Readings of an expression ----------------------------------------

  /**
   * Translates the expression of a clause: the condition of a rule or of a template block, the
   * value of a declaration, the expression of a template block. A single value where a sequence is
   * required is promoted to one; a clause that takes a value of any type rejects only a node,
   * which has no value.
   *
   * @param expression the expression
   * @param clause the clause
   * @return the value, converted to the parameter of the clause, to be read with the accessor of
   *         that parameter's type, such as {@link Arguments#booleanAt}
   * @throws TypeMismatchException if the value does not fit: a sequence where a single value is
   *         required, or a node, is reported by the reference the expression is written as where
   *         it explains the mistake, and any other value cannot be converted
   */
  Arguments<?> valueOf(final ExpressionContext expression, final Clause clause) {
    return this.checked(clause, List.of(this.visit(expression)), position -> expression);
  }

  /**
   * Checks a value that a clause is given as it is, written as nothing the clause could name in
   * its error, such as a part of a label block.
   *
   * @param value the value
   * @param clause the clause
   * @param where where the error is reported
   * @return the value, converted to the parameter of the clause
   * @throws TypeMismatchException if the value does not convert to what the clause takes
   */
  Arguments<?> valueOf(final TypedExpression value, final Clause clause,
      final ParserRuleContext where) {
    return this.checked(clause, List.of(this.subexpression(value)), position -> where);
  }

  /**
   * Translates the value a variable is initialized with, which must be of its declared type.
   *
   * @param initializer the initializer of the variable
   * @return the value, converted to the declared type
   * @throws TypeMismatchException if the value does not fit the declared type
   */
  TypedExpression initialValueOf(final TypedVariableInitializerContext initializer) {
    return this.valueOf(initializer.expression(),
        Clause.of("Variable '" + initializer.typeDeclaration().getText() + ":$"
            + initializer.variableName.getText() + "'",
            parameter("value", ParameterType.of(typeOf(initializer.typeDeclaration())))))
        .at(0, TypedExpression.class);
  }

  /**
   * @param fieldOrNodeContext a field or a node, with its predicates, where a context is declared
   * @return the context it declares
   */
  Context contextOf(final ParserRuleContext fieldOrNodeContext) {
    return this.visit(fieldOrNodeContext).context().orElseThrow();
  }

  /**
   * @param initializer the initializer of a context variable
   * @return the context it gives the variable: a field or a node, with its predicates
   * @throws TranslatorConfigurationException if the variable is initialized with the root, which
   *         no translator handles yet
   */
  Context contextAssignedBy(final ContextVariableInitializerContext initializer) {
    if (initializer.Slash() != null) {
      throw TranslatorConfigurationException.unhandledVariableContext(initializer.getText());
    }
    return this.contextOf(initializer.getRuleContext(ParserRuleContext.class, 0));
  }

  /**
   * @param expression a translated expression
   * @return the sub-expression of a translated expression written as anything but a reference
   */
  Subexpression<TypedExpression> subexpression(final TypedExpression expression) {
    return new Subexpression<>(expression, this.none);
  }

  private Subexpression<?> visitOnlyChild(final ParserRuleContext wrapper) {
    return this.visit(wrapper.getRuleContext(ParserRuleContext.class, 0));
  }

  // #endregion Readings of an expression -------------------------------------

  // #region Literals ---------------------------------------------------------

  @Override
  public Subexpression<?> visitLiteralExpression(final LiteralExpressionContext ctx) {
    return this.visit(ctx.literal());
  }

  @Override
  public Subexpression<?> visitLiteral(final LiteralContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitStringLiteral(final StringLiteralContext ctx) {
    return this.subexpression(this.translation.script().getStringLiteralEquivalent(ctx.getText()));
  }

  @Override
  public Subexpression<?> visitNumericLiteral(final NumericLiteralContext ctx) {
    return this.subexpression(this.translation.script().getNumericLiteralEquivalent(ctx.getText()));
  }

  @Override
  public Subexpression<?> visitBooleanLiteral(final BooleanLiteralContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitTrueBooleanLiteral(final TrueBooleanLiteralContext ctx) {
    return this.subexpression(this.translation.script().getBooleanEquivalent(true));
  }

  @Override
  public Subexpression<?> visitFalseBooleanLiteral(final FalseBooleanLiteralContext ctx) {
    return this.subexpression(this.translation.script().getBooleanEquivalent(false));
  }

  @Override
  public Subexpression<?> visitDateLiteral(final DateLiteralContext ctx) {
    return this.subexpression(
        this.translation.script().getDateLiteralEquivalent(ctx.DateLiteral().getText()));
  }

  @Override
  public Subexpression<?> visitTimeLiteral(final TimeLiteralContext ctx) {
    return this.subexpression(
        this.translation.script().getTimeLiteralEquivalent(ctx.TimeLiteral().getText()));
  }

  @Override
  public Subexpression<?> visitDurationLiteral(final DurationLiteralContext ctx) {
    return this.subexpression(
        this.translation.script().getDurationLiteralEquivalent(ctx.getText()));
  }

  // Parameter values: the arguments of a single expression

  @Override
  public Subexpression<?> visitParameterValue(final ParameterValueContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitScalarParameterValue(final ScalarParameterValueContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitSequenceParameterValue(final SequenceParameterValueContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitStringSequenceLiteral(final StringSequenceLiteralContext ctx) {
    return this.literalList(ctx.stringLiteral(), StringExpression.class,
        StringSequenceExpression.class);
  }

  @Override
  public Subexpression<?> visitBooleanSequenceLiteral(final BooleanSequenceLiteralContext ctx) {
    return this.literalList(ctx.booleanLiteral(), BooleanExpression.class,
        BooleanSequenceExpression.class);
  }

  @Override
  public Subexpression<?> visitNumericSequenceLiteral(final NumericSequenceLiteralContext ctx) {
    return this.literalList(ctx.numericLiteral(), NumericExpression.class,
        NumericSequenceExpression.class);
  }

  @Override
  public Subexpression<?> visitDateSequenceLiteral(final DateSequenceLiteralContext ctx) {
    return this.literalList(ctx.dateLiteral(), DateExpression.class,
        DateSequenceExpression.class);
  }

  @Override
  public Subexpression<?> visitTimeSequenceLiteral(final TimeSequenceLiteralContext ctx) {
    return this.literalList(ctx.timeLiteral(), TimeExpression.class,
        TimeSequenceExpression.class);
  }

  @Override
  public Subexpression<?> visitDurationSequenceLiteral(final DurationSequenceLiteralContext ctx) {
    return this.literalList(ctx.durationLiteral(), DurationExpression.class,
        DurationSequenceExpression.class);
  }

  private <T extends ScalarExpression> Subexpression<?> literalList(
      final List<? extends ParserRuleContext> literals, final Class<T> itemType,
      final Class<? extends SequenceExpression> listType) {
    final List<T> items = new ArrayList<>(literals.size());
    for (final ParserRuleContext literal : literals) {
      items.add(itemType.cast(this.visit(literal).value()));
    }
    return this.subexpression(this.translation.script().composeList(items, listType));
  }

  // #endregion Literals ------------------------------------------------------

  // #region References -------------------------------------------------------

  @Override
  public Subexpression<?> visitReferenceExpression(final ReferenceExpressionContext ctx) {
    return this.visit(ctx.reference());
  }

  @Override
  public Subexpression<?> visitReference(final ReferenceContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitFieldReference(final FieldReferenceContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  /**
   * A field, by its path from the current context.
   */
  @Override
  public Subexpression<?> visitLinkedFieldReference(final LinkedFieldReferenceContext ctx) {
    final String fieldId = this.fieldIdOf(ctx);
    return new Subexpression<>(this.relativePathOfField(fieldId),
        new FieldOrigin(this, ctx, fieldId, this.translation.efxContext().symbol()));
  }

  /**
   * A field, by its path from the root, which its error names as the context it is read from.
   */
  @Override
  public Subexpression<?> visitAbsoluteFieldReference(final AbsoluteFieldReferenceContext ctx) {
    final LinkedFieldReferenceContext reference = ctx.linkedFieldReference();
    final String fieldId = this.fieldIdOf(reference);
    return new Subexpression<>(this.translate(reference, () -> this.absolutePathOfField(fieldId)),
        new FieldOrigin(this, ctx, fieldId, NO_CONTEXT));
  }

  @Override
  public Subexpression<?> visitNodeReference(final NodeReferenceContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  @Override
  public Subexpression<?> visitSimpleNodeReference(final SimpleNodeReferenceContext ctx) {
    final String nodeId = ctx.NodeId().getText();
    return new Subexpression<>(this.relativePathOfNode(nodeId), new NodeOrigin(this, nodeId));
  }

  @Override
  public Subexpression<?> visitAbsoluteNodeReference(final AbsoluteNodeReferenceContext ctx) {
    final SimpleNodeReferenceContext reference = ctx.simpleNodeReference();
    final String nodeId = reference.NodeId().getText();
    return new Subexpression<>(this.translate(reference, () -> this.absolutePathOfNode(nodeId)),
        new NodeOrigin(this, nodeId));
  }

  /**
   * A field with its predicates and indexes, where a context is declared or a field is taken as
   * it is.
   */
  @Override
  public Subexpression<?> visitFieldContext(final FieldContextContext ctx) {
    return this.withBrackets(this.visitOnlyChild(ctx), ctx.contextBracket());
  }

  /**
   * A node with its predicates and indexes, where a context is declared.
   */
  @Override
  public Subexpression<?> visitNodeContext(final NodeContextContext ctx) {
    return this.withBrackets(this.visitOnlyChild(ctx), ctx.contextBracket());
  }

  /**
   * Applies the predicates and the indexes after the reference of a context, in the order they
   * are written.
   */
  private Subexpression<?> withBrackets(final Subexpression<?> reference,
      final List<ContextBracketContext> brackets) {
    Subexpression<?> selected = reference;
    for (final ContextBracketContext bracket : brackets) {
      selected = bracket.predicate() != null
          ? selected.filteredBy(bracket.predicate().expression())
          : selected.indexedBy(bracket.position);
    }
    return selected;
  }

  /**
   * A variable, a parameter, a context variable, or a name that none of them has. Such a name has
   * no value: it may only name a dictionary, which a key in brackets after it looks up, and its
   * sub-expression holds an empty expression, which no reading takes.
   */
  @Override
  public Subexpression<?> visitVariableReference(final VariableReferenceContext ctx) {
    final String name = ctx.variableName.getText();
    final Optional<TypedExpression> parameter = this.translation.stack().getParameter(name);
    if (parameter.isPresent()) {
      return new Subexpression<>(parameter.get(),
          new VariableOrigin(this, ctx, ParsedParameter.describe(name)));
    }
    final Optional<Variable> variable = this.translation.stack().getVariable(name);
    if (variable.isPresent()) {
      return this.variableReference(ctx, variable.get());
    }
    return new Subexpression<>(Expression.empty(StringExpression.class),
        new DictionaryNameOrigin(this, ctx, name));
  }

  /**
   * A variable, or a context variable, whose reference is a path to the one element of its context
   * that it holds.
   */
  private Subexpression<?> variableReference(final VariableReferenceContext ctx,
      final Variable variable) {
    final Context context = this.translation.efxContext().getContextFromVariable(variable);
    if (context == null) {
      return new Subexpression<>(variable.referenceExpression,
          new VariableOrigin(this, ctx, variable.describe()));
    }
    return new Subexpression<>((PathExpression) variable.referenceExpression,
        new ContextVariableOrigin(this, ctx, variable.describe(), context));
  }

  /**
   * Declares a context variable: a variable that holds one element of its context, and names the
   * context where it is read.
   *
   * @param variable the variable, whose reference is a path
   * @param context the context
   */
  void declareContextVariable(final Variable variable, final Context context) {
    this.translation.stack().declareIdentifier(variable);
    this.translation.efxContext().declareContextVariable(variable, context);
  }

  @Override
  public Subexpression<?> visitCodelistExpression(final CodelistExpressionContext ctx) {
    return this.visit(ctx.codelistReference());
  }

  @Override
  public Subexpression<?> visitCodelistReference(final CodelistReferenceContext ctx) {
    return this.subexpression(this.translation.script().composeList(
        this.translation.symbols().expandCodelist(ctx.codelistName.getText()).stream()
            .map(this.translation.script()::getStringLiteralFromUnquotedString)
            .collect(Collectors.toList()),
        StringSequenceExpression.class));
  }

  private String fieldIdOf(final LinkedFieldReferenceContext ctx) {
    return this.fieldIdOf(ctx, ctx.simpleFieldReference().fieldId.getText(),
        ctx.linkedFieldProperty());
  }

  private String fieldIdOf(final FieldMentionContext ctx) {
    return this.fieldIdOf(ctx, ctx.fieldId.getText(), ctx.linkedFieldProperty());
  }

  /**
   * @param reference the reference to the field, where an error is reported
   * @param baseFieldId the field written
   * @param property the linked field property after it, or {@code null}
   * @return the identifier of the field written, or of the field the property links to it
   * @throws InvalidUsageException if a property links to a field that cannot be withheld
   */
  private String fieldIdOf(final ParserRuleContext reference, final String baseFieldId,
      final LinkedFieldPropertyContext property) {
    return property == null ? baseFieldId
        : this.translate(reference, () -> this.linkedFieldIdOf(reference, baseFieldId, property));
  }

  /**
   * Gives the field linked to the privacy settings of a field. Only a field that can be withheld
   * has privacy settings, so any other field is rejected, as the privacy properties reject it.
   */
  private String linkedFieldIdOf(final ParserRuleContext reference, final String baseFieldId,
      final LinkedFieldPropertyContext ctx) {
    this.requireWithholdable(reference, baseFieldId);
    if (ctx.PublicationDate() != null) {
      return this.translation.symbols().getPrivacySettingOfField(baseFieldId,
          PrivacySetting.PUBLICATION_DATE_FIELD);
    }
    if (ctx.JustificationCode() != null) {
      return this.translation.symbols().getPrivacySettingOfField(baseFieldId,
          PrivacySetting.JUSTIFICATION_CODE_FIELD);
    }
    if (ctx.JustificationDescription() != null) {
      return this.translation.symbols().getPrivacySettingOfField(baseFieldId,
          PrivacySetting.JUSTIFICATION_DESCRIPTION_FIELD);
    }
    throw TranslatorConfigurationException.unhandledLinkedFieldProperty(ctx.getText());
  }

  /**
   * Returns the reference to the value of a field, given the path of the field. The value of an
   * attribute field is its attribute, on the element of the field. The value has the cardinality
   * of the path.
   *
   * @param fieldId the field
   * @param path the path of the field
   * @return the value of the field on that path
   */
  PathExpression fieldValueOf(final String fieldId, final PathExpression path) {
    if (!this.translation.symbols().isAttributeField(fieldId)) {
      return this.translation.script().composeFieldValueReference(path);
    }
    final PathExpression attribute = this.translation.script().composeFieldAttributeReference(
        this.translation.script().contextualizePath(
            this.translation.symbols().getAbsolutePathOfFieldWithoutTheAttribute(fieldId),
            this.absolutePathOfCurrentContext()),
        this.translation.symbols().getAttributeNameFromAttributeField(fieldId),
        ScalarPath.fromFieldType.get(
            FieldTypes.fromString(this.translation.symbols().getTypeOfField(fieldId))));
    return continuing(path, attribute);
  }

  /**
   * Types a path that continues from the nodes another path selects: it may select several nodes if
   * the other may, and otherwise as many as it selects from a single node.
   *
   * @param start the path whose nodes the continuation starts from
   * @param continuation the continuing path, typed as it is from a single node
   * @return the continuation, as a sequence if the start is one
   */
  private static PathExpression continuing(final PathExpression start,
      final PathExpression continuation) {
    return start instanceof SequenceExpression ? continuation.asSequence() : continuation;
  }

  // #endregion References ----------------------------------------------------

  // #region Steps after a reference ------------------------------------------

  /**
   * The attribute of a field, on each element the field's path selects.
   */
  @Override
  public Subexpression<?> visitAttributeExpression(final AttributeExpressionContext ctx) {
    final String attribute = ctx.attributeName.getText();
    return this.visit(ctx.expression()).readFromField(ctx, Spelling.operator("/@" + attribute),
        (field, origin) -> new Subexpression<>(continuing(field,
            this.translation.script().composeFieldAttributeReference(field, attribute,
                StringPath.class)),
            origin.asValue()));
  }

  @Override
  public Subexpression<?> visitRawValueExpression(final RawValueExpressionContext ctx) {
    return this.visit(ctx.expression()).readFromField(ctx,
        Spelling.operator(":" + ctx.RawValue().getText()),
        (field, origin) -> new Subexpression<>(
            this.translation.script().composeFieldRawValueReference(field), origin.asValue()));
  }

  @Override
  public Subexpression<?> visitPreferredLanguageExpression(
      final PreferredLanguageExpressionContext ctx) {
    return this.preferredLanguage(ctx, ctx.expression(), TemplateFunctions.PREFERRED_LANGUAGE,
        Spelling.operator(":" + ctx.PreferredLanguage().getText()));
  }

  @Override
  public Subexpression<?> visitPreferredLanguageTextExpression(
      final PreferredLanguageTextExpressionContext ctx) {
    return this.preferredLanguage(ctx, ctx.expression(), TemplateFunctions.PREFERRED_LANGUAGE_TEXT,
        Spelling.operator(":" + ctx.PreferredLanguageText().getText()));
  }

  /**
   * Translates the preferred language of a multilingual field, or its text in that language, as the
   * postfix form and the function form both write it: the sub-expression must be a field, and the
   * built-in takes it as a path.
   *
   * @param call the postfix form or the call of the function
   * @param field the sub-expression, which must be a field
   * @param builtIn the built-in
   * @param spelling the built-in as the form spells it
   * @return the result of the built-in
   */
  private Subexpression<?> preferredLanguage(final ParserRuleContext call,
      final ExpressionContext field, final BuiltIn builtIn, final Spelling spelling) {
    return this.visit(field).readFromField(call, spelling, (path, origin) -> this.pathCall(call,
        builtIn, spelling, List.of(new Subexpression<>(path, origin))));
  }

  /**
   * A field read in the context its left side names: {@code ND-1::BT-1}, {@code BT-1::BT-2},
   * {@code $context::BT-1}. The path of the field from that context continues the path to it.
   */
  @Override
  public Subexpression<?> visitFieldFromContextExpression(
      final FieldFromContextExpressionContext ctx) {
    final Context context = this.contextBefore(ctx.expression());
    final String fieldId = this.fieldIdOf(ctx.linkedFieldReference());
    final PathExpression field = this.translate(ctx.linkedFieldReference(),
        () -> this.translation.symbols().getRelativePathOfField(fieldId, context.symbol()));
    return new Subexpression<>(this.joinReference(context.relativePath(), field),
        new FieldOrigin(this, ctx, fieldId, this.translation.efxContext().symbol()));
  }

  /**
   * A node read in the context its left side names: {@code ND-1::ND-2}, {@code $context::ND-1}.
   */
  @Override
  public Subexpression<?> visitNodeFromContextExpression(
      final NodeFromContextExpressionContext ctx) {
    final Context context = this.contextBefore(ctx.expression());
    final String nodeId = ctx.simpleNodeReference().NodeId().getText();
    final PathExpression node = this.translate(ctx.simpleNodeReference(),
        () -> this.translation.symbols().getRelativePathOfNode(nodeId, context.symbol()));
    return new Subexpression<>(this.joinReference(context.relativePath(), node),
        new NodeOrigin(this, nodeId));
  }

  private Context contextBefore(final ExpressionContext left) {
    final Subexpression<?> operand = this.visit(left);
    return operand.context().orElseThrow(() -> this.unreadable(
        ExpressionConstruct.CONTEXT_OVERRIDE, List.of(operand), List.of(operand.expression()), 0,
        left));
  }

  /**
   * Joins the path to the context of a context override with the path of the reference from that
   * context, which the override composes. The joined reference may give several values if either
   * path may select several nodes, so the override is given the reference as it continues from
   * the context. A context variable holds one node, so its own path never does.
   *
   * @param context the path to the context, from the context the override is written in
   * @param reference the path of the reference, from the context of the override
   * @return the path of the reference from the context the override is written in
   */
  private PathExpression joinReference(final PathExpression context,
      final PathExpression reference) {
    return PathExpression.class.cast(ExpressionConstruct.CONTEXT_OVERRIDE
        .resolve(List.of(context, continuing(context, reference))).orElseThrow()
        .compose(this.translation.script()));
  }

  // #endregion Steps after a reference ---------------------------------------

  // #region Brackets ---------------------------------------------------------

  /**
   * A predicate, or a key in brackets after the name of a dictionary.
   */
  @Override
  public Subexpression<?> visitPredicateOrLookupExpression(
      final PredicateOrLookupExpressionContext ctx) {
    return this.visit(ctx.expression(0)).filteredBy(ctx.expression(1));
  }

  /**
   * An index: the element at a position in a sequence.
   */
  @Override
  public Subexpression<?> visitIndexedExpression(final IndexedExpressionContext ctx) {
    return this.visit(ctx.expression(0)).indexedBy(ctx.expression(1));
  }

  /**
   * A predicate after a field or a node. Its condition is read in the context of the field or
   * node, as each element the path selects, and keeps the elements for which it holds.
   *
   * @param path the path of the field or the node
   * @param origin the field or the node
   * @param condition the condition of the predicate
   * @return the elements the predicate keeps, with the origin of the field or node
   * @throws TypeMismatchException if the condition is not an indicator
   */
  Subexpression<?> filtered(final PathExpression path, final PathOrigin origin,
      final ExpressionContext condition) {
    origin.pushElementContext();
    final Subexpression<?> test = this.visit(condition);
    final PathExpression selected = PathExpression.class.cast(this.resolved(
        ExpressionConstruct.PREDICATE, List.of(new Subexpression<>(path, origin), test),
        List.of(path, test.value()), position -> condition, TypeMismatchException::cannotConvert)
        .compose(this.translation.script()));
    this.translation.efxContext().pop();
    return new Subexpression<>(selected, origin);
  }

  /**
   * A predicate after a value, which is rejected: a value has no elements that the condition could
   * be read from. The condition is translated only so that the error names its type.
   *
   * @param sequence the value
   * @param condition the condition of the predicate
   * @return the error
   */
  TypeMismatchException predicateOnValue(final Subexpression<?> sequence,
      final ExpressionContext condition) {
    final Subexpression<?> test = this.visit(condition);
    return this.unreadable(ExpressionConstruct.PREDICATE, List.of(sequence, test),
        List.of(sequence.expression(), test.value()), 0,
        this.translation.stack().peekParserContext());
  }

  /**
   * An index after a field or a node: the element at a position in the whole sequence the path
   * selects, rather than at that position among the children of each parent, which a predicate on
   * the last step of the path would select: see {@link #wholeSequence}. The position is read once,
   * where the expression stands: see {@link #elementAt}.
   *
   * @param path the path of the field or the node
   * @param origin the field or the node
   * @param position the position the index selects
   * @return the element at that position, with the origin of the field or node
   * @throws TypeMismatchException if the position is not a number
   */
  Subexpression<?> positionInPath(final PathExpression path, final PathOrigin origin,
      final ExpressionContext position) {
    final Reads reads = this.translation.efxContext().readsInCurrentContext();
    final int readsBefore = reads.ofContext();
    final Subexpression<?> index = this.visit(position);
    final Resolution indexing = this.resolved(ExpressionConstruct.INDEX_IN_PATH,
        List.of(new Subexpression<>(path, origin), index),
        List.of(this.wholeSequence(path), index.value()), at -> position,
        TypeMismatchException::cannotConvert);
    return new Subexpression<>(
        PathExpression.class.cast(this.elementAt(ExpressionConstruct.INDEX_IN_PATH, indexing,
            ExpressionConstruct.sequenceClassOf(path), reads.ofContext() > readsBefore)),
        origin);
  }

  /**
   * An index after a value: the value at a position in the sequence of its values. A single value
   * is a sequence of one, and a value that is still a path, as an attribute or a field context
   * variable is, selects from its whole sequence: see {@link #wholeSequence}. The position is read
   * once, where the expression stands: see
   * {@link #elementAt}. Here the target language would read it for each value if it reads the
   * context or the root, by a path or by a lookup in a dictionary, since a value has no element
   * that a path or a lookup could start from.
   *
   * @param sequence the value
   * @param position the position the index selects
   * @return the value at that position
   * @throws TypeMismatchException if the sequence has no value or the position is not a number
   */
  Subexpression<?> positionInValue(final Subexpression<?> sequence,
      final ExpressionContext position) {
    final TypedExpression given = sequence.value();
    final Reads reads = this.translation.efxContext().readsInCurrentContext();
    final int readsBefore = reads.all();
    final Subexpression<?> index = this.visit(position);
    final ParserRuleContext indexed = this.translation.stack().peekParserContext();
    final Resolution indexing = this.resolved(ExpressionConstruct.INDEX_IN_VALUE,
        List.of(sequence, index), List.of(this.wholeSequence(given), index.value()),
        at -> at == 0 ? indexed : position, TypeMismatchException::cannotConvert);
    return this.subexpression(this.elementAt(ExpressionConstruct.INDEX_IN_VALUE, indexing,
        indexing.getArguments().sequenceTypeAt(0), reads.all() > readsBefore));
  }

  /**
   * The element an index selects. The position is read once, where the expression stands. If the
   * target language would read it again for each element, it is bound first, and the element is
   * selected at the bound position, in parentheses, so that a step, a predicate or an operator
   * after it applies to the element: {@code (for $__index in position return sequence[$__index])}.
   *
   * @param index the index
   * @param indexing the resolution of the index, with the sequence and the position
   * @param sequenceType the class of the sequence
   * @param readForEachElement whether the target language would read the position for each element
   * @return the element at that position
   */
  private TypedExpression elementAt(final ExpressionConstruct index, final Resolution indexing,
      final Class<? extends SequenceExpression> sequenceType, final boolean readForEachElement) {
    final ScriptGenerator script = this.translation.script();
    if (!readForEachElement) {
      return indexing.compose(script);
    }
    final IteratorListExpression binding = script.composeIteratorList(
        List.of(script.composeIteratorExpression(
            script.composeVariableDeclaration(INDEX_VARIABLE, NumericExpression.class),
            TypedExpression.from(indexing.getArguments().numericAt(1),
                NumericSequenceExpression.class))));
    final ScalarExpression element = ScalarExpression.class.cast(index
        .resolve(List.of(indexing.getArguments().at(0, TypedExpression.class),
            script.composeVariableReference(INDEX_VARIABLE, NumericExpression.class)))
        .orElseThrow().compose(script));
    return ExpressionConstruct.inParentheses(script,
        script.composeForExpression(binding, element, sequenceType), element.getClass());
  }

  /**
   * A key in brackets after the name of a dictionary: the values the key indexes. A dictionary
   * indexes the fields of the whole notice, so a lookup reads the root.
   *
   * @param name the name of the dictionary
   * @param reference the name of the dictionary, as written
   * @param key the key
   * @return the values the dictionary indexes by the key
   * @throws InvalidIdentifierException if no dictionary has the name
   */
  Subexpression<?> lookUp(final String name, final ParserRuleContext reference,
      final ExpressionContext key) {
    final Dictionary dictionary = this.translation.stack().getDictionary(name)
        .orElseThrow(() -> InvalidIdentifierException.undeclaredIdentifier(
            this.translation.stack().peekParserContext(), Dictionary.describe(name)));
    this.translation.efxContext().readsInCurrentContext().countReadOfRoot();
    return new Subexpression<>(
        this.resolved(ExpressionConstruct.lookup(reference.getText(), dictionary),
            List.of(this.visit(key)), position -> key).compose(this.translation.script()),
        new DictionaryOrigin(this, reference, dictionary.name));
  }

  /**
   * @param fieldId a field
   * @return the path of the field from the current context, whose read is counted
   */
  private PathExpression relativePathOfField(final String fieldId) {
    this.countReadOfCurrentContext();
    return this.translation.symbols().getRelativePathOfField(fieldId,
        this.translation.efxContext().symbol());
  }

  /**
   * @param nodeId a node
   * @return the path of the node from the current context, whose read is counted
   */
  private PathExpression relativePathOfNode(final String nodeId) {
    this.countReadOfCurrentContext();
    return this.translation.symbols().getRelativePathOfNode(nodeId,
        this.translation.efxContext().symbol());
  }

  /**
   * @return the absolute path of the current context, whose read is counted
   */
  private PathExpression absolutePathOfCurrentContext() {
    this.countReadOfCurrentContext();
    return this.translation.efxContext().peek().absolutePath();
  }

  /**
   * @param fieldId a field
   * @return the path of the field from the root, whose read is counted
   */
  private PathExpression absolutePathOfField(final String fieldId) {
    this.translation.efxContext().readsInCurrentContext().countReadOfRoot();
    return this.translation.symbols().getRelativePathOfField(fieldId, NO_CONTEXT);
  }

  /**
   * @param nodeId a node
   * @return the path of the node from the root, whose read is counted
   */
  private PathExpression absolutePathOfNode(final String nodeId) {
    this.translation.efxContext().readsInCurrentContext().countReadOfRoot();
    return this.translation.symbols().getRelativePathOfNode(nodeId, NO_CONTEXT);
  }

  /**
   * Counts a read of the current context: a reference is resolved from it.
   *
   * <p>A reference is resolved only through {@link #relativePathOfField},
   * {@link #relativePathOfNode}, {@link #absolutePathOfCurrentContext}, {@link #absolutePathOfField}
   * and {@link #absolutePathOfNode}, and a dictionary is only looked up by {@link #lookUp}, which
   * count the read in the current context, so that an index can tell whether its position reads
   * what the target language would read differently for each element: see {@link Reads}.</p>
   */
  private void countReadOfCurrentContext() {
    this.translation.efxContext().readsInCurrentContext().countReadOfContext();
  }

  /**
   * Gives the sequence an index selects from. A path is put in parentheses, so that the index
   * selects from the whole sequence it selects, rather than among the children of each parent,
   * which a predicate on its last step would select from. The path keeps its type, which a type
   * error names: {@code BT-00-Text[at TRUE]} names {@code 'text'}. It takes the toolkit's own class
   * for that type, as the element the index selects does: see
   * {@link ExpressionConstruct#pathClassOf}.
   *
   * @param sequence the field, the node or the value the index is written after
   * @return the path in parentheses, or the value as it is
   */
  private TypedExpression wholeSequence(final TypedExpression sequence) {
    return sequence instanceof PathExpression
        ? ExpressionConstruct.inParentheses(this.translation.script(), sequence,
            ExpressionConstruct.pathClassOf((PathExpression) sequence))
        : sequence;
  }

  // #endregion Brackets ------------------------------------------------------

  // #region Operators --------------------------------------------------------

  @Override
  public Subexpression<?> visitMultiplicativeExpression(final MultiplicativeExpressionContext ctx) {
    return this.operation(ctx, this.operatorNamed(ctx.operator));
  }

  @Override
  public Subexpression<?> visitAdditiveExpression(final AdditiveExpressionContext ctx) {
    return this.operation(ctx, this.operatorNamed(ctx.operator));
  }

  @Override
  public Subexpression<?> visitComparisonExpression(final ComparisonExpressionContext ctx) {
    return this.operation(ctx, this.operatorNamed(ctx.operator));
  }

  @Override
  public Subexpression<?> visitMembershipCondition(final MembershipConditionContext ctx) {
    return this.operation(ctx,
        ctx.modifier == null ? ConditionOperators.IN : ConditionOperators.NOT_IN);
  }

  @Override
  public Subexpression<?> visitUniqueValueCondition(final UniqueValueConditionContext ctx) {
    return this.operation(ctx, ctx.modifier == null ? ConditionOperators.IS_UNIQUE_IN
        : ConditionOperators.IS_NOT_UNIQUE_IN);
  }

  @Override
  public Subexpression<?> visitEmptinessCondition(final EmptinessConditionContext ctx) {
    return this.condition(ctx,
        ctx.modifier == null ? ConditionOperators.IS_EMPTY : ConditionOperators.IS_NOT_EMPTY);
  }

  @Override
  public Subexpression<?> visitDuplicatesCondition(final DuplicatesConditionContext ctx) {
    return this.condition(ctx, ctx.modifier == null ? ConditionOperators.HAS_DUPLICATES
        : ConditionOperators.HAS_NO_DUPLICATES);
  }

  @Override
  public Subexpression<?> visitLikePatternCondition(final LikePatternConditionContext ctx) {
    final String pattern = ctx.pattern.getText();
    return this.condition(ctx,
        ctx.modifier == null ? PatternMatch.like(pattern) : PatternMatch.notLike(pattern));
  }

  /**
   * {@code is present} takes its operand as a path.
   */
  @Override
  public Subexpression<?> visitPresenceCondition(final PresenceConditionContext ctx) {
    final BuiltIn operator =
        ctx.modifier == null ? ConditionOperators.IS_PRESENT : ConditionOperators.IS_NOT_PRESENT;
    return this.pathCall(ctx, operator, operator.getSpelling(), this.subexpressionsOf(ctx));
  }

  @Override
  public Subexpression<?> visitLogicalAndCondition(final LogicalAndConditionContext ctx) {
    return this.operation(ctx, LogicalOperators.AND);
  }

  @Override
  public Subexpression<?> visitLogicalOrCondition(final LogicalOrConditionContext ctx) {
    return this.operation(ctx, LogicalOperators.OR);
  }

  /**
   * Finds the operator written as a token.
   *
   * @param operator the token of a comparison or arithmetic operator
   * @return the operator of the catalogue that has the token's symbol as its name
   */
  private BuiltIn operatorNamed(final Token operator) {
    return BuiltIns.INSTANCE.find(operator.getText()).orElseThrow(
        () -> TranslatorConfigurationException.unhandledOperator(operator.getText(),
            "operatorNamed"));
  }

  // #endregion Operators -----------------------------------------------------

  // #region Composites -------------------------------------------------------

  /**
   * Parentheses give the value of what they hold. It is no longer a reference, but keeps the error
   * of the reference. A node has no value, so it is rejected in parentheses.
   */
  @Override
  public Subexpression<?> visitParenthesizedExpression(final ParenthesizedExpressionContext ctx) {
    final Subexpression<?> inner = this.visit(ctx.expression());
    return new Subexpression<>(
        this.resolved(ExpressionConstruct.PARENTHESES, List.of(inner), position -> ctx)
            .compose(this.translation.script()),
        inner.origin().asValue());
  }

  /**
   * A conditional: the condition is a boolean, and the two branches have one primitive. Two single
   * values give a single value; a sequence on either side gives a sequence, the other branch
   * promoted to a sequence of one by the overload that takes them.
   */
  @Override
  public Subexpression<?> visitConditionalExpression(final ConditionalExpressionContext ctx) {
    return this.subexpression(this.resolved(ExpressionConstruct.CONDITIONAL,
        this.subexpressionsOf(ctx), position -> position == 1 ? ctx : ctx.expression(position))
        .compose(this.translation.script()));
  }

  /**
   * A list: its items are single values of the primitive of the first. The first item is reported
   * at the list when it gives {@code T} no primitive, and where it stands when it gives one but is
   * not a single value, as any other item is.
   */
  @Override
  public Subexpression<?> visitListExpression(final ListExpressionContext ctx) {
    final List<ExpressionContext> items = ctx.expression();
    final List<Subexpression<?>> parts = this.subexpressionsOf(ctx);
    final boolean bindsT = ParameterType.T.boundBy(parts.get(0).value()).isPresent();
    return this.subexpression(this.resolved(ExpressionConstruct.LIST, parts,
        position -> position == 0 && !bindsT ? ctx : items.get(position))
        .compose(this.translation.script()));
  }

  /**
   * {@code every} or {@code some}: the condition is read for each iteration, in the scope of the
   * variables of the iterators.
   */
  @Override
  public Subexpression<?> visitQuantifiedExpression(final QuantifiedExpressionContext ctx) {
    this.translation.stack().pushStackFrame();
    final IteratorListExpression iterators = this.iteratorsOf(ctx.iteratorList());
    final ExpressionConstruct quantifier = ctx.Every() != null
        ? ExpressionConstruct.every(ctx.Every().getText(), iterators)
        : ExpressionConstruct.some(ctx.Some().getText(), iterators);
    final TypedExpression quantified = this.resolved(quantifier,
        List.of(this.visit(ctx.expression())), position -> ctx.expression())
        .compose(this.translation.script());
    this.translation.stack().popStackFrame();
    return this.subexpression(quantified);
  }

  /**
   * An iteration: the body is read for each iteration, in the scope of the variables of the
   * iterators. It gives a sequence for each iteration, a single value being a sequence of one, and
   * the iteration gives their concatenation; with {@code distinct}, the distinct values of it, as
   * the built-in {@code distinct-values} gives them.
   */
  @Override
  public Subexpression<?> visitIterationExpression(final IterationExpressionContext ctx) {
    this.translation.stack().pushStackFrame();
    final IteratorListExpression iterators = this.iteratorsOf(ctx.iteratorList());
    final Subexpression<?> iteration = this.subexpression(
        this.resolved(ExpressionConstruct.iteration(iterators),
            List.of(this.visit(ctx.expression())), position -> ctx.expression())
            .compose(this.translation.script()));
    this.translation.stack().popStackFrame();
    return ctx.Distinct() == null ? iteration
        : this.builtIn(ctx, SequenceFunctions.DISTINCT_VALUES,
            SequenceFunctions.DISTINCT_VALUES.getSpelling(), List.of(iteration), ReadAs.VALUES);
  }

  /**
   * Declares the variables of the iterators in the current stack frame, each before the source of
   * the next is translated. Each iterator is kept on the call stack while it is declared, which
   * names it in the errors its declaration gives.
   */
  private IteratorListExpression iteratorsOf(final IteratorListContext ctx) {
    final List<IteratorExpression> iterators = new ArrayList<>(ctx.iteratorExpression().size());
    for (final IteratorExpressionContext iterator : ctx.iteratorExpression()) {
      iterators.add(this.translate(iterator, () -> iterator.accept(this.iteratorDeclarations)));
    }
    return this.translation.script().composeIteratorList(iterators);
  }

  /**
   * Translates an iterator and declares its variable.
   */
  private final class IteratorDeclarations extends EfxBaseVisitor<IteratorExpression> {

    /**
     * An iterator over values: they are of the primitive declared for the variable.
     */
    @Override
    public IteratorExpression visitValueIterator(final ValueIteratorContext ctx) {
      final Class<? extends EfxDataType.Primitive> primitive = primitiveOf(ctx.primitiveType());
      final String name = ctx.variableName.getText();
      return this.iterator(name, ScalarExpression.fromEfxDataType.get(primitive),
          EfxExpressionVisitor.this.valueOf(ctx.expression(),
              Clause.of("Iterator '" + ctx.primitiveType().getText() + ":$" + name + "'",
                  parameter("sequence",
                      ParameterType.of(SequenceExpression.fromEfxDataType.get(primitive)))))
              .sequenceAt(0));
    }

    private <T extends ScalarExpression> IteratorExpression iterator(final String name,
        final Class<T> variableType, final SequenceExpression source) {
      final ScriptGenerator script = EfxExpressionVisitor.this.translation.script();
      final Variable variable = new Variable(name,
          script.composeVariableDeclaration(name, variableType), source,
          script.composeVariableReference(name, variableType));
      EfxExpressionVisitor.this.translation.stack().declareIdentifier(variable);
      return script.composeIteratorExpression(variable.declarationExpression, source);
    }

    /**
     * An iterator over the elements of a field or a node: its variable names the element as a
     * context.
     */
    @Override
    public IteratorExpression visitContextIterator(final ContextIteratorContext ctx) {
      final ScriptGenerator script = EfxExpressionVisitor.this.translation.script();
      final Context context = EfxExpressionVisitor.this
          .contextOf(ctx.getRuleContext(ParserRuleContext.class, 0));
      final PathExpression path = context.relativePath();
      final String name = ctx.variableName.getText();
      final Class<? extends TypedExpression> variableType = path.asScalar().getClass();
      final Variable variable = new Variable(name,
          script.composeVariableDeclaration(name, variableType), Expression.empty(variableType),
          script.composeVariableReference(name, variableType));
      EfxExpressionVisitor.this.declareContextVariable(variable, context);
      return script.composeIteratorExpression(variable.declarationExpression, path.asSequence());
    }
  }

  // #endregion Composites ----------------------------------------------------

  // #region Field properties -------------------------------------------------

  @Override
  public Subexpression<?> visitFieldPropertyExpression(final FieldPropertyExpressionContext ctx) {
    return this.visit(ctx.fieldProperty());
  }

  @Override
  public Subexpression<?> visitFieldWasWithheldProperty(final FieldWasWithheldPropertyContext ctx) {
    final String fieldId = this.fieldIdOf(ctx.fieldMention());
    return this.subexpression(
        this.composeWasWithheldCondition(fieldId, this.privacyCodeOf(ctx, fieldId)));
  }

  @Override
  public Subexpression<?> visitFieldIsWithheldProperty(final FieldIsWithheldPropertyContext ctx) {
    final String fieldId = this.fieldIdOf(ctx.fieldMention());
    return this.subexpression(this.translation.script().composeLogicalAnd(
        this.composeWasWithheldCondition(fieldId, this.privacyCodeOf(ctx, fieldId)),
        this.composeStillWithheldCondition(fieldId)));
  }

  @Override
  public Subexpression<?> visitFieldIsWithholdableProperty(
      final FieldIsWithholdablePropertyContext ctx) {
    return this.subexpression(this.translation.script().getBooleanEquivalent(
        this.isWithholdable(this.fieldIdOf(ctx.fieldMention()))));
  }

  /**
   * A field is disclosed if it was withheld, is no longer withheld, and is not masked.
   */
  @Override
  public Subexpression<?> visitFieldIsDisclosedProperty(final FieldIsDisclosedPropertyContext ctx) {
    final String fieldId = this.fieldIdOf(ctx.fieldMention());
    return this.subexpression(this.translation.script().composeLogicalAnd(
        this.translation.script().composeLogicalAnd(
            this.composeWasWithheldCondition(fieldId, this.privacyCodeOf(ctx, fieldId)),
            this.translation.script().composeLogicalNot(
                this.composeStillWithheldCondition(fieldId))),
        this.translation.script().composeLogicalNot(this.composeIsMaskedCondition(fieldId))));
  }

  /**
   * A field is masked if it was withheld and its value is the privacy mask.
   */
  @Override
  public Subexpression<?> visitFieldIsMaskedProperty(final FieldIsMaskedPropertyContext ctx) {
    final String fieldId = this.fieldIdOf(ctx.fieldMention());
    return this.subexpression(this.translation.script().composeLogicalAnd(
        this.composeWasWithheldCondition(fieldId, this.privacyCodeOf(ctx, fieldId)),
        this.composeIsMaskedCondition(fieldId)));
  }

  @Override
  public Subexpression<?> visitFieldPrivacyCodeProperty(final FieldPrivacyCodePropertyContext ctx) {
    final String fieldId = this.fieldIdOf(ctx.fieldMention());
    return this.subexpression(this.translation.script().getStringLiteralFromUnquotedString(
        this.privacyCodeOf(ctx, fieldId)));
  }

  /**
   * @throws InvalidUsageException if the field cannot be withheld
   */
  private String privacyCodeOf(final ParserRuleContext property, final String fieldId) {
    this.requireWithholdable(property, fieldId);
    return this.translation.symbols().getPrivacyCodeOfField(fieldId);
  }

  /**
   * Rejects a field that cannot be withheld where only such a field has what is asked of it: its
   * privacy code, or a field linked to its privacy settings.
   *
   * @param reference where the error is reported
   * @param fieldId the field
   * @throws InvalidUsageException if the field cannot be withheld
   */
  private void requireWithholdable(final ParserRuleContext reference, final String fieldId) {
    if (!this.isWithholdable(fieldId)) {
      throw InvalidUsageException.fieldNotWithholdable(reference, fieldId);
    }
  }

  /**
   * @param fieldId a field
   * @return whether the field can be withheld from publication: whether it has a privacy code
   */
  private boolean isWithholdable(final String fieldId) {
    final String privacyCode = this.translation.symbols().getPrivacyCodeOfField(fieldId);
    return privacyCode != null && !privacyCode.isEmpty();
  }

  private BooleanExpression composeWasWithheldCondition(final String fieldId,
      final String privacyCode) {
    final String privacyCodeFieldId =
        this.translation.symbols().getPrivacySettingOfField(fieldId,
            PrivacySetting.PRIVACY_CODE_FIELD);
    if (privacyCodeFieldId == null) {
      throw SdkInconsistencyException.missingPrivacyCodeField(fieldId);
    }
    return this.translation.script().composeComparisonOperation(
        Expression.from(this.translation.script().composeFieldValueReference(
            this.relativePathOfField(privacyCodeFieldId)), StringExpression.class),
        "==", this.translation.script().getStringLiteralFromUnquotedString(privacyCode));
  }

  private BooleanExpression composeStillWithheldCondition(final String fieldId) {
    final String publicationDateFieldId =
        this.translation.symbols().getPrivacySettingOfField(fieldId,
            PrivacySetting.PUBLICATION_DATE_FIELD);
    if (publicationDateFieldId == null) {
      throw SdkInconsistencyException.missingPublicationDateField(fieldId);
    }
    final PathExpression publicationDate = this.relativePathOfField(publicationDateFieldId);
    return this.translation.script().composeParenthesizedExpression(
        this.translation.script().composeLogicalOr(
            this.translation.script().composeLogicalNot(
                this.translation.script().composeExistsCondition(publicationDate)),
            this.translation.script().composeComparisonOperation(
                Expression.from(this.translation.script()
                    .composeFieldValueReference(publicationDate), DateExpression.class),
                ">", this.translation.script().getCurrentDate())),
        BooleanExpression.class);
  }

  private BooleanExpression composeIsMaskedCondition(final String fieldId) {
    final PathExpression fieldValue =
        this.translation.script().composeFieldValueReference(this.relativePathOfField(fieldId));
    return this.translation.script().composeComparisonOperation(
        TypedExpression.from(fieldValue, ScalarExpression.class), "==",
        this.literalOf(this.translation.symbols().getPrivacyMask(fieldId),
            fieldValue.getDataType()));
  }

  private ScalarExpression literalOf(final String value,
      final Class<? extends EfxDataType> type) {
    if (EfxDataType.Number.class.isAssignableFrom(type)) {
      return this.translation.script().getNumericLiteralEquivalent(value);
    }
    if (EfxDataType.Date.class.isAssignableFrom(type)) {
      return this.translation.script().getDateLiteralEquivalent(value);
    }
    if (EfxDataType.Time.class.isAssignableFrom(type)) {
      return this.translation.script().getTimeLiteralEquivalent(value);
    }
    if (EfxDataType.Duration.class.isAssignableFrom(type)) {
      return this.translation.script().getDurationLiteralEquivalent(value);
    }
    if (EfxDataType.Boolean.class.isAssignableFrom(type)) {
      return this.translation.script().getBooleanEquivalent(Boolean.parseBoolean(value));
    }
    return this.translation.script().getStringLiteralFromUnquotedString(value);
  }

  // #endregion Field properties ----------------------------------------------

  // #region Function invocation ----------------------------------------------

  /**
   * An invocation of a declared function, typed by the declaration: a value of the type it
   * declares.
   */
  @Override
  public Subexpression<?> visitFunctionInvocation(final FunctionInvocationContext ctx) {
    final Function function = this.translation.stack().getFunction(ctx.functionName.getText());
    return new Subexpression<>(
        this.invocationOf(ctx, function).compose(this.translation.script()),
        new IdentifierOrigin<>(this, ctx, function.describe()));
  }

  /**
   * Resolves an invocation of a declared function: its arguments against the parameters it
   * declares, as the arguments of a call of a built-in are resolved against its overloads.
   *
   * @param invocation the invocation
   * @param function the function invoked
   * @return the resolution, which composes the invocation
   * @throws InvalidArgumentException if the number of arguments or the type of one does not match
   * @throws TypeMismatchException if an argument is a context variable that holds a node
   */
  Resolution invocationOf(final FunctionInvocationContext invocation, final Function function) {
    final List<Subexpression<?>> subexpressions = this.argumentsOf(invocation.argumentList());
    final List<TypedExpression> arguments =
        subexpressions.stream().map(Subexpression::value).collect(Collectors.toList());
    return function.resolve(arguments).orElseThrow(
        () -> this.invocationError(invocation, function, subexpressions, arguments));
  }

  /**
   * Translates the arguments of an invocation of a declared template, checked against the
   * parameters it declares. The check pairs them with the template's parameters as parameters of
   * any kind; they are paired again here with the declared parameters themselves, which the
   * template model reads, rather than cast.
   *
   * @param invocation the invocation
   * @param template the template invoked
   * @param list the arguments, or {@code null} if there are none
   * @return the arguments, each converted to the class of its parameter
   * @throws InvalidArgumentException if the number of arguments or the type of one does not match
   * @throws TypeMismatchException if an argument is a context variable that holds a node
   */
  Arguments<ParsedParameter> argumentsOf(final ParserRuleContext invocation,
      final Invocable template, final ArgumentListContext list) {
    final List<Subexpression<?>> subexpressions = this.argumentsOf(list);
    final List<TypedExpression> arguments =
        subexpressions.stream().map(Subexpression::value).collect(Collectors.toList());
    return new Arguments<>(template.parameters, template.match(arguments)
        .orElseThrow(() -> this.invocationError(invocation, template, subexpressions, arguments))
        .getArgumentValues());
  }

  /**
   * @param list the arguments of an invocation, or {@code null} if there are none
   * @return the arguments, translated in order
   */
  private List<Subexpression<?>> argumentsOf(final ArgumentListContext list) {
    final List<Subexpression<?>> subexpressions = new ArrayList<>();
    if (list != null) {
      for (final ArgumentContext argument : list.argument()) {
        subexpressions.add(this.visit(argument.expression()));
      }
    }
    return subexpressions;
  }

  /**
   * Gives the error of an invocation of a declared function or template whose arguments the
   * declared parameters do not take. The parameters reject an argument that does not fit them, a
   * node included.
   *
   * <p>An argument that does not fit is reported as such, even a sequence where a single value is
   * required. Only a context variable that holds a node is reported by itself, as it is wherever a
   * value is read from it.</p>
   *
   * @param call the invocation
   * @param declaration the function or template invoked
   * @param subexpressions the arguments as written, in order
   * @param arguments their values, in the same order
   * @return the error: the number of arguments, a context variable that holds a node, or the type
   *         of an argument
   */
  private EfxCompilationException invocationError(final ParserRuleContext call,
      final Invocable declaration, final List<Subexpression<?>> subexpressions,
      final List<TypedExpression> arguments) {
    if (!declaration.takes(arguments.size())) {
      return InvalidArgumentException.argumentNumberMismatch(call, declaration,
          arguments.size());
    }
    final Mismatch mismatch = declaration.mismatchOf(arguments);
    final Diagnosis diagnosis = mismatch.diagnose();
    final Optional<TypeMismatchException> noValue = subexpressions
        .get(mismatch.getRejection().getPosition()).errorOf(Rejection.Kind.PATH, diagnosis);
    if (noValue.isPresent()) {
      return noValue.get();
    }
    return TypeMismatchException.cannotConvert(call, diagnosis);
  }

  // #endregion Function invocation -------------------------------------------

  // #region Built-in functions -----------------------------------------------

  @Override
  public Subexpression<?> visitBuiltInFunctionExpression(
      final BuiltInFunctionExpressionContext ctx) {
    return this.visit(ctx.builtInFunction());
  }

  @Override
  public Subexpression<?> visitBuiltInFunction(final BuiltInFunctionContext ctx) {
    return this.visitOnlyChild(ctx);
  }

  // Boolean functions

  @Override
  public Subexpression<?> visitNotFunction(final NotFunctionContext ctx) {
    return this.call(ctx, LogicalOperators.NOT);
  }

  @Override
  public Subexpression<?> visitContainsFunction(final ContainsFunctionContext ctx) {
    return this.call(ctx, StringFunctions.CONTAINS);
  }

  @Override
  public Subexpression<?> visitStartsWithFunction(final StartsWithFunctionContext ctx) {
    return this.call(ctx, StringFunctions.STARTS_WITH);
  }

  @Override
  public Subexpression<?> visitEndsWithFunction(final EndsWithFunctionContext ctx) {
    return this.call(ctx, StringFunctions.ENDS_WITH);
  }

  @Override
  public Subexpression<?> visitSequenceEqualFunction(final SequenceEqualFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.SEQUENCE_EQUAL);
  }

  @Override
  public Subexpression<?> visitIndicatorConversionFunction(
      final IndicatorConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_INDICATOR);
  }

  @Override
  public Subexpression<?> visitEmptyFunction(final EmptyFunctionContext ctx) {
    return this.call(ctx, StringFunctions.EMPTY);
  }

  // Numeric functions

  @Override
  public Subexpression<?> visitCountFunction(final CountFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.COUNT);
  }

  @Override
  public Subexpression<?> visitCountDuplicatesFunction(final CountDuplicatesFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.COUNT_DUPLICATES);
  }

  @Override
  public Subexpression<?> visitNumberConversionFunction(final NumberConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_NUMBER);
  }

  @Override
  public Subexpression<?> visitSumFunction(final SumFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.SUM);
  }

  @Override
  public Subexpression<?> visitMinFunction(final MinFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.MIN);
  }

  @Override
  public Subexpression<?> visitMaxFunction(final MaxFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.MAX);
  }

  @Override
  public Subexpression<?> visitAverageFunction(final AverageFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.AVERAGE);
  }

  @Override
  public Subexpression<?> visitStringLengthFunction(final StringLengthFunctionContext ctx) {
    return this.call(ctx, StringFunctions.STRING_LENGTH);
  }

  @Override
  public Subexpression<?> visitIndexOfFunction(final IndexOfFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.INDEX_OF);
  }

  @Override
  public Subexpression<?> visitIndexOfSubstringFunction(final IndexOfSubstringFunctionContext ctx) {
    return this.call(ctx, StringFunctions.INDEX_OF_SUBSTRING);
  }

  @Override
  public Subexpression<?> visitAbsoluteFunction(final AbsoluteFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.ABSOLUTE);
  }

  @Override
  public Subexpression<?> visitRoundFunction(final RoundFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.ROUND);
  }

  @Override
  public Subexpression<?> visitRoundDownFunction(final RoundDownFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.ROUND_DOWN);
  }

  @Override
  public Subexpression<?> visitRoundUpFunction(final RoundUpFunctionContext ctx) {
    return this.call(ctx, NumericFunctions.ROUND_UP);
  }

  @Override
  public Subexpression<?> visitYearFunction(final YearFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.YEAR);
  }

  @Override
  public Subexpression<?> visitMonthFunction(final MonthFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.MONTH);
  }

  @Override
  public Subexpression<?> visitDayFunction(final DayFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.DAY);
  }

  @Override
  public Subexpression<?> visitHoursFunction(final HoursFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.HOURS);
  }

  @Override
  public Subexpression<?> visitMinutesFunction(final MinutesFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.MINUTES);
  }

  @Override
  public Subexpression<?> visitSecondsFunction(final SecondsFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.SECONDS);
  }

  @Override
  public Subexpression<?> visitYearsFunction(final YearsFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.YEARS);
  }

  @Override
  public Subexpression<?> visitMonthsFunction(final MonthsFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.MONTHS);
  }

  @Override
  public Subexpression<?> visitDaysFunction(final DaysFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.DAYS);
  }

  // String functions

  @Override
  public Subexpression<?> visitSubstringFunction(final SubstringFunctionContext ctx) {
    return this.call(ctx, StringFunctions.SUBSTRING);
  }

  @Override
  public Subexpression<?> visitSubstringBeforeFunction(final SubstringBeforeFunctionContext ctx) {
    return this.call(ctx, StringFunctions.SUBSTRING_BEFORE);
  }

  @Override
  public Subexpression<?> visitSubstringAfterFunction(final SubstringAfterFunctionContext ctx) {
    return this.call(ctx, StringFunctions.SUBSTRING_AFTER);
  }

  @Override
  public Subexpression<?> visitTextConversionFunction(final TextConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_TEXT);
  }

  @Override
  public Subexpression<?> visitConcatFunction(final ConcatFunctionContext ctx) {
    return this.call(ctx, StringFunctions.CONCAT);
  }

  @Override
  public Subexpression<?> visitStringJoinFunction(final StringJoinFunctionContext ctx) {
    return this.call(ctx, StringFunctions.STRING_JOIN);
  }

  @Override
  public Subexpression<?> visitFormatNumberFunction(final FormatNumberFunctionContext ctx) {
    return this.call(ctx, TemplateFunctions.FORMAT_NUMBER);
  }

  @Override
  public Subexpression<?> visitFormatShortFunction(final FormatShortFunctionContext ctx) {
    return this.call(ctx, TemplateFunctions.FORMAT_SHORT);
  }

  @Override
  public Subexpression<?> visitFormatMediumFunction(final FormatMediumFunctionContext ctx) {
    return this.call(ctx, TemplateFunctions.FORMAT_MEDIUM);
  }

  @Override
  public Subexpression<?> visitFormatLongFunction(final FormatLongFunctionContext ctx) {
    return this.call(ctx, TemplateFunctions.FORMAT_LONG);
  }

  @Override
  public Subexpression<?> visitUpperCaseFunction(final UpperCaseFunctionContext ctx) {
    return this.call(ctx, StringFunctions.UPPER_CASE);
  }

  @Override
  public Subexpression<?> visitLowerCaseFunction(final LowerCaseFunctionContext ctx) {
    return this.call(ctx, StringFunctions.LOWER_CASE);
  }

  @Override
  public Subexpression<?> visitNormalizeSpaceFunction(final NormalizeSpaceFunctionContext ctx) {
    return this.call(ctx, StringFunctions.NORMALIZE_SPACE);
  }

  @Override
  public Subexpression<?> visitTrimFunction(final TrimFunctionContext ctx) {
    return this.call(ctx, StringFunctions.TRIM);
  }

  @Override
  public Subexpression<?> visitTrimLeftFunction(final TrimLeftFunctionContext ctx) {
    return this.call(ctx, StringFunctions.TRIM_LEFT);
  }

  @Override
  public Subexpression<?> visitTrimRightFunction(final TrimRightFunctionContext ctx) {
    return this.call(ctx, StringFunctions.TRIM_RIGHT);
  }

  @Override
  public Subexpression<?> visitPadLeftFunction(final PadLeftFunctionContext ctx) {
    return this.call(ctx, StringFunctions.PAD_LEFT);
  }

  @Override
  public Subexpression<?> visitPadRightFunction(final PadRightFunctionContext ctx) {
    return this.call(ctx, StringFunctions.PAD_RIGHT);
  }

  @Override
  public Subexpression<?> visitReplaceFunction(final ReplaceFunctionContext ctx) {
    return this.call(ctx, StringFunctions.REPLACE);
  }

  /**
   * A pattern written as a literal is checked to be a regular expression that every target
   * supports.
   */
  @Override
  public Subexpression<?> visitReplaceRegexFunction(final ReplaceRegexFunctionContext ctx) {
    if (ctx.pattern instanceof LiteralExpressionContext
        && ((LiteralExpressionContext) ctx.pattern).literal().stringLiteral() != null) {
      EfxRegexValidator.validate(ctx.pattern.getText());
    }
    return this.call(ctx, StringFunctions.REPLACE_REGEX);
  }

  @Override
  public Subexpression<?> visitRepeatFunction(final RepeatFunctionContext ctx) {
    return this.call(ctx, StringFunctions.REPEAT);
  }

  @Override
  public Subexpression<?> visitUrlEncodeFunction(final UrlEncodeFunctionContext ctx) {
    return this.call(ctx, StringFunctions.URL_ENCODE);
  }

  @Override
  public Subexpression<?> visitCapitalizeFirstFunction(final CapitalizeFirstFunctionContext ctx) {
    return this.call(ctx, StringFunctions.CAPITALIZE_FIRST);
  }

  @Override
  public Subexpression<?> visitPreferredLanguageFunction(
      final PreferredLanguageFunctionContext ctx) {
    return this.preferredLanguage(ctx, ctx.expression(), TemplateFunctions.PREFERRED_LANGUAGE,
        Spelling.function(ctx.PreferredLanguageFunction().getText()));
  }

  @Override
  public Subexpression<?> visitPreferredLanguageTextFunction(
      final PreferredLanguageTextFunctionContext ctx) {
    return this.preferredLanguage(ctx, ctx.expression(), TemplateFunctions.PREFERRED_LANGUAGE_TEXT,
        Spelling.function(ctx.PreferredLanguageTextFunction().getText()));
  }

  // Date, time and duration functions

  @Override
  public Subexpression<?> visitDateConversionFunction(final DateConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_DATE);
  }

  @Override
  public Subexpression<?> visitAddDurationFunction(final AddDurationFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.ADD_DURATION);
  }

  @Override
  public Subexpression<?> visitSubtractDurationFunction(final SubtractDurationFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.SUBTRACT_DURATION);
  }

  @Override
  public Subexpression<?> visitCurrentDateFunction(final CurrentDateFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.CURRENT_DATE);
  }

  @Override
  public Subexpression<?> visitTimeConversionFunction(final TimeConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_TIME);
  }

  @Override
  public Subexpression<?> visitCurrentTimeFunction(final CurrentTimeFunctionContext ctx) {
    return this.call(ctx, DateTimeFunctions.CURRENT_TIME);
  }

  @Override
  public Subexpression<?> visitDayTimeDurationConversionFunction(
      final DayTimeDurationConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_DAY_TIME_DURATION);
  }

  @Override
  public Subexpression<?> visitYearMonthDurationConversionFunction(
      final YearMonthDurationConversionFunctionContext ctx) {
    return this.call(ctx, ConversionFunctions.TO_YEAR_MONTH_DURATION);
  }

  // Sequence functions

  @Override
  public Subexpression<?> visitDistinctValuesFunction(final DistinctValuesFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.DISTINCT_VALUES);
  }

  @Override
  public Subexpression<?> visitGetDuplicatesFunction(final GetDuplicatesFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.GET_DUPLICATES);
  }

  @Override
  public Subexpression<?> visitValueUnionFunction(final ValueUnionFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.VALUE_UNION);
  }

  @Override
  public Subexpression<?> visitValueIntersectFunction(final ValueIntersectFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.VALUE_INTERSECT);
  }

  @Override
  public Subexpression<?> visitValueExceptFunction(final ValueExceptFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.VALUE_EXCEPT);
  }

  @Override
  public Subexpression<?> visitSortFunction(final SortFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.SORT);
  }

  @Override
  public Subexpression<?> visitReverseFunction(final ReverseFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.REVERSE);
  }

  @Override
  public Subexpression<?> visitSubsequenceFunction(final SubsequenceFunctionContext ctx) {
    return this.call(ctx, SequenceFunctions.SUBSEQUENCE);
  }

  @Override
  public Subexpression<?> visitSplitFunction(final SplitFunctionContext ctx) {
    return this.call(ctx, StringFunctions.SPLIT);
  }

  // #endregion Built-in functions --------------------------------------------

  // #region Built-ins and sub-expressions ------------------------------------

  /**
   * Translates a call of a built-in function with {@link #builtIn}: its sub-expressions are the
   * expressions of its grammar label, in order, read as values, and an argument of the wrong
   * primitive cannot be converted. A type error names the function as the call spells it, which
   * may be another name of the built-in, as {@code string} is of {@code text}.
   *
   * @param call the call, which starts with the name of the function
   * @param function the built-in called
   * @return the result of the call
   */
  private Subexpression<?> call(final ParserRuleContext call, final BuiltIn function) {
    return this.builtIn(call, function, Spelling.function(call.getStart().getText()),
        this.subexpressionsOf(call), ReadAs.VALUES);
  }

  /**
   * Translates a condition written as an operator after its one operand, such as {@code like} or
   * {@code is empty}, with {@link #builtIn}: its operand is read as a value, and an operand of the
   * wrong primitive cannot be converted.
   *
   * @param condition the condition
   * @param operator the operator
   * @return the result of the condition
   */
  private Subexpression<?> condition(final ParserRuleContext condition, final BuiltIn operator) {
    return this.builtIn(condition, operator, operator.getSpelling(),
        this.subexpressionsOf(condition), ReadAs.VALUES);
  }

  /**
   * Translates a call of a built-in that reads its sub-expressions as paths, with {@link #builtIn}:
   * an argument of the wrong primitive cannot be converted.
   *
   * @param call the call
   * @param builtIn the built-in called
   * @param spelling the built-in as the call spells it
   * @param subexpressions the sub-expressions of the call
   * @return the result of the call
   */
  private Subexpression<?> pathCall(final ParserRuleContext call, final BuiltIn builtIn,
      final Spelling spelling, final List<Subexpression<?>> subexpressions) {
    return this.builtIn(call, builtIn, spelling, subexpressions, ReadAs.PATHS);
  }

  /**
   * Translates an infix operator with {@link #builtIn}: operands of the wrong primitive are
   * incompatible operands of the operator.
   *
   * @param operation the operation, whose two expressions are its operands
   * @param operator the operator
   * @return the result of the operation
   */
  private Subexpression<?> operation(final ParserRuleContext operation, final BuiltIn operator) {
    return this.builtIn(operation, operator, operator.getSpelling(),
        this.subexpressionsOf(operation), ReadAs.OPERANDS);
  }

  private List<Subexpression<?>> subexpressionsOf(final ParserRuleContext call) {
    final List<ExpressionContext> expressions = call.getRuleContexts(ExpressionContext.class);
    final List<Subexpression<?>> subexpressions = new ArrayList<>(expressions.size());
    for (final ExpressionContext expression : expressions) {
      subexpressions.add(this.visit(expression));
    }
    return subexpressions;
  }

  /**
   * Checks the arguments its sub-expressions give a built-in, and translates the call with the
   * built-in. An error is reported at the call, and names the built-in as the call spells it.
   *
   * @param call the call
   * @param builtIn the built-in called
   * @param spelling the built-in as the call spells it
   * @param subexpressions the sub-expressions of the call
   * @param readAs how the call reads its sub-expressions, and the error of a mismatch that the
   *        origin of a sub-expression does not report
   * @return the result of the call
   */
  private Subexpression<?> builtIn(final ParserRuleContext call, final BuiltIn builtIn,
      final Spelling spelling, final List<Subexpression<?>> subexpressions,
      final ReadAs readAs) {
    final List<TypedExpression> arguments =
        subexpressions.stream().map(readAs::of).collect(Collectors.toList());
    if (builtIn.isTemplateOnly() && !this.translatesTemplateOnlyBuiltIns()) {
      throw InvalidUsageException.templateOnlyFunction(call, spelling.describe());
    }
    final Optional<Resolution> resolution = builtIn.resolve(arguments);
    if (resolution.isEmpty()) {
      final Mismatch mismatch = builtIn.mismatchOf(arguments);
      throw this.errorOf(mismatch, mismatch.diagnose(spelling), subexpressions,
          position -> call, readAs::errorAt);
    }
    return this.subexpression(resolution.get().compose(this.translation.script()));
  }

  /**
   * How a call of a built-in reads its sub-expressions, and the error of a mismatch that the origin
   * of a sub-expression does not report.
   */
  private enum ReadAs {

    /** As values: an argument of the wrong primitive cannot be converted. */
    VALUES,

    /** As paths: an argument of the wrong primitive cannot be converted. */
    PATHS {
      @Override
      TypedExpression of(final Subexpression<?> subexpression) {
        return subexpression.path();
      }
    },

    /**
     * As the operands of an infix operator, which are values: operands of the wrong primitive are
     * incompatible operands of the operator.
     */
    OPERANDS {
      @Override
      TypeMismatchException errorAt(final ParserRuleContext call, final Diagnosis diagnosis) {
        return TypeMismatchException.incompatibleOperands(call, diagnosis);
      }
    };

    /**
     * @param subexpression a sub-expression of the call
     * @return what the call reads from it
     */
    TypedExpression of(final Subexpression<?> subexpression) {
      return subexpression.value();
    }

    /**
     * @param call the call
     * @param diagnosis what the built-in was given, and what the argument at fault must be
     * @return the error of the mismatch
     */
    TypeMismatchException errorAt(final ParserRuleContext call, final Diagnosis diagnosis) {
      return TypeMismatchException.cannotConvert(call, diagnosis);
    }
  }

  /**
   * Checks the expression of a clause, read as a value: a value that does not fit cannot be
   * converted, and is reported where the clause places the error.
   *
   * @param clause the clause
   * @param parts its expression, the one part
   * @param where where the error of the part is reported
   * @return the value of the part, converted to the parameter of the clause
   * @throws TypeMismatchException if the part does not fit
   */
  private Arguments<?> checked(final Clause clause, final List<Subexpression<?>> parts,
      final IntFunction<ParserRuleContext> where) {
    final List<TypedExpression> arguments =
        parts.stream().map(Subexpression::value).collect(Collectors.toList());
    return clause.match(arguments).orElseThrow(() -> this.errorOf(clause.mismatchOf(arguments),
        parts, where, TypeMismatchException::cannotConvert));
  }

  /**
   * Resolves the parts of an expression construct, read as values, with {@link #resolved}: a part
   * that does not fit cannot be converted, and is reported where the construct places the error
   * of its position. The part that binds {@code T}, such as the then branch of a conditional or
   * the first item of a list, is reported at the construct when it gives {@code T} no primitive,
   * as the argument that binds it is reported at the call of a built-in; any other part where it
   * stands.
   *
   * @param construct the construct
   * @param parts its parts, in order
   * @param where where the error of the part at a position is reported
   * @return the resolution, which composes the construct
   * @throws TypeMismatchException if a part does not fit
   */
  private Resolution resolved(final ExpressionConstruct construct,
      final List<Subexpression<?>> parts, final IntFunction<ParserRuleContext> where) {
    return this.resolved(construct, parts,
        parts.stream().map(Subexpression::value).collect(Collectors.toList()), where,
        TypeMismatchException::cannotConvert);
  }

  /**
   * Resolves the arguments of an expression construct: the overload that takes them, chosen as
   * the overload of a built-in is.
   *
   * @param construct the construct
   * @param subexpressions its sub-expressions, in order
   * @param arguments what it reads from each sub-expression, in the same order
   * @param where where the error of the argument at a position is reported
   * @param typeError the error of a mismatch that the origin of the sub-expression does not report
   * @return the resolution, which composes the construct
   * @throws TypeMismatchException if no overload takes the arguments
   */
  private Resolution resolved(final ExpressionConstruct construct,
      final List<Subexpression<?>> subexpressions, final List<TypedExpression> arguments,
      final IntFunction<ParserRuleContext> where,
      final BiFunction<ParserRuleContext, Diagnosis, TypeMismatchException> typeError) {
    return construct.resolve(arguments).orElseThrow(() -> this
        .errorOf(construct.mismatchOf(arguments), subexpressions, where, typeError));
  }

  /**
   * Gives the error of a mismatch, as the construct names itself and its arguments: see
   * {@link #errorOf(Mismatch, Diagnosis, List, IntFunction, BiFunction)}.
   */
  private TypeMismatchException errorOf(final Mismatch mismatch,
      final List<Subexpression<?>> subexpressions, final IntFunction<ParserRuleContext> where,
      final BiFunction<ParserRuleContext, Diagnosis, TypeMismatchException> typeError) {
    return this.errorOf(mismatch, mismatch.diagnose(), subexpressions, where, typeError);
  }

  /**
   * Gives the error of a mismatch. An argument is reported by the reference its sub-expression is
   * written as, where the reference explains the mistake, such as a field that may repeat where a
   * single value is required; any other mismatch gets the error the kind of construct gives, where
   * the construct places it.
   *
   * @param mismatch the mismatch
   * @param diagnosis what the error says
   * @param subexpressions the sub-expressions of the construct, in order
   * @param where where the error of the argument at a position is reported
   * @param typeError the error of a mismatch that the origin of the sub-expression does not report
   * @return the error
   */
  private TypeMismatchException errorOf(final Mismatch mismatch, final Diagnosis diagnosis,
      final List<Subexpression<?>> subexpressions, final IntFunction<ParserRuleContext> where,
      final BiFunction<ParserRuleContext, Diagnosis, TypeMismatchException> typeError) {
    final Rejection rejection = mismatch.getRejection();
    return subexpressions.get(rejection.getPosition()).errorOf(rejection.getKind(), diagnosis)
        .orElseGet(() -> typeError.apply(where.apply(rejection.getPosition()), diagnosis));
  }

  /**
   * Gives the error of an argument that a construct reads rather than resolves, where what the
   * argument is written as cannot be read so: the left side of {@code ::} that names no context,
   * or a value that a predicate follows, which has no elements. The error is the reference's
   * where it explains the mistake, and otherwise says that the argument does not convert to what
   * its parameter takes.
   *
   * @param construct the construct, which has one parameter list
   * @param subexpressions its sub-expressions, in order
   * @param arguments what it reads from each sub-expression, in the same order
   * @param position the position of the argument that cannot be read
   * @param where where the error is reported
   * @return the error
   */
  private TypeMismatchException unreadable(final Construct construct,
      final List<Subexpression<?>> subexpressions, final List<TypedExpression> arguments,
      final int position, final ParserRuleContext where) {
    return this.errorOf(Mismatch.unreadable(construct, arguments, position), subexpressions,
        at -> where, TypeMismatchException::cannotConvert);
  }

  /**
   * Tells whether the visitor translates the built-ins that only templates may call, such as the
   * formatting functions. An expression alone has no template, so it does not.
   *
   * @return {@code false}
   */
  protected boolean translatesTemplateOnlyBuiltIns() {
    return false;
  }

  /**
   * @param type the declared type of a variable, a parameter or a function
   * @return the class of its values
   */
  static Class<? extends TypedExpression> typeOf(final TypeDeclarationContext type) {
    final Class<? extends EfxDataType.Primitive> primitive = primitiveOf(type.primitiveType());
    return type.Star() == null
        ? ScalarExpression.fromEfxDataType.get(primitive)
        : SequenceExpression.fromEfxDataType.get(primitive);
  }

  private static Class<? extends EfxDataType.Primitive> primitiveOf(
      final PrimitiveTypeContext primitive) {
    return EfxTypeTokenLookup.toJavaType(primitive.getText());
  }

  // #endregion Built-ins and sub-expressions ---------------------------------
}
