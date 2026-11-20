/*
 * Copyright 2025 European Union
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
package eu.europa.ted.efx.model.identifiers;

import java.util.List;

import eu.europa.ted.efx.model.constructs.Composable;
import eu.europa.ted.efx.model.constructs.Composer;
import eu.europa.ted.efx.model.constructs.Overload;
import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.constructs.Signature;
import eu.europa.ted.efx.model.constructs.Spelling;
import eu.europa.ted.efx.model.expressions.TypedExpression;

/**
 * A user-defined function declared in EFX source code.
 *
 * Extends {@link Invocable} to add the function body expression. Its declaration gives it its
 * signature: the declared parameters, and the declared type of its value, which is the type of a
 * call of it. The one overload of that signature translates a call of it.
 */
public class Function extends Invocable implements Composable {

    public final TypedExpression expression;

    private final Overload overload;

    /**
     * @param name the name of the function, without its prefix
     * @param parameters the declared parameters
     * @param type the class of the declared type of its value
     * @param expression the body, converted to the declared type
     */
    public Function(String name, ParsedParameters parameters, Class<? extends TypedExpression> type,
            TypedExpression expression) {
        this(name, parameters, type, expression, (script, arguments) -> script
            .composeFunctionInvocation(name, arguments.getArgumentValues(), type));
    }

    /**
     * @param name the name of the function, without its prefix
     * @param parameters the declared parameters
     * @param type the class of the declared type of its value
     * @param expression the body, converted to the declared type
     * @param composer the composer that translates a call of the function
     */
    protected Function(String name, ParsedParameters parameters,
            Class<? extends TypedExpression> type, TypedExpression expression, Composer composer) {
        super(name, TypedExpression.getEfxDataType(type), parameters);
        this.expression = expression;
        this.overload = Overload.of(Signature.of(parameters, ParameterType.of(type)), composer);
    }

    /**
     * @return the one overload, of the declared signature
     */
    @Override
    public List<Overload> getOverloads() {
        return List.of(this.overload);
    }

    /**
     * @param name the name of a function declared in EFX, declared or not
     * @return the function as a call writes it, with its prefix, as {@code ?greeting}
     */
    public static Spelling spellingOf(final String name) {
        return Spelling.function("?" + name);
    }

    /**
     * @return the function as a call writes it, with its prefix, as {@code ?greeting}
     */
    @Override
    public Spelling getSpelling() {
        return spellingOf(this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Function function = (Function) o;
        if (parameters != null ? !parameters.equals(function.parameters) : function.parameters != null)
            return false;
        return expression != null ? expression.equals(function.expression) : function.expression == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        return result;
    }
}