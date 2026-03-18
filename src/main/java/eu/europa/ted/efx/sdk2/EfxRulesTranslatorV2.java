/*
 * Copyright 2025 European Union
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

import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.efx.interfaces.EfxRulesTranslator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DynamicExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.rules.AssertRule;
import eu.europa.ted.efx.model.rules.ValidationPlan;
import eu.europa.ted.efx.model.rules.NoticeSubtypeRange;
import eu.europa.ted.efx.model.rules.ReportRule;
import eu.europa.ted.efx.model.rules.RuleSet;
import eu.europa.ted.efx.model.rules.RuleScope;
import eu.europa.ted.efx.model.rules.RuleSeverity;
import eu.europa.ted.efx.model.rules.RuleStack;
import eu.europa.ted.efx.model.rules.ValidationRule;
import eu.europa.ted.efx.model.rules.ValidationStage;
import eu.europa.ted.efx.model.variables.DynamicFunction;
import eu.europa.ted.efx.model.variables.DynamicVariable;
import eu.europa.ted.efx.model.variables.ParsedParameter;
import eu.europa.ted.efx.model.variables.ParsedArguments;
import eu.europa.ted.efx.model.variables.ParsedParameters;
import eu.europa.ted.efx.model.variables.Variable;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * EFX Rules translator for SDK version 2.
 *
 * This translator parses EFX Rules files and produces an intermediate model
 * (List of ValidationStage) which is then passed to a ValidatorGenerator
 * for output generation.
 *
 * It extends EfxExpressionTranslatorV2 to reuse XPath expression generation and
 * implements EfxRulesTranslator to provide the rules translation API.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_RULES_TRANSLATOR)
public class EfxRulesTranslatorV2 extends EfxExpressionTranslatorV2
    implements EfxRulesTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxRulesTranslatorV2.class);

  /**
   * The ValidatorGenerator is used to generate validation output from the intermediate model.
   */
  private final ValidatorGenerator validatorGenerator;

  /**
   * List of validation stages collected during parsing.
   * This is the intermediate model passed to the validator generator.
   */
  private ValidationPlan validationPlan = new ValidationPlan();

  private List<String> cachedSortedNoticeSubtypeIds;

  private final RuleStack ruleStack = new RuleStack();

  /**
   * Tracks dynamic variable dependencies during variable initializer processing.
   * Set to non-null when inside a variable initializer, null otherwise.
   */
  private List<DynamicVariable> currentDynamicDependencies = null;

  /**
   * Constructor for EfxRulesTranslatorV2.
   *
   * @param validatorGenerator The generator for creating validation output.
   * @param symbolResolver The symbol resolver for looking up fields, nodes, and codelists.
   * @param scriptGenerator The script generator for creating XPath expressions.
   * @param errorListener The error listener for capturing parse errors.
   */
  public EfxRulesTranslatorV2(final ValidatorGenerator validatorGenerator,
      final SymbolResolver symbolResolver, final ScriptGenerator scriptGenerator,
      final BaseErrorListener errorListener) {
    super(symbolResolver, scriptGenerator, errorListener);
    this.validatorGenerator = validatorGenerator;
  }

  @Override
  public Map<String, String> translateRules(Path pathname, TranslatorOptions options)
      throws IOException {
    logger.debug("Translating EFX rules from file: {}", pathname);
    CharStream input = CharStreams.fromPath(pathname);

    // Default to filesystem-based include resolution relative to the input file
    if (options.getIncludedFileResolver() == null) {
      Path baseDir = pathname.toAbsolutePath().getParent();
      options = TranslatorOptions.withResolver(options, new FileSystemIncludedFileResolver(baseDir));
    }

    return translateRulesFromCharStream(input, options);
  }

  @Override
  public Map<String, String> translateRules(String rules, TranslatorOptions options) {
    logger.debug("Translating EFX rules from string");
    CharStream input = CharStreams.fromString(rules);
    try {
      return translateRulesFromCharStream(input, options);
    } catch (IOException e) {
      throw new UncheckedIOException("Include resolution failed during rules translation", e);
    }
  }

  @Override
  public Map<String, String> translateRules(InputStream stream, TranslatorOptions options)
      throws IOException {
    logger.debug("Translating EFX rules from input stream");
    CharStream input = CharStreams.fromStream(stream);
    return translateRulesFromCharStream(input, options);
  }

  /**
   * Internal method to translate EFX rules from a CharStream.
   *
   * @param input The CharStream containing the EFX rules.
   * @param options The translator options.
   * @return A map of output file paths to generated content.
   * @throws IOException If an I/O error occurs during translation.
   */
  private Map<String, String> translateRulesFromCharStream(CharStream input,
      TranslatorOptions options) throws IOException {
    logger.debug("Parsing EFX rules");

    // Resolve #include directives before parsing
    final CharStream resolvedInput =
        new IncludeProcessor(options.getIncludedFileResolver()).resolve(input);

    EfxLexer lexer = new EfxLexer(resolvedInput);
    lexer.removeErrorListeners();
    lexer.addErrorListener(this.errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    EfxParser parser = new EfxParser(tokens);
    parser.setErrorHandler(new EfxErrorStrategy());
    parser.removeErrorListeners();
    parser.addErrorListener(this.errorListener);

    // Parse the rules file
    ParseTree tree = parser.rulesFile();

    logger.debug("Walking parse tree to build intermediate model");

    // Initialize per-translation state
    this.validationPlan = new ValidationPlan();
    this.apiCallCounter = 0;
    this.stack = new CallStack();
    this.ruleStack.clear();

    // Walk the parse tree - this translator IS the listener
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);

    if (this.validationPlan.getStages().isEmpty()) {
      throw InvalidUsageException.emptyRulesFile();
    }

    // Generate output using the validator generator
    return this.validatorGenerator.generateOutput(this.validationPlan);
  }

  // #region ANTLR Listener Methods for EFX Rules Grammar

  // #region Variable Initializers

  @Override
  protected void resolveAndPushVariableReference(VariableReferenceContext ctx) {
    if (this.currentDynamicDependencies != null) {
      String variableName = ctx.variableName.getText();
      this.stack.getVariable(variableName).ifPresent(var -> {
        if (var instanceof DynamicVariable) {
          this.currentDynamicDependencies.add((DynamicVariable) var);
        } else if (var.hasDynamicDependencies()) {
          this.currentDynamicDependencies.addAll(var.getDynamicDependencies());
        }
      });
    }
    super.resolveAndPushVariableReference(ctx);
  }

  private void beginTrackingDynamicDependencies() {
    this.currentDynamicDependencies = new ArrayList<>();
  }

  private void endTrackingDynamicDependencies() {
    this.currentDynamicDependencies = null;
  }

  private void attachDynamicDependencies(final Variable variable) {
    if (this.currentDynamicDependencies != null && !this.currentDynamicDependencies.isEmpty()) {
      variable.addDynamicDependencies(this.currentDynamicDependencies);
    }
  }

  /**
   * Helper method to handle variable initializers.
   * Pops the expression, creates a Variable, and pushes it back.
   * This allows the variable to be declared in the stack for later reference.
   */
  private void exitVariableInitializer(String variableName,
      Class<? extends ScalarExpression> expressionType) {
    var expression = this.stack.pop(expressionType);
    var variable = new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, expression.getClass()),
        expression,
        this.script.composeVariableReference(variableName, expression.getClass()));
    this.attachDynamicDependencies(variable);
    this.stack.push(variable);
  }

  @Override
  public void enterStringVariableInitializer(StringVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitStringVariableInitializer(
      StringVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), StringExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterBooleanVariableInitializer(BooleanVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitBooleanVariableInitializer(
      BooleanVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), BooleanExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void exitDynamicFunctionInvocation(DynamicFunctionInvocationContext ctx) {
    String funcName = ctx.functionInvocation().functionName.getText();
    var func = this.stack.getFunction(funcName);
    if (!(func instanceof DynamicFunction)) {
      throw InvalidUsageException.notADynamicFunction(funcName);
    }
    DynamicFunction apiFunc = (DynamicFunction) func;
    List<? extends TypedExpression> args = this.stack.pop(ParsedArguments.class).getArgumentValues();
    var rawCall = this.script.composeDynamicFunction(apiFunc.endpointName, funcName, args);
    this.stack.push(new DynamicExpression(rawCall.getScript()));
    this.stack.push(apiFunc);
  }

  @Override
  public void exitDynamicVariableInitializer(DynamicVariableInitializerContext ctx) {
    String variableName = ctx.variableName.getText();
    var apiFunc = this.stack.pop(DynamicFunction.class);
    var rawCall = this.stack.pop(DynamicExpression.class);
    var declaration = this.script.composeVariableDeclaration(variableName, DynamicExpression.class);
    var varRef = this.script.composeVariableReference(variableName, NumericExpression.class);
    var one = this.script.getNumericLiteralEquivalent("1");
    var referenceExpression = this.script.composeComparisonOperation(varRef, "==", one);
    var variable = new DynamicVariable(variableName, declaration, rawCall, referenceExpression,
        apiFunc.endpointName, apiFunc.errorSeverity, apiFunc.errorLabel);
    this.stack.push(variable);
  }

  @Override
  public void enterNumericVariableInitializer(NumericVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitNumericVariableInitializer(
      NumericVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), NumericExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterDateVariableInitializer(DateVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitDateVariableInitializer(
      DateVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), DateExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterTimeVariableInitializer(TimeVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitTimeVariableInitializer(
      TimeVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), TimeExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterDurationVariableInitializer(DurationVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitDurationVariableInitializer(
      DurationVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), DurationExpression.class);
    this.endTrackingDynamicDependencies();
  }

  /**
   * Helper method to handle sequence variable initializers.
   * Pops the expression from the stack and creates a Variable object.
   */
  private void exitSequenceVariableInitializer(String variableName,
      Class<? extends SequenceExpression> expressionType) {
    var expression = this.stack.pop(expressionType);
    var variable = new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, expression.getClass()),
        expression,
        this.script.composeVariableReference(variableName, expression.getClass()));
    this.attachDynamicDependencies(variable);
    this.stack.push(variable);
  }

  @Override
  public void enterStringSequenceVariableInitializer(StringSequenceVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitStringSequenceVariableInitializer(
      StringSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), StringSequenceExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterBooleanSequenceVariableInitializer(BooleanSequenceVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitBooleanSequenceVariableInitializer(
      BooleanSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), BooleanSequenceExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterNumericSequenceVariableInitializer(NumericSequenceVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitNumericSequenceVariableInitializer(
      NumericSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), NumericSequenceExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterDateSequenceVariableInitializer(DateSequenceVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitDateSequenceVariableInitializer(
      DateSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), DateSequenceExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterTimeSequenceVariableInitializer(TimeSequenceVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitTimeSequenceVariableInitializer(
      TimeSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), TimeSequenceExpression.class);
    this.endTrackingDynamicDependencies();
  }

  @Override
  public void enterDurationSequenceVariableInitializer(DurationSequenceVariableInitializerContext ctx) {
    this.beginTrackingDynamicDependencies();
  }

  @Override
  public void exitDurationSequenceVariableInitializer(
      DurationSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), DurationSequenceExpression.class);
    this.endTrackingDynamicDependencies();
  }

  // #endregion Variable Initializers

  // #region Schema-level Variables

  /**
   * Called when exiting a schema-level variable declaration.
   * Declares the variable as a global identifier so it can be referenced later.
   * The variable will be passed to the validator generator for output generation.
   */
  @Override
  public void exitGlobalVariableDeclaration(GlobalVariableDeclarationContext ctx) {
    logger.debug("Processing schema-level variable declaration");

    // The variable initializer exit methods above should have created a Variable
    // object and pushed it onto the stack. Pop it and declare it as global.
    if (!this.stack.empty()) {
      Variable variable = this.stack.pop(Variable.class);

      // Declare the variable in the stack so it can be referenced later
      // and retrieved via stack.getGlobals() for the validator generator
      this.stack.declareGlobalIdentifier(variable);
      this.validationPlan.addVariable(variable);
      logger.debug("Declared global variable: {}", variable.name);
    }
  }

  /**
   * Called when entering a stage section.
   * Each stage section becomes a validation stage in the intermediate model.
   * Pushes a new ValidationStage to the stack and creates a stack frame for scoping.
   */
  @Override
  public void enterValidationStage(ValidationStageContext ctx) {

    this.stack.pushStackFrame();
    this.ruleStack.push(new ValidationStage(ctx.StageIdentifier().getText(), this.validationPlan));
  }

  /**
   * Called when exiting a stage section.
   * Pops the ValidationStage from the stack and adds it to the stages list.
   */
  @Override
  public void exitValidationStage(ValidationStageContext ctx) {
    logger.debug("Exiting STAGE section");

    var stage = this.ruleStack.pop(ValidationStage.class);
    this.validationPlan.addStage(stage);
    this.stack.popStackFrame();
  }

  /**
   * Called when exiting a pattern-level variable declaration.
   * Pattern-level variables are added to the current ValidationStage.
   * Uses peek to access the current stage on the stack.
   */
  @Override
  public void exitStageVariableDeclaration(StageVariableDeclarationContext ctx) {
    logger.debug("Processing pattern-level variable declaration");

    // The variable initializer should have created a Variable object
    if (!this.stack.empty()) {
      Variable variable = this.stack.pop(Variable.class);

      // Declare the variable in the stack so it can be referenced within this stage
      this.stack.declareIdentifier(variable);

      this.ruleStack.peek(ValidationStage.class).addVariable(variable);
    }
  }

  /**
   * Called when entering a rule block.
   * Pushes placeholders for rule components in the correct order.
   * These will be filled in by exit methods and popped in exitRuleBlock.
   */
  @Override
  public void enterRuleSet(RuleSetContext ctx) {
    this.stack.pushStackFrame();
    this.ruleStack.push(new RuleSet(this.ruleStack.peek(ValidationStage.class)));
  }

  /**
   * Called when exiting a rule block.
   * Pops all rule components from the stack, assembles them into a RuleSet,
   * and adds the rule set to the current stage.
   */
  @Override
  public void exitRuleSet(RuleSetContext ctx) {
    logger.debug("Exiting rule block");

    var ruleSet = this.ruleStack.pop(RuleSet.class);
    this.stack.popStackFrame();
    var stage = this.ruleStack.peek(ValidationStage.class);
    stage.addRuleSet(ruleSet);
    assert !this.efxContext.isEmpty() : "Expected context to be set in rule block (WITH clause should have pushed it)";
    this.efxContext.pop();
  }

  @Override
  public void exitVariableInitializer(VariableInitializerContext ctx) {
    var variable = this.stack.pop(Variable.class);
    this.stack.declareIdentifier(variable);
    this.ruleStack.peek(RuleSet.class).addVariable(variable);
  }

  @Override
  public void exitContextDeclaration(ContextDeclarationContext ctx) {
    String shortcut = ctx.shortcut != null ? ctx.shortcut.getText() : "none";
    switch (shortcut) {
      case ".":
        throw new UnsupportedOperationException("Same context (.) is not supported in EFX Rules.");
      case "..":
        throw new UnsupportedOperationException("Parent context (..) is not supported in EFX Rules.");
      case "/":
        this.exitRootContextDeclaration();
        break;
      default:
        PathExpression contextPath = this.stack.pop(PathExpression.class);
        if (ctx.fieldContext() != null) {
          String fieldId = getFieldId(ctx.fieldContext());
          assert fieldId != null : "We should have been able to locate the FieldId declared as context.";
          this.exitFieldContextDeclaration(fieldId, contextPath, null);
        } else if (ctx.contextVariableInitializer() != null) {
          Variable contextVariable = this.getContextVariable(ctx.contextVariableInitializer(), contextPath);
          assert contextVariable != null : "We should have been able to locate the ContextVariable declared as context.";
          if (ctx.contextVariableInitializer().fieldContext() != null) {
            String fieldId = getFieldId(ctx.contextVariableInitializer().fieldContext());
            this.exitFieldContextDeclaration(fieldId, contextPath, contextVariable);
          } else if (ctx.contextVariableInitializer().nodeContext() != null) {
            String nodeId = getNodeId(ctx.contextVariableInitializer().nodeContext());
            assert nodeId != null : "We should have been able to locate the NodeId declared as context.";
            this.exitNodeContextDeclaration(nodeId, contextPath, contextVariable);
          }
        } else if (ctx.nodeContext() != null) {
          String nodeId = getNodeId(ctx.nodeContext());
          assert nodeId != null : "We should have been able to locate the NodeId declared as context.";
          this.exitNodeContextDeclaration(nodeId, contextPath, null);
        }
        break;
    }
  }

  private void exitRootContextDeclaration() {
    this.exitNodeContextDeclaration(this.symbols.getRootNodeId(), this.symbols.getRootPath(), null);
  }

  private void exitFieldContextDeclaration(String fieldId, PathExpression contextPath, Variable contextVariable) {
    var context = new FieldContext(fieldId, contextPath, contextVariable);
    this.ruleStack.peek(RuleSet.class).setContext(context, contextVariable);
    this.efxContext.push(context);
    if (contextVariable != null) {
      this.stack.declareIdentifier(contextVariable);
      this.efxContext.declareContextVariable(contextVariable.name, context);
    }
  }

  private void exitNodeContextDeclaration(String nodeId, PathExpression contextPath, Variable contextVariable) {
    var context = new NodeContext(nodeId, contextPath, contextVariable);
    this.ruleStack.peek(RuleSet.class).setContext(context, contextVariable);
    this.efxContext.push(context);
    if (contextVariable != null) {
      this.stack.declareIdentifier(contextVariable);
      this.efxContext.declareContextVariable(contextVariable.name, context);
    }
  }

  private Variable getContextVariable(ContextVariableInitializerContext ctx,
      PathExpression contextPath) {
    if (ctx == null) {
      return null;
    }

    final String variableName = ctx.variableName.getText();
    final Class<? extends TypedExpression> variableType = contextPath.asScalar().getClass();

    return new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, variableType),
        this.script.contextualizePath(contextPath, contextPath),
        this.script.composeVariableReference(variableName, variableType));

  }


  /**
   * Called when exiting a WITH clause.
   * The WITH clause defines the context and local variables for the rule.
   * Pushes a RuleContextInfo object to the stack containing all WITH clause data.
   */
  @Override
  public void exitWithClause(WithClauseContext ctx) {
    logger.debug("Processing WITH clause");
  }


  /**
   * Called when exiting a WHEN clause.
   * The WHEN clause provides conditional application of the rule.
   * Pushes the condition XPath string to the stack.
   */
  @Override
  public void exitWhenClause(WhenClauseContext ctx) {
    var condition = this.stack.pop(BooleanExpression.class);
    var rule = this.ruleStack.peek(ValidationRule.class);
    var invertedCondition = this.script.composeLogicalNot(condition);
    rule.setCondition(condition, invertedCondition, this.combineWithOrParenthesized(rule.getExpression(), invertedCondition));
  }

  @Override
  public void exitAsClause(AsClauseContext ctx) {
    var rule = this.ruleStack.peek(ValidationRule.class);
    rule.setSeverity(RuleSeverity.fromString(ctx.severity().getText()));
    rule.setId(ctx.ruleId().getText().replaceAll("^\"|\"$", ""));
  }

  /**
   * Called when exiting a FOR clause.
   * The FOR clause specifies the subject (field or node) that the rule validates.
   * Pushes the subject Context to the stack.
   */
  @Override
  public void exitForClause(ForClauseContext ctx) {
    this.stack.pop(PathExpression.class); // Remove path expression pushed by base class

    if (ctx.simpleFieldReference() != null) {
      var fieldId = ctx.simpleFieldReference().FieldId().getText();
      this.ruleStack.peek(ValidationRule.class).setSubject(new FieldContext(fieldId, this.symbols.getAbsolutePathOfField(fieldId)));
    } else if (ctx.simpleNodeReference() != null) {
      var nodeId = ctx.simpleNodeReference().NodeId().getText();
      this.ruleStack.peek(ValidationRule.class).setSubject(new NodeContext(nodeId, this.symbols.getAbsolutePathOfNode(nodeId)));
    } else {
      assert false : "The grammar should prevent reaching this point without a field or node reference";
    }
  }

  /**
   * Called when exiting an IN clause.
   * The IN clause specifies which notice subtypes the assertion applies to.
   */
  @Override
  public void exitInClause(InClauseContext ctx) {
    logger.debug("Processing IN clause");

    String compressedList = ctx.noticeTypeList() instanceof AnyNoticeTypesContext ? "*" : ctx.noticeTypeList().getText();
    if (this.cachedSortedNoticeSubtypeIds == null) {
      this.cachedSortedNoticeSubtypeIds = this.symbols.getAllNoticeSubtypes().stream()
          .sorted()
          .map(SdkNoticeSubtype::getId)
          .collect(Collectors.toUnmodifiableList());
    }
    var noticeSubtypes = new NoticeSubtypeRange(compressedList, this.cachedSortedNoticeSubtypeIds);
    this.ruleStack.peek(ValidationRule.class).setNoticeSubtypeRange(noticeSubtypes);
    this.validationPlan.addNoticeSubtypes(noticeSubtypes.asList());
  }

  @Override
  public void exitScopeClause(ScopeClauseContext ctx) {
    var rule = this.ruleStack.peek(ValidationRule.class);
    if (ctx.flag() != null) {
      rule.setFlag(ctx.flag().flagName.getText());
    }
    if (ctx.scopeAnnotation() != null) {
      if (ctx.scopeAnnotation().Pre() != null) {
        rule.setScope(RuleScope.PRE);
      } else {
        rule.setScope(RuleScope.POST);
      }
    }
  }

  /**
   * Called when exiting an ASSERT clause.
   * Build the ValidationRule and wrap it in an AssertRule.
   */
  @Override
  public void exitAssertClause(AssertClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.ruleStack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.ruleStack.push(new AssertRule(rule));
  }

  @Override
  public void exitReportClause(ReportClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.ruleStack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.ruleStack.push(new ReportRule(rule));
  }

  /**
   * Called when exiting an OTHERWISE clause.
   * Build the ValidationRule and wrap it in an AssertRule.
   */
  @Override
  public void exitOtherwiseAssertClause(OtherwiseAssertClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.ruleStack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.ruleStack.push(new AssertRule(rule));
  }

  @Override
  public void exitOtherwiseReportClause(OtherwiseReportClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.ruleStack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.ruleStack.push(new ReportRule(rule));
  }


  private BooleanExpression combineWithOrParenthesized(BooleanExpression left, BooleanExpression right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return this.script.composeLogicalOr(
        this.script.composeParenthesizedExpression(left, BooleanExpression.class),
        this.script.composeParenthesizedExpression(right, BooleanExpression.class));
  }

  @Override
  public void enterSimpleRule(SimpleRuleContext ctx) {
    this.ruleStack.push(new ValidationRule(this.ruleStack.peek(RuleSet.class)));
  }

  @Override
  public void exitSimpleRule(SimpleRuleContext ctx) {
    var rule = this.ruleStack.pop(ValidationRule.class);
    this.ruleStack.peek(RuleSet.class).addRule(rule);
  }

  @Override
  public void enterConditionalRule(ConditionalRuleContext ctx) {
    this.ruleStack.push(new ValidationRule(this.ruleStack.peek(RuleSet.class)));
  }

  @Override
  public void exitConditionalRule(ConditionalRuleContext ctx) {
    var rule = this.ruleStack.pop(ValidationRule.class);
    this.ruleStack.peek(RuleSet.class).addRule(rule);
  }

  @Override
  public void enterFallbackRule(FallbackRuleContext ctx) {
    this.ruleStack.push(new ValidationRule(this.ruleStack.peek(RuleSet.class)));
  }

  @Override
  public void exitFallbackRule(FallbackRuleContext ctx) {
    var fallbackRule = this.ruleStack.pop(ValidationRule.class);
    var ruleSet = this.ruleStack.pop(RuleSet.class);

    BooleanExpression combined = null;
    for (ValidationRule rule : ruleSet) {
      assert rule != null &&  rule.getCondition() != null : "The EFX grammar should have prevented this. All rules in a RuleSet should have conditions if a fallback is defined.";
      combined = (combined == null) ? rule.getInvertedCondition() : this.script.composeLogicalAnd(combined, rule.getInvertedCondition());
    }
    var invertedCombined = this.script.composeLogicalNot(combined);

    fallbackRule.setCondition(combined, invertedCombined, this.combineWithOrParenthesized(fallbackRule.getExpression(), invertedCombined));

    ruleSet.setFallbackRule(fallbackRule);
    this.ruleStack.push(ruleSet);
  }

  // #region Dynamic Function Declarations

  @Override
  public void exitApiEndpointDeclaration(ApiEndpointDeclarationContext ctx) {
    String name = unquote(ctx.endpointName.getText());
    String url = ctx.endpointUrl != null ? unquote(ctx.endpointUrl.getText()) : null;
    this.validationPlan.declareEndpoint(name, url);
  }

  @Override
  public void enterDynamicFunctionDeclaration(DynamicFunctionDeclarationContext ctx) {
    this.stack.pushStackFrame();
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDynamicFunctionDeclaration(DynamicFunctionDeclarationContext ctx) {
    ParsedParameters params = this.stack.pop(ParsedParameters.class);
    this.stack.popStackFrame();
    String apiName = ctx.functionName.getText();
    String endpointName = ctx.endpointName != null ? unquote(ctx.endpointName.getText()) : "default";
    if (!this.validationPlan.getEndpoints().containsKey(endpointName)) {
      throw InvalidIdentifierException.undeclaredEndpoint(apiName, endpointName);
    }
    RuleSeverity errorSeverity = ctx.onErrorClause() != null && ctx.onErrorClause().Warn() != null
        ? RuleSeverity.WARNING : RuleSeverity.ERROR;
    String errorLabel = ctx.onErrorClause() != null && ctx.onErrorClause().errorLabel != null
        ? unquote(ctx.onErrorClause().errorLabel.getText()) : null;
    this.stack.declareFunction(new DynamicFunction(apiName, params, endpointName, errorSeverity, errorLabel));
  }

  @Override
  public void exitBooleanFunctionInvocation(BooleanFunctionInvocationContext ctx) {
    if (this.isApiFunction(ctx.functionInvocation())) {
      this.composeAutoGeneratedDynamicVariable(ctx.functionInvocation());
    } else {
      super.exitBooleanFunctionInvocation(ctx);
    }
  }

  @Override
  public void exitScalarFromFunctionInvocation(ScalarFromFunctionInvocationContext ctx) {
    if (this.isApiFunction(ctx.functionInvocation())) {
      this.composeAutoGeneratedDynamicVariable(ctx.functionInvocation());
    } else {
      super.exitScalarFromFunctionInvocation(ctx);
    }
  }

  private boolean isApiFunction(FunctionInvocationContext ctx) {
    return this.stack.getFunction(ctx.functionName.getText()) instanceof DynamicFunction;
  }

  private int apiCallCounter = 0;

  /**
   * Handles an inline dynamic function call in a rule expression.
   * Creates an auto-generated dynamic variable on the current ValidationRule and pushes
   * a boolean reference expression ($varName = 1) onto the stack.
   */
  private void composeAutoGeneratedDynamicVariable(FunctionInvocationContext ctx) {
    String funcName = ctx.functionName.getText();
    if (!this.ruleStack.contains(ValidationRule.class)) {
      throw InvalidUsageException.dynamicFunctionOutsideRule(funcName);
    }

    // Resolve the dynamic function and compose the raw dynamic function call.
    DynamicFunction apiFunc = (DynamicFunction) this.stack.getFunction(funcName);
    List<? extends TypedExpression> args = this.stack.pop(ParsedArguments.class).getArgumentValues();
    var rawCall = this.script.composeDynamicFunction(apiFunc.endpointName, funcName, args);

    // Compose the variable's expressions: declaration, initialization, and success check ($var == 1).
    String varName = "__apiResult" + (++this.apiCallCounter);
    var declaration = this.script.composeVariableDeclaration(varName, DynamicExpression.class);
    var initExpression = new DynamicExpression(rawCall.getScript());
    var varRef = this.script.composeVariableReference(varName, NumericExpression.class);
    var one = this.script.getNumericLiteralEquivalent("1");
    var referenceExpression = this.script.composeComparisonOperation(varRef, "==", one);

    // Create the auto-generated variable and register it on the current rule.
    var rule = this.ruleStack.peek(ValidationRule.class);
    int index = rule.getAutoGeneratedVariables().size() + 1;
    var variable = new DynamicVariable.AutoGenerated(index, varName, declaration,
        initExpression, referenceExpression, apiFunc.endpointName, apiFunc.errorSeverity,
        apiFunc.errorLabel);
    rule.addAutoGeneratedVariable(variable);

    // Push the success check so it can be used in the rule's condition.
    this.stack.push(referenceExpression);
  }

  @Override
  protected void exitParameterDeclaration(ParserRuleContext ctx, String parameterName,
      Class<? extends TypedExpression> parameterType) {
    ParsedParameter parameter = new ParsedParameter(parameterName,
        this.script.composeParameterReference(parameterName, parameterType));
    this.stack.declareIdentifier(parameter);
    this.stack.peek(ParsedParameters.class).add(parameter);
  }

  private static String unquote(String literal) {
    if (literal.length() >= 2
        && ((literal.startsWith("'") && literal.endsWith("'"))
            || (literal.startsWith("\"") && literal.endsWith("\"")))) {
      return literal.substring(1, literal.length() - 1);
    }
    return literal;
  }

  // #endregion Dynamic Function Declarations

  // #endregion ANTLR Listener Methods

}
