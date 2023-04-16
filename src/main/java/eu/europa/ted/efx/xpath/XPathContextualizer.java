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
import eu.europa.ted.efx.xpath.XPath20Parser.AxisstepContext;
import eu.europa.ted.efx.xpath.XPath20Parser.FilterexprContext;
import eu.europa.ted.efx.xpath.XPath20Parser.PredicateContext;

public class XPathContextualizer extends XPath20BaseListener {

  private final CharStream inputStream;
  private final LinkedList<StepInfo> steps = new LinkedList<>();

  public XPathContextualizer(CharStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * Parses the XPath represented by th e given {@link PathExpression}} and
   * returns a queue containing a {@link StepInfo} object for each step that the
   * XPath is comprised of.
   */
  private static Queue<StepInfo> getSteps(PathExpression xpath) {
    return getSteps(xpath.script);
  }

  /**
   * Parses the given xpath and returns a queue containing a {@link StepInfo} for
   * each step that the XPath is comprised of.
   */
  private static Queue<StepInfo> getSteps(String xpath) {

    final CharStream inputStream = CharStreams.fromString(xpath);
    final XPath20Lexer lexer = new XPath20Lexer(inputStream);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final XPath20Parser parser = new XPath20Parser(tokens);
    final ParseTree tree = parser.xpath();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final XPathContextualizer contextualizer = new XPathContextualizer(inputStream);
    walker.walk(contextualizer, tree);

    return contextualizer.steps;
  }

  /**
   * Makes the given xpath relative to the given context xpath.
   * @param contextXpath
   * @param xpath
   * @return
   */
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

  public static PathExpression addPredicate(final PathExpression pathExpression, final String predicate) {
    return new PathExpression(addPredicate(pathExpression.script, predicate));
  }

  /**
   * Attempts to add a predicate to the given xpath.
   * It will add the predicate to the last axis-step in the xpath.
   * If there is no axis-step in the xpath then it will add the predicate to the last step.
   * If the xpath is empty then it will still return a PathExpression but with an empty xpath.
   */
  public static String addPredicate(final String xpath, final String predicate) {
    if (predicate == null) {
      return xpath;
    }
    
    String _predicate = predicate.trim();

    if (_predicate.isEmpty()) {
      return xpath;
    }

    if (!_predicate.startsWith("[")) {
      _predicate = "[" + _predicate;
    }

    if (!_predicate.endsWith("]")) {
      _predicate = _predicate + "]";
    }

    LinkedList<StepInfo> steps = new LinkedList<>(getSteps(xpath));

    StepInfo lastAxisStep = getLastAxisStep(steps);
    if (lastAxisStep != null) {
      lastAxisStep.predicates.add(_predicate); 
    } else if (steps.size() > 0) {
      steps.getLast().predicates.add(_predicate);
    }
    return steps.stream().map(s -> s.stepText + s.getPredicateText()).collect(Collectors.joining("/"));
  }

  private static StepInfo getLastAxisStep(LinkedList<StepInfo> steps) {
    int i = steps.size() - 1;
    while (i >= 0 && !AxisStepInfo.class.isInstance(steps.get(i))) {
      i--;
    }
    if (i < 0) {
      return null;
    }
    return steps.get(i);
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
        contextQueue.poll();                    // consume the step
        relativeXpath = "../" + relativeXpath;  // prepend a back-step 
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
  public void exitAxisstep(AxisstepContext ctx) {
    if (inPredicateMode()) {
      return;
    }

    // When we recognize a step, we add it to the queue if is is empty.
    // If the queue is not empty, and the depth of the new step is not smaller than
    // the depth of the last step in the queue, then this step needs to be added to
    // the queue too.
    // Otherwise, the last step in the queue is a sub-expression of the new step,
    // and we need to
    // replace it in the queue with the new step.
    if (this.steps.isEmpty() || !this.steps.getLast().isPartOf(ctx.getSourceInterval())) {
      this.steps.offer(new AxisStepInfo(ctx, this::getInputText));
    } else {
      Interval removedInterval = ctx.getSourceInterval();
      while(!this.steps.isEmpty() && this.steps.getLast().isPartOf(removedInterval)) {
        this.steps.removeLast();
      }
      this.steps.offer(new AxisStepInfo(ctx, this::getInputText));
    }    
  }

  @Override
  public void exitFilterexpr(FilterexprContext ctx) {
    if (inPredicateMode()) {
      return;
    }

    // Same logic as for axis steps here (sse exitAxisstep).
    if (this.steps.isEmpty() || !this.steps.getLast().isPartOf(ctx.getSourceInterval())) {
      this.steps.offer(new FilterStepInfo(ctx, this::getInputText));
    } else {
      Interval removedInterval = ctx.getSourceInterval();
      while(!this.steps.isEmpty() && this.steps.getLast().isPartOf(removedInterval)) {
        this.steps.removeLast();
      }
      this.steps.offer(new FilterStepInfo(ctx, this::getInputText));
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

  public class AxisStepInfo extends StepInfo {

    public AxisStepInfo(AxisstepContext ctx, Function<ParserRuleContext, String> getInputText) {
      super(ctx.reversestep() != null? getInputText.apply(ctx.reversestep()) : getInputText.apply(ctx.forwardstep()), 
      ctx.predicatelist().predicate().stream().map(getInputText).collect(Collectors.toList()), ctx.getSourceInterval());
    }
  }

  public class FilterStepInfo extends StepInfo {

    public FilterStepInfo(FilterexprContext ctx, Function<ParserRuleContext, String> getInputText) {
      super(getInputText.apply(ctx.primaryexpr()), 
      ctx.predicatelist().predicate().stream().map(getInputText).collect(Collectors.toList()), ctx.getSourceInterval());
    }
  }

  public class StepInfo {
    String stepText;
    List<String> predicates;
    int a;
    int b;

    protected StepInfo(String stepText, List<String> predicates, Interval interval) {
      this.stepText = stepText;
      this.predicates = predicates;
      this.a = interval.a;
      this.b = interval.b;
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

    public Boolean isPartOf(Interval interval) {
      return this.a >= interval.a && this.b <= interval.b;
    }
  }
}
