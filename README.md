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

## Command line usage of EFX

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.efx.app.EfxMainApp" -Dexec.args="commandName cmdArg1 cmdArg2 ..."
```

Commands: command, arg1, arg2, ...
* contextualize contextXpath xpath, so ` -Dexec.args="contextualize /a/b/ /a/b/c"`


## Export of dependency into local m2 repo

Do this if you want to use the EFX via Java in another project.
Assuming you are at the root of this project.
Assuming `mvn clean install` worked.

Run this (adapt version if necessary):

```
mvn install:install-file \
   -Dfile=target/eforms-expression-language-0.0.1-shaded.jar \
   -DgroupId=eforms-expression-language \
   -DartifactId=eforms-expression-language \
   -Dversion=0.0.1 \
   -Dpackaging=jar \
   -DgeneratePom=true
```

You should see something like this when you run it:

```
...
[INFO] Installing C:\Users\rouschr\AppData\Local\Temp\1\mvninstall5890104367507873957.pom to C:\Users\rouschr\.m2\repository\eforms-expression-language\eforms-expression-language\0.0.1\eforms-expression-language-0.0.1.pom
...
```

AFTER THAT THE DEPENDENCY SHOULD BE AVAILABLE LOCALLY IN OTHER PROJECTS (for example in the MDC).

In the pom.xml of the MDC (or any other project):

```
    <dependency>
      <groupId>eforms-expression-language</groupId>
      <artifactId>eforms-expression-language</artifactId>
      <version>0.0.1</version>
    </dependency>
```

Note that the version could have changed.
This method is documented here: 
https://stackoverflow.com/questions/4955635/how-to-add-local-jar-files-to-a-maven-project

We could have the project in our Nexus later.
