**[:memo: Latest Release Notes](CHANGELOG.md)** | **[:package: Latest Release Artifacts](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)**

---
# Java toolkit for the eForms Expression Language (EFX)

A Java library for the [eForms Expression Language (EFX)](https://docs.ted.europa.eu/eforms/latest/efx)

The EFX Toolkit for Java developers is a library that enables the transpilation of EFX expressions and templates to different target languages. It also provides an implementation that turns an EFX expression into XPath.

The documentation is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

## Using the EFX toolkit

The EFX toolkit requires Java 11 or later.

It is available as a Maven package on Maven Central and can be used by adding the following to the project's `pom.xml`.

```
<dependencies>
  ...
  <dependency>
    <groupId>eu.europa.ted.eforms</groupId>
    <artifactId>efx-toolkit-java</artifactId>
    <version>${efx-toolkit.version}</version>
  </dependency>
  ...
</dependencies>
```

Replace `${efx-toolkit.version}` with the latest version available, or define the corresponding property in your `pom.xml`.

The [documentation](https://docs.ted.europa.eu/eforms/latest/efx-toolkit) describes the capabilities of the library, and the interfaces you might need to implement.

## Building

You can build this project as usual using Maven.

The build process uses the grammar files provided in the [eForms SDK](https://github.com/OP-TED/eForms-SDK/tree/develop/efx-grammar) to generate a parser, using [ANTLR4](https://www.antlr.org).

## Testing

Unit tests are available under `src/test/java/`. They show in particular a variety of EFX expressions and the corresponding XPath expression.

After running the unit tests with `mvn test`, you can generate a coverage report with`mvn jacoco:report`.
The report is available under `target/site/jacoco/`, in HTML, CSV, and XML format.

## Download

You can download the latest EFX Toolkit from Maven Central.

[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)

