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
package eu.europa.ted.efx.model.rules;

import java.util.ArrayList;
import java.util.List;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.variables.Variable;

public class ValidationStage implements ParsedEntity {

    String name;
    List<RuleSet> ruleSets;
    List<Variable> variables;

    public ValidationStage(String name) {
        this.name = name;
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

    public List<RuleSet> getRuleSets() {
        return this.ruleSets;
    }

    public List<Variable> getVariables() {
        return this.variables;
    }
}