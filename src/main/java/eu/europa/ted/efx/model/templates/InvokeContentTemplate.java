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
package eu.europa.ted.efx.model.templates;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.constructs.Arguments;
import eu.europa.ted.efx.model.identifiers.ParsedParameter;

/**
 * A content template that calls a declared template, as described by the invokeTemplate rule of the
 * grammar.
 */
public class InvokeContentTemplate extends ContentTemplate {

  private final String templateName;
  private final Arguments<ParsedParameter> arguments;

  public InvokeContentTemplate(String templateName, Arguments<ParsedParameter> arguments) {
    this.templateName = templateName;
    this.arguments = arguments;
  }

  @Override
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    Set<Argument> args = this.arguments.stream()
        .map(a -> new Argument.Impl(a.parameter.name,
            markupGenerator.getEfxDataTypeEquivalent(a.parameter.dataType), a.value))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return markupGenerator.renderFragmentInvocation(this.templateName, args, translatorContext);
  }
}
