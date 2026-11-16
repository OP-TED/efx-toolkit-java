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
package eu.europa.ted.efx.sdk2;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.misc.Interval;

/**
 * The EFX-2 lexer, which words its errors as {@link EfxErrorStrategy} words those of the parser:
 * "Syntax error: unexpected character '¤'." for a character that starts no token, and "Syntax
 * error: unexpected end of line in text literal." for a token it cannot finish.
 */
public class EfxErrorReportingLexer extends EfxLexer {

    public EfxErrorReportingLexer(final CharStream input) {
        super(input);
    }

    @Override
    public void notifyListeners(final LexerNoViableAltException e) {
        final String read = this._input
                .getText(Interval.of(this._tokenStartCharIndex, this._input.index() - 1));
        this.getErrorListenerDispatch().syntaxError(this, null, this._tokenStartLine,
                this._tokenStartCharPositionInLine,
                EfxErrorStrategy.unreadable(read, this._input.LA(1), this._mode), e);
    }
}
