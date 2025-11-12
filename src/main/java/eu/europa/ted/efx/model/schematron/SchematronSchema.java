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
 * Represents a Schematron schema (the root &lt;schema&gt; element).
 * This is used for the complete-validation.sch master file.
 */
public class SchematronSchema {
  private final String title;
  private final List<SchematronLet> globalVariables;
  private final List<SchematronPhase> phases;
  private final List<String> includes;

  public SchematronSchema(String title) {
    this.title = title;
    this.globalVariables = new ArrayList<>();
    this.phases = new ArrayList<>();
    this.includes = new ArrayList<>();
  }

  public String getTitle() {
    return title;
  }

  public List<SchematronLet> getGlobalVariables() {
    return globalVariables;
  }

  public List<SchematronPhase> getPhases() {
    return phases;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void addGlobalVariable(SchematronLet variable) {
    this.globalVariables.add(variable);
  }

  public void addPhase(SchematronPhase phase) {
    this.phases.add(phase);
  }

  public void addInclude(String href) {
    this.includes.add(href);
  }
}
