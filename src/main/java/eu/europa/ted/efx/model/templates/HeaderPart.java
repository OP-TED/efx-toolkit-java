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
package eu.europa.ted.efx.model.templates;

/**
 * A part of the header of a notice. In the header section of a view template, a line that starts
 * with the name of a part, as in {@code #PUBLICATION-DATE}, displays that part. These lines are
 * written in the order of this enumeration, each at most once.
 */
public enum HeaderPart {
  /** The main heading of the page. */
  HEADING,
  /** The line under the main heading of the page. */
  SUBHEADING,
  /** The title of the notice. */
  TITLE,
  /** The country. */
  COUNTRY,
  /** The main classification. */
  MAIN_CLASSIFICATION,
  /** The main nature of the contract. */
  MAIN_NATURE,
  /** The form type. */
  FORM_TYPE,
  /** The notice type. */
  NOTICE_TYPE,
  /** The publication number. */
  PUBLICATION_NUMBER,
  /** The issue of the OJ S. */
  OJ_ISSUE,
  /** The publication date. */
  PUBLICATION_DATE
}
