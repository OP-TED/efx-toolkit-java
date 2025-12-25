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
import java.util.Iterator;
import java.util.List;

import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.variables.Variable;

public class RuleSet implements ParsedEntity, Iterable<ValidationRule> {

    Context context;
    List<ValidationRule> rules;
    ValidationRule fallbackRule;

    List<Variable> stageVariables;
    List<Variable> localVariables;

    public RuleSet() {
        this.context = null;
        this.rules = new ArrayList<>();
        this.stageVariables = new ArrayList<>();
        this.localVariables = new ArrayList<>();
        this.fallbackRule = null;
    }

    public void setContext(Context context, Variable contextVariable) {
        this.context = context;
        if (contextVariable != null) {
            this.addVariable(contextVariable);
        }
    }

    public void addRule(ValidationRule rule) {
        this.rules.add(rule);
    }

    public void setFallbackRule(ValidationRule fallbackRule) {
        this.fallbackRule = fallbackRule;
    }

    public void addVariable(Variable variable) {
        if (this.context == null) {
            this.stageVariables.add(variable);
        } else {
            this.localVariables.add(variable);
        }
    }

    @Override
    public Iterator<ValidationRule> iterator() {
        return this.rules.iterator();
    }

    public Context getContext() {
        return this.context;
    }

    public List<Variable> getStageVariables() {
        return this.stageVariables;
    }

    public List<Variable> getLocalVariables() {
        return this.localVariables;
    }

    public ValidationRule getFallbackRule() {
        return this.fallbackRule;
    }
}