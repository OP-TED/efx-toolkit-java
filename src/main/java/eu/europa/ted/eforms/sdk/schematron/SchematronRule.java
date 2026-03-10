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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.rules.RuleNature;
import eu.europa.ted.efx.model.rules.RuleScope;
import eu.europa.ted.efx.model.rules.ReportRule;
import eu.europa.ted.efx.model.rules.RuleSet;
import eu.europa.ted.efx.model.rules.ValidationRule;
import eu.europa.ted.efx.model.variables.Variable;

/**
 * Represents a Schematron &lt;rule&gt; element.
 */
public class SchematronRule {
  private final List<SchematronLet> variables;
  private final List<SchematronTest> tests;
  private final Context context;

  /**
   * Creates a SchematronRule for rules that apply to all notice subtypes (shared pattern).
   */
  public static SchematronRule createUniversalRule(RuleSet ruleSet) {
    return new SchematronRule(ruleSet, rule -> isUniversal(rule) && isForPostValidation(rule));
  }

  /**
   * Creates a SchematronRule for rules specific to a single notice subtype.
   * Excludes rules that apply to all subtypes (those go into the shared pattern).
   */
  public static SchematronRule createSubtypeSpecificRule(RuleSet ruleSet, String noticeSubtype) {
    return new SchematronRule(ruleSet,
        rule -> !isUniversal(rule) && isForPostValidation(rule) && appliesToNoticeSubtype(rule, noticeSubtype));
  }

  private SchematronRule(RuleSet ruleSet, Predicate<ValidationRule> filter) {
    this.context = ruleSet.getContext();

    List<SchematronLet> variables = new ArrayList<>();
    List<SchematronTest> tests = new ArrayList<>();

    for (Variable var : ruleSet.getLocalVariables()) {
      variables.add(new SchematronLet(var.name, var.initializationExpression.getScript()));
    }

    for (ValidationRule validationRule : ruleSet) {
      if (filter.test(validationRule)) {
        if (validationRule instanceof ReportRule) {
          tests.add(new SchematronReport(validationRule, this.context));
        } else {
          tests.add(new SchematronAssert(validationRule, this.context));
        }
      }
    }

    ValidationRule fallback = ruleSet.getFallbackRule();
    if (fallback != null && filter.test(fallback)) {
      if (fallback instanceof ReportRule) {
        tests.add(new SchematronReport(fallback, this.context));
      } else {
        tests.add(new SchematronAssert(fallback, this.context));
      }
    }

    this.variables = variables;
    this.tests = tests;
  }

  private static boolean isUniversal(ValidationRule rule) {
    return rule.getNoticeSubtypeRange() != null
        && rule.getNoticeSubtypeRange().isUniversal();
  }

  private static boolean appliesToNoticeSubtype(ValidationRule rule, String noticeSubtype) {
    return rule.getNoticeSubtypeRange() != null
        && rule.getNoticeSubtypeRange().asList().contains(noticeSubtype);
  }

  private static boolean isForPostValidation(ValidationRule rule) {
    return rule.getScope() != RuleScope.PRE;
  }

  private static boolean isForPreValidation(ValidationRule rule) {
    return rule.getScope() != RuleScope.POST;
  }

  /** Used by pattern.ftl */
  public String getContext() {
    return this.context.absolutePath().getScript();
  }

  /** Used by pattern.ftl */
  public List<SchematronLet> getVariables() {
    return this.variables;
  }

  /** Used by pattern.ftl */
  public List<SchematronTest> getTests() {
    return this.tests;
  }

  /** Returns true if this rule has at least one test. */
  public boolean hasTests() {
    return !this.tests.isEmpty();
  }

  /**
   * Returns true if this rule has at least one test matching the given rule natures.
   *
   * @param ruleNatures The set of rule natures to include
   * @return true if any test's nature is in the included set
   */
  public boolean hasTestsFor(Set<RuleNature> ruleNatures) {
    return this.tests.stream().anyMatch(t -> ruleNatures.contains(t.getRuleNature()));
  }

  /**
   * Returns true if this rule has at least one test matching the given tags.
   * Used by FreeMarker templates.
   *
   * @param tags The list of tags to include
   * @return true if any test's tag is in the list
   */
  public boolean hasTestsForTags(List<String> tags) {
    return this.tests.stream().anyMatch(t -> tags.contains(t.getTag()));
  }

  /**
   * Returns diagnostics for tests that need them (where subject differs from context).
   */
  public Map<String, SchematronDiagnostic> getDiagnostics() {
    Map<String, SchematronDiagnostic> diagnostics = new LinkedHashMap<>();
    for (SchematronTest test : this.tests) {
      SchematronDiagnostic diagnostic = test.getDiagnostic();
      if (diagnostic != null) {
        diagnostics.put(diagnostic.getId(), diagnostic);
      }
    }
    return diagnostics;
  }
}
