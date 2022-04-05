package eu.europa.ted.efx.interfaces;

import org.antlr.v4.runtime.BaseErrorListener;

public interface TranslatorDependencyFactory {

    /**
     * Get the instance of the SymbolMap to be used by the translator to resolve symbols.
     */
    public SymbolMap createSymbolMap(String sdkVersion);

    /**
     * Get the instance of the SyntaxMap to be used by the translator to translate expressions.
     */
    public SyntaxMap createSyntaxMap();

    /**
     * Get the instance of the renderer to be used by the translator for rendering the target template.
     */
    public Renderer createRenderer();

    /**
     * Get the instance of the error listener to be used for handling translation errors.
     */
    public BaseErrorListener createErrorListener();
}