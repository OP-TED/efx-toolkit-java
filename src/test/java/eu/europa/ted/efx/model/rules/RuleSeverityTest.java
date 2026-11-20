/*
 * Copyright 2026 European Union
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

class RuleSeverityTest {

  @Test
  @ResourceLock(Resources.LOCALE)
  void testFromString_LowerCaseWhateverTheDefaultLocale() {
    // Case conversion reads the default locale, not the one for formatting
    Locale defaultLocale = Locale.getDefault();
    Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
    Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      assertEquals(RuleSeverity.INFO, RuleSeverity.fromString("info"));
      assertEquals(RuleSeverity.WARNING, RuleSeverity.fromString("warning"));
    } finally {
      Locale.setDefault(defaultLocale);
      Locale.setDefault(Locale.Category.FORMAT, formatLocale);
      Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
    }
  }
}
