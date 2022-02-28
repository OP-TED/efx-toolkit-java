package eu.europa.ted.efx.xpath;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Stack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.AbstractXpathBaseListener;
import eu.europa.ted.efx.AbstractXpathLexer;
import eu.europa.ted.efx.AbstractXpathParser;
import eu.europa.ted.efx.AbstractXpathParser.PathContext;
import eu.europa.ted.efx.AbstractXpathParser.StepContext;

public class XpathContextualizer extends AbstractXpathBaseListener {

  private final Stack<Queue<String>> stack = new Stack<>();

  public XpathContextualizer() {}

  public static String contextualize(final String contextXpath, final String xpath) {

    final AbstractXpathLexer lexer = new AbstractXpathLexer(
        CharStreams.fromString(String.format("%s, %s", contextXpath, xpath)));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final AbstractXpathParser parser = new AbstractXpathParser(tokens);
    final ParseTree tree = parser.pair();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final XpathContextualizer contextualizer = new XpathContextualizer();
    walker.walk(contextualizer, tree);

    return contextualizer.getContextualizedXpath();
  }

  public String getContextualizedXpath() {
    final StringBuilder result = new StringBuilder();

    while (!this.stack.isEmpty()) {

      // We will store the relative xPath here as we build it.
      String relativeXpath = "";

      // This queue contains the steps of the xPath we want to contextualize.
      final Queue<String> pathQueue = this.stack.pop();

      // This queue contains the steps of the context xPath.
      // If the syntax of the file was correct the stack should not be empty at this point.
      final Queue<String> contextQueue = this.stack.isEmpty() ? null : this.stack.pop();

      if (contextQueue != null) {

        // First we will "consume" all nodes that are the same in both xPaths.
        while (!contextQueue.isEmpty() && !pathQueue.isEmpty()
            && Objects.equals(contextQueue.peek(), pathQueue.peek())) {
          contextQueue.poll();
          pathQueue.poll();
        }

        // At this point there are no more matching nodes in the two queues.

        // We start building the resulting relativeXpath by appending any nodes remaining in the
        // pathQueue.
        while (!pathQueue.isEmpty()) {
          relativeXpath += pathQueue.poll();
        }

        // We remove any leading forward slashes from the resulting xPath.
        while (relativeXpath.startsWith("/")) {
          relativeXpath = relativeXpath.substring(1);
        }

        // For each step remaining in the contextQueue we prepend a back-step (..) in the resulting
        // relativeXpath.
        while (!contextQueue.isEmpty()) {
          if (!contextQueue.poll().startsWith("/@")) { // consume the step
            relativeXpath = "../" + relativeXpath; // prepend a backstep if the step is not an
                                                   // attribute reference
          }
        }

        // The relativeXpath will be empty if the path was identical to the context.
        // In this case we return a dot.
        if (relativeXpath.isEmpty()) {
          relativeXpath = ".";
        }
      }

      // As we use a stack we are traversing the lines of xPath pairs in the
      // reverse order. Therefore we prepend the preciding lines in the result.
      result.append(relativeXpath).append(result.isEmpty() ? "" : ("\n" + result));

    }
    return result.toString();
  }


  @Override
  public void enterPath(PathContext ctx) {
    this.stack.push(new LinkedList<String>());
  }

  @Override
  public void exitStep(StepContext ctx) {
    this.stack.peek().offer(ctx.getText());
  }
}
