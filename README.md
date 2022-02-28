# eForms Expression Language (EFX)

This project uses ANTLR: https://www.antlr.org

ANTLR grammars, see .g4 files

## IDE setup

Build and add "target/generated-source/antlr4" to the build path.
Rebuild sources after that.


## Testing

See unit tests.
See EfxToXpathTranslator.java (translateFile, translateCondition, ...)

`mvn clean install` will also run the tests.

## Command line

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.efx.app.MainApp" -Dexec.args="commandName cmdArg1 cmdArg2 ..."
```

Commands: command, arg1, arg2, ...
* contextualize contextxpath xpath
