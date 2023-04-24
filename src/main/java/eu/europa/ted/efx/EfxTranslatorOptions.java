package eu.europa.ted.efx;

import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.DecimalFormat;

public class EfxTranslatorOptions implements TranslatorOptions {

    // Change to EfxDecimalFormatSymbols.EFX_DEFAULT to use the decimal format
    // preferred by OP (space as thousands separator and comma as decimal separator).
    public static final EfxTranslatorOptions DEFAULT = new EfxTranslatorOptions(DecimalFormat.XSL_DEFAULT);

    private final DecimalFormat symbols;

    public EfxTranslatorOptions(DecimalFormat symbols) {
        this.symbols = symbols;
    }

    @Override
    public DecimalFormat getDecimalFormat() {
        return this.symbols;
    }
}