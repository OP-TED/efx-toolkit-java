package eu.europa.ted.efx.model;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DecimalFormat extends DecimalFormatSymbols {

    public final static DecimalFormat XSL_DEFAULT = DecimalFormat.getXslDefault();
    public final static DecimalFormat EFX_DEFAULT = DecimalFormat.getEfxDefault();

    DecimalFormat(Locale locale) {
        super(locale);
    }

    private static DecimalFormat getXslDefault() {
        DecimalFormat symbols = new DecimalFormat(Locale.US);
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        symbols.setMinusSign('-');
        symbols.setPercent('%');
        symbols.setPerMill('‰');
        symbols.setZeroDigit('0');
        symbols.setDigit('#');
        symbols.setPatternSeparator(';');
        symbols.setInfinity("Infinity");
        symbols.setNaN("NaN");
        return symbols;
    }

    private static DecimalFormat getEfxDefault() {
        DecimalFormat symbols = DecimalFormat.getXslDefault();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        return symbols;
    }

    public String adaptFormatString(final String originalString) {

        final char decimalSeparatorPlaceholder = '\uE000';
        final char groupingSeparatorPlaceholder = '\uE001';
        final char minusSignPlaceholder = '\uE002';
        final char percentPlaceholder = '\uE003';
        final char perMillePlaceholder = '\uE004';
        final char zeroDigitPlaceholder = '\uE005';
        final char digitPlaceholder = '\uE006';
        final char patternSeparatorPlaceholder = '\uE007';    

        String adaptedString = originalString;
        adaptedString = adaptedString.replace(XSL_DEFAULT.getDecimalSeparator(), decimalSeparatorPlaceholder);
        adaptedString = adaptedString.replace(XSL_DEFAULT.getGroupingSeparator(), groupingSeparatorPlaceholder);
        adaptedString = adaptedString.replace(XSL_DEFAULT.getMinusSign(), minusSignPlaceholder); 
        adaptedString = adaptedString.replace(XSL_DEFAULT.getPercent(), percentPlaceholder);
        adaptedString = adaptedString.replace(XSL_DEFAULT.getPerMill(), perMillePlaceholder);
        adaptedString = adaptedString.replace(XSL_DEFAULT.getZeroDigit(), zeroDigitPlaceholder);
        adaptedString = adaptedString.replace(XSL_DEFAULT.getDigit(), digitPlaceholder);
        adaptedString = adaptedString.replace(XSL_DEFAULT.getPatternSeparator(), patternSeparatorPlaceholder);
 
        adaptedString = adaptedString.replace(decimalSeparatorPlaceholder, this.getDecimalSeparator());
        adaptedString = adaptedString.replace(groupingSeparatorPlaceholder, this.getGroupingSeparator());
        adaptedString = adaptedString.replace(minusSignPlaceholder, this.getMinusSign());
        adaptedString = adaptedString.replace(percentPlaceholder, this.getPercent());
        adaptedString = adaptedString.replace(perMillePlaceholder, this.getPerMill());
        adaptedString = adaptedString.replace(zeroDigitPlaceholder, this.getZeroDigit());
        adaptedString = adaptedString.replace(digitPlaceholder, this.getDigit());
        adaptedString = adaptedString.replace(patternSeparatorPlaceholder, this.getPatternSeparator());
    
        return adaptedString;
    }
}