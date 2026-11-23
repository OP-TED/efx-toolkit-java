package eu.europa.ted.efx.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Checks the factories of {@link FragmentOutline} and the rules they keep.
 */
class FragmentOutlineTest {

  @Test
  void testAHeadingKeepsWhatItIsGiven() {
    final FragmentOutline heading = FragmentOutline.heading("1.2", 3, 2, "1_2");
    assertEquals(FragmentKind.HEADING, heading.getKind());
    assertEquals("1.2", heading.getNumber());
    assertEquals(3, heading.getNestingLevel());
    assertEquals(2, heading.getHeadingLevel());
    assertEquals("1_2", heading.getNavIdBody());
  }

  @Test
  void testAHeadingWrittenWithABareMarkerHasNoNumberAndNoNavIdBody() {
    final FragmentOutline heading = FragmentOutline.heading("", 1, 1, "");
    assertEquals("", heading.getNumber());
    assertEquals("", heading.getNavIdBody());
  }

  @Test
  void testAHeadingHasAHeadingLevelOfAtLeast1() {
    assertThrows(IllegalArgumentException.class, () -> FragmentOutline.heading("1", 1, 0, "1"));
  }

  @Test
  void testAHeadingIsNotNestedInMoreHeadingsThanLines() {
    assertThrows(IllegalArgumentException.class, () -> FragmentOutline.heading("1", 2, 3, "1"));
  }

  @Test
  void testANormalLineHasHeadingLevel0AndNoNavIdBody() {
    final FragmentOutline line = FragmentOutline.normalLine("2", 2);
    assertEquals(FragmentKind.NORMAL, line.getKind());
    assertEquals("2", line.getNumber());
    assertEquals(2, line.getNestingLevel());
    assertEquals(0, line.getHeadingLevel());
    assertEquals("", line.getNavIdBody());
  }

  @Test
  void testAnInvocationHasNoNumberHeadingLevel0AndNoNavIdBody() {
    final FragmentOutline invocation = FragmentOutline.invocation(3);
    assertEquals(FragmentKind.INVOCATION, invocation.getKind());
    assertEquals("", invocation.getNumber());
    assertEquals(3, invocation.getNestingLevel());
    assertEquals(0, invocation.getHeadingLevel());
    assertEquals("", invocation.getNavIdBody());
  }

  @Test
  void testATemplateIsAtNestingLevel0WithNothingElse() {
    final FragmentOutline template = FragmentOutline.template();
    assertEquals(FragmentKind.TEMPLATE, template.getKind());
    assertEquals("", template.getNumber());
    assertEquals(0, template.getNestingLevel());
    assertEquals(0, template.getHeadingLevel());
    assertEquals("", template.getNavIdBody());
    assertSame(template, FragmentOutline.template());
  }

  @Test
  void testALineIsAtNestingLevel1OrMore() {
    assertThrows(IllegalArgumentException.class, () -> FragmentOutline.heading("1", 0, 1, "1"));
    assertThrows(IllegalArgumentException.class, () -> FragmentOutline.normalLine("", 0));
    assertThrows(IllegalArgumentException.class, () -> FragmentOutline.invocation(0));
  }

  @Test
  void testTheTextsCannotBeNull() {
    assertThrows(NullPointerException.class, () -> FragmentOutline.heading(null, 1, 1, ""));
    assertThrows(NullPointerException.class, () -> FragmentOutline.heading("", 1, 1, null));
    assertThrows(NullPointerException.class, () -> FragmentOutline.normalLine(null, 1));
  }

  @Test
  void testOutlinesWithTheSameValuesAreEqual() {
    assertEquals(FragmentOutline.heading("1", 1, 1, "1"), FragmentOutline.heading("1", 1, 1, "1"));
    assertEquals(FragmentOutline.heading("1", 1, 1, "1").hashCode(),
        FragmentOutline.heading("1", 1, 1, "1").hashCode());
    assertNotEquals(FragmentOutline.heading("1", 1, 1, "1"),
        FragmentOutline.heading("1", 1, 1, "lot"));
    assertNotEquals(FragmentOutline.normalLine("", 1), FragmentOutline.invocation(1));
  }
}
