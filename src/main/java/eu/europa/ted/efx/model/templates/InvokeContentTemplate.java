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

import org.antlr.v4.runtime.Token;

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
  private final Token start;

  /**
   * @param start the token where the invocation starts, where an error about it is placed
   */
  public InvokeContentTemplate(String templateName, Arguments<ParsedParameter> arguments,
      Token start) {
    this.templateName = templateName;
    this.arguments = arguments;
    this.start = start;
  }

  /**
   * @return the name of the template it invokes
   */
  public String getTemplateName() {
    return this.templateName;
  }

  /**
   * @return the arguments of the call, in the order they are written
   */
  public Arguments<ParsedParameter> getArguments() {
    return this.arguments;
  }

  /**
   * @return the token where the invocation starts, where an error about it is placed
   */
  public Token getStart() {
    return this.start;
  }

  /**
   * Renders the call without anything above the template ({@link OutlineOffset#NONE}).
   */
  @Override
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    return this.render(markupGenerator, translatorContext, OutlineOffset.NONE);
  }

  /**
   * Renders the call with the arguments written in EFX.
   *
   * @param offset what the call puts above the lines of the template: what is above the invoking
   *        line in its own section or template
   */
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext,
      OutlineOffset offset) {
    Set<Argument> args = this.arguments.stream()
        .map(a -> new Argument.Impl(a.parameter.name,
            markupGenerator.getEfxDataTypeEquivalent(a.parameter.dataType), a.value))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return markupGenerator.renderFragmentInvocation(this.templateName, offset, args,
        translatorContext);
  }
}
