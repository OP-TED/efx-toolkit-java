/*
 * Copyright 2022 European Union
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
package eu.europa.ted.eforms.sdk.schematron;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.europa.ted.efx.model.rules.RuleNature;
import eu.europa.ted.efx.model.rules.RuleSet;
import eu.europa.ted.efx.model.rules.ValidationRule;
import eu.europa.ted.efx.model.rules.ValidationStage;
import eu.europa.ted.efx.model.variables.Variable;

/**
 * Represents a Schematron &lt;pattern&gt; element.
 * Each pattern is specific to a stage AND a notice type, containing only
 * the assertions that apply to that notice type.
 */
public class SchematronPattern {
  private final String stage;
  private final String noticeType;
  private final List<SchematronLet> variables;
  private final List<SchematronRule> rules;

  /**
   * Creates a pattern for a specific stage and notice type combination.
   * Only includes rules/assertions that apply to the given notice type.
   *
   * @param validationStage The validation stage
   * @param noticeType The notice type to filter by
   */
  public SchematronPattern(ValidationStage validationStage, String noticeType) {
    this.stage = validationStage.getName();
    this.noticeType = noticeType;
    this.variables = collectVariables(validationStage);
    this.rules = createRules(validationStage, noticeType);
  }

  private static List<SchematronLet> collectVariables(ValidationStage stage) {
    List<SchematronLet> vars = new ArrayList<>();
    for (Variable var : stage.getVariables()) {
      vars.add(new SchematronLet(var.name, var.initializationExpression.getScript()));
    }
    for (RuleSet ruleSet : stage.getRuleSets()) {
      for (Variable var : ruleSet.getStageVariables()) {
        vars.add(new SchematronLet(var.name, var.initializationExpression.getScript()));
      }
    }
    return vars;
  }

  private static List<SchematronRule> createRules(ValidationStage stage, String noticeType) {
    List<SchematronRule> rules = new ArrayList<>();
    for (RuleSet ruleSet : stage.getRuleSets()) {
      SchematronRule rule = new SchematronRule(ruleSet, noticeType);
      if (rule.hasTests()) {
        rules.add(rule);
      }
    }
    return rules;
  }

  /**
   * Returns all notice types referenced in the given stage.
   * Used by SchematronMarkupGenerator to determine which patterns to create.
   */
  public static Set<String> getNoticeTypesInStage(ValidationStage stage) {
    Set<String> types = new LinkedHashSet<>();
    for (RuleSet ruleSet : stage.getRuleSets()) {
      for (ValidationRule rule : ruleSet) {
        if (rule.getNoticeSubtypes() != null) {
          types.addAll(rule.getNoticeSubtypes().asList());
        }
      }
      ValidationRule fallback = ruleSet.getFallbackRule();
      if (fallback != null && fallback.getNoticeSubtypes() != null) {
        types.addAll(fallback.getNoticeSubtypes().asList());
      }
    }
    return types;
  }

  /** Used by pattern.ftl */
  public String getId() {
    return "validation-stage-" + this.stage + "-" + this.noticeType;
  }

  public String getStage() {
    return this.stage;
  }

  public String getNoticeType() {
    return this.noticeType;
  }

  /** Used by pattern.ftl */
  public List<SchematronLet> getVariables() {
    return this.variables;
  }

  /** Used by pattern.ftl */
  public List<SchematronRule> getRules() {
    return this.rules;
  }

  /** Returns true if this pattern has at least one rule with assertions. */
  public boolean hasRules() {
    return !this.rules.isEmpty();
  }

  /**
   * Returns true if this pattern has at least one rule with tests matching the given rule natures.
   *
   * @param ruleNatures The set of rule natures to include
   * @return true if any rule has tests of the included natures
   */
  public boolean hasRulesFor(Set<RuleNature> ruleNatures) {
    return this.rules.stream().anyMatch(r -> r.hasTestsFor(ruleNatures));
  }

  /** Computed on-demand from rules. */
  public Map<String, SchematronDiagnostic> getDiagnostics() {
    Map<String, SchematronDiagnostic> diagnostics = new LinkedHashMap<>();
    for (SchematronRule rule : this.rules) {
      diagnostics.putAll(rule.getDiagnostics());
    }
    return diagnostics;
  }
}
