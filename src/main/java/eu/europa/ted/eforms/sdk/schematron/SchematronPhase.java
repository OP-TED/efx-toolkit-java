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
 * Represents a Schematron &lt;phase&gt; element.
 */
public class SchematronPhase {
  private final String noticeSubtype;
  private final List<String> activePatterns = new ArrayList<>();

  public SchematronPhase(String noticeSubtype) {
    this.noticeSubtype = noticeSubtype;
  }

  /** Used by complete-validation.ftl */
  public String getId() {
    return "eforms-" + this.noticeSubtype;
  }

  /** Used by complete-validation.ftl */
  public List<String> getActivePatterns() {
    return this.activePatterns;
  }

  public void addActivePattern(String patternId) {
    this.activePatterns.add(patternId);
  }
}
