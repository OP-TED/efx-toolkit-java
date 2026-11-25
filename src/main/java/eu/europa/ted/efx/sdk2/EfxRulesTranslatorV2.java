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

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.ParameterType.INDICATOR;

import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
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
import eu.europa.ted.efx.exceptions.EfxCompilationException;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.builtins.BuiltIn;
import eu.europa.ted.efx.model.builtins.ComparisonOperators;
import eu.europa.ted.efx.model.builtins.ConditionOperators;
import eu.europa.ted.efx.model.builtins.ConversionFunctions;
import eu.europa.ted.efx.model.builtins.LogicalOperators;
import eu.europa.ted.efx.model.builtins.NumericFunctions;
import eu.europa.ted.efx.model.builtins.PatternMatch;
import eu.europa.ted.efx.model.builtins.StringFunctions;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DynamicExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.rules.AssertRule;
import eu.europa.ted.efx.model.rules.Behaviour;
import eu.europa.ted.efx.model.rules.BehaviourKind;
import eu.europa.ted.efx.model.rules.ValidationPlan;
import eu.europa.ted.efx.model.rules.NoticeSubtypeRange;
import eu.europa.ted.efx.model.rules.ReportRule;
import eu.europa.ted.efx.model.rules.RuleSet;
import eu.europa.ted.efx.xpath.XPathContextualizer;
import eu.europa.ted.efx.model.rules.RuleScope;
import eu.europa.ted.efx.model.rules.RuleSeverity;
import eu.europa.ted.efx.model.rules.RuleStack;
import eu.europa.ted.efx.model.rules.ValidationRule;
import eu.europa.ted.efx.model.rules.ValidationStage;
import eu.europa.ted.efx.model.identifiers.DynamicFunction;
import eu.europa.ted.efx.model.identifiers.DynamicVariable;
import eu.europa.ted.efx.model.identifiers.Function;
import eu.europa.ted.efx.model.identifiers.ParsedParameters;
import eu.europa.ted.efx.model.identifiers.Variable;
import eu.europa.ted.efx.sdk2.EfxExpressionVisitor.ReadAs;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * EFX Rules translator for SDK version 2.
 *
 * This translator parses EFX Rules files and produces an intermediate model
 * (List of ValidationStage) which is then passed to a ValidatorGenerator
 * for output generation.
 *
 * It listens to the rules of a rules file that are not expressions, asks an
 * {@link EfxExpressionVisitor} for each expression, field context and node context they hold, and
 * implements EfxRulesTranslator to provide the rules translation API.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_RULES_TRANSLATOR)
public class EfxRulesTranslatorV2 extends TranslatorListener
    implements EfxRulesTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxRulesTranslatorV2.class);

  /** The clauses of a rule that take a condition, as a type error names them. */
  private static final Clause WHEN = Clause.of(Clause.describe("WHEN"),
      parameter("condition", INDICATOR));
  private static final Clause ASSERT = Clause.of(Clause.describe("ASSERT"),
      parameter("condition", INDICATOR));
  private static final Clause REPORT = Clause.of(Clause.describe("REPORT"),
      parameter("condition", INDICATOR));
  private static final Clause OTHERWISE_ASSERT =
      Clause.of(Clause.describe("OTHERWISE ASSERT"), parameter("condition", INDICATOR));
  private static final Clause OTHERWISE_REPORT =
      Clause.of(Clause.describe("OTHERWISE REPORT"), parameter("condition", INDICATOR));

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
  EfxExpressionVisitor visitorOf(final Translation translation) {
    return new RulesExpressionVisitor(translation);
  }

  /**
   * Translates a single expression, as {@link EfxExpressionTranslatorV2} does.
   */
  @Override
  public String translateExpression(final String expression, final String... arguments) {
    return new EfxExpressionTranslatorV2(this.translation.symbols(), this.translation.script(),
        this.errorListener)
        .translateExpression(expression, arguments);
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

    // Parse the rules file
    ParseTree tree = this.parserOf(resolvedInput).rulesFile();

    logger.debug("Walking parse tree to build intermediate model");

    // Initialize per-translation state
    this.validationPlan = new ValidationPlan();
    this.apiCallCounter = 0;
    this.ruleStack.clear();

    // Walk the parse tree - this translator IS the listener
    this.translate(tree);

    if (this.validationPlan.getStages().isEmpty()) {
      throw InvalidUsageException.emptyRulesFile();
    }

    // Generate output using the validator generator
    return this.validatorGenerator.generateOutput(this.validationPlan);
  }

  // #region ANTLR Listener Methods for EFX Rules Grammar

  // #region Variable Initializers

  /**
   * Records a variable that an initializer reads, if it is a dynamic variable or depends on one.
   */
  private void trackDynamicDependenciesOf(final String variableName) {
    if (this.currentDynamicDependencies != null) {
      this.translation.stack().getVariable(variableName).ifPresent(var -> {
        if (var instanceof DynamicVariable) {
          this.currentDynamicDependencies.add((DynamicVariable) var);
        } else if (var.hasDynamicDependencies()) {
          this.currentDynamicDependencies.addAll(var.getDynamicDependencies());
        }
      });
    }
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
   * A variable is initialized with a value of its declared type. The dynamic variables its
   * initializer reads become its dynamic dependencies.
   */
  @Override
  public void exitTypedVariableInitializer(TypedVariableInitializerContext ctx) {
    var variableName = ctx.variableName.getText();
    this.beginTrackingDynamicDependencies();
    try {
      var expression = this.expressions.initialValueOf(ctx);
      var variable = new Variable(variableName,
          this.translation.script().composeVariableDeclaration(variableName, expression.getClass()),
          expression,
          this.translation.script().composeVariableReference(variableName, expression.getClass()));
      this.attachDynamicDependencies(variable);
      this.exitVariableInitializer(ctx, variable);
    } finally {
      this.endTrackingDynamicDependencies();
    }
  }

  @Override
  public void exitDynamicFunctionInvocation(DynamicFunctionInvocationContext ctx) {
    FunctionInvocationContext invocation = ctx.functionInvocation();
    String funcName = invocation.functionName.getText();
    var func = this.translation.stack().getFunction(funcName);
    var call = this.expressions.invocationOf(invocation, func);
    this.translation.stack().push(call.compose(this.translation.script()));
    this.translation.stack().push(func);
  }

  @Override
  public void exitDynamicVariableInitializer(DynamicVariableInitializerContext ctx) {
    String variableName = ctx.variableName.getText();
    var apiFunc = this.translation.stack().pop(DynamicFunction.class);
    var rawCall = this.translation.stack().pop(DynamicExpression.class);
    var declaration = this.translation.script().composeVariableDeclaration(variableName,
        DynamicExpression.class);
    var varRef = this.translation.script().composeVariableReference(variableName,
        NumericExpression.class);
    var one = this.translation.script().getNumericLiteralEquivalent("1");
    var referenceExpression = this.translation.script().composeComparisonOperation(varRef, "==",
        one);
    var variable = new DynamicVariable(variableName, declaration, rawCall, referenceExpression,
        apiFunc.endpointName, apiFunc.errorSeverity, apiFunc.errorLabel);
    this.exitVariableInitializer(ctx, variable);
  }

  /**
   * A variable of a WITH clause is declared at once, so that the initializers after it can read
   * it, and belongs to the rule set. The variable of a global or stage declaration is declared
   * when the declaration exits.
   */
  private void exitVariableInitializer(VariableInitializerContext initializer, Variable variable) {
    if (initializer.getParent() instanceof VariableListContext) {
      this.translation.stack().declareIdentifier(variable);
      this.ruleStack.peek(RuleSet.class).addVariable(variable);
    } else {
      this.translation.stack().push(variable);
    }
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
    if (!this.translation.stack().empty()) {
      Variable variable = this.translation.stack().pop(Variable.class);

      // Declare the variable in the stack so it can be referenced later
      // and retrieved via stack.getGlobals() for the validator generator
      this.translation.stack().declareGlobalIdentifier(variable);
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

    this.translation.stack().pushStackFrame();
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
    this.translation.stack().popStackFrame();
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
    if (!this.translation.stack().empty()) {
      Variable variable = this.translation.stack().pop(Variable.class);

      // Declare the variable in the stack so it can be referenced within this stage
      this.translation.stack().declareIdentifier(variable);

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
    this.translation.stack().pushStackFrame();
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
    this.translation.stack().popStackFrame();
    if (ruleSet.getContext() == null) {
      // After a syntax error, the parser may keep a rule set whose WITH clause it could not read,
      // as when the semicolon before OTHERWISE is missing. It declares no context and has no rule
      // to add; the error is already reported.
      return;
    }
    var stage = this.ruleStack.peek(ValidationStage.class);
    stage.addRuleSet(ruleSet);
    assert !this.translation.efxContext().isEmpty() : "Expected context to be set in rule block (WITH clause should have pushed it)";
    this.translation.efxContext().pop();
  }


  @Override
  public void exitContextDeclaration(ContextDeclarationContext ctx) {
    if (ctx.contextVariableInitializer() != null) {
      Context declared = this.expressions.contextAssignedBy(ctx.contextVariableInitializer());
      Variable contextVariable =
          this.getContextVariable(ctx.contextVariableInitializer(), declared.relativePath());
      this.exitContextDeclaration(declared, contextVariable);
    } else {
      this.exitContextDeclaration(
          this.expressions.contextOf(ctx.getRuleContext(ParserRuleContext.class, 0)), null);
    }
    if (isEveryContext(ctx)) {
      this.setEveryRuleContextPath();
    }
  }


  /**
   * The context declared is also the context of the rule set.
   */
  @Override
  void declareContext(final Context context, final Variable contextVariable) {
    this.ruleStack.peek(RuleSet.class).setContext(context, contextVariable);
    super.declareContext(context, contextVariable);
  }

  /**
   * True when the context is declared with WITH EVERY.
   */
  private static boolean isEveryContext(ContextDeclarationContext ctx) {
    return ctx.getParent() instanceof ContextDeclarationBlockContext
        && ctx.getParent().getParent() instanceof WithClauseContext
        && ((WithClauseContext) ctx.getParent().getParent()).EVERY() != null;
  }

  /**
   * For WITH EVERY, sets the context of the Schematron rule to the path of the context without
   * the declared predicates on its last step. The EFX context keeps the full path, so everything
   * inside the rule set is read as for WITH without EVERY.
   */
  private void setEveryRuleContextPath() {
    final RuleSet ruleSet = this.ruleStack.peek(RuleSet.class);
    final Context context = ruleSet.getContext();
    final PathExpression declaredPath = context.isFieldContext()
        ? this.translation.symbols().getAbsolutePathOfField(context.symbol())
        : this.translation.symbols().getAbsolutePathOfNode(context.symbol());
    ruleSet.setRuleContextPath(
        XPathContextualizer.removeDeclaredPredicatesOfLastStep(context.absolutePath(), declaredPath));
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


  /** True while the condition of a WHEN clause is being translated. */
  private boolean translatingCondition = false;

  @Override
  public void enterWhenClause(WhenClauseContext ctx) {
    this.translatingCondition = true;
  }

  /**
   * Called when exiting a WHEN clause.
   * The WHEN clause provides conditional application of the rule.
   * Pushes the condition XPath string to the stack.
   */
  @Override
  public void exitWhenClause(WhenClauseContext ctx) {
    var condition = this.expressions.valueOf(ctx.expression(), WHEN).booleanAt(0);
    this.translatingCondition = false;
    var rule = this.ruleStack.peek(ValidationRule.class);
    var invertedCondition = this.translation.script().composeLogicalNot(condition);
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
   * Pushes the subject Context to the stack. An error in the subject is placed at it.
   */
  @Override
  public void exitForClause(ForClauseContext ctx) {
    final ParserRuleContext subject = ctx.simpleFieldReference() != null ? ctx.simpleFieldReference() : ctx.simpleNodeReference();
    try {
      if (ctx.simpleFieldReference() != null) {
        var fieldId = ctx.simpleFieldReference().FieldId().getText();
        this.ruleStack.peek(ValidationRule.class).setSubject(new FieldContext(fieldId, this.translation.symbols().getAbsolutePathOfField(fieldId)));
      } else if (ctx.simpleNodeReference() != null) {
        var nodeId = ctx.simpleNodeReference().NodeId().getText();
        this.ruleStack.peek(ValidationRule.class).setSubject(new NodeContext(nodeId, this.translation.symbols().getAbsolutePathOfNode(nodeId)));
      } else {
        assert false : "The grammar should prevent reaching this point without a field or node reference";
      }
    } catch (final EfxCompilationException e) {
      throw e.at(subject);
    }
  }

  /**
   * Called when exiting an IN clause.
   * The IN clause specifies which notice subtypes the assertion applies to.
   */
  @Override
  public void exitInClause(InClauseContext ctx) {
    logger.debug("Processing IN clause");

    var noticeSubtypes = this.noticeSubtypesOf(ctx.noticeTypeList());
    this.ruleStack.peek(ValidationRule.class).setNoticeSubtypeRange(noticeSubtypes);
    this.validationPlan.addNoticeSubtypes(noticeSubtypes.asList());
  }

  /**
   * Reads the notice subtypes of an IN clause from the names that the parser matched in it. After a
   * syntax error, the parser can keep in a range a token that it skipped, which is not read, or
   * leave a range without one of its names; such a range adds no notice subtype, and the syntax
   * error says what is missing.
   */
  private NoticeSubtypeRange noticeSubtypesOf(NoticeTypeListContext list) {
    if (this.cachedSortedNoticeSubtypeIds == null) {
      this.cachedSortedNoticeSubtypeIds = this.translation.symbols().getAllNoticeSubtypes().stream()
          .sorted()
          .map(SdkNoticeSubtype::getId)
          .collect(Collectors.toUnmodifiableList());
    }
    if (list instanceof AnyNoticeTypesContext) {
      return NoticeSubtypeRange.all(this.cachedSortedNoticeSubtypeIds);
    }
    final NoticeSubtypeRange.Builder noticeSubtypes =
        new NoticeSubtypeRange.Builder(this.cachedSortedNoticeSubtypeIds);
    // After a syntax error the list can be neither of its two alternatives
    if (list instanceof ExplicitNoticeTypesContext) {
      for (final NoticeTypeRangeContext range : ((ExplicitNoticeTypesContext) list).noticeTypeRange()) {
        addNoticeSubtypes(range, noticeSubtypes);
      }
    }
    return noticeSubtypes.build();
  }

  /**
   * Adds the notice subtypes of a range of an IN list, as in {@code 1}, {@code 1-3} or
   * {@code E3-E6}. The lexer reads each notice subtype as one token, so a range has its two ends.
   */
  private static void addNoticeSubtypes(final NoticeTypeRangeContext range,
      final NoticeSubtypeRange.Builder noticeSubtypes) {
    // One notice subtype, or the first and the last of the range. A token that the parser
    // invented to recover from a syntax error is not one of them.
    final List<Token> ends = new ArrayList<>();
    for (final TerminalNode end : range.NoticeType()) {
      if (end instanceof ErrorNode) {
        return;
      }
      ends.add(end.getSymbol());
    }
    if (ends.size() != (range.Minus() == null ? 1 : 2)) {
      return;
    }
    try {
      if (ends.size() == 1) {
        noticeSubtypes.add(ends.get(0).getText());
      } else {
        noticeSubtypes.add(ends.get(0).getText(), ends.get(1).getText());
      }
    } catch (final EfxCompilationException e) {
      throw e.at(ends.get(0));
    }
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
   * The clause is written with an expression or with a behaviour. A rule written with a behaviour
   * has its test already: the listener method of the behaviour gave it to the rule.
   */
  @Override
  public void exitAssertClause(AssertClauseContext ctx) {
    if (ctx.behaviour() == null) {
      this.setTest(this.expressions.valueOf(ctx.expression(), ASSERT).booleanAt(0));
    }
    this.ruleStack.push(new AssertRule(this.ruleStack.pop(ValidationRule.class)));
  }

  @Override
  public void exitReportClause(ReportClauseContext ctx) {
    var test = this.expressions.valueOf(ctx.expression(), REPORT).booleanAt(0);
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
    if (ctx.behaviour() == null) {
      this.setTest(this.expressions.valueOf(ctx.expression(), OTHERWISE_ASSERT).booleanAt(0));
    }
    this.ruleStack.push(new AssertRule(this.ruleStack.pop(ValidationRule.class)));
  }

  @Override
  public void exitOtherwiseReportClause(OtherwiseReportClauseContext ctx) {
    var test =
        this.expressions.valueOf(ctx.expression(), OTHERWISE_REPORT).booleanAt(0);
    var rule = this.ruleStack.pop(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    this.ruleStack.push(new ReportRule(rule));
  }


  /**
   * Gives the rule that is being translated its test. The rule keeps the test also combined with
   * the condition of its WHEN clause, if it has one.
   *
   * @param test the test
   * @return the rule
   */
  private ValidationRule setTest(final BooleanExpression test) {
    var rule = this.ruleStack.peek(ValidationRule.class);
    var invertedCondition = rule.getInvertedCondition();
    rule.setExpression(test, invertedCondition == null ? test : this.combineWithOrParenthesized(test, invertedCondition));
    return rule;
  }

  // #region Behaviours

  // A behaviour is written after ASSERT, or after OTHERWISE ASSERT, in the place of an
  // expression. It names no field or node: it applies to the subject of its rule, which the FOR
  // clause names. Each method below gives the rule its behaviour, and the test that checks it.
  // The test is the one of the expression that the behaviour stands for: it is built with the
  // same built-ins, in the same order, so that both give exactly the same output. In the
  // comments, F is the subject of the rule.

  /** {@code IS NOT EMPTY} stands for {@code F is present}. */
  @Override
  public void exitMandatoryBehaviour(MandatoryBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.of(BehaviourKind.MANDATORY),
        this.call(ctx, ConditionOperators.IS_PRESENT, ReadAs.PATHS, this.subjectOf(ctx)));
  }

  /** {@code IS NOT PRESENT} stands for {@code F is not present}. */
  @Override
  public void exitForbiddenBehaviour(ForbiddenBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.of(BehaviourKind.FORBIDDEN),
        this.call(ctx, ConditionOperators.IS_NOT_PRESENT, ReadAs.PATHS, this.subjectOf(ctx)));
  }

  /** {@code NOT REPEATED} stands for {@code count(F) < 2}. */
  @Override
  public void exitNotRepeatedBehaviour(NotRepeatedBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.of(BehaviourKind.NOT_REPEATED),
        this.compared(ctx, this.countOf(ctx), ComparisonOperators.LESS, this.numberOf("2")));
  }

  /** {@code NOT MODIFIED VIA e} stands for {@code e}. */
  @Override
  public void exitNotModifiedBehaviour(NotModifiedBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.of(BehaviourKind.NOT_MODIFIED), ctx.expression());
  }

  /** {@code NOT ADDED VIA e} stands for {@code e}. */
  @Override
  public void exitNotAddedBehaviour(NotAddedBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.of(BehaviourKind.NOT_ADDED), ctx.expression());
  }

  /** {@code NOT REMOVED VIA e} stands for {@code e}. */
  @Override
  public void exitNotRemovedBehaviour(NotRemovedBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.of(BehaviourKind.NOT_REMOVED), ctx.expression());
  }

  /**
   * {@code OCCURS >= n} stands for {@code count(F) >= n}, and {@code OCCURS BETWEEN a AND b} for
   * {@code count(F) >= a and count(F) <= b}.
   */
  @Override
  public void exitLimitCountBehaviour(LimitCountBehaviourContext ctx) {
    if (ctx.Comparison() != null) {
      this.setLimit(ctx, BehaviourKind.LIMIT_COUNT, this.countOf(ctx),
          ctx.Comparison().getSymbol(), ctx.number.getText());
    } else {
      this.setLimits(ctx, BehaviourKind.LIMIT_COUNT, () -> this.countOf(ctx),
          ctx.lower.getText(), ctx.upper.getText());
    }
  }

  /**
   * {@code LENGTH <= n} stands for {@code string-length(F) <= n}, and
   * {@code LENGTH BETWEEN a AND b} for {@code string-length(F) >= a and string-length(F) <= b}.
   */
  @Override
  public void exitLimitSizeBehaviour(LimitSizeBehaviourContext ctx) {
    if (ctx.Comparison() != null) {
      this.setLimit(ctx, BehaviourKind.LIMIT_SIZE, this.lengthOf(ctx),
          ctx.Comparison().getSymbol(), ctx.number.getText());
    } else {
      this.setLimits(ctx, BehaviourKind.LIMIT_SIZE, () -> this.lengthOf(ctx),
          ctx.lower.getText(), ctx.upper.getText());
    }
  }

  /**
   * {@code VALUE >= n} stands for {@code F >= n}, and {@code VALUE BETWEEN a AND b} for
   * {@code F >= a and F <= b}.
   */
  @Override
  public void exitLimitRangeBehaviour(LimitRangeBehaviourContext ctx) {
    if (ctx.Comparison() != null) {
      this.setLimit(ctx, BehaviourKind.LIMIT_RANGE, this.subjectOf(ctx),
          ctx.Comparison().getSymbol(), ctx.number.getText());
    } else {
      this.setLimits(ctx, BehaviourKind.LIMIT_RANGE, () -> this.subjectOf(ctx),
          ctx.lower.getText(), ctx.upper.getText());
    }
  }

  /**
   * {@code IN [...c]} stands for {@code F in [...c]} where the value of F is a text, and for
   * {@code text(F) in [...c]} where it is a number, an indicator, a date, a time or a duration,
   * which {@code text} converts.
   */
  @Override
  public void exitLimitValuesBehaviour(LimitValuesBehaviourContext ctx) {
    final Subexpression<?> subject = this.subjectOf(ctx);
    final Subexpression<?> value =
        ConversionFunctions.TO_TEXT.resolve(List.of(subject.value())).isPresent()
            ? this.call(ctx, ConversionFunctions.TO_TEXT, ReadAs.VALUES, subject)
            : subject;
    this.setBehaviour(ctx, Behaviour.inCodelist(ctx.codelistReference().codelistName.getText()),
        this.call(ctx, ConditionOperators.IN, ReadAs.OPERANDS, value,
            this.expressions.visit(ctx.codelistReference())));
  }

  /** {@code LIKE 'p'} stands for {@code F:rawValue like 'p'}. */
  @Override
  public void exitMatchPatternBehaviour(MatchPatternBehaviourContext ctx) {
    this.setBehaviour(ctx, Behaviour.matching(patternOf(ctx.pattern.getText())),
        this.call(ctx, PatternMatch.like(ctx.pattern.getText()), ReadAs.VALUES,
            this.expressions.rawValueOf(this.subjectOf(ctx), ctx)));
  }

  /**
   * Reads the regular expression that a pattern is written as. The quotes around the pattern are
   * removed, and a quote written with a backslash, as {@code \'} or {@code \"}, becomes the
   * quote. Any other backslash sequence, such as {@code \.} or {@code \\}, belongs to the
   * regular expression and stays as it is. The test of the rule matches the same regular
   * expression.
   *
   * @param literal the pattern as it is written, with the quotes around it
   * @return the regular expression
   */
  private static String patternOf(final String literal) {
    final String content = literal.substring(1, literal.length() - 1);
    final StringBuilder pattern = new StringBuilder(content.length());
    for (int i = 0; i < content.length(); i++) {
      final char c = content.charAt(i);
      if (c == '\\' && i + 1 < content.length()) {
        final char escaped = content.charAt(++i);
        if (escaped != '\'' && escaped != '"') {
          pattern.append(c);
        }
        pattern.append(escaped);
      } else {
        pattern.append(c);
      }
    }
    return pattern.toString();
  }

  /**
   * Gives the rule that is being translated its behaviour, and the test that checks it.
   *
   * @param ctx the behaviour, at which an error of the test is reported
   * @param behaviour the behaviour, as the rule keeps it
   * @param test the test, built from the subject of the rule
   */
  private void setBehaviour(final ParserRuleContext ctx, final Behaviour behaviour,
      final Subexpression<?> test) {
    this.setTest(this.expressions.valueOf(test.value(), clauseOf(ctx), ctx).booleanAt(0))
        .setBehaviour(behaviour);
  }

  /**
   * Gives the rule that is being translated its behaviour, and the test that checks it: the
   * expression written after VIA, which must be a condition, as the expression of the clause.
   *
   * @param ctx the behaviour
   * @param behaviour the behaviour, as the rule keeps it
   * @param check the expression after VIA
   */
  private void setBehaviour(final ParserRuleContext ctx, final Behaviour behaviour,
      final ExpressionContext check) {
    this.setTest(this.expressions.valueOf(check, clauseOf(ctx)).booleanAt(0))
        .setBehaviour(behaviour);
  }

  /**
   * Gives the rule a limit written with a comparison, as in {@code LENGTH <= 100}, and its test:
   * what is limited, compared with the number.
   *
   * @param ctx the behaviour
   * @param kind the kind of the limit
   * @param limited what is limited: the count, the length or the value of the subject
   * @param comparison the comparison operator, as it is written
   * @param number the number, as it is written
   */
  private void setLimit(final ParserRuleContext ctx, final BehaviourKind kind,
      final Subexpression<?> limited, final Token comparison, final String number) {
    this.setBehaviour(ctx, Behaviour.compared(kind, comparison.getText(), new BigDecimal(number)),
        this.compared(ctx, limited, this.expressions.operatorNamed(comparison),
            this.numberOf(number)));
  }

  /**
   * Gives the rule a limit written with a range, as in {@code VALUE BETWEEN 0 AND 9999}, and its
   * test: what is limited is not below the lower bound and not above the upper bound.
   *
   * @param ctx the behaviour
   * @param kind the kind of the limit
   * @param limited gives what is limited: the count, the length or the value of the subject. It
   *        is read twice, as the expression names it twice.
   * @param lower the lower bound, as it is written
   * @param upper the upper bound, as it is written
   */
  private void setLimits(final ParserRuleContext ctx, final BehaviourKind kind,
      final Supplier<Subexpression<?>> limited, final String lower, final String upper) {
    final Subexpression<?> notBelow = this.compared(ctx, limited.get(),
        ComparisonOperators.GREATER_OR_EQUAL, this.numberOf(lower));
    final Subexpression<?> notAbove = this.compared(ctx, limited.get(),
        ComparisonOperators.LESS_OR_EQUAL, this.numberOf(upper));
    this.setBehaviour(ctx, Behaviour.between(kind, new BigDecimal(lower), new BigDecimal(upper)),
        this.call(ctx, LogicalOperators.AND, ReadAs.OPERANDS, notBelow, notAbove));
  }

  /**
   * @param behaviour a behaviour
   * @return the clause it is written in, as a type error names it
   */
  private static Clause clauseOf(final ParserRuleContext behaviour) {
    return behaviour.getParent() instanceof OtherwiseAssertClauseContext ? OTHERWISE_ASSERT
        : ASSERT;
  }

  /**
   * @param behaviour a behaviour
   * @return the subject of its rule, which the FOR clause names, read from the context of the
   *         rule set as an expression reads a field or a node
   */
  private Subexpression<?> subjectOf(final ParserRuleContext behaviour) {
    final ForClauseContext subject =
        behaviour.getParent().getParent().getRuleContext(ForClauseContext.class, 0);
    return this.expressions.visit(subject.simpleFieldReference() != null
        ? subject.simpleFieldReference()
        : subject.simpleNodeReference());
  }

  private Subexpression<?> countOf(final ParserRuleContext behaviour) {
    return this.call(behaviour, NumericFunctions.COUNT, ReadAs.VALUES, this.subjectOf(behaviour));
  }

  private Subexpression<?> lengthOf(final ParserRuleContext behaviour) {
    return this.call(behaviour, StringFunctions.STRING_LENGTH, ReadAs.VALUES,
        this.subjectOf(behaviour));
  }

  private Subexpression<?> numberOf(final String number) {
    return this.expressions
        .subexpression(this.translation.script().getNumericLiteralEquivalent(number));
  }

  private Subexpression<?> compared(final ParserRuleContext behaviour,
      final Subexpression<?> left, final BuiltIn comparison, final Subexpression<?> right) {
    return this.call(behaviour, comparison, ReadAs.OPERANDS, left, right);
  }

  /**
   * Calls a built-in as the expression that a behaviour stands for calls it.
   *
   * @param behaviour the behaviour, at which an error of the call is reported
   * @param builtIn the built-in
   * @param readAs how the call reads its arguments
   * @param arguments the arguments of the call
   * @return the result of the call
   */
  private Subexpression<?> call(final ParserRuleContext behaviour, final BuiltIn builtIn,
      final ReadAs readAs, final Subexpression<?>... arguments) {
    return this.expressions.builtIn(behaviour, builtIn, builtIn.getSpelling(),
        List.of(arguments), readAs);
  }

  // #endregion Behaviours

  private BooleanExpression combineWithOrParenthesized(BooleanExpression left, BooleanExpression right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return this.translation.script().composeLogicalOr(
        this.translation.script().composeParenthesizedExpression(left, BooleanExpression.class),
        this.translation.script().composeParenthesizedExpression(right, BooleanExpression.class));
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
      combined = (combined == null) ? rule.getInvertedCondition() : this.translation.script().composeLogicalAnd(combined, rule.getInvertedCondition());
    }
    var invertedCombined = this.translation.script().composeLogicalNot(combined);

    fallbackRule.setCondition(combined, invertedCombined, this.combineWithOrParenthesized(fallbackRule.getExpression(), invertedCombined));

    ruleSet.setFallbackRule(fallbackRule);
    this.ruleStack.push(ruleSet);
  }

  // #region Dynamic Function Declarations

  private static final java.util.regex.Pattern VALID_ENDPOINT_NAME = java.util.regex.Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]*");

  @Override
  public void exitApiEndpointDeclaration(ApiEndpointDeclarationContext ctx) {
    String name = unquote(ctx.endpointName.getText());
    if (!VALID_ENDPOINT_NAME.matcher(name).matches()) {
      throw InvalidUsageException.invalidEndpointName(ctx, name);
    }
    String url = ctx.endpointUrl != null ? unquote(ctx.endpointUrl.getText()) : null;
    this.validationPlan.declareEndpoint(name, url);
  }

  @Override
  public void enterDynamicFunctionDeclaration(DynamicFunctionDeclarationContext ctx) {
    this.translation.stack().pushStackFrame();
    this.translation.stack().push(new ParsedParameters());
  }

  /**
   * Declares a dynamic function. The function is declared before the scope of its parameters
   * closes, so it cannot take the name of one of its parameters.
   */
  @Override
  public void exitDynamicFunctionDeclaration(DynamicFunctionDeclarationContext ctx) {
    ParsedParameters params = this.translation.stack().pop(ParsedParameters.class);
    String apiName = ctx.functionName.getText();
    String endpointName = ctx.endpointName != null ? unquote(ctx.endpointName.getText()) : "default";
    if (!this.validationPlan.getEndpoints().containsKey(endpointName)) {
      throw InvalidIdentifierException.undeclaredEndpoint(ctx,
          Function.spellingOf(apiName).describe(), endpointName);
    }
    RuleSeverity errorSeverity = ctx.onErrorClause() != null && ctx.onErrorClause().Warn() != null
        ? RuleSeverity.WARNING : RuleSeverity.ERROR;
    String errorLabel = ctx.onErrorClause() != null && ctx.onErrorClause().errorLabel != null
        ? unquote(ctx.onErrorClause().errorLabel.getText()) : null;
    this.translation.stack().declareFunction(new DynamicFunction(apiName, params, endpointName, errorSeverity, errorLabel));
    this.translation.stack().popStackFrame();
  }

  private int apiCallCounter = 0;

  /**
   * Handles an inline dynamic function call in a rule expression.
   * Creates an auto-generated dynamic variable on the current ValidationRule and returns
   * a boolean reference expression ($varName = 1).
   */
  private BooleanExpression composeAutoGeneratedDynamicVariable(FunctionInvocationContext ctx,
      DynamicFunction apiFunc) {
    // Compose the raw dynamic function call.
    var call = this.expressions.invocationOf(ctx, apiFunc);
    if (!this.ruleStack.contains(ValidationRule.class)) {
      throw InvalidUsageException.dynamicFunctionOutsideRule(ctx, apiFunc);
    }

    // Compose the variable's expressions: declaration, initialization, and success check ($var == 1).
    String varName = Translation.RESERVED_PREFIX + "apiResult" + (++this.apiCallCounter);
    var declaration = this.translation.script().composeVariableDeclaration(varName,
        DynamicExpression.class);
    var initExpression = TypedExpression.from(call.compose(this.translation.script()),
        DynamicExpression.class);
    var varRef = this.translation.script().composeVariableReference(varName,
        NumericExpression.class);
    var one = this.translation.script().getNumericLiteralEquivalent("1");
    var referenceExpression = this.translation.script().composeComparisonOperation(varRef, "==",
        one);

    // Create the auto-generated variable and register it on the current rule.
    var rule = this.ruleStack.peek(ValidationRule.class);
    int index = rule.getAutoGeneratedVariables().size() + 1;
    var variable = new DynamicVariable.AutoGenerated(index, varName, declaration,
        initExpression, referenceExpression, apiFunc.endpointName, apiFunc.errorSeverity,
        apiFunc.errorLabel, this.translatingCondition);
    rule.addAutoGeneratedVariable(variable);

    // Return the success check so it can be used in the rule's condition.
    return referenceExpression;
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


  /**
   * Translates the expressions of a rules file. A call of an API function inside a rule becomes an
   * auto-generated dynamic variable of the rule, and a variable that a variable initializer reads
   * is recorded among the dynamic dependencies of the variable it initializes.
   */
  private final class RulesExpressionVisitor extends EfxExpressionVisitor {

    RulesExpressionVisitor(final Translation translation) {
      super(translation);
    }

    /**
     * Translates a call of a function as a call of an API function, since a rules file declares no
     * other kind of function.
     */
    @Override
    public Subexpression<?> visitFunctionInvocation(final FunctionInvocationContext ctx) {
      final DynamicFunction function = (DynamicFunction) EfxRulesTranslatorV2.this.translation
          .stack().getFunction(ctx.functionName.getText());
      return this.subexpression(
          EfxRulesTranslatorV2.this.composeAutoGeneratedDynamicVariable(ctx, function));
    }

    @Override
    public Subexpression<?> visitVariableReference(final VariableReferenceContext ctx) {
      EfxRulesTranslatorV2.this.trackDynamicDependenciesOf(ctx.variableName.getText());
      return super.visitVariableReference(ctx);
    }
  }
}
