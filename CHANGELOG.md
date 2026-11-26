# Changes since 2.0.0-alpha.9

These changes are on the `develop` branch and will be part of the next pre-release.

## Improvements

- **Clearer error messages**: an error in an EFX-2 expression, template or rule says where it is, with its line and column, also in a file included with `#include`, but not an error found while a file is included, such as a circular include. It says what is wrong and, where it can, how to fix it.
- **Faster translation**: in our measurements, the view templates and the validation rules of the SDK translate about five times faster.
- **Formatted values in templates**: numbers, dates, times, amounts, measures and durations are displayed formatted, with the label of their unit in the right plural. A template can choose the format, as in `${BT-00-Number|2}`.
- **Dynamic rules**: each call of the API is made once, and every validation engine sends the same request.
- **`WITH EVERY` in rules**: a rule set can apply to every element of a node or a field.
- **Semantic assertions in rules**: after `ASSERT` or `OTHERWISE ASSERT`, a rule can state what is required of its field or node, in the place of an expression, as in `ASSERT IS NOT EMPTY` or `ASSERT LENGTH <= 100`. A semantic assertion is translated to the same Schematron as the expression it stands for. With `BETWEEN`, the lower bound comes first: a first number greater than the second is an error. The rule keeps what it states as a `Behaviour` (`ValidationRule.getBehaviour()`), so that a generator for a client application can read how to configure the control of the field.
- **Backslashes in texts**: a text or a pattern that has two backslashes directly before a quote of the other kind, as in `'a\\"b'`, keeps both backslashes. Before, one of them was lost.
- **Completions for editors**: the jar now includes the EFX-2 completions (functions, keywords and properties) in `eu/europa/ted/efx/autocomplete/sdk2-completions.json`, for editors written in other languages.
- **The EFX-2 language changes of the SDK**: see the SDK changelog.
- **Kind and levels of template lines**: a markup generator receives, for each fragment, its outline (`FragmentOutline`): what it is (`FragmentKind`: a heading, a normal line, a line that invokes a template, or a template that holds lines), its outline number, its nesting level, its heading level and the body of its navigation id. In EFX-2, a line that starts with an outline marker (a number, `?` or `#`) is a heading.
- **Navigation entries link to headings**: a navigation entry links to the heading of the main section that has the same outline address, that is the outline numbers and tags of the headings above it and then its own, as in `5_1_10` or `1_notice-info`. This also works for the headings of a callable template, under every line that invokes it. The toolkit reports a navigation entry that has no heading to link to, two headings with the same address displayed for the same element, a navigation heading that displays a hyperlink, and a template that invokes itself.
- **EFX-1** works as before, with clearer error messages and a few corrections.

## What you need to change

- Build with the SDK pre-release that follows 2.0.0-alpha.5. Its EFX-2 grammar no longer types expressions, and this version does not build with an earlier one.
- Write EFX-2 indexes as `[at n]`, and statement keywords, such as `WITH` and `LET`, in upper case. The SDK changelog lists the other language changes.
- Mark the outline number of an EFX-2 template line with `?` where the line relied on an automatic number, and remove the `0` written for a line without a number: a line without a number, `?` or `#` now has no number.
- Replace the context shortcuts `.`, `..` and `/` with the node or field they stand for, for example `ND-Root` instead of `/`. They are no longer accepted as the context of a template line, a `WITH` clause or a context variable.
- Write `::` only after a context variable, as in `for context:$c in ND-1 return $c::BT-1`. Where you wrote `BT-1::BT-2` to read the BT-2 next to a BT-1, write `BT-2[BT-1 is present]`.
- Validate the generated Schematron with an XSLT 3.0 processor, such as Saxon 12.
- If you wrote your own `efx:call-api`, it now receives the whole query string.
- If you wrote your own `SymbolResolver`, implement its three new methods, and remove `getFieldIdFromAlias` and `getNodeIdFromAlias`: aliases are no longer part of EFX. If you extend `XPathSymbolResolver`, also remove `fieldByAlias` and `nodeByAlias`.
- If you wrote your own `MarkupGenerator`, implement `composeFragmentDefinition` with the new parameter `outline` (a `FragmentOutline`, in place of `number`) and the WHEN alternatives as a `List<Conditional>`, in their order, and `renderFragmentInvocation` with the new parameter `offset` (an `OutlineOffset`: a nesting level offset, a heading level offset and a navigation id prefix). The interface explains how they make the nesting level, the heading level and the navigation id of a line where it is displayed. The methods without them are deprecated, and the toolkit no longer calls them.
- If your code extends the EFX-2 translators or uses the toolkit's model or exception classes, see the javadoc: many of them changed. The public constructors of the translators did not change.
- The dependency graph writes `codelists`, where it wrote `codeLists`.

## Download

You can download the latest EFX Toolkit from Maven Central.
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://central.sonatype.com/artifact/eu.europa.ted.eforms/efx-toolkit-java)

Documentation for the EFX Toolkit is available at: <https://docs.ted.europa.eu/eforms/latest/efx-toolkit>
