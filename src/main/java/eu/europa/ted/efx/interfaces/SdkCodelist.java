package eu.europa.ted.efx.interfaces;

import java.util.List;
import java.util.StringJoiner;

public interface SdkCodelist {
  String getCodelistId();

  String getVersion();

  List<String> getCodes();

  default String toString(CharSequence delimiter, CharSequence prefix, CharSequence suffix,
      Character quote) {
    final StringJoiner joiner = new StringJoiner(delimiter, prefix, suffix);
    for (final String code : getCodes()) {
      joiner.add(quote + code + quote);
    }

    return joiner.toString();
  }
}
