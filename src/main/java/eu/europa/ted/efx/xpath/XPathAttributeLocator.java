package eu.europa.ted.efx.xpath;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.xpath.XPath20Parser.AbbrevforwardstepContext;
import eu.europa.ted.efx.xpath.XPath20Parser.PredicateContext;

/**
 * Uses the {@link XPath20Parser} to extract an attribute from an XPath expression. Arguably one
 * could try to do the same thing using regular expressions, however, with the parser we can handle
 * correctly any XPath expression regardless of how complicated it maybe.
 * 
 * The reason we need to examine if an XPath expression points to an attribute is because some
 * eForms Fields represent attribute values. This means that these attributes are effectively hidden
 * behind a Field identifier and cannot be visible by the lexical analyzer or the parser. They are
 * only visible to the translator after dereferencing such Field identifiers. At this point, the
 * translator relies on this class to detect the presence of such attributes and translate
 * accordingly.
 */
public class XPathAttributeLocator extends XPath20BaseListener {

  private int inPredicate = 0;
  private int splitPosition = -1;
  private PathExpression path;
  private String attribute;

  public PathExpression getPath() {
    return path;
  }

  public String getAttribute() {
    return attribute;
  }

  public Boolean hasAttribute() {
    return attribute != null;
  }

  @Override
  public void enterPredicate(PredicateContext ctx) {
    this.inPredicate++;
  }

  @Override
  public void exitPredicate(PredicateContext ctx) {
    this.inPredicate--;
  }

  @Override
  public void exitAbbrevforwardstep(AbbrevforwardstepContext ctx) {
    if (this.inPredicate == 0 && ctx.AT() != null) {
      this.splitPosition = ctx.AT().getSymbol().getCharPositionInLine();
      this.attribute = ctx.nodetest().getText();
    }
  }

  public static XPathAttributeLocator findAttribute(final PathExpression xpath) {

    final XPathAttributeLocator locator = new XPathAttributeLocator();

    if (!xpath.script.contains("@")) {
      locator.path = xpath;
      locator.attribute = null;
      return locator;
    }

    final CharStream inputStream = CharStreams.fromString(xpath.script);
    final XPath20Lexer lexer = new XPath20Lexer(inputStream);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final XPath20Parser parser = new XPath20Parser(tokens);
    final ParseTree tree = parser.xpath();
    final ParseTreeWalker walker = new ParseTreeWalker();

    walker.walk(locator, tree);

    if (locator.splitPosition > -1) {
      // The attribute we are looking for is at splitPosition
      String path = xpath.script.substring(0, locator.splitPosition);
      while (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      locator.path = new PathExpression(path);
    } else {
      // the XPAth does not point to an attribute
      locator.path = xpath;
    }

    return locator;
  }
}
