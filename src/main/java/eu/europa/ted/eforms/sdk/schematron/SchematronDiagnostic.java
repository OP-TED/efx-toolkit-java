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
package eu.europa.ted.eforms.sdk.schematron;

import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.model.Context;

/**
 * Represents a Schematron &lt;diagnostic&gt; element.
 * Used in the &lt;diagnostics&gt; section of complete-validation.sch.
 * Format: &lt;diagnostic id="..." see="field:..."&gt;xpath&lt;/diagnostic&gt;
 */
public class SchematronDiagnostic {

  private final String id;
  private final String seeAttribute;
  private final String xpath;

  private static String sanitize(String identifier) {
    return identifier.replace("(", "_").replace(")", "_");
  }

  public SchematronDiagnostic(Context subject, Context ruleContext) {
    this.xpath = XPathProcessor.contextualize(
        ruleContext.absolutePath().getScript(), subject.absolutePath().getScript());
    this.id = sanitize(ruleContext.symbol()) + "_" + sanitize(subject.symbol());
    String prefix = subject.isFieldContext() ? "field:" : "node:";
    this.seeAttribute = prefix + subject.symbol();
  }

  /** Used by pattern.ftl and complete-validation.ftl */
  public String getId() {
    return this.id;
  }

  /** Used by complete-validation.ftl */
  public String getSeeAttribute() {
    return this.seeAttribute;
  }

  /** Used by complete-validation.ftl */
  public String getXpath() {
    return this.xpath;
  }
}
