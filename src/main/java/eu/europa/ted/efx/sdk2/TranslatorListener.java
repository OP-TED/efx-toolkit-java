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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.identifiers.ParsedParameter;
import eu.europa.ted.efx.model.identifiers.ParsedParameters;
import eu.europa.ted.efx.model.identifiers.Variable;
import eu.europa.ted.efx.sdk2.EfxParser.ContextVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.IncludeDirectiveContext;
import eu.europa.ted.efx.sdk2.EfxParser.ParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.StandardExpressionBlockContext;

/**
 * The listener that the translators of SDK 2 share. It listens to the rules of a file that are not
 * expressions, and asks an {@link EfxExpressionVisitor} for each expression, field context and
 * node context they hold.
 *
 * <p>Each translation walks its parse tree with a new {@link Translation} and a new
 * visitor, so that the declarations and contexts of one translation are not visible in the
 * next.</p>
 */
abstract class TranslatorListener extends EfxBaseListener {

  /** An expression block, as a type error names it. */
  static final Clause EXPRESSION_BLOCK = Clause.ofValueAsGiven("Expression block", "expression");

  /**
   * Listens to the syntax errors of the lexer and the parser, instead of their own listeners, if it
   * is not {@code null}.
   */
  final BaseErrorListener errorListener;

  /**
   * The state of the translation in progress: the symbols, the script generator, the call stack
   * and the contexts. Each translation starts with the {@link Translation#next next} one.
   */
  Translation translation;

  /**
   * Translates the expressions, field contexts and node contexts of the file.
   */
  EfxExpressionVisitor expressions;

  TranslatorListener(final SymbolResolver symbols, final ScriptGenerator script,
      final BaseErrorListener errorListener) {
    this.errorListener = errorListener;
    this.translation = new Translation(symbols, script);
  }

  /**
   * @param translation the state of a translation
   * @return the visitor that translates the expressions of the translation
   */
  abstract EfxExpressionVisitor visitorOf(Translation translation);

  /**
   * @param input the text to parse
   * @return a parser of the text, which reports its syntax errors, and those of its lexer, to the
   *         error listener
   */
  EfxParser parserOf(final CharStream input) {
    return this.parserOf(new EfxErrorReportingLexer(input));
  }

  /**
   * @param lexer the lexer of the text to parse, in the mode the text starts in
   * @return a parser of the text, which reports its syntax errors, and those of its lexer, to the
   *         error listener
   */
  EfxParser parserOf(final EfxLexer lexer) {
    final EfxParser parser = new EfxParser(new CommonTokenStream(lexer));
    parser.setErrorHandler(new EfxErrorStrategy());

    if (this.errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(this.errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(this.errorListener);
    }
    return parser;
  }

  /**
   * Translates a parse tree: walks it with a new translation state, without entering the
   * expressions, field contexts and node contexts, which the visitor translates.
   *
   * @param tree the parse tree of the file
   */
  void translate(final ParseTree tree) {
    this.translation = this.translation.next();
    this.expressions = this.visitorOf(this.translation);

    new ExpressionSkippingWalker().walk(this, tree);
  }

  @Override
  public void enterEveryRule(final ParserRuleContext ctx) {
    this.translation.stack().pushParserContext(ctx);
  }

  @Override
  public void exitEveryRule(final ParserRuleContext ctx) {
    this.translation.stack().popParserContext();
  }

  /**
   * Guard: throws if an {@code #include} directive survived into the parse tree, meaning the
   * {@link IncludeProcessor} did not resolve it.
   */
  @Override
  public void exitIncludeDirective(final IncludeDirectiveContext ctx) {
    final String path =
        ctx.IncludePath() != null ? ctx.IncludePath().getText().trim() : "<unknown>";
    throw TranslatorConfigurationException.unresolvedIncludeDirective(path);
  }

  /**
   * Leaves the value of the expression for the rule that holds the block: the single expression,
   * or the label block that takes it as the asset type, the label type, the asset id or the
   * pluraliser.
   */
  @Override
  public void exitStandardExpressionBlock(final StandardExpressionBlockContext ctx) {
    this.translation.stack().push(this.expressions.valueOf(ctx.expression(), EXPRESSION_BLOCK)
        .at(0, TypedExpression.class));
  }

  // #region Parameter Declarations -------------------------------------------

  /**
   * Declares a parameter of a template, a function or an API function, which its body reads by
   * reference.
   */
  @Override
  public void exitParameterDeclaration(final ParameterDeclarationContext ctx) {
    final String parameterName = ctx.parameterName.getText();
    final ParsedParameter parameter = new ParsedParameter(parameterName,
        this.translation.script().composeParameterReference(parameterName,
            EfxExpressionVisitor.typeOf(ctx.typeDeclaration())));
    this.translation.stack().declareIdentifier(parameter);
    this.translation.stack().peek(ParsedParameters.class).add(parameter);
  }

  // #endregion Parameter Declarations ----------------------------------------

  // #region Context Declarations ---------------------------------------------

  /**
   * Declares the context of a field or a node, with its predicates.
   *
   * @param declared the context, as the visitor translates it
   * @param contextVariable the variable the context is assigned to, or {@code null}
   */
  void exitContextDeclaration(final Context declared, final Variable contextVariable) {
    if (declared.isFieldContext()) {
      this.exitFieldContextDeclaration(declared.symbol(), declared.relativePath(), contextVariable);
    } else {
      this.exitNodeContextDeclaration(declared.symbol(), declared.relativePath(), contextVariable);
    }
  }

  void exitRootContextDeclaration() {
    this.exitNodeContextDeclaration(this.translation.symbols().getRootNodeId(),
        this.translation.symbols().getRootPath(), null);
  }

  void exitFieldContextDeclaration(final String fieldId, final PathExpression contextPath,
      final Variable contextVariable) {
    this.declareContext(new FieldContext(fieldId, contextPath, contextVariable), contextVariable);
  }

  void exitNodeContextDeclaration(final String nodeId, final PathExpression contextPath,
      final Variable contextVariable) {
    this.declareContext(new NodeContext(nodeId, contextPath, contextVariable), contextVariable);
  }

  /**
   * Switches to a context, and declares the variable it is assigned to, if any.
   *
   * @param context the context
   * @param contextVariable the variable the context is assigned to, or {@code null}
   */
  void declareContext(final Context context, final Variable contextVariable) {
    this.translation.efxContext().push(context);
    if (contextVariable != null) {
      this.expressions.declareContextVariable(contextVariable, context);
    }
  }

  /**
   * @param ctx the initializer of a context variable, or {@code null}
   * @param contextPath the path to the context the variable is assigned
   * @return the variable, which holds one element of the context, or {@code null} if there is no
   *         initializer
   */
  Variable getContextVariable(final ContextVariableInitializerContext ctx,
      final PathExpression contextPath) {
    if (ctx == null) {
      return null;
    }

    final String variableName = ctx.variableName.getText();
    final Class<? extends TypedExpression> variableType = contextPath.asScalar().getClass();

    return new Variable(variableName,
        this.translation.script().composeVariableDeclaration(variableName, variableType),
        this.translation.script().contextualizePath(contextPath, contextPath),
        this.translation.script().composeVariableReference(variableName, variableType));
  }

  // #endregion Context Declarations ------------------------------------------
}
