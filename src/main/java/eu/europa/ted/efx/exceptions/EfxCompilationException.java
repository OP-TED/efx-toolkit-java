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
package eu.europa.ted.efx.exceptions;

import java.util.Locale;

import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.model.CombinedText;
import eu.europa.ted.efx.model.LineMap;

/**
 * Abstract base class for all EFX compilation exceptions that represent user errors in EFX code.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing and bypass error recovery
 * mechanisms.
 *
 * <p>An error has a position in the EFX source when it is raised with the parse context it is
 * about, or when it is placed there afterwards with {@link #at}. Its message starts with that
 * position, as "line X:Y ", or as "line X:Y in 'file' " when the error is in a file included with
 * {@code #include}.
 *
 * <p>A message writes its numbers in ASCII digits whatever the default locale.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public abstract class EfxCompilationException extends ParseCancellationException {

    /**
     * Where the EFX source the error is about starts, as "line X:Y" or "line X:Y in 'file'"; null
     * while the error has no position.
     */
    private String position;

    protected EfxCompilationException(String template, Object... args) {
        super(args.length > 0 ? String.format(Locale.ROOT, template, args) : template);
    }

    protected EfxCompilationException(ParserRuleContext ctx, String template, Object... args) {
        super(String.format(Locale.ROOT, template, args));
        this.position = positionOf(ctx);
    }

    /**
     * Places an error that has no position yet at the start of the given parse context. An error
     * that has a position keeps it.
     *
     * @param ctx the parse context being translated when the error was raised
     * @return this error
     */
    public EfxCompilationException at(final ParserRuleContext ctx) {
        if (this.position == null) {
            this.position = positionOf(ctx);
        }
        return this;
    }

    /**
     * @return the message, after the position of the error when it has one
     */
    @Override
    public String getMessage() {
        if (this.position == null) {
            return super.getMessage();
        }
        return this.position + " " + super.getMessage();
    }

    private static String positionOf(final ParserRuleContext ctx) {
        final Token start = ctx.getStart();
        return positionOf(start.getLine(), start.getCharPositionInLine(), start.getInputStream());
    }

    /**
     * Writes a position in the text the lexer read, as an error message starts with it.
     *
     * @param line the line, counted from 1
     * @param column the column, counted from 0
     * @param text the text the lexer read
     * @return the position as "line X:Y", in the file it was written in when the text is a
     *         {@link CombinedText}, as {@link LineMap#positionOf} writes it
     */
    static String positionOf(final int line, final int column, final IntStream text) {
        final LineMap lineMap =
            text instanceof CombinedText ? ((CombinedText) text).getLineMap() : new LineMap();
        return lineMap.positionOf(line, column);
    }
}
