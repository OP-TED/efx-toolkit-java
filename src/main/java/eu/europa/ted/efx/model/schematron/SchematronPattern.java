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
 * the Lic
 */
package eu.europa.ted.efx.model.schematron;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Schematron &lt;pattern&gt; element.
 */
public class SchematronPattern {
  private final String id;
  private final List<SchematronLet> variables;
  private final List<SchematronRule> rules;

  public SchematronPattern(String id) {
    this.id = id;
    this.variables = new ArrayList<>();
    this.rules = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public List<SchematronLet> getVariables() {
    return variables;
  }

  public List<SchematronRule> getRules() {
    return rules;
  }

  public void addVariable(SchematronLet variable) {
    this.variables.add(variable);
  }

  public void addRule(SchematronRule rule) {
    this.rules.add(rule);
  }
}
