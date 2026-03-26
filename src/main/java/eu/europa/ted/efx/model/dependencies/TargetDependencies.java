package eu.europa.ted.efx.model.dependencies;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dependency information for a single target field or node.
 *
 * Tracks both what this target depends on (for validation and computation) and what other targets
 * require this one.
 */
public class TargetDependencies {

  private final String id;
  private final boolean isField;

  private final Set<String> computeFieldDeps = new LinkedHashSet<>();
  private final Set<String> computeNodeDeps = new LinkedHashSet<>();
  private final List<RuleDependency> assertDeps = new ArrayList<>();

  private final Set<String> requiredByComputeFields = new LinkedHashSet<>();
  private final Set<String> requiredByComputeNodes = new LinkedHashSet<>();
  private final Set<String> requiredByAssertFields = new LinkedHashSet<>();
  private final Set<String> requiredByAssertNodes = new LinkedHashSet<>();

  public TargetDependencies(final String id, final boolean isField) {
    this.id = id;
    this.isField = isField;
  }

  public String getId() {
    return this.id;
  }

  public boolean isField() {
    return this.isField;
  }

  public void addAssertDependency(final RuleDependency ruleDependency) {
    this.assertDeps.add(ruleDependency);
  }

  public List<RuleDependency> getAssertDependencies() {
    return this.assertDeps;
  }

  public Set<String> getComputeFieldDeps() {
    return this.computeFieldDeps;
  }

  public Set<String> getComputeNodeDeps() {
    return this.computeNodeDeps;
  }

  public void addRequiredByAssertField(final String fieldId) {
    this.requiredByAssertFields.add(fieldId);
  }

  public void addRequiredByAssertNode(final String nodeId) {
    this.requiredByAssertNodes.add(nodeId);
  }

  public void addRequiredByComputeField(final String fieldId) {
    this.requiredByComputeFields.add(fieldId);
  }

  public void addRequiredByComputeNode(final String nodeId) {
    this.requiredByComputeNodes.add(nodeId);
  }

  public Set<String> getRequiredByComputeFields() {
    return this.requiredByComputeFields;
  }

  public Set<String> getRequiredByComputeNodes() {
    return this.requiredByComputeNodes;
  }

  public Set<String> getRequiredByAssertFields() {
    return this.requiredByAssertFields;
  }

  public Set<String> getRequiredByAssertNodes() {
    return this.requiredByAssertNodes;
  }
}
