package eu.europa.ted.efx.model.dependencies;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A dependency graph mapping targets (fields and nodes) to their dependencies and dependants.
 *
 * The graph is built incrementally by the dependency extractor: assert dependencies are added
 * per-rule during the tree walk, and reverse dependencies (requiredBy) are computed at the end.
 */
public class DependencyGraph {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Map<String, TargetDependencies> fieldEntries = new LinkedHashMap<>();
  private final Map<String, TargetDependencies> nodeEntries = new LinkedHashMap<>();

  public TargetDependencies getOrCreateFieldEntry(final String fieldId) {
    return this.fieldEntries.computeIfAbsent(fieldId, id -> new TargetDependencies(id, true));
  }

  public TargetDependencies getOrCreateNodeEntry(final String nodeId) {
    return this.nodeEntries.computeIfAbsent(nodeId, id -> new TargetDependencies(id, false));
  }

  public List<TargetDependencies> getFieldEntries() {
    return new ArrayList<>(this.fieldEntries.values());
  }

  public List<TargetDependencies> getNodeEntries() {
    return new ArrayList<>(this.nodeEntries.values());
  }

  /**
   * Computes the reverse dependencies (requiredBy) from the forward dependencies (dependsOn).
   * Must be called after all forward dependencies have been added.
   */
  public void computeRequiredBy() {
    for (TargetDependencies target : new ArrayList<>(this.fieldEntries.values())) {
      this.addRequiredByFromAssertDeps(target);
    }
    for (TargetDependencies target : new ArrayList<>(this.nodeEntries.values())) {
      this.addRequiredByFromAssertDeps(target);
    }
  }

  private void addRequiredByFromAssertDeps(final TargetDependencies target) {
    for (RuleDependency rule : target.getAssertDependencies()) {
      for (String depFieldId : rule.getFields()) {
        this.addRequiredByAssert(this.getOrCreateFieldEntry(depFieldId), target);
      }
      for (String depNodeId : rule.getNodes()) {
        this.addRequiredByAssert(this.getOrCreateNodeEntry(depNodeId), target);
      }
    }
  }

  private void addRequiredByAssert(final TargetDependencies dependency,
      final TargetDependencies requirer) {
    if (requirer.isField()) {
      dependency.addRequiredByAssertField(requirer.getId());
    } else {
      dependency.addRequiredByAssertNode(requirer.getId());
    }
  }

  public String toJson() {
    final ObjectNode root = MAPPER.createObjectNode();
    root.set("fields", this.serializeEntries(this.fieldEntries));
    root.set("nodes", this.serializeEntries(this.nodeEntries));
    return root.toPrettyString();
  }

  private ArrayNode serializeEntries(final Map<String, TargetDependencies> entries) {
    final ArrayNode array = MAPPER.createArrayNode();
    for (TargetDependencies entry : entries.values()) {
      array.add(this.serializeEntry(entry));
    }
    return array;
  }

  private ObjectNode serializeEntry(final TargetDependencies entry) {
    final ObjectNode node = MAPPER.createObjectNode();
    node.put("id", entry.getId());
    this.putIfNotEmpty("dependsOn", this.serializeDependsOn(entry), node);
    this.putIfNotEmpty("requiredBy", this.serializeRequiredBy(entry), node);
    return node;
  }

  private ObjectNode serializeDependsOn(final TargetDependencies entry) {
    final ObjectNode dependsOn = MAPPER.createObjectNode();
    this.putIfNotEmpty("compute", this.serializeIdentifierSets(
        entry.getComputeFieldDeps(), entry.getComputeNodeDeps()), dependsOn);

    final ArrayNode assertArray = MAPPER.createArrayNode();
    for (RuleDependency rule : entry.getAssertDependencies()) {
      final ObjectNode ruleNode = MAPPER.createObjectNode();
      ruleNode.put("ruleId", rule.getRuleId());
      this.putIfNotEmpty("fields", this.toStringArray(rule.getFields()), ruleNode);
      this.putIfNotEmpty("nodes", this.toStringArray(rule.getNodes()), ruleNode);
      assertArray.add(ruleNode);
    }
    this.putIfNotEmpty("assert", assertArray, dependsOn);
    return dependsOn;
  }

  private ObjectNode serializeRequiredBy(final TargetDependencies entry) {
    final ObjectNode requiredBy = MAPPER.createObjectNode();
    this.putIfNotEmpty("compute", this.serializeIdentifierSets(
        entry.getRequiredByComputeFields(), entry.getRequiredByComputeNodes()), requiredBy);
    this.putIfNotEmpty("assert", this.serializeIdentifierSets(
        entry.getRequiredByAssertFields(), entry.getRequiredByAssertNodes()), requiredBy);
    return requiredBy;
  }

  private ObjectNode serializeIdentifierSets(final Iterable<String> fields,
      final Iterable<String> nodes) {
    final ObjectNode obj = MAPPER.createObjectNode();
    this.putIfNotEmpty("fields", this.toStringArray(fields), obj);
    this.putIfNotEmpty("nodes", this.toStringArray(nodes), obj);
    return obj;
  }

  private void putIfNotEmpty(final String name, final ObjectNode value, final ObjectNode parent) {
    if (value.size() > 0) {
      parent.set(name, value);
    }
  }

  private void putIfNotEmpty(final String name, final ArrayNode value, final ObjectNode parent) {
    if (value.size() > 0) {
      parent.set(name, value);
    }
  }

  private ArrayNode toStringArray(final Iterable<String> values) {
    final ArrayNode array = MAPPER.createArrayNode();
    for (String value : values) {
      array.add(value);
    }
    return array;
  }
}
