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
package eu.europa.ted.efx.model.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.variables.DynamicVariable;
import eu.europa.ted.efx.model.variables.Variable;

public class ValidationStage implements ParsedEntity {

    String name;
    ValidationPlan validationPlan;
    List<RuleSet> ruleSets;
    List<Variable> variables;

    public ValidationStage(final String name, final ValidationPlan validationPlan) {
        this.name = name;
        this.validationPlan = validationPlan;
        this.ruleSets = new ArrayList<>();
        this.variables = new ArrayList<>();
    }

    public void addRuleSet(RuleSet ruleSet) {
        this.ruleSets.add(ruleSet);
    }

    public void addVariable(Variable variable) {
        this.variables.add(variable);
    }

    public String getName() {
        return this.name;
    }

    public ValidationPlan getValidationPlan() {
        return this.validationPlan;
    }

    public List<RuleSet> getRuleSets() {
        return this.ruleSets;
    }

    public List<Variable> getVariables() {
        return this.variables;
    }

    public List<Variable> getInheritedVariables() {
        return this.validationPlan.getVariables();
    }

    public List<Variable> getAllVariables() {
        List<Variable> allVariables = new ArrayList<>(this.validationPlan.getVariables());
        allVariables.addAll(this.variables);
        return allVariables;
    }

    public List<DynamicVariable> getDynamicVariables() {
        return this.variables.stream()
                .filter(DynamicVariable.class::isInstance)
                .map(DynamicVariable.class::cast)
                .collect(Collectors.toList());
    }

    public boolean containsUniversalRules() {
        for (RuleSet ruleSet : this.ruleSets) {
            for (ValidationRule rule : ruleSet) {
                if (rule.getNoticeSubtypeRange() != null && rule.getNoticeSubtypeRange().isUniversal()) {
                    return true;
                }
            }
            ValidationRule fallback = ruleSet.getFallbackRule();
            if (fallback != null && fallback.getNoticeSubtypeRange() != null
                && fallback.getNoticeSubtypeRange().isUniversal()) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getNoticeSubtypes() {
        Set<String> subtypes = new LinkedHashSet<>();
        for (RuleSet ruleSet : this.ruleSets) {
            for (ValidationRule rule : ruleSet) {
                if (rule.getNoticeSubtypeRange() != null && !rule.getNoticeSubtypeRange().isUniversal()) {
                    subtypes.addAll(rule.getNoticeSubtypeRange().asList());
                }
            }
            ValidationRule fallback = ruleSet.getFallbackRule();
            if (fallback != null && fallback.getNoticeSubtypeRange() != null
                && !fallback.getNoticeSubtypeRange().isUniversal()) {
                subtypes.addAll(fallback.getNoticeSubtypeRange().asList());
            }
        }
        return subtypes;
    }
}
