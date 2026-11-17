/*
 * Copyright 2022 European Union
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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.identifiers.ParsedParameter;
import eu.europa.ted.efx.sdk2.EfxParser.AttributeReferenceContext;
import eu.europa.ted.efx.sdk2.EfxParser.ParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.SelectionContext;
import eu.europa.ted.efx.sdk2.EfxParser.SingleExpressionContext;

/**
 * The goal of the EfxExpressionTranslator is to take an EFX expression and translate it to a
 * target scripting language.
 *
 * <p>The target language syntax is not hardcoded into the translator so that this class can be
 * reused to translate to several different languages. Instead a {@link ScriptGenerator} interface
 * is used to provide specifics on the syntax of the target scripting language.</p>
 *
 * <p>Apart from writing expressions that can be translated and evaluated in a target scripting
 * language (e.g. XPath/XQuery, JavaScript etc.), EFX also allows the definition of templates that
 * can be translated to a target template markup language (e.g. XSLT, Thymeleaf etc.). The
 * {@link EfxExpressionTranslatorV2} only focuses on EFX expressions. To translate EFX templates you
 * need to use the {@link EfxTemplateTranslatorV2}.</p>
 *
 * <p>This translator handles what a single expression declares around its expression: the context
 * it is evaluated in and its parameters. The expression itself, or the selection, is translated by
 * an {@link EfxExpressionVisitor}.</p>
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_EXPRESSION_TRANSLATOR)
public class EfxExpressionTranslatorV2 extends TranslatorListener
    implements EfxExpressionTranslator {

  /** The expression after {@code COMPUTE}, as a type error names it. */
  private static final Clause COMPUTE = Clause.ofValueAsGiven(Clause.describe("COMPUTE"),
      "expression");

  /**
   * Creates the visitor that translates the expression, for each translation.
   */
  private final Function<Translation, EfxExpressionVisitor> visitors;

  /**
   * The values of the declared parameters, in the order of their declarations, for the translation
   * in progress.
   */
  private Deque<String> expressionArguments;

  protected EfxExpressionTranslatorV2() {
    this(null, null, null);
  }

  public EfxExpressionTranslatorV2(final SymbolResolver symbolResolver,
      final ScriptGenerator scriptGenerator, final BaseErrorListener errorListener) {
    this(symbolResolver, scriptGenerator, errorListener, EfxExpressionVisitor::new);
  }

  /**
   * Creates a translator of single expressions whose expressions are translated by visitors of a
   * subclass of {@link EfxExpressionVisitor}, such as the template translator's.
   *
   * @param visitors creates the visitor that translates the expression, for each translation
   */
  EfxExpressionTranslatorV2(final SymbolResolver symbolResolver,
      final ScriptGenerator scriptGenerator, final BaseErrorListener errorListener,
      final Function<Translation, EfxExpressionVisitor> visitors) {
    super(symbolResolver, scriptGenerator, errorListener);
    this.visitors = visitors;
  }

  @Override
  EfxExpressionVisitor visitorOf(final Translation translation) {
    return this.visitors.apply(translation);
  }

  @Override
  public String translateExpression(final String expression, final String... arguments) {
    final SingleExpressionContext tree =
        this.parserOf(CharStreams.fromString(expression)).singleExpression();

    this.expressionArguments = new LinkedList<>(Arrays.asList(arguments));
    this.translate(tree);

    return this.getTranslatedScript();
  }

  /**
   * Translates the value passed for a parameter of the expression. The value is translated in a
   * translation of its own, which {@link Translation#next} gives, so that the declarations and
   * contexts of the expression are not visible in it. The value is a literal written on its own,
   * so its lexer starts in expression mode.
   *
   * @param parameterValue the value passed, as EFX
   * @param parameterType the expression class of the parameter
   * @return the translated value
   */
  private <T extends Expression> T translateArgument(final String parameterValue,
      final Class<T> parameterType) {
    final EfxLexer lexer = new EfxErrorReportingLexer(CharStreams.fromString(parameterValue));
    lexer.pushMode(EfxLexer.EXPRESSION);
    final EfxParser parser = this.parserOf(lexer);
    final EfxExpressionVisitor visitor =
        new EfxExpressionVisitor(this.translation.next());
    return Expression.instantiate(visitor.visit(parser.parameterValue()).value().getScript(),
        parameterType);
  }

  /**
   * Used to get the translated target language script, after the walker finished its walk.
   *
   * @return The translated code, trimmed
   */
  private String getTranslatedScript() {
    final StringBuilder sb = new StringBuilder(this.translation.stack().size() * 100);
    while (!this.translation.stack().empty()) {
      sb.insert(0, '\n').insert(0, this.translation.stack().pop(Expression.class).getScript());
    }
    return sb.toString().trim();
  }

  // #region Single expression ------------------------------------------------

  @Override
  public void enterSingleExpression(SingleExpressionContext ctx) {
    final TerminalNode fieldContext = ctx.FieldId();
    if (fieldContext != null) {
      this.translation.efxContext().pushFieldContext(fieldContext.getText());
    } else {
      final TerminalNode nodeContext = ctx.NodeId();
      if (nodeContext != null) {
        this.translation.efxContext().pushNodeContext(nodeContext.getText());
      } else {
        final TerminalNode alias = ctx.Identifier();
        if (alias != null) {
          String fieldId = this.translation.symbols().getFieldIdFromAlias(alias.getText());
          if (fieldId != null) {
            this.translation.efxContext().pushFieldContext(fieldId);
          } else {
            String nodeId = this.translation.symbols().getNodeIdFromAlias(alias.getText());
            if (nodeId != null) {
              this.translation.efxContext().pushNodeContext(nodeId);
            } else {
              throw SymbolResolutionException.unknownSymbol(alias.getText());
            }
          }
        }
      }
    }
  }

  /**
   * Translates the expression after {@code Compute}, once the parameters are declared, and then
   * leaves the context of the single expression.
   */
  @Override
  public void exitSingleExpression(SingleExpressionContext ctx) {
    if (ctx.expression() != null) {
      this.translation.stack().push(
          this.expressions.valueOf(ctx.expression(), COMPUTE).at(0, TypedExpression.class));
    }
    this.translation.efxContext().pop();
  }

  /**
   * A selector yields the reference itself rather than its value: the value step that every other
   * reference position applies is deliberately not composed here. The path is otherwise resolved
   * exactly as it would be in an expression, relative to the declared context unless the author
   * wrote it as an absolute reference.
   */
  @Override
  public void exitSelection(final SelectionContext ctx) {
    final AttributeReferenceContext attribute = ctx.attributeReference();
    if (attribute != null) {
      this.translation.stack().push(this.translation.script().composeFieldAttributeReference(
          this.pathOf(attribute.fieldContext()), attribute.attributeName.getText(),
          StringPath.class));
    } else {
      this.translation.stack().push(this.pathOf(ctx.getRuleContext(ParserRuleContext.class, 0)));
    }
  }

  /**
   * @param reference a field or a node, with its predicates
   * @return the path to the elements it selects
   */
  private PathExpression pathOf(final ParserRuleContext reference) {
    return this.expressions.contextOf(reference).relativePath();
  }

  // #endregion Single expression ---------------------------------------------

  // #region Parameter Declarations -------------------------------------------

  /**
   * Declares a parameter of the single expression, whose value is the next argument passed to
   * {@link #translateExpression}.
   */
  @Override
  public void exitParameterDeclaration(final ParameterDeclarationContext ctx) {
    final String parameterName = ctx.parameterName.getText();
    if (this.expressionArguments.isEmpty()) {
      throw InvalidArgumentException.missingArgument(ctx, ParsedParameter.describe(parameterName));
    }

    final ParsedParameter parameter = new ParsedParameter(parameterName,
        this.translateArgument(this.expressionArguments.pop(),
            EfxExpressionVisitor.typeOf(ctx.typeDeclaration())));
    this.translation.stack().declareIdentifier(parameter);
  }

  // #endregion Parameter Declarations ----------------------------------------
}
