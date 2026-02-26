package eu.europa.ted.efx.autocomplete;

/**
 * The kind of an EFX autocomplete suggestion.
 * Aligns with CodeMirror's completion "type" and LSP's CompletionItemKind.
 */
public enum CompletionKind {
  FUNCTION,
  KEYWORD,
  PROPERTY
}
