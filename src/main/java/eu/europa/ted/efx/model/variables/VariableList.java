package eu.europa.ted.efx.model.variables;

import java.util.Collections;
import java.util.LinkedList;

import eu.europa.ted.efx.model.ParsedEntity;

public class VariableList extends LinkedList<Variable> implements ParsedEntity {

  public VariableList() {
  }

  public VariableList declaredOrder() {
    VariableList reversedList = new VariableList();
    reversedList.addAll(this);
    Collections.reverse(reversedList);
    return reversedList;
  }
}
