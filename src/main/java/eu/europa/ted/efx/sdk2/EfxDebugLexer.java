package eu.europa.ted.efx.sdk2;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EfxDebugLexer extends EfxLexer {

      private static final Logger logger = LoggerFactory.getLogger(EfxDebugLexer.class);
      private static final String FORMAT_16S = "%-16s";

    public EfxDebugLexer(CharStream input) {
        super(input);
    }

    @Override
    public Token nextToken() {
        // remember mode before
        int modeBefore = this._mode;
        Token t = super.nextToken();
        if (t.getType() == Token.EOF)
            return t;

        // remember mode after
        int modeAfter = this._mode;

        String nameBefore = String.format(FORMAT_16S, getModeNames()[modeBefore]);
        String nameAfter = String.format(FORMAT_16S, getModeNames()[modeAfter]);
        String tokName = String.format(FORMAT_16S, getVocabulary().getSymbolicName(t.getType()));
        String text = t.getText().replace("\n", "\\n");

        // build a human-readable stack of mode-names
        StringBuilder sb = new StringBuilder("[");
        // modeStack is protected in Lexer
        for (int i = 0; i < this._modeStack.size(); i++) {
            if (i > 0)
                sb.append(", ");
            int m = this._modeStack.get(i);
            sb.append(getModeNames()[m]);
        }
        sb.append("]");

        String paddedStack = String.format("%-41s", sb.toString());

        logger.debug(
                "[before={} after={} stack={}] {} text=\"{}\"",
                nameBefore, nameAfter, paddedStack, tokName, text);
        return t;
    }

}