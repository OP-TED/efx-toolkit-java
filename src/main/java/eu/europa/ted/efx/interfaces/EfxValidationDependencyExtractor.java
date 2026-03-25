package eu.europa.ted.efx.interfaces;

import java.io.IOException;
import java.nio.file.Path;

import eu.europa.ted.efx.model.dependencies.DependencyGraph;

/**
 * Defines the API of an EFX validation dependency extractor.
 *
 * Given an EFX rules file, extracts all field and node dependencies for each validation rule
 * and builds a dependency graph.
 */
public interface EfxValidationDependencyExtractor {

  /**
   * Extracts a dependency graph from the given EFX rules string.
   *
   * @param rules A string containing EFX validation rules.
   * @return A {@link DependencyGraph} mapping each rule's target to its dependencies.
   */
  DependencyGraph extractDependencyGraph(final String rules);

  /**
   * Extracts a dependency graph from an EFX rules file.
   *
   * @param pathname The path to the EFX rules file.
   * @return A {@link DependencyGraph} mapping each rule's target to its dependencies.
   * @throws IOException If the file cannot be read.
   */
  DependencyGraph extractDependencyGraph(final Path pathname) throws IOException;
}
