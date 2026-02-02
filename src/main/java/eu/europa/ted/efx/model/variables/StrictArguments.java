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
package eu.europa.ted.efx.model.variables;

import java.util.Objects;

import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.interfaces.TypeChecker;
import eu.europa.ted.efx.model.expressions.TypedExpression;

public class StrictArguments extends ParsedArguments {

    public final transient Parametrised identifier;

    public StrictArguments(Parametrised identifier) {
        super();
        this.identifier = identifier;
    }

    public boolean addArgument(TypedExpression argument) {
        int position = this.size(); // Get the current position of the argument
        if (position >= identifier.parameters.size()) {
            throw InvalidArgumentException.argumentNumberMismatch(this.identifier, this.identifier.parameters.size());
        }

        var actualType = argument.getClass();
        var expectedType = this.identifier.parameters.get(position).getParameterType();
        if (!TypeChecker.V2.canConvert(actualType, expectedType)) {
            throw InvalidArgumentException.argumentTypeMismatch(position, this.identifier, expectedType, actualType);
        }
        return this.add(new ParsedArgument(this.identifier.parameters.get(position), argument));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof StrictArguments)) {
            return false;
        }
        StrictArguments other = (StrictArguments) obj;
        return Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(this.identifier);
    }
}