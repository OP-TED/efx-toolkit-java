**[:memo: Latest Release Notes](CHANGELOG.md)** | **[:package: Latest Release Artifacts](https://central.sonatype.com/artifact/eu.europa.ted.eforms/efx-toolkit-java)**

---
# Java toolkit for the eForms Expression Language (EFX)

The **EFX Toolkit[^1] for Java developers** is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also provides an implementation for translating EFX expressions to XPath.

The documentation is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit


## Using the EFX toolkit[^1]

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

In order to be able to use snapshot versions of dependencies, the following should be added to the "profiles" section of the Maven configuration file "settings.xml" (normally under ${HOME}/.m2):

```
<servers>
  <server>
    <id>ossrh</id>
    <username>${env.MAVEN_USERNAME}</username>
    <password>${env.MAVEN_PASSWORD}</password>
  </server>
</servers>

<profile>
  <id>repositories</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
  <repositories>
    <repository>
      <id>ossrh</id>
      <name>OSSRH Snapshots</name>
      <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
</profile>
```

See ".github/workflows/settings.xml".

## Testing

Unit tests are available under `src/test/java/`. They show in particular a variety of EFX expressions and the corresponding XPath expression.

After running the unit tests with `mvn test`, you can generate a coverage report with `mvn jacoco:report`.
The report is available under `target/site/jacoco/`, in HTML, CSV, and XML format.

## Download

You can download the latest EFX Toolkit from Maven Central.

[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://central.sonatype.com/artifact/eu.europa.ted.eforms/efx-toolkit-java)

[^1]: _Copyright 2022 European Union_  
_Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission –
subsequent versions of the EUPL (the "Licence");_
_You may not use this work except in compliance with the Licence. You may obtain [a copy of the Licence here](LICENSE)._  
_Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language governing permissions and limitations under the Licence._
