package eu.europa.ted.efx.interfaces;

import eu.europa.ted.efx.model.DecimalFormat;

public interface TranslatorOptions {
    public DecimalFormat getDecimalFormat();

    public String getPrimaryLanguage2LetterCode();

    public String getPrimaryLanguage3LetterCode();

    public String[] getAllLanguage2LetterCodes();

    public String[] getAllLanguage3LetterCodes();
}
