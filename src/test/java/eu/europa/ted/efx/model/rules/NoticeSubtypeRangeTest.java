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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class NoticeSubtypeRangeTest {

  private static final List<String> SUBTYPES = List.of("1", "2", "3");

  @Test
  void testStar_ReadByItsText() {
    // Trimming " * " gives a string other than the constant "*", with the same text
    NoticeSubtypeRange range = new NoticeSubtypeRange(" * ", SUBTYPES);
    assertEquals(SUBTYPES, range.asList());
    assertTrue(range.isUniversal());
  }

  @Test
  void testRanges_GiveTheSubtypesOfTheirList() {
    NoticeSubtypeRange ranges = new NoticeSubtypeRange(
        List.of(new NoticeSubtypeRange("3", SUBTYPES), new NoticeSubtypeRange("1-2", SUBTYPES)),
        SUBTYPES);
    assertEquals(new NoticeSubtypeRange("3, 1-2", SUBTYPES).asList(), ranges.asList());
    assertEquals(List.of("3", "1", "2"), ranges.asList());
    assertTrue(ranges.isUniversal());
  }

  @Test
  void testRanges_ThatMissASubtype_AreNotUniversal() {
    NoticeSubtypeRange ranges = new NoticeSubtypeRange(
        List.of(new NoticeSubtypeRange("1", SUBTYPES), new NoticeSubtypeRange("3", SUBTYPES)),
        SUBTYPES);
    assertEquals(List.of("1", "3"), ranges.asList());
    assertFalse(ranges.isUniversal());
  }
}
