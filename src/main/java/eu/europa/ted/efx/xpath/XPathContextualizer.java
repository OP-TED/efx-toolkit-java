package eu.europa.ted.efx.xpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.xpath.XPath20Parser.PredicateContext;
import eu.europa.ted.efx.xpath.XPath20Parser.StepexprContext;

public class XPathContextualizer extends XPath20BaseListener {

  private final CharStream inputStream;
  private final Queue<StepInfo> steps = new LinkedList<>();

  public XPathContextualizer(CharStream inputStream) {
    this.inputStream = inputStream;
  }

  private static Queue<StepInfo> getSteps(PathExpression xpath) {

    final CharStream inputStream = CharStreams.fromString(xpath.script);
    final XPath20Lexer lexer = new XPath20Lexer(inputStream);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final XPath20Parser parser = new XPath20Parser(tokens);
    final ParseTree tree = parser.xpath();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final XPathContextualizer contextualizer = new XPathContextualizer(inputStream);
    walker.walk(contextualizer, tree);

    return contextualizer.steps;
  }


  public static PathExpression contextualize(final PathExpression contextXpath,
      final PathExpression xpath) {

    // If we are asked to contextualise against a null or empty context
    // then we must return the original xpath (instead of throwing an exception).
    if (contextXpath == null || contextXpath.script.isEmpty()) {
      return xpath;
    }

    Queue<StepInfo> contextSteps = new LinkedList<StepInfo>(getSteps(contextXpath));
    Queue<StepInfo> pathSteps = new LinkedList<StepInfo>(getSteps(xpath));

    return getContextualizedXpath(contextSteps, pathSteps);
  }

  public static PathExpression join(final PathExpression first, final PathExpression second) {

    if (first == null || first.script.trim().isEmpty()) {
      return second;
    }

    if (second == null || second.script.trim().isEmpty()) {
      return first;
    }

    LinkedList<StepInfo> firstPartSteps = new LinkedList<>(getSteps(first));
    LinkedList<StepInfo> secondPartSteps = new LinkedList<>(getSteps(second));

    return getJoinedXPath(firstPartSteps, secondPartSteps);
  }

  public static PathExpression addAxis(String axis, PathExpression path) {
    LinkedList<StepInfo> steps = new LinkedList<>(getSteps(path));

    while (steps.getFirst().stepText.equals("..")) {
      steps.removeFirst();
    }

    return new PathExpression(axis + "::" + steps.stream().map(s -> s.stepText).collect(Collectors.joining("/")));
  }

  private static PathExpression getContextualizedXpath(Queue<StepInfo> contextQueue,
      final Queue<StepInfo> pathQueue) {

    // We will store the relative xPath here as we build it.
    String relativeXpath = "";

    if (contextQueue != null) {

      // First we will "consume" all nodes that are the same in both xPaths.
      while (!contextQueue.isEmpty() && !pathQueue.isEmpty()
          && pathQueue.peek().isTheSameAs(contextQueue.peek())) {
        contextQueue.poll();
        pathQueue.poll();
      }

      // At this point there are no more matching nodes in the two queues.

      // We start building the resulting relativeXpath by appending any nodes
      // remaining in the
      // pathQueue.
      while (!pathQueue.isEmpty()) {
        final StepInfo step = pathQueue.poll();
        relativeXpath += "/" + step.stepText + step.getPredicateText();
      }

      // We remove any leading forward slashes from the resulting xPath.
      while (relativeXpath.startsWith("/")) {
        relativeXpath = relativeXpath.substring(1);
      }

      // For each step remaining in the contextQueue we prepend a back-step (..) in
      // the resulting relativeXpath.
      while (!contextQueue.isEmpty()) {
        if (!contextQueue.poll().isAttributeStep()) { // consume the step
          relativeXpath = "../" + relativeXpath; // prepend a back-step if the step is
                                                 // not an
                                                 // attribute reference
        }
      }

      // We remove any trailing forward slashes from the resulting xPath.
      while (relativeXpath.endsWith("/")) {
        relativeXpath = relativeXpath.substring(0, relativeXpath.length() - 1);
      }


      // The relativeXpath will be empty if the path was identical to the context.
      // In this case we return a dot.
      if (relativeXpath.isEmpty()) {
        relativeXpath = ".";
      }
    }

    return new PathExpression(relativeXpath);
  }


  private static PathExpression getJoinedXPath(LinkedList<StepInfo> first,
      final LinkedList<StepInfo> second) {
    List<String> dotSteps = Arrays.asList("..", ".");
    while (second.getFirst().stepText.equals("..")
        && !dotSteps.contains(first.getLast().stepText) && !first.getLast().isVariableStep()) {
      second.removeFirst();
      first.removeLast();
    }

    return new PathExpression(first.stream().map(f -> f.stepText).collect(Collectors.joining("/"))
        + "/" + second.stream().map(s -> s.stepText).collect(Collectors.joining("/")));
  }

  /**
   * Helper method that returns the input text that matched a parser rule context. It is useful
   * because {@link ParserRuleContext#getText()} omits whitespace and other lexer tokens in the
   * HIDDEN channel.
   *
   * @param context
   * @return
   */
  private String getInputText(ParserRuleContext context) {
    return this.inputStream
        .getText(new Interval(context.start.getStartIndex(), context.stop.getStopIndex()));
  }

  int predicateMode = 0;

  private Boolean inPredicateMode() {
    return predicateMode > 0;
  }

  @Override
  public void exitStepexpr(StepexprContext ctx) {
    if (!inPredicateMode()) {
      this.steps.offer(new StepInfo(ctx, this::getInputText));
    }
  }

  @Override
  public void enterPredicate(PredicateContext ctx) {
    this.predicateMode++;
  }

  @Override
  public void exitPredicate(PredicateContext ctx) {
    this.predicateMode--;
  }

  private class StepInfo {
    String stepText;
    List<String> predicates;

    public StepInfo(StepexprContext ctx, Function<ParserRuleContext, String> getInputText) {
      this.stepText = getInputText.apply(ctx.step());
      this.predicates =
          ctx.predicatelist().predicate().stream().map(getInputText).collect(Collectors.toList());
    }

    public Boolean isAttributeStep() {
      return this.stepText.startsWith("/@");
    }

    public Boolean isVariableStep() {
      return this.stepText.startsWith("$");
    }

    public String getPredicateText() {
      return String.join("", this.predicates);
    }

    public Boolean isTheSameAs(final StepInfo contextStep) {

      // First check the step texts are the different.
      if (!Objects.equals(contextStep.stepText, this.stepText)) {
        return false;
      }

      // If one of the two steps has more predicates that the other,
      if (this.predicates.size() != contextStep.predicates.size()) {
        // then the steps are the same is the path has no predicates
        // or all the predicates of the path are also found in the context.
        return this.predicates.isEmpty() || contextStep.predicates.containsAll(this.predicates);
      }

      // If there are no predicates then the steps are the same.
      if (this.predicates.isEmpty()) {
        return true;
      }

      // If there is only one predicate in each step, then we can do a quick comparison.
      if (this.predicates.size() == 1) {
        return Objects.equals(contextStep.predicates.get(0), this.predicates.get(0));
      }

      // Both steps contain multiple predicates.
      // We need to compare them one by one.
      // First we make a copy so that we can sort them without affecting the original lists.
      List<String> pathPredicates = new ArrayList<>(this.predicates);
      List<String> contextPredicates = new ArrayList<>(contextStep.predicates);
      Collections.sort(pathPredicates);
      Collections.sort(contextPredicates);
      return pathPredicates.equals(contextPredicates);
    }
  }
}
