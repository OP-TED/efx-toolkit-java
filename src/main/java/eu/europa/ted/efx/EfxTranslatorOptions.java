package eu.europa.ted.efx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.DecimalFormat;

public class EfxTranslatorOptions implements TranslatorOptions {

    // Change to EfxDecimalFormatSymbols.EFX_DEFAULT to use the decimal format
    // preferred by OP (space as thousands separator and comma as decimal separator).
    public static final EfxTranslatorOptions DEFAULT = new EfxTranslatorOptions(DecimalFormat.XSL_DEFAULT, Locale.ENGLISH);

    private final DecimalFormat symbols;
    private Locale primaryLocale;
    private ArrayList<Locale> otherLocales;

    public EfxTranslatorOptions(DecimalFormat symbols) {
        this(symbols, Locale.ENGLISH);
    }

    public EfxTranslatorOptions(DecimalFormat symbols, String primaryLanguage, String... otherLanguages) {
        this(symbols, Locale.forLanguageTag(primaryLanguage), Arrays.stream(otherLanguages).map(Locale::forLanguageTag).toArray(Locale[]::new));
    }

    public EfxTranslatorOptions(DecimalFormat symbols, Locale primaryLocale, Locale... otherLocales) {
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

    public EfxTranslatorOptions withLanguage(String language) {
        this.primaryLocale = Locale.forLanguageTag(language);
        return this;
    }
}