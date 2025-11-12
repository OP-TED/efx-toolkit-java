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

/**
 * Represents a Schematron &lt;assert&gt; element.
 */
public class SchematronAssert {
  private final String id;
  private final String role;
  private final String test;
  private final String message;

  public SchematronAssert(String id, String role, String test, String message) {
    this.id = id;
    this.role = role;
    this.test = test;
    this.message = message;
  }

  public String getId() {
    return id;
  }

  public String getRole() {
    return role;
  }

  public String getTest() {
    return test;
  }

  public String getMessage() {
    return message;
  }
}
