# eForms Expression Language (EFX)

This project uses ANTLR: https://www.antlr.org

ANTLR grammars, see .g4 files

## IDE setup

`mvn clean install` always do this first to ensure everything is OK outside the IDE.

Build and add "target/generated-source/antlr4" to the build path.
Rebuild sources after that.

## Testing

See unit tests under `src/test/java/`.

`mvn clean install` will also run the tests.

After running the unit tests with `mvn test`, you can generate a coverage report with`mvn jacoco:report`.
The report is available under `target/site/jacoco/`, in HTML, CSV, and XML format.

## Export of artifact into local m2 repo

`mvn clean install` copies the JAR and pom file to your local maven repository.
You can then add the dependency as usual in pom.xml of your project:

```
    <dependency>
      <groupId>eu.europa.ted.eforms</groupId>
      <artifactId>efx-toolkit-java</artifactId>
      <version>0.0.1</version>
    </dependency>
```

Make sure to update the version number to the latest version available.