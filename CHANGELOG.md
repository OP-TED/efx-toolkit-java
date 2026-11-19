# Changes since 2.0.0-alpha.9

These changes are on the `develop` branch and will be part of the next pre-release.

## Improvements

- **Clearer error messages**: an error in an EFX-2 expression, template or rule says where it is, with its line and column, also in a file included with `#include`, but not an error found while a file is included, such as a circular include. It says what is wrong and, where it can, how to fix it.
- **Faster translation**: in our measurements, the view templates and the validation rules of the SDK translate about five times faster.
- **Formatted values in templates**: numbers, dates, times, amounts, measures and durations are displayed formatted, with the label of their unit in the right plural. A template can choose the format, as in `${BT-00-Number|2}`.
- **Dynamic rules**: each call of the API is made once, and every validation engine sends the same request.
- **`WITH EVERY` in rules**: a rule set can apply to every element of a node or a field.
- **The EFX-2 language changes of the SDK**: see the SDK changelog.
- **EFX-1** works as before, with clearer error messages and a few corrections.

## What you need to change

- Build with the SDK pre-release that follows 2.0.0-alpha.5. Its EFX-2 grammar no longer types expressions, and this version does not build with an earlier one.
- Write EFX-2 indexes as `[at n]`, and statement keywords, such as `WITH` and `LET`, in upper case. The SDK changelog lists the other language changes.
- Validate the generated Schematron with an XSLT 3.0 processor, such as Saxon 12.
- If you wrote your own `efx:call-api`, it now receives the whole query string.
- If you wrote your own `SymbolResolver`, implement its three new methods.
- If your code extends the EFX-2 translators or uses the toolkit's model or exception classes, see the javadoc: many of them changed. The public constructors of the translators did not change.
- The dependency graph writes `codelists`, where it wrote `codeLists`.

## Download

You can download the latest EFX Toolkit from Maven Central.
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://central.sonatype.com/artifact/eu.europa.ted.eforms/efx-toolkit-java)

Documentation for the EFX Toolkit is available at: <https://docs.ted.europa.eu/eforms/latest/efx-toolkit>
