# EFX Toolkit 1.1.0 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---
## In this release:

In this release we moved some utility classes to a new java library: [eforms-core-java](https://github.com/OP-TED/eforms-core-java). These utility classes enable the parallel use of multiple major versions of the SDK by applications. We decided to extract this functionality to a new shared library so that it can also be used by applications that do not necessarily need EFX translation.  

This release also removes the need to use the "classindex" annotation processor plugin. You can remove the following section from your pom.xml.

```
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>${version.compiler.plugin}</version>
    <configuration>
      <annotationProcessorPaths>
        <annotationProcessorPath>
          <groupId>org.atteo.classindex</groupId>
          <artifactId>classindex</artifactId>
          <version>${version.classindex}</version>
        </annotationProcessorPath>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
```

---

You can download the latest EFX Toolkit from Maven Central.  
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)

Documentation for the EFX Toolkit is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

---

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:
- eForms SDK 0.6.x
- eForms SDK 0.7.x
- eForms SDK 1.x.x

It also depends on the [eForms Core Java library](https://github.com/OP-TED/eforms-core-java) version 0.1.0.
