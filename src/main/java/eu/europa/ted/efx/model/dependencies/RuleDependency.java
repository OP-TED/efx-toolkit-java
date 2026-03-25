package eu.europa.ted.efx.model.dependencies;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The dependencies of a single validation rule.
 */
public class RuleDependency {

  private final String ruleId;
  private final Set<String> fields;
  private final Set<String> nodes;

  public RuleDependency(final String ruleId, final Set<String> fields, final Set<String> nodes) {
    this.ruleId = ruleId;
    this.fields = Collections.unmodifiableSet(new LinkedHashSet<>(fields));
    this.nodes = Collections.unmodifiableSet(new LinkedHashSet<>(nodes));
  }

  public String getRuleId() {
    return this.ruleId;
  }

  public Set<String> getFields() {
    return this.fields;
  }

  public Set<String> getNodes() {
    return this.nodes;
  }
}
