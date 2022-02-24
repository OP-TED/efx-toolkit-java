package eu.europa.ted.efx.xpath;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple location path in the XPath syntax, used to infer stage 1 rules. It's made of the list of
 * steps that compose the location. For example, the XPath location "/a/b/c" is made of 3 steps:
 * "a", "b", and "c". Only the "child" axis is supported, in its abbreviated form: "/".
 */
public class XPathLocation implements Comparable<XPathLocation> {

  private List<XPathStep> steps;

  public List<XPathStep> getSteps() {
    return steps;
  }

  /**
   * Create a location with no step, corresponding to a document root.
   */
  public XPathLocation() {
    steps = new ArrayList<>();
  }

  public XPathLocation(List<XPathStep> steps) {
    this.steps = steps;
  }

  public int size() {
    return steps.size();
  }

  public void append(XPathStep step) {
    steps.add(step);
  }

  public void append(XPathLocation location) {
    steps.addAll(location.getSteps());
  }

  public XPathStep getLast() {
    if (steps.size() > 0) {
      int lastIndex = steps.size() - 1;
      return steps.get(lastIndex);
    } else {
      return null;
    }
  }

  /**
   * Remove the last step from the xpath location
   *
   * @return The step removed, or null if there is no step to remove
   */
  public XPathStep removeLast() {
    if (steps.size() > 0) {
      int lastIndex = steps.size() - 1;
      XPathStep last = steps.get(lastIndex);
      steps.remove(lastIndex);
      return last;
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (XPathStep step : steps) {
      sb.append('/');
      sb.append(step);
    }
    return sb.toString();
  }

  @Override
  public int compareTo(XPathLocation other) {
    return this.toString().compareTo(other.toString());
  }

  /**
   * Return true if the current location is the same or an ancestor of the given location
   */
  public boolean isAncestorOf(XPathLocation location) {
    if (this.size() > location.size()) {
      return false;
    }
    for (int i = 0; i < this.size(); i++) {
      if (!this.getSteps().get(i).equals(location.getSteps().get(i))) {
        // If location has a different step, we're not an ancestor
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the current location corresponds to a container node. Returns false if it
   * corresponds to a field.
   */
  public boolean isContainerNode() {
    int last = getSteps().size() - 1;
    // If the last step is a container node, then the whole location is one
    return getSteps().get(last).isContainerNode();
  }
}
