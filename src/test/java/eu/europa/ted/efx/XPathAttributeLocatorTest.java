package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.xpath.XPathAttributeLocator;

class XPathAttributeLocatorTest {
  private void testAttribute(final String attributePath, final String expectedPath,
      final String expectedAttribute) {
    final XPathAttributeLocator locator =
        XPathAttributeLocator.findAttribute(attributePath);

    assertEquals(expectedPath, locator.getPath().getScript());
    assertEquals(expectedAttribute, locator.getAttribute());
  }

  @Test
  void testXPathAttributeLocator_WithAttribute() {
    testAttribute("/path/path/@attribute", "/path/path", "attribute");
  }

  @Test
  void testXPathAttributeLocator_WithMultipleAttributes() {
    testAttribute("/path/path[@otherAttribute = 'text']/@attribute",
        "/path/path[@otherAttribute = 'text']", "attribute");
  }

  @Test
  void testXPathAttributeLocator_WithoutAttribute() {
    final XPathAttributeLocator locator = XPathAttributeLocator
        .findAttribute("/path/path[@otherAttribute = 'text']");
    assertEquals("/path/path[@otherAttribute = 'text']", locator.getPath().getScript());
    assertNull(locator.getAttribute());
  }

  @Test
  void testXPathAttributeLocator_WithoutPath() {
    testAttribute("@attribute", "", "attribute");
  }
}
