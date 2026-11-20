/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.sdk2;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.europa.ted.eforms.sdk.SdkConstants.SdkResource;
import eu.europa.ted.eforms.sdk.SdkVersion;
import eu.europa.ted.eforms.sdk.resource.SdkResourceLoader;
import eu.europa.ted.util.ArchiveUtils;

/**
 * The EFX corpus of the eForms SDK: the view templates and the validation rules bundled in the
 * eforms-sdk jar on the test classpath.
 * <p>
 * The symbol resolver and the include resolver read the SDK from the file system, so the jar is
 * first unpacked into an SDK root, the way eforms-core unpacks a downloaded SDK.
 */
class SdkCorpus {

  private static final String BUNDLED_VERSION_FILE = "eforms-sdk/VERSION";

  private final String sdkVersion;

  private final Path sdkRoot;

  private SdkCorpus(final String sdkVersion, final Path sdkRoot) {
    this.sdkVersion = sdkVersion;
    this.sdkRoot = sdkRoot;
  }

  /**
   * Unpacks the eforms-sdk jar on the classpath, replacing any SDK previously unpacked there.
   *
   * @param sdkVersion The version of the SDK, as the translators name it (for example
   *        "eforms-sdk-2.0").
   * @param sdkRoot The SDK root directory to unpack into.
   * @return The corpus of the unpacked SDK.
   * @throws IOException If the jar cannot be found or unpacked.
   */
  static SdkCorpus unpack(final String sdkVersion, final Path sdkRoot) throws IOException {
    ArchiveUtils.unzip(bundledSdkJar(),
        sdkRoot.resolve(new SdkVersion(sdkVersion).toStringWithoutPatch()));
    return new SdkCorpus(sdkVersion, sdkRoot);
  }

  /**
   * @return The root directory to load the SDK metadata from.
   */
  Path getSdkRoot() {
    return this.sdkRoot;
  }

  /**
   * @return The view template files, sorted by name.
   * @throws IOException If the view templates folder cannot be listed.
   */
  List<Path> getViewTemplates() throws IOException {
    return this.listEfxFiles(SdkResource.VIEW_TEMPLATES);
  }

  /**
   * @return The validation rules file, {@code validation/rules.efx}. It includes the other files of
   *         the validation folder, which are parts of it and are not translated alone.
   */
  Path getRulesFile() {
    return SdkResourceLoader.getResourceAsPath(this.sdkVersion, SdkResource.VALIDATION_RULES_EFX,
        this.sdkRoot);
  }

  private List<Path> listEfxFiles(final SdkResource folder) throws IOException {
    final Path folderPath = SdkResourceLoader.getResourceAsPath(this.sdkVersion, folder, this.sdkRoot);
    try (Stream<Path> files = Files.list(folderPath)) {
      return files.filter(file -> file.getFileName().toString().endsWith(".efx"))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  private static File bundledSdkJar() throws IOException {
    final URL versionFile = SdkCorpus.class.getClassLoader().getResource(BUNDLED_VERSION_FILE);
    if (versionFile == null) {
      throw new IOException("The eforms-sdk jar is not on the test classpath");
    }
    final URLConnection connection = versionFile.openConnection();
    if (!(connection instanceof JarURLConnection)) {
      throw new IOException("The eforms-sdk resources are not in a jar: " + versionFile);
    }
    try {
      return new File(((JarURLConnection) connection).getJarFileURL().toURI());
    } catch (URISyntaxException e) {
      throw new IOException(e.getMessage(), e);
    }
  }
}
