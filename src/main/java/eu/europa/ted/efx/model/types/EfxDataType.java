/*
 * Copyright 2023 European Union
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
package eu.europa.ted.efx.model.types;

/**
 * Marker interfaces defining the EFX type system as a 2D lattice.
 *
 * The type system has two dimensions:
 * - Primitive type (String, Boolean, Number, Date, Time, Duration, Node)
 * - Cardinality (Scalar for single values, Sequence for multiple values)
 *
 * Concrete types combine both dimensions (e.g., StringScalar, BooleanSequence).
 * Expression classes use {@link EfxDataTypeAssociation} to declare their type.
 *
 * @see EfxTypeLattice for type conversion operations
 */
public interface EfxDataType {
    Class<? extends EfxDataType> VOID = EfxDataType.Void.class;

    /**
     * Cardinality markers for EFX types (scalar vs sequence).
     * Nested within EfxDataType but NOT extending it, to prevent
     * accidental misuse where primitive types are expected.
     */
    interface Cardinality {
        /** Marker for single-value types */
        interface Scalar extends Cardinality {}
        /** Marker for multi-value types */
        interface Sequence extends Cardinality {}
    }

    /**
     * Type category markers that extend EfxDataType.
     * Used for type-safe return types in EfxTypeLattice.
     */
    interface Primitive extends EfxDataType {}
    interface ConcreteScalar extends EfxDataType {}
    interface ConcreteSequence extends EfxDataType {}

    // EFX primitive types (extend Primitive marker)
    interface Boolean extends Primitive {}
    interface Dynamic extends Boolean {}
    interface String extends Primitive {}
    interface MultilingualString extends String {}
    interface Number extends Primitive {}
    interface Date extends Primitive {}
    interface Time extends Primitive {}
    interface Duration extends Primitive {}
    interface Node extends Primitive {}
    /** Unit type for templates/procedures that produce output but don't return a value. */
    interface Void extends EfxDataType {}

    // Concrete scalar types (primitive + Cardinality.Scalar + ConcreteScalar)
    interface BooleanScalar extends Boolean, Cardinality.Scalar, ConcreteScalar {}
    interface DynamicScalar extends BooleanScalar, Dynamic {}
    interface StringScalar extends String, Cardinality.Scalar, ConcreteScalar {}
    interface MultilingualStringScalar extends StringScalar, MultilingualString {}
    interface NumberScalar extends Number, Cardinality.Scalar, ConcreteScalar {}
    interface DateScalar extends Date, Cardinality.Scalar, ConcreteScalar {}
    interface TimeScalar extends Time, Cardinality.Scalar, ConcreteScalar {}
    interface DurationScalar extends Duration, Cardinality.Scalar, ConcreteScalar {}
    interface NodeScalar extends Node, Cardinality.Scalar, ConcreteScalar {}

    // Concrete sequence types (primitive + Cardinality.Sequence + ConcreteSequence)
    interface BooleanSequence extends Boolean, Cardinality.Sequence, ConcreteSequence {}
    interface DynamicSequence extends BooleanSequence, Dynamic {}
    interface StringSequence extends String, Cardinality.Sequence, ConcreteSequence {}
    interface MultilingualStringSequence extends StringSequence, MultilingualString {}
    interface NumberSequence extends Number, Cardinality.Sequence, ConcreteSequence {}
    interface DateSequence extends Date, Cardinality.Sequence, ConcreteSequence {}
    interface TimeSequence extends Time, Cardinality.Sequence, ConcreteSequence {}
    interface DurationSequence extends Duration, Cardinality.Sequence, ConcreteSequence {}
    interface NodeSequence extends Node, Cardinality.Sequence, ConcreteSequence {}
}
