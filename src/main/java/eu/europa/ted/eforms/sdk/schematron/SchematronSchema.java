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
import java.util.List;

/**
 * Represents a Schematron schema (the root &lt;schema&gt; element).
 * This is used for the complete-validation.sch master file.
 */
public class SchematronSchema {
  private final String title;
  private final List<SchematronParam> params = new ArrayList<>();
  private final List<SchematronLet> letElements = new ArrayList<>();
  private final List<SchematronDiagnostic> diagnostics = new ArrayList<>();
  private final List<SchematronPhase> phases = new ArrayList<>();
  private final List<String> includes = new ArrayList<>();

  public SchematronSchema(String title) {
    this.title = title;
  }

  /** Used by complete-validation.ftl */
  public String getTitle() {
    return this.title;
  }

  /** Used by complete-validation.ftl */
  public List<SchematronParam> getParams() {
    return this.params;
  }

  /** Used by complete-validation.ftl */
  public List<SchematronLet> getLetElements() {
    return this.letElements;
  }

  /** Used by complete-validation.ftl */
  public List<SchematronDiagnostic> getDiagnostics() {
    return this.diagnostics;
  }

  /** Used by complete-validation.ftl */
  public List<SchematronPhase> getPhases() {
    return this.phases;
  }

  /** Used by complete-validation.ftl */
  public List<String> getIncludes() {
    return this.includes;
  }

  public void addParam(SchematronParam param) {
    this.params.add(param);
  }

  public void addLetElement(SchematronLet letElement) {
    this.letElements.add(letElement);
  }

  public void addDiagnostic(SchematronDiagnostic diagnostic) {
    this.diagnostics.add(diagnostic);
  }

  public void addPhase(SchematronPhase phase) {
    this.phases.add(phase);
  }

  public void addInclude(String href) {
    this.includes.add(href);
  }
}
