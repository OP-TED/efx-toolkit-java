package eu.europa.ted.efx.model.templates;

import java.util.LinkedList;

import eu.europa.ted.efx.model.ParsedEntity;

public class Conditionals extends LinkedList<Conditional> implements ParsedEntity {

    public Conditionals() {
        super();
    }

    public Conditionals(Conditionals other) {
        super(other);
    }
}