/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Lic
 */
package eu.europa.ted.efx.interfaces;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.templates.Markup;

/**
 * The role of this interface is to allow the reuse of the Sdk6EfxTemplateTranslator to generate
 * markup for any target template language,
 * 
 * The methods provided by this interface cover two needs: a) Take an {@link Expression} as a
 * parameter and generate the target template markup necessary for rendering it; and b) Take
 * multiple {@link Markup} objects already generated by other method calls and generate the markup
 * to properly combine them in the target template.
 */
public interface MarkupGenerator {

  /**
   * Given a body (main content) and a set of fragments, this method returns the full content of the
   * target template file.
   * 
   * @param content the body (main content) of the template.
   * @param fragments the fragments to be included in the template file.
   * @return the full content of the target template file.
   */
  Markup composeOutputFile(final List<Markup> content, final List<Markup> fragments);

  /**
   * Given an expression (which will eventually, at runtime, evaluate to the value of a field), this
   * method returns the template code that dereferences it (retrieves the value) in the target
   * template.
   * 
   * @param variableExpression the expression to be evaluated and rendered.
   * @return the template code that dereferences the expression in the target template.
   */
  Markup renderVariableExpression(final Expression variableExpression);

  /**
   * Given a label key (which will eventually, at runtime, be dereferenced to a label text), this
   * method returns the template code that renders this label in the target template language.
   * 
   * @param key the label key to be dereferenced.
   * @return the template code that renders the label in the target template language.
   */
  Markup renderLabelFromKey(final StringExpression key);

  /**
   * Given a label key (which will eventually, at runtime, be dereferenced to a label text), this
   * method returns the template code that renders this label in the target template language.
   * 
   * @param key the label key to be dereferenced.
   * @param quantity a numeric quantity used to decide if the label needs to be pluralized.
   * @return the template code that renders the label in the target template language.
   */
  Markup renderLabelFromKey(final StringExpression key, final NumericExpression quantity);

  /**
   * Given an expression (which will eventually, at runtime, be evaluated to a label key and
   * subsequently dereferenced to a label text), this method returns the template code that renders
   * this label in the target template language.
   * 
   * @param expression the expression that returns the label key.
   * @return the template code that renders the label in the target template language.
   */
  Markup renderLabelFromExpression(final Expression expression);

  /**
   * Given an expression (which will eventually, at runtime, be evaluated to a label key and
   * subsequently dereferenced to a label text), this method returns the template code that renders
   * this label in the target template language.
   * 
   * @param expression the expression that returns the label key.
   * @param quantity a numeric quantity used to decide if the label needs to be pluralized.
   * @return the template code that renders the label in the target template language.
   */
  Markup renderLabelFromExpression(final Expression expression, final NumericExpression quantity);

  /**
   * Given a string of free text, this method returns the template code that adds this text in the
   * target template.
   * 
   * @param freeText the free text to be rendered.
   * @return the template code that adds this text in the target template.
   */
  Markup renderFreeText(final String freeText);

  Markup renderLineBreak();

  /**
   * @deprecated Use {@link #composeFragmentDefinition(String, String, Markup, Set)} instead.
   * 
   * @param name the name of the fragment.
   * @param number the outline number of the fragment.
   * @param content the content of the fragment.
   * @return the code that encapsulates the fragment in the target template.
   */
  @Deprecated(since = "2.0.0", forRemoval = true)
  Markup composeFragmentDefinition(final String name, String number, Markup content);

  /**
   * Given a fragment name (identifier) and some pre-rendered content, this method returns the code
   * that encapsulates it in the target template
   * 
   * @param name the name of the fragment.
   * @param number the outline number of the fragment.
   * @param content the content of the fragment.
   * @param parameters the parameters of the fragment.
   * @return the code that encapsulates the fragment in the target template.
   */
  Markup composeFragmentDefinition(final String name, String number, Markup content,
      Set<String> parameters);

  /**
   * @deprecated Use {@link #renderFragmentInvocation(String, PathExpression, Set)} instead.
   * 
   * @param name the name of the fragment.
   * @param context the context of the fragment.
   * @return the code that invokes (uses) the fragment.
   */
  @Deprecated(since = "2.0.0", forRemoval = true)
  Markup renderFragmentInvocation(final String name, final PathExpression context);

  /**
   * Given a fragment name (identifier), and an evaluation context, this method returns the code
   * that invokes (uses) the fragment.
   * 
   * @param name the name of the fragment.
   * @param context the context of the fragment.
   * @param variables the variables of the fragment.
   * @return the code that invokes (uses) the fragment.
   */
  Markup renderFragmentInvocation(final String name, final PathExpression context,
      final Set<Pair<String, String>> variables);
}
