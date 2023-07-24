package eu.europa.ted.efx.xpath;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.path.NodePathExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
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
  private String path;
  private String attribute;

  /**
   * Gets the XPath to the XML element that contains the attribute.
   * The returned XPath therefore does not contain the attribute itself.
   * 
   * @return A {@link NodePathExpression} pointing to the XML element that contains the attribute.
   */
  public NodePathExpression getElementPath() {
    return Expression.instantiate(path, NodePathExpression.class);
  }

  /** 
   * Gets the name of the attribute (without the @ prefix).
   * If the parsed XPath did not point to an attribute, then this method returns null.
   * 
   * @return The name of the attribute (or null if the parsed XPath did not point to an attribute).
   */
  public String getAttributeName() {
    return StringUtils.isBlank(attribute) ? null : attribute;
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
    return findAttribute(xpath.getScript());
  }

  public static XPathAttributeLocator findAttribute(final String xpath) {

    final XPathAttributeLocator locator = new XPathAttributeLocator();

    if (!xpath.contains("@")) {
      locator.path = xpath;
      locator.attribute = null;
      return locator;
    }

    final CharStream inputStream = CharStreams.fromString(xpath);
    final XPath20Lexer lexer = new XPath20Lexer(inputStream);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final XPath20Parser parser = new XPath20Parser(tokens);
    final ParseTree tree = parser.xpath();
    final ParseTreeWalker walker = new ParseTreeWalker();

    walker.walk(locator, tree);

    if (locator.splitPosition > -1) {
      // The attribute we are looking for is at splitPosition
      String path = xpath.substring(0, locator.splitPosition);
      while (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      locator.path = path;
    } else {
      // the XPAth does not point to an attribute
      locator.path = xpath;
    }

    return locator;
  }
}
