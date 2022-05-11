# Java toolkit for the eForms Expression Language (EFX)

A Java library for the [eForms Expression Language (EFX)](https://docs.ted.europa.eu/eforms/latest/efx)

The EFX Toolkit for Java developers is a library that enables the transpilation of EFX expressions and templates to different target languages. It also provides an implementation that turns an EFX expression into XPath.

The documentation is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

## Using the EFX toolkit

The EFX toolkit is available as a Maven package in our GitHub repository. So you can use it by adding the following dependency:

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

In order to access our packages on GitHub, you need to define the repository in your Maven settings or your `pom.xml`:

```
<repositories>
  ...
  <repository>
    <id>github-public</id>
    <name>Github Packages</name>
    <url>https://public:&#103;hp_fkKwOmLCctdXETIEqjul8vX7EFw6HE12kl4t@maven.pkg.github.com/OP-TED/*</url>
  </repository>
</repositories>
```

We are working on making packages available via the Maven Central Repository.

The [documentation](https://docs.ted.europa.eu/eforms/latest/efx-toolkit) describes the capabilities of the library, and the interfaces you might need to implement.

## Building

You can build this project as usual using Maven.

The build process uses the grammar files provided in the [eForms SDK](https://github.com/OP-TED/eForms-SDK) to generate a parser, using [ANTLR](https://www.antlr.org).

## Testing

Unit tests are available under `src/test/java/`. They show in particular a variety of EFX expressions and the corresponding XPath expression.

After running the unit tests with `mvn test`, you can generate a coverage report with`mvn jacoco:report`.
The report is available under `target/site/jacoco/`, in HTML, CSV, and XML format.
