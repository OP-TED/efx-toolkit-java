/*
 * Copyright 2026 European Union
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.templates.ContentBlock;
import eu.europa.ted.efx.model.templates.ContentTemplate;
import eu.europa.ted.efx.model.templates.DisplayContentTemplate;
import eu.europa.ted.efx.model.templates.FragmentKind;
import eu.europa.ted.efx.model.templates.InvokeContentTemplate;
import eu.europa.ted.efx.model.templates.OutlineAddress;
import eu.europa.ted.efx.model.templates.ViewTemplate;

/**
 * Checks the navigation of a translated EFX-2 view template, once all its lines are known. It
 * follows the invocations of templates, and composes the outline addresses of their headings with
 * that of the invoking line, as the generated template does:
 * <ul>
 * <li>no template invokes itself, directly or through other templates;</li>
 * <li>no two headings displayed in the main section have the same address when they can be
 * displayed for the same element;</li>
 * <li>each heading displayed in the navigation section, by a line or by a template it invokes, has
 * an address that a heading displayed in the main section has, and displays no hyperlink itself,
 * because what it displays is the link to that heading.</li>
 * </ul>
 * Two headings with the same address are reported only when they are reached by the same display
 * path (see {@link DisplayPath}) and are not in different alternatives of the same line. The
 * expressions on the paths are compared only for equality, as they are written: the check does not
 * read them. So it is not exhaustive. It misses, for example, two headings displayed for the same
 * element when one of them is under a line that declares again, as a nested context, the element
 * it is already in, and two calls of a template with different arguments that the template does not
 * use to choose its elements. It catches the conflicts that the template file shows; it cannot tell
 * which headings a given notice displays.
 */
final class NavigationCheck {

  private final ViewTemplate viewTemplate;


  // The declared templates, by name.
  private final Map<String, ContentBlock> templates = new LinkedHashMap<>();

  // The templates already found not to invoke themselves.
  private final Set<String> notRecursive = new HashSet<>();

  // The templates already checked for headings without an address.
  private final Set<String> checkedForAddresses = new HashSet<>();

  // For each template, the headings it displays, as summaries that each invocation places.
  private final Map<String, Headings> templateHeadings = new HashMap<>();

  // The headings displayed in the main section, by address.
  private final Map<OutlineAddress, List<Occurrence>> targets = new HashMap<>();

  /**
   * How a heading is reached, from the top of its section, with the calls of templates followed. It
   * is made of steps, compared only for equality, as they are written:
   * <ul>
   * <li>for each line on the way that declares a context: the absolute path and the path relative
   * to the context above, of that context. A line adds nothing when its context step equals the
   * last context step on the way, with no variables or arguments between them, and its relative
   * path is its absolute path. The absolute path holds none of the conditions that a context
   * declaration adds, so such a context adds no condition and selects the same elements wherever it
   * is: here, the elements the line is already in;</li>
   * <li>for each line that declares variables: the variables, each with its expression;</li>
   * <li>for each call of a template with arguments: the parameters and the argument expressions.
   * Which template is called is not part of the path: what counts is the elements and the values,
   * not which template displays the heading.</li>
   * </ul>
   * Two headings reached by equal paths are displayed for the same elements. Two headings reached
   * by different paths may still be, which the check does not establish.
   */
  private static final class DisplayPath {
    static final DisplayPath TOP = new DisplayPath(List.of(), null);

    // The steps, each a ContextStep or a list of variables or arguments.
    private final List<Object> steps;

    // The last context step on the way, or null at the top or after variables or arguments, which
    // can give the same expressions another meaning.
    private final ContextStep lastContext;

    private DisplayPath(final List<Object> steps, final ContextStep lastContext) {
      this.steps = steps;
      this.lastContext = lastContext;
    }

    /**
     * @return this path, followed by what the line declares
     */
    DisplayPath through(final ContentBlock line) {
      DisplayPath path = this;
      if (line.getContext() != null) {
        path = path.withContext(new ContextStep(line.getContext().absolutePath(),
            line.getContext().relativePath()));
      }
      if (!line.getDeclaredVariables().isEmpty()) {
        path = path.followedBy(List.copyOf(line.getDeclaredVariables()));
      }
      return path;
    }

    /**
     * @return this path, followed by the arguments of the call of a template
     */
    DisplayPath through(final InvokeContentTemplate invocation) {
      return invocation.getArguments().isEmpty()
          ? this
          : this.followedBy(List.copyOf(invocation.getArguments()));
    }

    /**
     * @return this path, followed by a path inside a template that it calls, whose context steps
     *         are taken in turn as if they followed this path
     */
    DisplayPath then(final DisplayPath inside) {
      DisplayPath path = this;
      for (Object step : inside.steps) {
        path = step instanceof ContextStep
            ? path.withContext((ContextStep) step)
            : path.followedBy(step);
      }
      return path;
    }

    private DisplayPath withContext(final ContextStep context) {
      if (context.equals(this.lastContext) && context.selectsTheSameElementsWhereverItIs()) {
        return this;
      }
      final List<Object> extended = new ArrayList<>(this.steps);
      extended.add(context);
      return new DisplayPath(List.copyOf(extended), context);
    }

    private DisplayPath followedBy(final Object step) {
      final List<Object> extended = new ArrayList<>(this.steps);
      extended.add(step);
      return new DisplayPath(List.copyOf(extended), null);
    }

    @Override
    public boolean equals(final Object other) {
      return other instanceof DisplayPath && ((DisplayPath) other).steps.equals(this.steps);
    }

    @Override
    public int hashCode() {
      return this.steps.hashCode();
    }
  }

  /**
   * The context that a line declares, as two expressions compared only for equality: its absolute
   * path, and its path relative to the context above.
   */
  private static final class ContextStep {
    private final PathExpression absolute;
    private final PathExpression relative;

    ContextStep(final PathExpression absolute, final PathExpression relative) {
      this.absolute = absolute;
      this.relative = relative;
    }

    /**
     * @return whether the context is its absolute path as it is, which holds none of the conditions
     *         that a context declaration adds, so that it does not depend on the elements it is
     *         declared in
     */
    boolean selectsTheSameElementsWhereverItIs() {
      return this.relative.equals(this.absolute);
    }

    @Override
    public boolean equals(final Object other) {
      return other instanceof ContextStep && ((ContextStep) other).absolute.equals(this.absolute)
          && ((ContextStep) other).relative.equals(this.relative);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.absolute, this.relative);
    }
  }

  /**
   * A heading displayed by a section or a template, where it is displayed.
   */
  private static final class Occurrence {
    final ContentBlock heading;

    // Its address, as it is in the section or the template that displays it.
    final OutlineAddress address;

    // How it is reached, from the top of the section or the template that displays it.
    final DisplayPath path;

    // For each line with WHEN alternatives on the way to the heading, the alternatives it is in.
    final Map<ContentBlock, Set<Integer>> alternatives;

    // Whether the heading is displayed more than once for the same choice of alternatives.
    final boolean repeated;

    Occurrence(final ContentBlock heading, final OutlineAddress address, final DisplayPath path,
        final Map<ContentBlock, Set<Integer>> alternatives, final boolean repeated) {
      this.heading = heading;
      this.address = address;
      this.path = path;
      this.alternatives = alternatives;
      this.repeated = repeated;
    }

    /**
     * @return the heading as it is displayed by an invocation of the template that displays it
     */
    Occurrence invokedAt(final OutlineAddress invocation, final DisplayPath invocationPath,
        final Map<ContentBlock, Set<Integer>> invocationAlternatives) {
      final Map<ContentBlock, Set<Integer>> chosen = new HashMap<>(invocationAlternatives);
      chosen.putAll(this.alternatives);
      return new Occurrence(this.heading, this.address.after(invocation),
          invocationPath.then(this.path), chosen, this.repeated);
    }

    /**
     * @return the heading, displayed only in the given alternatives of the line
     */
    Occurrence inAlternatives(final ContentBlock line, final Set<Integer> indexes) {
      final Map<ContentBlock, Set<Integer>> chosen = new HashMap<>(this.alternatives);
      chosen.put(line, indexes);
      return new Occurrence(this.heading, this.address, this.path, chosen, this.repeated);
    }

    Occurrence repeated() {
      return new Occurrence(this.heading, this.address, this.path, this.alternatives, true);
    }

    /**
     * @return whether the two headings can be displayed together: no line has to choose different
     *         alternatives to display them
     */
    boolean canBeDisplayedWith(final Occurrence other) {
      return this.alternatives.entrySet().stream()
          .noneMatch(choice -> other.alternatives.containsKey(choice.getKey())
              && other.alternatives.get(choice.getKey()).stream()
                  .noneMatch(choice.getValue()::contains));
    }

    /**
     * @return what identifies the heading where it is displayed, regardless of how often
     */
    List<Object> place() {
      return List.of(new Identity(this.heading), this.address, this.path, this.alternatives);
    }
  }

  /**
   * Compares lines by identity, so that they can be part of a key.
   */
  private static final class Identity {
    final ContentBlock line;

    Identity(final ContentBlock line) {
      this.line = line;
    }

    @Override
    public boolean equals(final Object other) {
      return other instanceof Identity && ((Identity) other).line == this.line;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this.line);
    }
  }

  /**
   * The headings displayed by a section or a template. A heading displayed twice at the same place
   * is kept once, as repeated, so that the number of headings does not grow with the number of
   * ways to reach them.
   */
  private static final class Headings {
    private final Map<List<Object>, Occurrence> byPlace = new LinkedHashMap<>();

    void add(final Occurrence occurrence) {
      this.byPlace.merge(occurrence.place(), occurrence, (known, added) -> known.repeated());
    }

    Iterable<Occurrence> all() {
      return this.byPlace.values();
    }
  }

  NavigationCheck(final ViewTemplate viewTemplate) {
    this.viewTemplate = viewTemplate;
    for (ContentBlock template : viewTemplate.getTemplateDeclarations().getChildren()) {
      this.templates.put(template.getId(), template);
    }
  }

  /**
   * @throws InvalidUsageException for the first rule that the navigation breaks
   */
  void run() {
    for (String template : this.templates.keySet()) {
      this.checkNotRecursive(template, new ArrayDeque<>());
    }
    final Headings main = new Headings();
    for (ContentBlock line : this.viewTemplate.getBody().getRoot().getChildren()) {
      this.collectHeadings(line, DisplayPath.TOP, Map.of(), main);
    }
    for (Occurrence heading : main.all()) {
      this.targets.computeIfAbsent(heading.address, a -> new ArrayList<>()).add(heading);
    }
    this.checkTargetsAreDistinct();
    final Headings entries = new Headings();
    for (ContentBlock line : this.viewTemplate.getNavigation().getRoot().getChildren()) {
      this.checkHeadingsHaveAddresses(line);
      this.collectHeadings(line, DisplayPath.TOP, Map.of(), entries);
    }
    for (Occurrence entry : entries.all()) {
      this.checkNavigationEntry(entry);
    }
  }

  /**
   * @param template the name of a template
   * @param invoking the templates invoked on the way to it, the outermost first
   */
  private void checkNotRecursive(final String template, final Deque<String> invoking) {
    if (this.notRecursive.contains(template)) {
      return;
    }
    invoking.addLast(template);
    this.checkInvocationsNotRecursive(this.templates.get(template), invoking);
    invoking.removeLast();
    this.notRecursive.add(template);
  }

  private void checkInvocationsNotRecursive(final ContentBlock block,
      final Deque<String> invoking) {
    for (InvokeContentTemplate invocation : invocationsOf(block)) {
      if (invoking.contains(invocation.getTemplateName())) {
        throw InvalidUsageException.recursiveTemplateInvocation(invocation.getStart(),
            invocation.getTemplateName());
      }
      this.checkNotRecursive(invocation.getTemplateName(), invoking);
    }
    for (ContentBlock child : block.getChildren()) {
      this.checkInvocationsNotRecursive(child, invoking);
    }
  }

  /**
   * Collects the headings that a line displays: its own, those of the lines nested in it, and those
   * of the templates they invoke, with their addresses in the section or the template of the line.
   *
   * @param line a line, or a template
   * @param path how the line above is reached
   * @param alternatives the WHEN alternatives on the way to the line
   * @param headings where to add the headings
   */
  private void collectHeadings(final ContentBlock line, final DisplayPath path,
      final Map<ContentBlock, Set<Integer>> alternatives, final Headings headings) {
    final DisplayPath reached = path.through(line);
    line.getOutlineAddress().ifPresent(address -> headings
        .add(new Occurrence(line, address, reached, alternatives, false)));
    // The headings of each alternative are placed first, as one invocation can display the same
    // heading twice at the same place. A heading that several alternatives of the line display in
    // the same way is then kept once, with all of them.
    final Map<List<Object>, Occurrence> invoked = new LinkedHashMap<>();
    final Map<List<Object>, Set<Integer>> invokedIn = new HashMap<>();
    final List<ContentTemplate> contentTemplates = line.getContentTemplates();
    for (int index = 0; index < contentTemplates.size(); index++) {
      if (contentTemplates.get(index) instanceof InvokeContentTemplate) {
        final Headings placed = new Headings();
        final InvokeContentTemplate invocation = (InvokeContentTemplate) contentTemplates.get(index);
        for (Occurrence heading : this.headingsOfTemplate(invocation.getTemplateName()).all()) {
          placed.add(heading.invokedAt(line.getAddressOfNestedLines(), reached.through(invocation),
              alternatives));
        }
        for (Occurrence heading : placed.all()) {
          final List<Object> key = List.of(heading.place(), heading.repeated);
          invoked.putIfAbsent(key, heading);
          invokedIn.computeIfAbsent(key, k -> new TreeSet<>()).add(index);
        }
      }
    }
    for (Map.Entry<List<Object>, Occurrence> heading : invoked.entrySet()) {
      headings.add(contentTemplates.size() > 1
          ? heading.getValue().inAlternatives(line, invokedIn.get(heading.getKey()))
          : heading.getValue());
    }
    for (ContentBlock child : line.getChildren()) {
      this.collectHeadings(child, reached, alternatives, headings);
    }
  }

  /**
   * @return the headings that a template displays, with their addresses in the template, each
   *         template being summarised once
   */
  private Headings headingsOfTemplate(final String template) {
    final Headings known = this.templateHeadings.get(template);
    if (known != null) {
      return known;
    }
    final Headings headings = new Headings();
    this.collectHeadings(this.templates.get(template), DisplayPath.TOP, Map.of(), headings);
    this.templateHeadings.put(template, headings);
    return headings;
  }

  private void checkTargetsAreDistinct() {
    for (Map.Entry<OutlineAddress, List<Occurrence>> sameAddress : this.targets.entrySet()) {
      final List<Occurrence> headings = sameAddress.getValue();
      for (Occurrence heading : headings) {
        if (heading.repeated) {
          throw InvalidUsageException.navigationAddressGivenTwice(
              heading.heading.getMarker().getToken(), sameAddress.getKey().toString());
        }
      }
      for (int second = 1; second < headings.size(); second++) {
        for (int first = 0; first < second; first++) {
          if (headings.get(first).path.equals(headings.get(second).path)
              && headings.get(first).canBeDisplayedWith(headings.get(second))) {
            throw InvalidUsageException.navigationAddressGivenTwice(
                headings.get(second).heading.getMarker().getToken(),
                sameAddress.getKey().toString());
          }
        }
      }
    }
  }

  /**
   * Checks a heading displayed in the navigation section, which is a navigation entry: it links to
   * the heading displayed in the main section with the same address.
   */
  private void checkNavigationEntry(final Occurrence entry) {
    final ContentBlock line = entry.heading;
    if (!this.targets.containsKey(entry.address)) {
      throw InvalidUsageException.navigationEntryWithoutHeading(line.getMarker().getToken(),
          entry.address.toString());
    }
    if (displaysHyperlink(line)) {
      throw InvalidUsageException.hyperlinkInNavigationHeading(line.getMarker().getToken());
    }
  }

  /**
   * @throws InvalidUsageException for a heading displayed in the navigation section, by a line or
   *         by a template it invokes, written with a bare '#', which has no address to link to
   */
  private void checkHeadingsHaveAddresses(final ContentBlock line) {
    if (line.getKind() == FragmentKind.HEADING && line.getOutlineAddress().isEmpty()) {
      throw InvalidUsageException.navigationHeadingWithoutTag(line.getMarker().getToken());
    }
    for (InvokeContentTemplate invocation : invocationsOf(line)) {
      if (this.checkedForAddresses.add(invocation.getTemplateName())) {
        this.checkHeadingsHaveAddresses(this.invokedBy(invocation));
      }
    }
    for (ContentBlock child : line.getChildren()) {
      this.checkHeadingsHaveAddresses(child);
    }
  }

  /**
   * What a heading of the navigation displays itself, its number and its own content, is the link
   * to its heading in the main section. The lines nested in it, and the template it invokes, are
   * displayed after the link, and the headings among them are links of their own.
   *
   * @return whether the heading displays a hyperlink block itself, in one of its alternatives
   */
  private static boolean displaysHyperlink(final ContentBlock heading) {
    return heading.getContentTemplates().stream()
        .anyMatch(content -> content instanceof DisplayContentTemplate
            && ((DisplayContentTemplate) content).displaysHyperlink());
  }

  private ContentBlock invokedBy(final InvokeContentTemplate invocation) {
    return this.templates.get(invocation.getTemplateName());
  }

  private static List<InvokeContentTemplate> invocationsOf(final ContentBlock line) {
    final List<InvokeContentTemplate> invocations = new ArrayList<>();
    for (ContentTemplate contentTemplate : line.getContentTemplates()) {
      if (contentTemplate instanceof InvokeContentTemplate) {
        invocations.add((InvokeContentTemplate) contentTemplate);
      }
    }
    return invocations;
  }
}
