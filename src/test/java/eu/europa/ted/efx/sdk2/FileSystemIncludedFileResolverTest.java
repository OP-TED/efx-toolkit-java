package eu.europa.ted.efx.sdk2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemIncludedFileResolverTest {

  @TempDir
  Path tempDir;

  @Test
  void testResolve_ValidPath_ReturnsContent() throws IOException {
    Files.writeString(tempDir.resolve("rules.efx"), "---- STAGE 1a ----\n");

    FileSystemIncludedFileResolver resolver = new FileSystemIncludedFileResolver(tempDir);
    String content = resolver.resolve("rules.efx");

    assertEquals("---- STAGE 1a ----\n", content);
  }

  @Test
  void testResolve_Subdirectory_ReturnsContent() throws IOException {
    Path subDir = tempDir.resolve("sub");
    Files.createDirectories(subDir);
    Files.writeString(subDir.resolve("extra.efx"), "// extra\n");

    FileSystemIncludedFileResolver resolver = new FileSystemIncludedFileResolver(tempDir);
    String content = resolver.resolve("sub/extra.efx");

    assertEquals("// extra\n", content);
  }

  @Test
  void testResolve_PathTraversal_ThrowsIOException() throws IOException {
    // Create a file outside the base directory
    Path outsideDir = tempDir.resolve("outside");
    Files.createDirectories(outsideDir);
    Files.writeString(outsideDir.resolve("secret.efx"), "secret");

    // Resolver rooted at a subdirectory
    Path baseDir = tempDir.resolve("project");
    Files.createDirectories(baseDir);

    FileSystemIncludedFileResolver resolver = new FileSystemIncludedFileResolver(baseDir);
    IOException thrown = assertThrows(IOException.class,
        () -> resolver.resolve("../outside/secret.efx"));
    assertTrue(thrown.getMessage().contains("outside the base directory"));
  }

  @Test
  void testResolve_MissingFile_ThrowsIOException() throws IOException {
    FileSystemIncludedFileResolver resolver = new FileSystemIncludedFileResolver(tempDir);
    assertThrows(IOException.class, () -> resolver.resolve("nonexistent.efx"));
  }
}
