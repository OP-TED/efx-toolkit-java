/*
 * Copyright 2025 European Union
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
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import eu.europa.ted.efx.model.rules.RuleNature;
import eu.europa.ted.efx.model.rules.ValidationRule;
import eu.europa.ted.efx.model.rules.ValidationStage;

/**
 * Represents a Schematron &lt;pattern&gt; element.
 * A pattern can be either subtype-specific (for a single notice subtype) or shared
 * (for rules that apply to all notice subtypes).
 */
public class SchematronPattern {
  private final String stage;
  private final String noticeSubtype;
  private final List<SchematronLet> letElements;
  private final List<SchematronRule> rules;

  /**
   * Creates a shared pattern for a stage, containing only rules that apply to all subtypes.
   *
   * @param validationStage The validation stage
   */
  public SchematronPattern(final ValidationStage validationStage) {
    this(validationStage, null);
  }

  /**
   * Creates a pattern for a specific stage and notice subtype combination.
   * When noticeSubtype is null, creates a shared pattern for rules that apply to all subtypes.
   *
   * @param validationStage The validation stage
   * @param noticeSubtype The notice subtype to filter by, or null for universal rules
   */
  public SchematronPattern(final ValidationStage validationStage, final String noticeSubtype) {
    this.stage = validationStage.getName();
    this.noticeSubtype = noticeSubtype;

    Predicate<ValidationRule> applicableRules = noticeSubtype == null
        ? rule -> rule.isUniversal() && rule.isForPostValidation()
        : rule -> !rule.isUniversal() && rule.isForPostValidation()
            && rule.isForNoticeSubtype(noticeSubtype);

    this.rules = validationStage.getRuleSets().stream()
        .map(ruleSet -> new SchematronRule(ruleSet, applicableRules))
        .filter(SchematronRule::hasTests)
        .collect(Collectors.toCollection(ArrayList::new));

    this.letElements = validationStage.getVariables().stream()
        .map(SchematronLet::new)
        .collect(Collectors.toCollection(ArrayList::new));
    this.letElements.addAll(this.rules.stream()
        .flatMap(rule -> rule.getPromotedLetElements().stream())
        .collect(Collectors.toList()));
  }

  /** Used by pattern.ftl */
  public String getId() {
    if (this.noticeSubtype == null) {
      return "validation-stage-" + this.stage;
    }
    return "validation-stage-" + this.stage + "-" + this.noticeSubtype;
  }

  public String getStage() {
    return this.stage;
  }

  public String getNoticeSubtype() {
    return this.noticeSubtype;
  }

  /** Returns true if this is a shared pattern (applies to all notice subtypes). */
  public boolean isShared() {
    return this.noticeSubtype == null;
  }

  /** Used by pattern.ftl */
  public List<SchematronLet> getLetElements() {
    return this.letElements;
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
  public boolean hasRulesFor(final Set<RuleNature> ruleNatures) {
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
