package eu.europa.ted.efx.model.identifiers;

import eu.europa.ted.efx.model.constructs.Spelling;
import eu.europa.ted.efx.model.types.EfxDataType;

public class Template extends Invocable {

    public Template(String name, ParsedParameters parameters) {
        super(name, EfxDataType.VOID, parameters);
    }

    /**
     * @param name the name of a template, declared or not
     * @return the template as {@code INVOKE} writes it, by its name
     */
    public static Spelling spellingOf(final String name) {
        return Spelling.template(name);
    }

    /**
     * @return the template as {@code INVOKE} writes it, by its name
     */
    @Override
    public Spelling getSpelling() {
        return spellingOf(this.name);
    }
}