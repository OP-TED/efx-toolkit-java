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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.efx.interfaces.EfxRulesTranslator;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
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
import eu.europa.ted.efx.model.rules.CompleteValidation;
import eu.europa.ted.efx.model.rules.NoticeSubtypeRange;
import eu.europa.ted.efx.model.rules.ReportRule;
import eu.europa.ted.efx.model.rules.RuleSet;
import eu.europa.ted.efx.model.rules.RuleSeverity;
import eu.europa.ted.efx.model.rules.ValidationRule;
import eu.europa.ted.efx.model.rules.ValidationStage;
import eu.europa.ted.efx.model.types.FieldTypes;
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
  private CompleteValidation completeValidation = new CompleteValidation();

  private List<String> cachedSortedNoticeSubtypeIds;

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

    // New in EFX-2: rules preprocessing
    final RulesPreprocessor preprocessor = this.new RulesPreprocessor(input, options.getIncludedFileResolver());
    final String preprocessedRules = preprocessor.processRules();

    // Now parse the preprocessed rules
    EfxLexer lexer = new EfxLexer(CharStreams.fromString(preprocessedRules));
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

    // Initialize the stages list for this translation
    this.completeValidation = new CompleteValidation();

    // Walk the parse tree - this translator IS the listener
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);

    if (this.completeValidation.getStages().isEmpty()) {
      throw new ParseCancellationException(
          "Rules file must contain at least one validation stage");
    }

    // Generate output using the validator generator
    return this.validatorGenerator.generateOutput(this.completeValidation);
  }

  // #region ANTLR Listener Methods for EFX Rules Grammar

  // #region Variable Initializers

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
    this.stack.push(variable);
  }

  @Override
  public void exitStringVariableInitializer(
      StringVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), StringExpression.class);
  }

  @Override
  public void exitBooleanVariableInitializer(
      BooleanVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), BooleanExpression.class);
  }

  @Override
  public void exitNumericVariableInitializer(
      NumericVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), NumericExpression.class);
  }

  @Override
  public void exitDateVariableInitializer(
      DateVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), DateExpression.class);
  }

  @Override
  public void exitTimeVariableInitializer(
      TimeVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), TimeExpression.class);
  }

  @Override
  public void exitDurationVariableInitializer(
      DurationVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), DurationExpression.class);
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
    this.stack.push(variable);
  }

  @Override
  public void exitStringSequenceVariableInitializer(
      StringSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceVariableInitializer(
      BooleanSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceVariableInitializer(
      NumericSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), NumericSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceVariableInitializer(
      DateSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceVariableInitializer(
      TimeSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceVariableInitializer(
      DurationSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), DurationSequenceExpression.class);
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
      this.completeValidation.addGlobalVariable(variable);
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
    this.stack.push(new ValidationStage(ctx.StageIdentifier().getText()));
  }

  /**
   * Called when exiting a stage section.
   * Pops the ValidationStage from the stack and adds it to the stages list.
   */
  @Override
  public void exitValidationStage(ValidationStageContext ctx) {
    logger.debug("Exiting STAGE section");

    var stage = this.stack.pop(ValidationStage.class);
    this.completeValidation.addStage(stage);
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

      this.stack.peek(ValidationStage.class).addVariable(variable);
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
    this.stack.push(new RuleSet());
  }

  /**
   * Called when exiting a rule block.
   * Pops all rule components from the stack, assembles them into a RuleSet,
   * and adds the rule set to the current stage.
   */
  @Override
  public void exitRuleSet(RuleSetContext ctx) {
    logger.debug("Exiting rule block");

    var ruleSet = this.stack.pop(RuleSet.class);
    this.stack.popStackFrame();
    var stage = this.stack.peek(ValidationStage.class);
    stage.addRuleSet(ruleSet);
    assert !this.efxContext.isEmpty() : "Expected context to be set in rule block (WITH clause should have pushed it)";
    this.efxContext.pop();
  }

  @Override
  public void exitVariableInitializer(VariableInitializerContext ctx) {
    var variable = this.stack.pop(Variable.class);
    this.stack.declareIdentifier(variable);
    this.stack.peek(RuleSet.class).addVariable(variable);
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
    this.stack.peek(RuleSet.class).setContext(context, contextVariable);
    this.efxContext.push(context);
    if (contextVariable != null) {
      this.stack.declareIdentifier(contextVariable);
      this.efxContext.declareContextVariable(contextVariable.name, context);
    }
  }

  private void exitNodeContextDeclaration(String nodeId, PathExpression contextPath, Variable contextVariable) {
    var context = new NodeContext(nodeId, contextPath, contextVariable);
    this.stack.peek(RuleSet.class).setContext(context, contextVariable);
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
    var rule = this.stack.pop(ValidationRule.class);
    var invertedCondition = this.script.composeLogicalNot(condition);
    rule.setCondition(condition, invertedCondition, this.combineWithOrParenthesized(rule.getExpression(), invertedCondition));
    this.stack.push(rule);
  }

  @Override
  public void exitAsClause(AsClauseContext ctx) {
    var rule = this.stack.pop(ValidationRule.class);
    rule.setSeverity(RuleSeverity.fromString(ctx.severity().getText()));
    rule.setId(ctx.ruleId().getText().replaceAll("^\"|\"$", ""));
    this.stack.push(rule);
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
      this.stack.peek(ValidationRule.class).setSubject(new FieldContext(fieldId, this.symbols.getAbsolutePathOfField(fieldId)));
    } else if (ctx.simpleNodeReference() != null) {
      var nodeId = ctx.simpleNodeReference().NodeId().getText();
      this.stack.peek(ValidationRule.class).setSubject(new NodeContext(nodeId, this.symbols.getAbsolutePathOfNode(nodeId)));
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
    this.stack.peek(ValidationRule.class).setNoticeSubtypeRange(noticeSubtypes);
    this.completeValidation.addNoticeSubtypes(noticeSubtypes.asList());
  }

  /**
   * Called when exiting an ASSERT clause.
   * Build the ValidationRule and wrap it in an AssertRule.
   */
  @Override
  public void exitAssertClause(AssertClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.stack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.stack.push(new AssertRule(rule));
  }

  @Override
  public void exitReportClause(ReportClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.stack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.stack.push(new ReportRule(rule));
  }

  /**
   * Called when exiting an OTHERWISE clause.
   * Build the ValidationRule and wrap it in an AssertRule.
   */
  @Override
  public void exitOtherwiseAssertClause(OtherwiseAssertClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.stack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.stack.push(new AssertRule(rule));
  }

  @Override
  public void exitOtherwiseReportClause(OtherwiseReportClauseContext ctx) {
    var test = this.stack.pop(BooleanExpression.class);
    var rule = this.stack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.stack.push(new ReportRule(rule));
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
    this.stack.push(new ValidationRule());
  }

  @Override
  public void exitSimpleRule(SimpleRuleContext ctx) {
    var rule = this.stack.pop(ValidationRule.class);
    this.stack.peek(RuleSet.class).addRule(rule);
  }

  @Override
  public void enterConditionalRule(ConditionalRuleContext ctx) {
    this.stack.push(new ValidationRule());
  }

  @Override
  public void exitConditionalRule(ConditionalRuleContext ctx) {
    var rule = this.stack.pop(ValidationRule.class);
    this.stack.peek(RuleSet.class).addRule(rule);
  }

  @Override
  public void enterFallbackRule(FallbackRuleContext ctx) {
    this.stack.push(new ValidationRule());
  }

  @Override
  public void exitFallbackRule(FallbackRuleContext ctx) {
    var fallbackRule = this.stack.pop(ValidationRule.class);
    var ruleSet = this.stack.pop(RuleSet.class);

    BooleanExpression combined = null;
    for (ValidationRule rule : ruleSet) {
      assert rule != null &&  rule.getCondition() != null : "The EFX grammar should have prevented this. All rules in a RuleSet should have conditions if a fallback is defined.";
      combined = (combined == null) ? rule.getInvertedCondition() : this.script.composeLogicalAnd(combined, rule.getInvertedCondition());
    }
    var invertedCombined = this.script.composeLogicalNot(combined);

    fallbackRule.setCondition(combined, invertedCombined, this.combineWithOrParenthesized(fallbackRule.getExpression(), invertedCombined));

    ruleSet.setFallbackRule(fallbackRule);
    this.stack.push(ruleSet);
  }

  // #endregion ANTLR Listener Methods

  // #region RulesPreprocessor - Inner class for handling late-bound expressions

  /**
   * Preprocessor for EFX Rules.
   * Extends ExpressionPreprocessor to inherit common late-bound expression handling
   * and adds Rules-specific variable declaration processing.
   */
  class RulesPreprocessor extends ExpressionPreprocessor {

    RulesPreprocessor(final CharStream charStream, IncludedFileResolver resolver)
        throws IOException {
      super(new IncludeProcessor(resolver).resolve(charStream));
    }

    String processRules() {
      final ParseTree tree = parser.rulesFile();
      final ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(this, tree);
      return this.rewriter.getText();
    }

    // #region Rules-specific variable declarations

    /**
     * Process schema-level variable declarations in the preprocessor.
     * This allows variable types to be known for later reference.
     */
    @Override
    public void exitGlobalVariableDeclaration(GlobalVariableDeclarationContext ctx) {
      // The variable initializer should have created a Variable and pushed it
      if (!this.stack.empty()) {
        Variable variable = this.stack.pop(Variable.class);
        // Declare it in the preprocessor's stack so it can be referenced
        this.stack.declareGlobalIdentifier(variable);
      }
    }

    /**
     * Process pattern-level variable declarations in the preprocessor.
     */
    @Override
    public void exitStageVariableDeclaration(
        StageVariableDeclarationContext ctx) {
      if (!this.stack.empty()) {
        Variable variable = this.stack.pop(Variable.class);
        this.stack.declareIdentifier(variable);
      }
    }

    /**
     * Push a new stack frame when entering a validation stage.
     * This ensures stage-level variables are scoped to their stage.
     */
    @Override
    public void enterValidationStage(ValidationStageContext ctx) {
      this.stack.pushStackFrame();
    }

    /**
     * Pop the stack frame when exiting a validation stage.
     */
    @Override
    public void exitValidationStage(ValidationStageContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void exitContextDeclaration(ContextDeclarationContext ctx) {
      // Handle context shortcuts (., .., /) - these don't have context variable initializers
      String shortcut = ctx.shortcut != null ? ctx.shortcut.getText() : "none";
      switch (shortcut) {
        case "/":
          this.efxContext.push(new NodeContext(this.symbols.getRootNodeId(), this.symbols.getRootPath()));
          return;
        case ".":
        case "..":
          // Same/parent context not supported in rules, but still need to push something
          return;
        default:
          break;
      }

      // Handle field or node context without variable
      if (ctx.fieldContext() != null) {
        final String fieldId = getFieldId(ctx.fieldContext());
        this.efxContext.push(new FieldContext(fieldId, this.symbols.getAbsolutePathOfField(fieldId),
            this.symbols.getRelativePathOfField(fieldId, this.efxContext.symbol())));
        return;
      }
      if (ctx.nodeContext() != null) {
        final String nodeId = getNodeId(ctx.nodeContext());
        this.efxContext.push(new NodeContext(nodeId, this.symbols.getAbsolutePathOfNode(nodeId),
            this.symbols.getRelativePathOfNode(nodeId, this.efxContext.symbol())));
        return;
      }

      // Handle context variable initializer
      final var initializer = ctx.contextVariableInitializer();
      if (initializer == null) {
        return;
      }
      final String variableName = initializer.variableName.getText();
      if (initializer.fieldContext() != null) {
        final String fieldId = getFieldId(initializer.fieldContext());
        var fieldType = FieldTypes.fromString(this.symbols.getTypeOfField(fieldId));
        this.stack.declareIdentifier(
            new Variable(variableName, ScalarPath.empty(fieldType),
                ScalarPath.empty(fieldType), ScalarPath.empty(fieldType)));
        var context = new FieldContext(fieldId, this.symbols.getAbsolutePathOfField(fieldId),
            this.symbols.getRelativePathOfField(fieldId, this.efxContext.symbol()));
        this.efxContext.push(context);
        this.efxContext.declareContextVariable(variableName, context);
      }
      if (initializer.nodeContext() != null) {
        final String nodeId = getNodeId(initializer.nodeContext());
        this.stack.declareIdentifier(new Variable(variableName, NodePath.empty(),
            NodePath.empty(), NodePath.empty()));
        var context = new NodeContext(nodeId, this.symbols.getAbsolutePathOfNode(nodeId),
            this.symbols.getRelativePathOfNode(nodeId, this.efxContext.symbol()));
        this.efxContext.push(context);
        this.efxContext.declareContextVariable(variableName, context);
      }
    }

    @Override
    public void enterRuleSet(RuleSetContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitRuleSet(RuleSetContext ctx) {
      this.stack.popStackFrame();
      this.efxContext.pop();
    }

    /**
     * Process template-level variable declarations in the preprocessor (WITH clause).
     */
    @Override
    public void exitVariableInitializer(
        VariableInitializerContext ctx) {
        Variable variable = this.stack.pop(Variable.class);
        this.stack.declareIdentifier(variable);
    }

    // #endregion Rules-specific variable declarations

    // #region Variable initializers for type tracking

    /**
     * Variable initializer exit methods for the preprocessor.
     * These create simple Variable objects with empty expressions just for type tracking.
     */
    @Override
    public void exitStringVariableInitializer(StringVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          StringExpression.empty(), StringExpression.empty()));
    }

    @Override
    public void exitBooleanVariableInitializer(BooleanVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          BooleanExpression.empty(), BooleanExpression.empty()));
    }

    @Override
    public void exitNumericVariableInitializer(NumericVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          NumericExpression.empty(), NumericExpression.empty()));
    }

    @Override
    public void exitDateVariableInitializer(DateVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          DateExpression.empty(), DateExpression.empty()));
    }

    @Override
    public void exitTimeVariableInitializer(TimeVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          TimeExpression.empty(), TimeExpression.empty()));
    }

    @Override
    public void exitDurationVariableInitializer(DurationVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          DurationExpression.empty(), DurationExpression.empty()));
    }

    // Sequence variable initializers for type tracking

    @Override
    public void exitStringSequenceVariableInitializer(StringSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          new StringSequenceExpression(""), new StringSequenceExpression("")));
    }

    @Override
    public void exitBooleanSequenceVariableInitializer(BooleanSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          new BooleanSequenceExpression(""), new BooleanSequenceExpression("")));
    }

    @Override
    public void exitNumericSequenceVariableInitializer(NumericSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          new NumericSequenceExpression(""), new NumericSequenceExpression("")));
    }

    @Override
    public void exitDateSequenceVariableInitializer(DateSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          new DateSequenceExpression(""), new DateSequenceExpression("")));
    }

    @Override
    public void exitTimeSequenceVariableInitializer(TimeSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          new TimeSequenceExpression(""), new TimeSequenceExpression("")));
    }

    @Override
    public void exitDurationSequenceVariableInitializer(DurationSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(),
          new DurationSequenceExpression(""), new DurationSequenceExpression("")));
    }

    // #endregion Variable initializers for type tracking
  }

  // #endregion RulesPreprocessor
}
