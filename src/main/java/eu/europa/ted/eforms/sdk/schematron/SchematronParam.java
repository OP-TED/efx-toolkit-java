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

/**
 * Represents a Schematron &lt;param&gt; element for API endpoint parameters.
 */
public class SchematronParam {
  private final String name;
  private final String value;

  public SchematronParam(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /** Used by complete-validation.ftl */
  public String getName() {
    return this.name;
  }

  /** Used by complete-validation.ftl */
  public String getValue() {
    return this.value;
  }
}
