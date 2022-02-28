package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.AbstractXpathLexer;
import eu.europa.ted.efx.AbstractXpathParser;

@SuppressWarnings("static-method")
public class SimpleXpathTest {

  @Test
  void testArray() {
    // @formatter:off
		String[][] tests = {
			// {"context", 	"xpath to contextualize", 	"expected relative xpath"}
			{"/a/b", 		"/a/b/c", 					"c"},
			{"/a/b[a]", 	"/a/b[a]/c", 				"c"},
			{"/a/b[a]", 	"/a/b[b]/c", 				"../b[b]/c"},
	 	};
		// @formatter:on
    for (String[] test : tests) {
      assertEquals(test[2], XpathContextualizer.contextualize(test[0], test[1]));
    }
  }

  @Test
  void contextualizeFile() {
    String contextualized = null;
    try {

      final AbstractXpathLexer lexer =
          new AbstractXpathLexer(CharStreams.fromFileName("src/test/resources/examples.xpath"));
      final CommonTokenStream tokens = new CommonTokenStream(lexer);
      final AbstractXpathParser parser = new AbstractXpathParser(tokens);
      final ParseTree tree = parser.file();

      final ParseTreeWalker walker = new ParseTreeWalker();
      final XpathContextualizer simplifier = new XpathContextualizer();
      walker.walk(simplifier, tree);
      contextualized = simplifier.getContextualizedXpath();

    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.print(contextualized);
  }

}
