package eu.europa.ted.efx.sdk2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.dependencies.DependencySet;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * Extracts field and node identifiers referenced in an EFX single expression.
 *
 * This listener walks the parse tree and collects every field and node identifier it encounters
 * into a stack of {@link DependencySet} frames.
 *
 * Subclasses push and pop frames at the appropriate boundaries to group dependencies by scope.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_COMPUTE_DEPENDENCY_EXTRACTOR)
public class EfxComputeDependencyExtractor extends EfxBaseListener
    implements eu.europa.ted.efx.interfaces.EfxComputeDependencyExtractor {

  protected final BaseErrorListener errorListener;
  protected final Deque<DependencySet> stack = new ArrayDeque<>();

  /**
   * @param symbolResolver not used: identifiers are collected as written; the component factory
   *        passes it to every EFX component
   * @param errorListener the listener told of syntax errors, or {@code null}
   */
  public EfxComputeDependencyExtractor(final SymbolResolver symbolResolver,
      final BaseErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public Set<String> extractDependencies(final String expression) {
    this.stack.clear();
    this.stack.push(new DependencySet());

    final EfxLexer lexer = new EfxErrorReportingLexer(CharStreams.fromString(expression));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    parser.setErrorHandler(new EfxErrorStrategy());

    if (this.errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(this.errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(this.errorListener);
    }

    final ParseTree tree = parser.singleExpression();
    new ParseTreeWalker().walk(this, tree);

    return this.stack.pop().allIds();
  }

  // #region Listener methods --------------------------------------------------

  @Override
  public void enterSingleExpression(final SingleExpressionContext ctx) {
    final TerminalNode fieldId = ctx.FieldId();
    if (fieldId != null) {
      this.stack.peek().addField(fieldId.getText());
      return;
    }

    final TerminalNode nodeId = ctx.NodeId();
    if (nodeId != null) {
      this.stack.peek().addNode(nodeId.getText());
    }
  }

  @Override
  public void enterSimpleFieldReference(final SimpleFieldReferenceContext ctx) {
    this.stack.peek().addField(ctx.FieldId().getText());
  }

  @Override
  public void enterFieldMention(final FieldMentionContext ctx) {
    this.stack.peek().addField(ctx.FieldId().getText());
  }

  @Override
  public void enterSimpleNodeReference(final SimpleNodeReferenceContext ctx) {
    this.stack.peek().addNode(ctx.NodeId().getText());
  }

  @Override
  public void enterCodelistReference(final CodelistReferenceContext ctx) {
    this.stack.peek().addCodelist(ctx.codelistName.getText());
  }

  // #endregion Listener methods
}
