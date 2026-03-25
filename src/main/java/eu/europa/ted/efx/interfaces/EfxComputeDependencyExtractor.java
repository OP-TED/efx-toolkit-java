package eu.europa.ted.efx.interfaces;

import java.util.Set;

/**
 * Defines the API of an EFX compute dependency extractor.
 *
 * Given an EFX single expression, extracts all field and node identifiers referenced in it.
 */
public interface EfxComputeDependencyExtractor {

  /**
   * Extracts all field and node identifiers referenced in the given EFX expression.
   *
   * @param expression A string containing the EFX single expression to analyse.
   * @return An unmodifiable set of field and node identifiers referenced in the expression.
   */
  Set<String> extractDependencies(final String expression);
}
