package eu.europa.ted.efx.interfaces;

/**
 * Enum representing different sections of a template.
 * 
 * Used in the {@link TranslatorContext} to communicate the section of the template
 * being processed to the {@link MarkupGenerator}.
 */
public enum TemplateSection {
  DEFAULT,
  SUMMARY,
  NAVIGATION
}