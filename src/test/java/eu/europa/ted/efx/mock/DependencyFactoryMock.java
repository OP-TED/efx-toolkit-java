package eu.europa.ted.efx.mock;

import org.antlr.v4.runtime.BaseErrorListener;

import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

public class DependencyFactoryMock implements TranslatorDependencyFactory {

    @Override
    public SymbolResolver createSymbolResolver(String sdkVersion) {
        return SymbolResolverMock.getInstance(sdkVersion);
    }

    @Override
    public ScriptGenerator createScriptGenerator() {
        return new XPathScriptGenerator();
    }

    @Override
    public MarkupGenerator createMarkupGenerator() {
        return new MarkupGeneratorMock();
    }

    @Override
    public BaseErrorListener createErrorListener() {
        return ThrowingErrorListener.INSTANCE;
    }
}
