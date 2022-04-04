package eu.europa.ted.efx.interfaces;

import org.antlr.v4.runtime.BaseErrorListener;

public interface TranslatorDependencyFactory {

    public SymbolMap getSymbolMap(String sdkVersion);

    public NoticeRenderer getNoticeRenderer();

    public BaseErrorListener getErrorListener();
}