package eu.europa.ted.efx.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Checks the values of {@link OutlineOffset} and the rules they keep.
 */
class OutlineOffsetTest {

  @Test
  void testNonePutsNothingAbove() {
    assertEquals(0, OutlineOffset.NONE.getNestingLevelOffset());
    assertEquals(0, OutlineOffset.NONE.getHeadingLevelOffset());
    assertEquals("", OutlineOffset.NONE.getNavIdPrefix());
  }

  @Test
  void testATemplateCallKeepsWhatItIsGiven() {
    final OutlineOffset offset = OutlineOffset.ofTemplateCall(2, 1, "1");
    assertEquals(2, offset.getNestingLevelOffset());
    assertEquals(1, offset.getHeadingLevelOffset());
    assertEquals("1", offset.getNavIdPrefix());
  }

  @Test
  void testATemplateCallAtTheTopPutsNothingAbove() {
    assertEquals(OutlineOffset.NONE, OutlineOffset.ofTemplateCall(0, 0, ""));
  }

  @Test
  void testATemplateCallWithoutAHeadingAboveHasNoPrefix() {
    final OutlineOffset offset = OutlineOffset.ofTemplateCall(2, 0, "");
    assertEquals(0, offset.getHeadingLevelOffset());
    assertEquals("", offset.getNavIdPrefix());
    assertThrows(IllegalArgumentException.class, () -> OutlineOffset.ofTemplateCall(2, 0, "1"));
  }

  @Test
  void testTheOffsetsCannotBeNegative() {
    assertThrows(IllegalArgumentException.class, () -> OutlineOffset.ofTemplateCall(-1, 0, ""));
    assertThrows(IllegalArgumentException.class, () -> OutlineOffset.ofTemplateCall(1, -1, ""));
  }

  @Test
  void testTheCallingLineIsNotNestedInMoreHeadingsThanLines() {
    assertThrows(IllegalArgumentException.class, () -> OutlineOffset.ofTemplateCall(1, 2, "1_2"));
  }

  @Test
  void testThePrefixCannotBeNull() {
    assertThrows(NullPointerException.class, () -> OutlineOffset.ofTemplateCall(0, 0, null));
  }

  @Test
  void testOffsetsWithTheSameValuesAreEqual() {
    assertEquals(OutlineOffset.ofTemplateCall(2, 2, "1_2"),
        OutlineOffset.ofTemplateCall(2, 2, "1_2"));
    assertEquals(OutlineOffset.ofTemplateCall(2, 2, "1_2").hashCode(),
        OutlineOffset.ofTemplateCall(2, 2, "1_2").hashCode());
    assertNotEquals(OutlineOffset.ofTemplateCall(2, 1, "1"),
        OutlineOffset.ofTemplateCall(2, 1, "2"));
  }
}
