package eu.europa.ted.efx.interfaces;

import java.io.IOException;

/**
 * Resolves {@code #include} directive paths to their text content.
 *
 * <p>
 * Implementations determine how include paths are mapped to file contents. For example, a
 * file-system resolver may resolve paths relative to a base directory, while a database-backed
 * resolver may look up the content by name.
 * </p>
 */
@FunctionalInterface
public interface IncludedFileResolver {

  /**
   * Resolves the given include path and returns the text content of the included file.
   *
   * @param path The include path as specified in the {@code #include} directive (without quotes).
   * @return The text content of the included file.
   * @throws IOException If the path cannot be resolved or the content cannot be read.
   */
  String resolve(String path) throws IOException;
}
