package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.nio.file.Path;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.dependencies.DependencyGraph;
import eu.europa.ted.efx.model.dependencies.DependencySet;
import eu.europa.ted.efx.model.dependencies.RuleDependency;
import eu.europa.ted.efx.model.dependencies.TargetDependencies;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * Extracts field and node dependencies from an EFX rules file and builds a
 * {@link DependencyGraph}.
 *
 * Uses the inherited dependency stack from {@link EfxComputeDependencyExtractor}:
 * <ul>
 *   <li>{@code enterRuleSet} pushes a frame for ruleSet-level (WITH clause) dependencies.</li>
 *   <li>{@code enterSimpleRule} / {@code enterConditionalRule} / {@code enterFallbackRule}
 *       push a rule frame seeded with the ruleSet dependencies.</li>
 *   <li>Field/node references during the rule's expressions accumulate on the rule frame.</li>
 *   <li>{@code exitSimpleRule} / etc. pop the rule frame, extract target and rule ID from the
 *       context, and add the result to the graph.</li>
 *   <li>{@code exitRuleSet} pops the ruleSet frame.</li>
 * </ul>
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_VALIDATION_DEPENDENCY_EXTRACTOR)
public class EfxValidationDependencyExtractor extends EfxComputeDependencyExtractor
    implements eu.europa.ted.efx.interfaces.EfxValidationDependencyExtractor {

  private DependencyGraph graph;

  public EfxValidationDependencyExtractor(final SymbolResolver symbolResolver,
      final BaseErrorListener errorListener) {
    super(symbolResolver, errorListener);
  }

  // #region Public API --------------------------------------------------------

  @Override
  public DependencyGraph extractDependencyGraph(final String rules) {
    return this.extractFromCharStream(CharStreams.fromString(rules));
  }

  @Override
  public DependencyGraph extractDependencyGraph(final Path pathname) throws IOException {
    final Path baseDir = pathname.toAbsolutePath().getParent();
    final IncludedFileResolver resolver = new FileSystemIncludedFileResolver(baseDir);
    final CharStream raw = CharStreams.fromPath(pathname);
    final CharStream resolved = new IncludeProcessor(resolver).resolve(raw);
    return this.extractFromCharStream(resolved);
  }

  // #endregion Public API

  // #region Parsing -----------------------------------------------------------

  private DependencyGraph extractFromCharStream(final CharStream input) {
    this.graph = new DependencyGraph();
    this.stack.clear();
    this.stack.push(new DependencySet());

    final EfxLexer lexer = new EfxLexer(input);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    parser.setErrorHandler(new EfxErrorStrategy());

    if (this.errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(this.errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(this.errorListener);
    }

    final ParseTree tree = parser.rulesFile();
    new ParseTreeWalker().walk(this, tree);

    this.graph.computeRequiredBy();
    return this.graph;
  }

  // #endregion Parsing

  // #region Scope lifecycle ----------------------------------------------------

  @Override
  public void enterValidationStage(final ValidationStageContext ctx) {
    final DependencySet stageFrame = new DependencySet();
    stageFrame.addAll(this.stack.peek());
    this.stack.push(stageFrame);
  }

  @Override
  public void exitValidationStage(final ValidationStageContext ctx) {
    this.stack.pop();
  }

  @Override
  public void enterRuleSet(final RuleSetContext ctx) {
    final DependencySet ruleSetFrame = new DependencySet();
    ruleSetFrame.addAll(this.stack.peek());
    this.stack.push(ruleSetFrame);
  }

  @Override
  public void exitRuleSet(final RuleSetContext ctx) {
    this.stack.pop();
  }

  // #endregion Scope lifecycle

  // #region Rule lifecycle ----------------------------------------------------

  @Override
  public void enterSimpleRule(final SimpleRuleContext ctx) {
    this.pushRuleFrame();
  }

  @Override
  public void exitSimpleRule(final SimpleRuleContext ctx) {
    this.commitRule(ctx.asClause(), ctx.forClause());
  }

  @Override
  public void enterConditionalRule(final ConditionalRuleContext ctx) {
    this.pushRuleFrame();
  }

  @Override
  public void exitConditionalRule(final ConditionalRuleContext ctx) {
    this.commitRule(ctx.asClause(), ctx.forClause());
  }

  @Override
  public void enterFallbackRule(final FallbackRuleContext ctx) {
    this.pushRuleFrame();
  }

  @Override
  public void exitFallbackRule(final FallbackRuleContext ctx) {
    this.commitRule(ctx.asClause(), ctx.forClause());
  }

  private void pushRuleFrame() {
    final DependencySet ruleFrame = new DependencySet();
    ruleFrame.addAll(this.stack.peek());
    this.stack.push(ruleFrame);
  }

  private void commitRule(final AsClauseContext asClause, final ForClauseContext forClause) {
    final DependencySet ruleDeps = this.stack.pop();
    final String ruleId = asClause.ruleId().getText().replaceAll("^\"|\"$", "");

    final String targetId;
    final boolean targetIsField;

    if (forClause.simpleFieldReference() != null) {
      targetId = forClause.simpleFieldReference().FieldId().getText();
      targetIsField = true;
    } else {
      targetId = forClause.simpleNodeReference().NodeId().getText();
      targetIsField = false;
    }

    ruleDeps.removeField(targetId);
    ruleDeps.removeNode(targetId);

    if (ruleDeps.isEmpty()) {
      return;
    }

    final TargetDependencies target = targetIsField
        ? this.graph.getOrCreateFieldEntry(targetId)
        : this.graph.getOrCreateNodeEntry(targetId);

    target.addAssertDependency(new RuleDependency(ruleId, ruleDeps.getFieldIds(), ruleDeps.getNodeIds()));
  }

  // #endregion Rule lifecycle
}
