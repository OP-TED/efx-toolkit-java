package eu.europa.ted.efx.interfaces;

import org.antlr.v4.runtime.BaseErrorListener;

public interface TranslatorDependencyFactory {

    /**
     * Get the instance of the SymbolMap to be used by the translator to resolve symbols.
     */
    public SymbolMap getSymbolMap(String sdkVersion);

    /**
     * Get the instance of the renderer to be used by the translator for rendering the target template.
     */
    public Renderer Renderer();

    /**
     * Get the instance of the error listener to be used for handling translation errors.
     */
    public BaseErrorListener getErrorListener();
}