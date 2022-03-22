package eu.europa.ted.efx.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * @author rouschr
 */
public class JavaTools {

  /**
   * @param resourcePath Path relative to "src/main/resources/" if you are in a Maven project.
   *
   * @return The path of the specified resource
   * @throws URISyntaxException
   */
  public static Path getResourceAsPath(final String resourcePath) {
    assert StringUtils.isNotBlank(resourcePath) : "resourcePath is blank";
    assert !resourcePath.startsWith("/") : String
        .format("resourcePath should not start with forward slash: %s", resourcePath);
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try {
      return Paths.get(classLoader.getResource(resourcePath).toURI());
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param resourcePath Path relative to "src/main/resources/" if you are in a Maven project.
   *
   * @return The inputstream to the specified resource
   */
  public static InputStream getResourceAsStream(final String resourcePath) {
    assert StringUtils.isNotBlank(resourcePath) : "resourcePath is blank";
    assert !resourcePath.startsWith("/") : String
        .format("resourcePath should not start with forward slash: %s", resourcePath);
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    return classLoader.getResourceAsStream(resourcePath);
  }

  /**
   * @param resourcePath Path relative to "src/main/resources/" if you are in a Maven project.
   *
   * @return The inputstream to the specified resource
   */
  public static InputStream getResourceAsStream(final ClassLoader classLoader,
      final String resourcePath) {
    assert StringUtils.isNotBlank(resourcePath) : "resourcePath is blank";
    assert !resourcePath.startsWith("/") : String
        .format("resourcePath should not start with forward slash: %s", resourcePath);
    return classLoader.getResourceAsStream(resourcePath);
  }

  /**
   * @return empty if blank, otherwise present and stripped.
   */
  public static final Optional<String> getOpt(final String str) {
    return StringUtils.isBlank(str) ? Optional.empty() : Optional.of(str.strip());
  }

  /**
   * @param pathFolder Folder to crawl
   * @param maxDepth Maximum folder depth, minimum is 1 level
   * @param dotExtension Something like .xlsx, or .xml, ... usually to exclude .md or other
   *        irrelevant files. Ends with is performed using this string
   *
   * @return list of paths (files only) ending with the passed the extension.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive.")
  public static List<Path> listFilesUsingFileWalk(final Path pathFolder, final int maxDepth,
      final String dotExtension) throws IOException {
    assert maxDepth > 0 : "maxDepth should be > 0";

    if (!pathFolder.toFile().isDirectory()) {
      throw new RuntimeException(String.format("Expecting folder but got: %s", pathFolder));
    }
    // Works only if name is unique ...
    // final Path rootPath = Paths.get(ClassLoader.getSystemResource(dir).toURI());
    try (Stream<Path> stream = Files.walk(pathFolder, maxDepth)) {
      return stream
          .filter(pathFile -> !Files.isDirectory(pathFile)
              && pathFile.toString().endsWith(dotExtension))
          .sorted()// Alphanumeric sort.
          .collect(Collectors.toList());
    }
  }

}
