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
 * Represents a single Schematron pattern file (e.g., validation-stage-1a.sch).
 * Each file contains one pattern element.
 */
public class SchematronFile {
  private final String filename;
  private final SchematronPattern pattern;
  private final boolean isDynamic;

  public SchematronFile(String filename, SchematronPattern pattern, boolean isDynamic) {
    this.filename = filename;
    this.pattern = pattern;
    this.isDynamic = isDynamic;
  }

  public String getFilename() {
    return filename;
  }

  public SchematronPattern getPattern() {
    return pattern;
  }

  public boolean isDynamic() {
    return isDynamic;
  }
}
