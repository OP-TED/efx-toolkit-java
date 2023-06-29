package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.xpath.XPathContextualizer;

class XPathContextualizerTest {
  private String contextualize(final String context, final String xpath) {
    return XPathContextualizer.contextualize(new PathExpression(context),
        new PathExpression(xpath)).script;
  }

  @Test
  void testIdentical() {
    assertEquals(".", contextualize("/a/b/c", "/a/b/c"));
  }

  @Test
  void testContextEmpty() {
    assertEquals("/a/b/c", contextualize("", "/a/b/c"));
  }

  @Test
  void testUnderContext() {
    assertEquals("c", contextualize("/a/b", "/a/b/c"));
  }

  @Test
  void testAboveContext() {
    assertEquals("..", contextualize("/a/b/c", "/a/b"));
  }

  @Test
  void testSibling() {
    assertEquals("../d", contextualize("/a/b/c", "/a/b/d"));
  }

  @Test
  void testTwoLevelsDifferent() {
    assertEquals("../../x/y", contextualize("/a/b/c/d", "/a/b/x/y"));
  }

  @Test
  void testAllDifferent() {
    assertEquals("../../../x/y/z", contextualize("/a/b/c/d", "/a/x/y/z"));
  }

  @Test
  void testDifferentRoot() {
    // Not realistic, as XML has a single root, but a valid result
    assertEquals("../../../x/y/z", contextualize("/a/b/c", "/x/y/z"));
  }

  @Test
  void testAttributeInXpath() {
    assertEquals("../c/@attribute", contextualize("/a/b", "/a/c/@attribute"));
  }

  @Test
  void testAttributeInContext() {
    assertEquals("../c/d", contextualize("/a/b/@attribute", "/a/b/c/d"));
  }

  @Test
  void testAttributeInBoth() {
    assertEquals("../@x", contextualize("/a/b/c/@d", "/a/b/c/@x"));
  }

  @Test
  void testAttributeInBothSame() {
    assertEquals(".", contextualize("/a/b/c/@d", "/a/b/c/@d"));
  }

  @Test
  void testPredicateInXpathLeaf() {
    assertEquals("../d[x/y = 'z']", contextualize("/a/b/c", "/a/b/d[x/y = 'z']"));
  }

  @Test
  void testPredicateBeingTheOnlyDifference() {
    assertEquals(".[x/y = 'z']", contextualize("/a/b/c", "/a/b/c[x/y = 'z']"));
  }

  @Test
  void testPredicatesBeingTheOnlyDifferences() {
    assertEquals("..[x/y = 'z']/c[x/y = 'z']", contextualize("/a/b/c", "/a/b[x/y = 'z']/c[x/y = 'z']"));
  }

  @Test
  void testPredicateInContextLeaf() {
    assertEquals("../d", contextualize("/a/b/c[e/f = 'z']", "/a/b/d"));
  }

  @Test
  void testPredicateInBothLeaf() {
    assertEquals("../d[x = 'y']", contextualize("/a/b/c[e = 'f']", "/a/b/d[x = 'y']"));
  }

  @Test
  void testPredicateInXpathMiddle() {
    assertEquals("..[x/y = 'z']/d", contextualize("/a/b/c", "/a/b[x/y = 'z']/d"));
  }

  @Test
  void testPredicateInContextMiddle() {
    assertEquals("../d", contextualize("/a/b[e/f = 'z']/c", "/a/b/d"));
  }

  @Test
  void testPredicateSameInBoth() {
    assertEquals("../d", contextualize("/a/b[e/f = 'z']/c", "/a/b[e/f = 'z']/d"));
  }

  @Test
  void testPredicateDifferentOnSameElement() {
    assertEquals("../../b[x = 'y']/d", contextualize("/a/b[e = 'f']/c", "/a/b[x = 'y']/d"));
  }

  @Test
  void testPredicateDifferent() {
    assertEquals(".[x = 'y']/d", contextualize("/a/b[e = 'f']/c", "/a/b/c[x = 'y']/d"));
  }

  @Test
  void testPredicateMoreInXpath() {
    assertEquals("../../b[e][f]/c/d", contextualize("/a/b[e]/c", "/a/b[e][f]/c/d"));
  }

  @Test
  void testPredicateMoreInContext() {
    assertEquals("d", contextualize("/a/b[e][f]/c", "/a/b[e]/c/d"));
  }

  @Test
  void testSeveralPredicatesIdentical() {
    assertEquals("d", contextualize("/a/b[e][f]/c", "/a/b[e][f]/c/d"));
  }

  @Test
  void testSeveralPredicatesOneDifferent() {
    assertEquals("../../b[e][x]/c/d", contextualize("/a/b[e][f]/c", "/a/b[e][x]/c/d"));
  }
}
