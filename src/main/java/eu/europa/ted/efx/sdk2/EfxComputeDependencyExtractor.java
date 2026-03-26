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
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.dependencies.DependencySet;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * Extracts field and node identifiers referenced in an EFX single expression.
 *
 * This listener walks the parse tree and collects every field and node identifier it encounters
 * into a stack of {@link DependencySet} frames. Identifiers used as aliases are resolved through
 * the {@link SymbolResolver}.
 *
 * Subclasses push and pop frames at the appropriate boundaries to group dependencies by scope.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_COMPUTE_DEPENDENCY_EXTRACTOR)
public class EfxComputeDependencyExtractor extends EfxBaseListener
    implements eu.europa.ted.efx.interfaces.EfxComputeDependencyExtractor {

  protected final SymbolResolver symbols;
  protected final BaseErrorListener errorListener;
  protected final Deque<DependencySet> stack = new ArrayDeque<>();

  public EfxComputeDependencyExtractor(final SymbolResolver symbolResolver,
      final BaseErrorListener errorListener) {
    this.symbols = symbolResolver;
    this.errorListener = errorListener;
  }

  @Override
  public Set<String> extractDependencies(final String expression) {
    this.stack.clear();
    this.stack.push(new DependencySet());

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(expression));
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
      return;
    }

    final TerminalNode alias = ctx.Identifier();
    if (alias != null) {
      this.resolveAlias(alias.getText());
    }
  }

  @Override
  public void enterSimpleFieldReference(final SimpleFieldReferenceContext ctx) {
    final TerminalNode fieldId = ctx.FieldId();
    if (fieldId != null) {
      this.stack.peek().addField(fieldId.getText());
      return;
    }

    final TerminalNode alias = ctx.Identifier();
    if (alias != null) {
      this.resolveAlias(alias.getText());
    }
  }

  @Override
  public void enterFieldMention(final FieldMentionContext ctx) {
    final TerminalNode fieldId = ctx.FieldId();
    if (fieldId != null) {
      this.stack.peek().addField(fieldId.getText());
      return;
    }

    final TerminalNode alias = ctx.Identifier();
    if (alias != null) {
      this.resolveAlias(alias.getText());
    }
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

  protected void resolveAlias(final String alias) {
    final String fieldId = this.symbols.getFieldIdFromAlias(alias);
    if (fieldId != null) {
      this.stack.peek().addField(fieldId);
      return;
    }

    final String nodeId = this.symbols.getNodeIdFromAlias(alias);
    if (nodeId != null) {
      this.stack.peek().addNode(nodeId);
      return;
    }

    throw SymbolResolutionException.unknownSymbol(alias);
  }
}
