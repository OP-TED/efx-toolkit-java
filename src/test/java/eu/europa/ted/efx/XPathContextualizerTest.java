package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.xpath.XPathContextualizer;

public class XPathContextualizerTest {

  private String contextualize(final String context, final String xpath) {
    return XPathContextualizer.contextualize(new PathExpression(context),
        new PathExpression(xpath)).script;
  }

  @Test
  public void testIdentical() {
    assertEquals(".", contextualize("/a/b/c", "/a/b/c"));
  }

  @Test
  public void testContextEmpty() {
    assertEquals("/a/b/c", contextualize("", "/a/b/c"));
  }

  @Test
  public void testUnderContext() {
    assertEquals("c", contextualize("/a/b", "/a/b/c"));
  }

  @Test
  public void testAboveContext() {
    assertEquals("..", contextualize("/a/b/c", "/a/b"));
  }

  @Test
  public void testSibling() {
    assertEquals("../d", contextualize("/a/b/c", "/a/b/d"));
  }

  @Test
  public void testTwoLevelsDifferent() {
    assertEquals("../../x/y", contextualize("/a/b/c/d", "/a/b/x/y"));
  }

  @Test
  public void testAllDifferent() {
    assertEquals("../../../x/y/z", contextualize("/a/b/c/d", "/a/x/y/z"));
  }

  @Test
  public void testDifferentRoot() {
    // Not realistic, as XML has a single root, but a valid result
    assertEquals("../../../x/y/z", contextualize("/a/b/c", "/x/y/z"));
  }

  @Test
  public void testAttributeInXpath() {
    assertEquals("../c/@attribute", contextualize("/a/b", "/a/c/@attribute"));
  }

  @Test
  public void testAttributeInContext() {
    assertEquals("../c/d", contextualize("/a/b/@attribute", "/a/b/c/d"));
  }

  @Test
  public void testAttributeInBoth() {
    assertEquals("../@x", contextualize("/a/b/c/@d", "/a/b/c/@x"));
  }

  @Test
  public void testAttributeInBothSame() {
    assertEquals(".", contextualize("/a/b/c/@d", "/a/b/c/@d"));
  }

  @Test
  public void testPredicateInXpathLeaf() {
    assertEquals("../d[x/y = 'z']", contextualize("/a/b/c", "/a/b/d[x/y = 'z']"));
  }

  @Test
  public void testPredicateInContextLeaf() {
    assertEquals("../d", contextualize("/a/b/c[e/f = 'z']", "/a/b/d"));
  }

  @Test
  public void testPredicateInBothLeaf() {
    assertEquals("../d[x = 'y']", contextualize("/a/b/c[e = 'f']", "/a/b/d[x = 'y']"));
  }

  @Test
  public void testPredicateInXpathMiddle() {
    assertEquals("../../b[x/y = 'z']/d", contextualize("/a/b/c", "/a/b[x/y = 'z']/d"));
  }

  @Test
  public void testPredicateInContextMiddle() {
    assertEquals("../d", contextualize("/a/b[e/f = 'z']/c", "/a/b/d"));
  }

  @Test
  public void testPredicateSameInBoth() {
    assertEquals("../d", contextualize("/a/b[e/f = 'z']/c", "/a/b[e/f = 'z']/d"));
  }

  @Test
  public void testPredicateDifferentOnSameElement() {
    assertEquals("../../b[x = 'y']/d", contextualize("/a/b[e = 'f']/c", "/a/b[x = 'y']/d"));
  }

  @Test
  public void testPredicateDifferent() {
    assertEquals("../c[x = 'y']/d", contextualize("/a/b[e = 'f']/c", "/a/b/c[x = 'y']/d"));
  }

  @Test
  public void testPredicateMoreInXpath() {
    assertEquals("../../b[e][f]/c/d", contextualize("/a/b[e]/c", "/a/b[e][f]/c/d"));
  }

  @Test
  public void testPredicateMoreInContext() {
    assertEquals("d", contextualize("/a/b[e][f]/c", "/a/b[e]/c/d"));
  }

  @Test
  public void testSeveralPredicatesIdentical() {
    assertEquals("d", contextualize("/a/b[e][f]/c", "/a/b[e][f]/c/d"));
  }

  @Test
  public void testSeveralPredicatesOneDifferent() {
    assertEquals("../../b[e][x]/c/d", contextualize("/a/b[e][f]/c", "/a/b[e][x]/c/d"));
  }
}
