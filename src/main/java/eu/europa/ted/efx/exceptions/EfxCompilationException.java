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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Abstract base class for all EFX compilation exceptions that represent user errors in EFX code.
 * Extends ParseCancellationException to properly stop ANTLR4 parsing and bypass error recovery
 * mechanisms.
 *
 * Provides a shared utility for formatting source position information from ANTLR tokens.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public abstract class EfxCompilationException extends ParseCancellationException {

    protected EfxCompilationException(String template, Object... args) {
        super(args.length > 0 ? String.format(template, args) : template);
    }

    protected EfxCompilationException(ParserRuleContext ctx, String template, Object... args) {
        super(formatMessage(ctx, template, args));
    }

    /**
     * Formats a complete error message with source position prefix.
     *
     * @param ctx the ANTLR parser rule context indicating the source position of the error
     * @param template the message template (as used by {@link String#format})
     * @param args the arguments to substitute into the template
     * @return the formatted message prefixed with "line X:Y "
     */
    protected static String formatMessage(ParserRuleContext ctx, String template, Object... args) {
        Token token = ctx.getStart();
        return String.format("line %d:%d ", token.getLine(), token.getCharPositionInLine())
                + String.format(template, args);
    }
}
