/*
 * Copyright 2025 European Union
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

/**
 * Exception thrown when symbol resolution fails during EFX template processing.
 * This includes unknown fields, nodes, codelists, or other symbol lookup failures.
 */
public class SymbolResolutionException extends EfxCompilationException {

    public enum ErrorCode {
        UNKNOWN_SYMBOL,
        UNKNOWN_CODELIST,
        NO_CODELIST_FOR_FIELD,
        ROOT_NODE_NOT_FOUND,
        UNKNOWN_NOTICE_SUBTYPE
    }

    private static final String UNKNOWN_SYMBOL = "Unknown symbol '%s'.";
    private static final String UNKNOWN_CODELIST = "Unknown codelist '%s'.";
    private static final String NO_CODELIST_FOR_FIELD = "Field '%s' is not associated with a codelist.";
    private static final String ROOT_NODE_NOT_FOUND = "Could not find the root node. Check that node metadata is loaded correctly.";
    private static final String UNKNOWN_NOTICE_SUBTYPE = "Unknown notice subtype '%s' in range '%s'.";

    private final ErrorCode errorCode;

    private SymbolResolutionException(ErrorCode errorCode, String template, Object... args) {
        super(template, args);
        this.errorCode = errorCode;
    }

    private SymbolResolutionException(ErrorCode errorCode, ParserRuleContext ctx, String template, Object... args) {
        super(ctx, template, args);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public static SymbolResolutionException unknownCodelist(String codelistId) {
        return new SymbolResolutionException(ErrorCode.UNKNOWN_CODELIST, UNKNOWN_CODELIST, codelistId);
    }

    public static SymbolResolutionException unknownSymbol(String symbol) {
        return new SymbolResolutionException(ErrorCode.UNKNOWN_SYMBOL, UNKNOWN_SYMBOL, symbol);
    }

    public static SymbolResolutionException noCodelistForField(String fieldId) {
        return new SymbolResolutionException(ErrorCode.NO_CODELIST_FOR_FIELD, NO_CODELIST_FOR_FIELD, fieldId);
    }

    public static SymbolResolutionException rootNodeNotFound() {
        return new SymbolResolutionException(ErrorCode.ROOT_NODE_NOT_FOUND, ROOT_NODE_NOT_FOUND);
    }

    public static SymbolResolutionException unknownNoticeSubtype(String noticeSubtype, String rangeString) {
        return new SymbolResolutionException(ErrorCode.UNKNOWN_NOTICE_SUBTYPE, UNKNOWN_NOTICE_SUBTYPE, noticeSubtype, rangeString);
    }
}
