package eu.europa.ted.efx.interfaces;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.templates.Markup;

public interface Argument extends Parameter {

    String getValue();

    public class Impl extends Parameter.Impl implements Argument {
        private final String value;

        public Impl(String name, Markup type, TypedExpression value) {
            super(name, type);
            this.value = value.getScript();
        }

        @Override
        public String getValue() {
            return value;
        }
    }
}