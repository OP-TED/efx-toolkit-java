package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import eu.europa.ted.efx.interfaces.IncludedFileResolver;

/**
 * Resolves include paths relative to a base directory on the file system.
 *
 * <p>
 * This is the default resolver used when translating rules from a file path. Include paths
 * specified in {@code #include} directives are resolved relative to the base directory.
 * </p>
 */
public class FileSystemIncludedFileResolver implements IncludedFileResolver {

  private final Path baseDir;

  public FileSystemIncludedFileResolver(Path baseDir) throws IOException {
    this.baseDir = baseDir.toRealPath();
  }

  @Override
  public String resolve(String path) throws IOException {
    Path normalized = this.baseDir.resolve(path).normalize();
    if (!normalized.startsWith(this.baseDir)) {
      throw new IOException(
          "Include path '" + path + "' resolves outside the base directory");
    }
    // Resolve symlinks to prevent symlink-based traversal
    Path real = normalized.toRealPath();
    if (!real.startsWith(this.baseDir)) {
      throw new IOException(
          "Include path '" + path + "' resolves outside the base directory");
    }
    return Files.readString(real);
  }
}
