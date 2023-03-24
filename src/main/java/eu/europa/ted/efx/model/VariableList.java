package eu.europa.ted.efx.model;

import java.util.LinkedList;
import java.util.List;

public class VariableList extends CallStackObject {

    LinkedList<Variable<? extends Expression>> variables;

    public VariableList() {
        this.variables = new LinkedList<>();
    }

    public <T extends Expression> void push(Variable<T> variable) {
        this.variables.push(variable);
    }

    public synchronized Variable<? extends Expression> pop() {
        return this.variables.pop();
    }

    public boolean isEmpty() {
        return this.variables.isEmpty();
    }

    public List<Variable<? extends Expression>> asList() {
        return this.variables;
    }
}