package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.DecimalFormat;

public class EfxTranslatorOptions implements TranslatorOptions {

    /**
     * This is the default namespace for user-defined functions (UDFs) in EFX.
     * There is no need to change this but we made it customizable anyway.
     * 
     * This setting is relevant mostly for the EFX to XSLT translation process but it can also 
     * be used for other target languages that need a namespace declaration for UDFs.
     */
    public static final String DEFAULT_UDF_NAMESPACE = "efx-udf";

    // Change to EfxDecimalFormatSymbols.EFX_DEFAULT to use the decimal format
    // preferred by OP (space as thousands separator and comma as decimal separator).
    public static final EfxTranslatorOptions DEFAULT = new EfxTranslatorOptions(DEFAULT_UDF_NAMESPACE, DecimalFormat.XSL_DEFAULT, Locale.ENGLISH);

    private final DecimalFormat symbols;
    private final Locale primaryLocale;
    private final ArrayList<Locale> otherLocales;
    private final String userDefinedFunctionNamespace;

    public EfxTranslatorOptions(DecimalFormat symbols) {
        this(DEFAULT_UDF_NAMESPACE, symbols);
    }

    public EfxTranslatorOptions(String udfNamespace, DecimalFormat symbols) {
        this(udfNamespace, symbols, Locale.ENGLISH);
    }

    public EfxTranslatorOptions(DecimalFormat symbols, String primaryLanguage, String... otherLanguages) {
        this(symbols, Locale.forLanguageTag(primaryLanguage), Arrays.stream(otherLanguages).map(Locale::forLanguageTag).toArray(Locale[]::new));
    }

    public EfxTranslatorOptions(String udfNamespace, DecimalFormat symbols, String primaryLanguage, String... otherLanguages) {
        this(udfNamespace, symbols, Locale.forLanguageTag(primaryLanguage), Arrays.stream(otherLanguages).map(Locale::forLanguageTag).toArray(Locale[]::new));
    }

    public EfxTranslatorOptions(DecimalFormat symbols, Locale primaryLocale, Locale... otherLocales) {
        this(DEFAULT_UDF_NAMESPACE, symbols, primaryLocale, otherLocales);
    }
    
    public EfxTranslatorOptions(String udfNamespace, DecimalFormat symbols, Locale primaryLocale, Locale... otherLocales) {
        this.userDefinedFunctionNamespace = udfNamespace;
        this.symbols = symbols;
        this.primaryLocale = primaryLocale;
        this.otherLocales = new ArrayList<>(Arrays.asList(otherLocales));
    }

    @Override
    public DecimalFormat getDecimalFormat() {
        return this.symbols;
    }

    @Override
    public String getPrimaryLanguage2LetterCode() {
        return this.primaryLocale.getLanguage();
    }

    @Override
    public String getPrimaryLanguage3LetterCode() {
        return this.primaryLocale.getISO3Language();
    }

    @Override
    public String[] getAllLanguage2LetterCodes() {
        List<String> languages = new ArrayList<>();
        languages.add(primaryLocale.getLanguage());
        for (Locale locale : otherLocales) {
            languages.add(locale.getLanguage());
        }
        return languages.toArray(new String[0]);
    }
    
    @Override
    public String[] getAllLanguage3LetterCodes() {
        List<String> languages = new ArrayList<>();
        languages.add(primaryLocale.getISO3Language());
        for (Locale locale : otherLocales) {
            languages.add(locale.getISO3Language());
        }
        return languages.toArray(new String[0]);
    }

    /**
     * This will be used by the Script Generator when generating script to invoke a user-defined function.
     * It will also be used by the Markup Generator to generate the namespace declaration as well as the
     * user-defined function definitions.
     */
    @Override
    public String getUserDefinedFunctionNamespace() {
        return this.userDefinedFunctionNamespace;
    }
}