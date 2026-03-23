package eu.europa.ted.efx.interfaces;

import java.nio.file.Path;

import eu.europa.ted.efx.model.DecimalFormat;

public interface TranslatorOptions {
    public DecimalFormat getDecimalFormat();

    public String getPrimaryLanguage2LetterCode();

    public String getPrimaryLanguage3LetterCode();

    public String[] getAllLanguage2LetterCodes();

    public String[] getAllLanguage3LetterCodes();

    public String getUserDefinedFunctionNamespace();
    
    /**
     * Returns whether EFX profiling is enabled for performance analysis.
     * 
     * @return true if EFX profiling should be enabled, false otherwise
     */
    public boolean isProfilerEnabled();
    
    /**
     * Returns the output path for EFX profiling results.
     *
     * @return Path where profiling results should be written, or null if no file output is desired
     */
    public Path getProfilerOutputPath();

    /**
     * Returns the root directory for Freemarker templates. When set, templates are loaded
     * from this directory first, falling back to the classpath-bundled defaults.
     *
     * @return The templates root directory, or null to use classpath defaults only.
     */
    default Path getTemplatesRoot() {
        return null;
    }

    /**
     * Returns the include resolver for resolving {@code #include} directives in rules files.
     *
     * @return The include resolver, or null if include resolution is not configured.
     */
    default IncludedFileResolver getIncludedFileResolver() {
        return null;
    }

    /**
     * Returns a new {@link TranslatorOptions} that delegates all methods to this instance
     * but overrides the {@link IncludedFileResolver}.
     */
    static TranslatorOptions withResolver(TranslatorOptions delegate, IncludedFileResolver resolver) {
        return new TranslatorOptions() {
            @Override public DecimalFormat getDecimalFormat() { return delegate.getDecimalFormat(); }
            @Override public String getPrimaryLanguage2LetterCode() { return delegate.getPrimaryLanguage2LetterCode(); }
            @Override public String getPrimaryLanguage3LetterCode() { return delegate.getPrimaryLanguage3LetterCode(); }
            @Override public String[] getAllLanguage2LetterCodes() { return delegate.getAllLanguage2LetterCodes(); }
            @Override public String[] getAllLanguage3LetterCodes() { return delegate.getAllLanguage3LetterCodes(); }
            @Override public String getUserDefinedFunctionNamespace() { return delegate.getUserDefinedFunctionNamespace(); }
            @Override public boolean isProfilerEnabled() { return delegate.isProfilerEnabled(); }
            @Override public Path getProfilerOutputPath() { return delegate.getProfilerOutputPath(); }
            @Override public IncludedFileResolver getIncludedFileResolver() { return resolver; }
        };
    }
}
