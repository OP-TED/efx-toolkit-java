/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model.rules;

import java.util.ArrayDeque;
import java.util.Deque;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.model.ParsedEntity;

/**
 * A typed stack for building the rules intermediate model during translation.
 *
 * Holds ValidationStage, RuleSet, and ValidationRule instances, providing
 * typed pop/peek methods that give clear error messages instead of ClassCastExceptions.
 */
public class RuleStack {

  private final Deque<ParsedEntity> stack = new ArrayDeque<>();

  public void push(ParsedEntity item) {
    this.stack.push(item);
  }

  public <T extends ParsedEntity> T pop(Class<T> expectedType) {
    if (this.stack.isEmpty()) {
      throw TranslatorConfigurationException.rulesStackEmpty(expectedType);
    }
    ParsedEntity item = this.stack.pop();
    if (!expectedType.isInstance(item)) {
      throw TranslatorConfigurationException.rulesStackError(expectedType, item.getClass());
    }
    return expectedType.cast(item);
  }

  public void clear() {
    this.stack.clear();
  }

  public <T extends ParsedEntity> boolean contains(final Class<T> type) {
    return this.stack.stream().anyMatch(type::isInstance);
  }

  public <T extends ParsedEntity> T peek(Class<T> expectedType) {
    if (this.stack.isEmpty()) {
      throw TranslatorConfigurationException.rulesStackEmpty(expectedType);
    }
    ParsedEntity item = this.stack.peek();
    if (!expectedType.isInstance(item)) {
      throw TranslatorConfigurationException.rulesStackError(expectedType, item.getClass());
    }
    return expectedType.cast(item);
  }
}
