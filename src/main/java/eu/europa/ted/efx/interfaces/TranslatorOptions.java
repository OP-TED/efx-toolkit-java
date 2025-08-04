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
}
