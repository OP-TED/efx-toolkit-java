package eu.europa.ted.efx.exceptions;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Stops a translation at the first error of the lexer or the parser. The message starts with the
 * position of the error, as the message of an {@link EfxCompilationException} does.
 */
public class ThrowingErrorListener extends BaseErrorListener {

  public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
      int charPositionInLine, String msg, RecognitionException e)
      throws ParseCancellationException {
    throw new ParseCancellationException(String.format("%s %s",
        EfxCompilationException.positionOf(line, charPositionInLine,
            textOf(recognizer, offendingSymbol)),
        msg));
  }

  /**
   * @param recognizer the lexer or the parser that met the error
   * @param offendingSymbol the token the parser met the error at; the lexer gives none
   * @return the text the lexer read: that of the token, or the lexer's own
   */
  private static IntStream textOf(final Recognizer<?, ?> recognizer,
      final Object offendingSymbol) {
    return offendingSymbol instanceof Token ? ((Token) offendingSymbol).getInputStream()
        : recognizer.getInputStream();
  }
}

