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
  void testAll_IsUniversal() {
    NoticeSubtypeRange range = NoticeSubtypeRange.all(SUBTYPES);
    assertEquals(SUBTYPES, range.asList());
    assertTrue(range.isUniversal());
  }

  @Test
  void testBuilder_EveryNoticeSubtype_IsUniversal() {
    NoticeSubtypeRange range = new NoticeSubtypeRange.Builder(SUBTYPES).add("1").add("2", "3").build();
    assertEquals(SUBTYPES, range.asList());
    assertTrue(range.isUniversal());
  }

  @Test
  void testBuilder_KeepsTheOrderWritten() {
    NoticeSubtypeRange range =
        new NoticeSubtypeRange.Builder(SUBTYPES).add("3").add("1", "2").add("3").build();
    assertEquals(List.of("3", "1", "2", "3"), range.asList());
    assertTrue(range.isUniversal());
  }

  @Test
  void testBuilder_SomeNoticeSubtypes_IsNotUniversal() {
    NoticeSubtypeRange range = new NoticeSubtypeRange.Builder(SUBTYPES).add("2").build();
    assertEquals(List.of("2"), range.asList());
    assertFalse(range.isUniversal());
  }

  @Test
  void testBuilder_NoNoticeSubtype_IsNotUniversal() {
    NoticeSubtypeRange range = new NoticeSubtypeRange.Builder(SUBTYPES).build();
    assertEquals(List.of(), range.asList());
    assertFalse(range.isUniversal());
  }
}
