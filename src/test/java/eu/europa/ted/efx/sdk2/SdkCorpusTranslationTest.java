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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import eu.europa.ted.eforms.sdk.SdkSymbolResolver;
import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.DecimalFormat;

/**
 * Translates the EFX corpus of the eForms SDK, its view templates and its validation rules, and
 * writes every output under {@code target/corpus/output}, so that the outputs of two builds can
 * be compared file by file. Each view template is a test of its own, and so is the rules file, so a
 * failure names the file and the surefire report times each translation separately.
 * <p>
 * The test is opt-in: surefire excludes the tag {@code corpus} by default. Run it with
 * {@code mvn test -Dgroups=corpus -DexcludedGroups=}.
 */
@Tag("corpus")
class SdkCorpusTranslationTest {

  private static final String SDK_VERSION = "eforms-sdk-2.0";

  private static final Path CORPUS_DIRECTORY = Path.of("target", "corpus");

  private static final Path OUTPUT_DIRECTORY = CORPUS_DIRECTORY.resolve("output");

  private static final TranslatorOptions OPTIONS =
      new EfxTranslatorOptions("udf", DecimalFormat.EFX_DEFAULT);

  private static SdkCorpus corpus;

  private static TranslatorDependencyFactory dependencies;

  @BeforeAll
  static void unpackCorpus() throws IOException, InstantiationException {
    corpus = SdkCorpus.unpack(SDK_VERSION, CORPUS_DIRECTORY.resolve("sdk"));
    dependencies = new SdkCorpusDependencyFactory(
        new SdkSymbolResolver(SDK_VERSION, corpus.getSdkRoot()));
    deleteOutputsOfPreviousRun();
  }

  @TestFactory
  Stream<DynamicTest> translateViewTemplates() throws IOException {
    return corpus.getViewTemplates().stream()
        .map(template -> DynamicTest.dynamicTest(template.getFileName().toString(),
            () -> translateViewTemplate(template)));
  }

  @Test
  void translateRules() throws IOException, InstantiationException {
    final Path rules = corpus.getRulesFile();
    final Map<String, String> outputs =
        EfxTranslator.translateRules(dependencies, SDK_VERSION, rules, OPTIONS);
    final Path rulesOutputDirectory =
        OUTPUT_DIRECTORY.resolve("validation").resolve(rules.getFileName().toString());
    for (Map.Entry<String, String> output : outputs.entrySet()) {
      writeOutput(rulesOutputDirectory.resolve(output.getKey()), output.getValue());
    }
  }

  private static void translateViewTemplate(final Path template)
      throws IOException, InstantiationException {
    final String output = EfxTranslator.translateTemplate(dependencies, SDK_VERSION, template, OPTIONS);
    writeOutput(OUTPUT_DIRECTORY.resolve("view-templates").resolve(template.getFileName() + ".txt"),
        output);
  }

  private static void deleteOutputsOfPreviousRun() throws IOException {
    if (Files.notExists(OUTPUT_DIRECTORY)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(OUTPUT_DIRECTORY)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
        Files.delete(path);
      }
    }
  }

  private static void writeOutput(final Path file, final String content) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
