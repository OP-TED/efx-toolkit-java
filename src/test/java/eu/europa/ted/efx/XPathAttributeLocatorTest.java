package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

public class XPathAttributeLocatorTest {
	@Test
    public void testXPathAttributeLocator_WithAttribute() {
        final XPathAttributeLocator locator = XPathAttributeLocator.findAttribute(
				new PathExpression("/path/path/@attribute"));
        assertEquals("/path/path", locator.getPath().script);
        assertEquals("attribute", locator.getAttribute());
    }

    @Test
    public void testXPathAttributeLocator_WithMultipleAttributes() {
        final XPathAttributeLocator locator = XPathAttributeLocator.findAttribute(
				new PathExpression("/path/path[@otherAttribute = 'text']/@attribute"));
        assertEquals("/path/path[@otherAttribute = 'text']", locator.getPath().script);
        assertEquals("attribute", locator.getAttribute());
    }

    @Test
    public void testXPathAttributeLocator_WithoutAttribute() {
        final XPathAttributeLocator locator = XPathAttributeLocator.findAttribute(
				new PathExpression("/path/path[@otherAttribute = 'text']"));
        assertEquals("/path/path[@otherAttribute = 'text']", locator.getPath().script);
        assertNull(locator.getAttribute());
    }

    @Test
    public void testXPathAttributeLocator_WithoutPath() {
        final XPathAttributeLocator locator = XPathAttributeLocator.findAttribute(
				new PathExpression("@attribute"));
        assertEquals("", locator.getPath().script);
        assertEquals("attribute", locator.getAttribute());
    }
}
