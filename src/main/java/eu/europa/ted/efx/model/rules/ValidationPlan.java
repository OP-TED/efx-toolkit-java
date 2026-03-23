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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.variables.DynamicVariable;
import eu.europa.ted.efx.model.variables.Variable;

public class ValidationPlan implements ParsedEntity {

    List<Variable> variables = new ArrayList<>();

    List<ValidationStage> stages = new ArrayList<>();

    List<String> noticeSubtypes = new ArrayList<>();

    Map<String, String> endpoints = new LinkedHashMap<>();

    public List<String> getNoticeSubtypes() {
        return new ArrayList<>(this.noticeSubtypes);
    }

    public List<ValidationStage> getStages() {
        return stages;
    }

    public void addStage(ValidationStage stage) {
        this.stages.add(stage);
    }
    
    public void addNoticeSubtype(String noticeSubtype) {
        if (!this.noticeSubtypes.contains(noticeSubtype)) {
            this.noticeSubtypes.add(noticeSubtype);
            sortNoticeSubtypes();
        }
    }

    public void addNoticeSubtypes(List<String> noticeSubtypes) {
        boolean added = false;
        for (String subtype : noticeSubtypes) {
            if (!this.noticeSubtypes.contains(subtype)) {
                this.noticeSubtypes.add(subtype);
                added = true;
            }
        }
        if (added) {
            sortNoticeSubtypes();
        }
    }

    public void addVariable(Variable variable) {
        this.variables.add(variable);
    }

    public List<Variable> getVariables() {
        return new ArrayList<>(this.variables);
    }

    public List<DynamicVariable> getDynamicVariables() {
        return this.variables.stream()
                .filter(DynamicVariable.class::isInstance)
                .map(DynamicVariable.class::cast)
                .collect(Collectors.toList());
    }

    public void declareEndpoint(String name, String url) {
        this.endpoints.put(name, url);
    }

    public Map<String, String> getEndpoints() {
        return new LinkedHashMap<>(this.endpoints);
    }

    private void sortNoticeSubtypes() {
        this.noticeSubtypes.sort(this::compareNoticeSubtype);
    }

    private int compareNoticeSubtype(String a, String b) {
        boolean aNum = isNumeric(a);
        boolean bNum = isNumeric(b);
        if (aNum && bNum) {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }
        return a.compareTo(b);
    }

    private boolean isNumeric(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}