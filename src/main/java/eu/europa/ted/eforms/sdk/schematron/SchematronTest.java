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

import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.rules.RuleNature;
import eu.europa.ted.efx.model.rules.ValidationRule;

/**
 * Base class for Schematron test elements (assert and report).
 * Both have a test attribute containing an XPath expression.
 */
public abstract class SchematronTest {
  protected final ValidationRule rule;
  protected final SchematronDiagnostic diagnostic;

  protected SchematronTest(ValidationRule rule, Context ruleContext) {
    this.rule = rule;
    this.diagnostic = rule.getSubject().symbol().equals(ruleContext.symbol())
        ? null
        : new SchematronDiagnostic(rule.getSubject(), ruleContext);
  }

  /** Used by pattern.ftl */
  public String getId() {
    return this.rule.getId();
  }

  /** Used by pattern.ftl */
  public String getRole() {
    return this.rule.getSeverity() != null
        ? this.rule.getSeverity().toString().toLowerCase()
        : "error";
  }

  /** Used by pattern.ftl */
  public String getTest() {
    return this.rule.getInvertedConditionOrExpressionCombination().getScript();
  }

  /** Used by pattern.ftl */
  public String getMessage() {
    return "rule|text|" + this.rule.getId();
  }

  /** Used by pattern.ftl */
  public SchematronDiagnostic getDiagnostic() {
    return this.diagnostic;
  }

  /** Returns the nature of the underlying validation rule */
  public RuleNature getRuleNature() {
    return this.rule.getNature();
  }

  /** Used by pattern.ftl - returns the tag for filtering (derived from rule nature) */
  public String getTag() {
    return this.rule.getNature().name();
  }

  /** Used by pattern.ftl - returns the Schematron element name ("assert" or "report") */
  public abstract String getElementName();
}
