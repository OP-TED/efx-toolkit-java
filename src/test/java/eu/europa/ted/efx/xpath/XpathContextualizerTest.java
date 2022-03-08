package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.AbstractXpathLexer;

@SuppressWarnings("static-method")
public class XpathContextualizerTest {

  @Test
  void testArray() {
    // @formatter:off
		String[][] tests = {
			// {"context", 	"xpath to contextualize", 	"expected relative xpath"}
			{"/a/b", 		      "/a/b/c", 			      "c"},
			{"/a/b[a]", 	    "/a/b[a]/c", 		      "c"},
      {"/*/a/b[c][d]/e", "/*/a/b[f]/g",         "../../b[f]/g"},
			{"/a/b[a]", 	    "/a/b[b]/c", 		      "../b[b]/c"},
      {"/*/a[if (b/c, d/e) then /a else b]",  "/*/a/b", "b"},
      {"/*/a:b/c", "/*/a:b[for $var in ../f/c, $var2 in ./x/y return 0]/d", "../../a:b[for $var in ../f/c, $var2 in ./x/y return 0]/d"}
	 	};
		// @formatter:on
    for (String[] test : tests) {
      assertEquals(test[2], XpathContextualizer.contextualize(test[0], test[1]));
    }
  }

  @Test
  public void ContextualizePair() {
    String context =  "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:NoticeResult/efac:LotTender/efac:SubcontractingTerm/efac:FieldsPrivacy[efbc:FieldIdentifierCode/text()='sub-val']";
    String path =     "/*/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/efext:EformsExtension/efac:Organizations/efac:Organization/efac:TouchPoint/cac:PostalAddress/cbc:AdditionalStreetName";
    System.out.println(XpathContextualizer.contextualize(context, path));
  }
}
