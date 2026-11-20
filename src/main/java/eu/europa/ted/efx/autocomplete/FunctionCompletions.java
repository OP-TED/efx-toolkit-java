/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.europa.ted.efx.model.builtins.BuiltIn;
import eu.europa.ted.efx.model.constructs.Parameter;
import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.constructs.Signature;

/**
 * The completion items of a built-in function: the mapping from the signature catalogue to the
 * words of the editors, whose caller is {@link eu.europa.ted.efx.EfxAutocomplete}.
 *
 * <p>Each overload gives one item for each of its concrete forms, one per value type when it
 * mentions {@code T}. A form whose parameters are the first ones of a longer form, with the same
 * names, types and result, is folded into that form, whose extra parameters become optional. A
 * form that takes or gives a node is left out, since the editors have no data type for one.</p>
 */
public final class FunctionCompletions {

  private final String name;
  private final List<Form> forms;

  private FunctionCompletions(final BuiltIn function) {
    this.name = function.getName();
    this.forms = function.getOverloads().stream()
        .flatMap(overload -> overload.getSignature().concreteForms().stream())
        .map(Form::new)
        .filter(Form::isNamed)
        .collect(Collectors.toList());
  }

  /**
   * Gives the completion items of a built-in function.
   *
   * @param function a built-in whose {@link BuiltIn#isFunction()} is {@code true}
   * @return its items, one for each form that is not folded into another, in the order of the
   *     overloads and of the value types {@code T} takes
   */
  public static List<CompletionItem> itemsOf(final BuiltIn function) {
    return new FunctionCompletions(function).items();
  }

  private List<CompletionItem> items() {
    return this.forms.stream()
        .filter(form -> this.forms.stream().noneMatch(form::begins))
        .map(form -> form.itemNamed(this.name, this.requiredParametersOf(form)))
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Counts the parameters of a form that a call must give: those of the shortest form folded into
   * it, or all of its own.
   */
  private int requiredParametersOf(final Form form) {
    return this.forms.stream()
        .filter(shorter -> shorter.begins(form))
        .mapToInt(Form::arity)
        .min()
        .orElse(form.arity());
  }

  /**
   * A signature with {@code T} bound, as the editors see it.
   */
  private static final class Form {

    private final Signature signature;

    Form(final Signature signature) {
      this.signature = signature;
    }

    /**
     * @return {@code true} if the editors have a data type for every parameter and the result
     */
    boolean isNamed() {
      return this.types().allMatch(type -> EfxDataType.of(type).isPresent());
    }

    int arity() {
      return this.fixed().size();
    }

    /**
     * Tells whether this form's parameters are the first ones of a longer form, with the same
     * names, types and result, so that this form folds into it.
     */
    boolean begins(final Form longer) {
      if (this.variadic().isPresent()
          || this.arity() >= longer.arity()
          || !sameType(this.signature.getResult(), longer.signature.getResult())) {
        return false;
      }
      for (int position = 0; position < this.arity(); position++) {
        final Parameter own = this.fixed().get(position);
        final Parameter other = longer.fixed().get(position);
        if (!own.getName().equals(other.getName()) || !sameType(own.getType(), other.getType())) {
          return false;
        }
      }
      return true;
    }

    /**
     * @param name the name of the function
     * @param required how many of the fixed parameters a call must give; the rest are optional
     * @return the completion item of this form
     */
    CompletionItem itemNamed(final String name, final int required) {
      final List<EfxFunctionParameter> parameters = new ArrayList<>();
      for (int position = 0; position < this.arity(); position++) {
        final Parameter parameter = this.fixed().get(position);
        parameters.add(new EfxFunctionParameter(parameter.getName(),
            dataTypeOf(parameter.getType()), position >= required, false));
      }
      this.variadic().ifPresent(tail -> parameters.add(
          new EfxFunctionParameter(tail.getName(), dataTypeOf(tail.getType()), false, true)));
      return new CompletionItem(name, CompletionKind.FUNCTION,
          dataTypeOf(this.signature.getResult()), List.copyOf(parameters));
    }

    private List<? extends Parameter> fixed() {
      return this.signature.getParameters();
    }

    private Optional<? extends Parameter> variadic() {
      return this.signature.getParameters().getVariadic();
    }

    private Stream<ParameterType<?>> types() {
      return Stream.concat(Stream.concat(this.fixed().stream(), this.variadic().stream())
          .map(Parameter::getType), Stream.of(this.signature.getResult()));
    }

    private static boolean sameType(final ParameterType<?> one, final ParameterType<?> other) {
      return one.getExpressionType().equals(other.getExpressionType());
    }

    private static EfxDataType dataTypeOf(final ParameterType<?> type) {
      return EfxDataType.of(type).orElseThrow();
    }
  }
}
