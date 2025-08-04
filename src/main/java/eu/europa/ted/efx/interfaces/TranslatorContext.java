package eu.europa.ted.efx.interfaces;

/**
 * Used to pass context information from the translator to the {@link MarkupGenerator}.
 */
public class TranslatorContext {

    TemplateSection currentSection;

    /**
     * Gets the current section of the template being processed.
     *
     * @return the current section
     */
    public TemplateSection getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(TemplateSection currentSection) {
        this.currentSection = currentSection;
    }

    /**
     * Default instance of {@link TranslatorContext} with the section set to {@link TemplateSection#DEFAULT}.
     * This is used throughout the code for EFX-1 processing where only the default section is relevant.
     */
    public static final TranslatorContext DEFAULT = new TranslatorContext();

    public TranslatorContext() {
        this(TemplateSection.DEFAULT);
    }

    public TranslatorContext(TemplateSection currentSection) {
        this.currentSection = currentSection;
    }
}
