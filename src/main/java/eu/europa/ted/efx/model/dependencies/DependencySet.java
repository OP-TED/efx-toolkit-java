package eu.europa.ted.efx.model.dependencies;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set of field and node identifiers collected during a parse tree walk.
 * Used as a stack frame in the dependency extraction process.
 */
public class DependencySet {

  private final Set<String> fieldIds = new LinkedHashSet<>();
  private final Set<String> nodeIds = new LinkedHashSet<>();
  private final Set<String> codelistNames = new LinkedHashSet<>();

  public void addField(final String fieldId) {
    this.fieldIds.add(fieldId);
  }

  public void addNode(final String nodeId) {
    this.nodeIds.add(nodeId);
  }

  public void addCodelist(final String codelistName) {
    this.codelistNames.add(codelistName);
  }

  public void removeField(final String fieldId) {
    this.fieldIds.remove(fieldId);
  }

  public void removeNode(final String nodeId) {
    this.nodeIds.remove(nodeId);
  }

  public void addAll(final DependencySet other) {
    this.fieldIds.addAll(other.fieldIds);
    this.nodeIds.addAll(other.nodeIds);
    this.codelistNames.addAll(other.codelistNames);
  }

  public Set<String> getFieldIds() {
    return Collections.unmodifiableSet(this.fieldIds);
  }

  public Set<String> getNodeIds() {
    return Collections.unmodifiableSet(this.nodeIds);
  }

  public Set<String> getCodelistNames() {
    return Collections.unmodifiableSet(this.codelistNames);
  }

  public boolean isEmpty() {
    return this.fieldIds.isEmpty() && this.nodeIds.isEmpty() && this.codelistNames.isEmpty();
  }

  public Set<String> allIds() {
    final Set<String> result = new LinkedHashSet<>();
    result.addAll(this.fieldIds);
    result.addAll(this.nodeIds);
    result.addAll(this.codelistNames);
    return Collections.unmodifiableSet(result);
  }
}
