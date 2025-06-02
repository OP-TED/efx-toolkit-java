package eu.europa.ted.efx.interfaces;

import eu.europa.ted.efx.model.templates.Markup;

public interface Parameter {
    String getName();
    String getType();

    public class Impl implements Parameter {
        private final String name;
        private final String type;

        public Impl(String name, Markup type) {
            this.name = name;
            this.type = type.script;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getType() {
            return this.type;
        }
    }
}