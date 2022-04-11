# eForms Expression Language (EFX)

This project uses ANTLR: https://www.antlr.org

ANTLR grammars, see .g4 files

## IDE setup

`mvn clean install` always do this first to ensure everything is OK outside the IDE.

Build and add "target/generated-source/antlr4" to the build path.
Rebuild sources after that.

## Testing

See unit tests.
See EfxToXpathTranslator.java (translateFile, translateCondition, ...)

`mvn clean install` will also run the tests.

## Export of artifact into local m2 repo

`mvn clean install` copies the JAR and pom file to your local maven repository.
You can then add the dependency as usual in pom.xml of your project:

```
    <dependency>
      <groupId>eforms-expression-language</groupId>
      <artifactId>eforms-expression-language</artifactId>
      <version>0.0.1</version>
    </dependency>
```

Make sure to update the version number to the latest version available.