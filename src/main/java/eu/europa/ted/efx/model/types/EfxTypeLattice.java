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
package eu.europa.ted.efx.model.types;

import java.util.Arrays;
import java.util.List;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;

/**
 * Provides type conversion operations for the EFX type system.
 *
 * The EFX type system is a two-dimensional lattice with primitive types
 * (String, Boolean, Number, Date, Time, Duration, Node) and cardinalities
 * (Scalar for single values, Sequence for multiple values).
 *
 * @see #toPrimitive(Class) Get the primitive type (e.g., String from StringScalar)
 * @see #toScalar(Class) Get the scalar variant (e.g., StringScalar from String)
 * @see #toSequence(Class) Get the sequence variant (e.g., StringSequence from String)
 */
public final class EfxTypeLattice {

    private EfxTypeLattice() {
        // Utility class - prevent instantiation
    }

    /**
     * Groups a primitive type with its scalar and sequence variants.
     */
    private static final class TypeVariants {
        final Class<? extends EfxDataType.Primitive> primitive;
        final Class<? extends EfxDataType.ConcreteScalar> scalar;
        final Class<? extends EfxDataType.ConcreteSequence> sequence;

        TypeVariants(Class<? extends EfxDataType.Primitive> primitive,
                     Class<? extends EfxDataType.ConcreteScalar> scalar,
                     Class<? extends EfxDataType.ConcreteSequence> sequence) {
            this.primitive = primitive;
            this.scalar = scalar;
            this.sequence = sequence;
        }
    }

    /**
     * Registered type variants. Order matters: specific types must come before
     * their supertypes (e.g., MultilingualString before String) for correct
     * subtype matching.
     */
    private static final List<TypeVariants> TYPE_VARIANTS = Arrays.asList(
        new TypeVariants(EfxDataType.MultilingualString.class,
                         EfxDataType.MultilingualStringScalar.class,
                         EfxDataType.MultilingualStringSequence.class),
        new TypeVariants(EfxDataType.String.class,
                         EfxDataType.StringScalar.class,
                         EfxDataType.StringSequence.class),
        new TypeVariants(EfxDataType.Number.class,
                         EfxDataType.NumberScalar.class,
                         EfxDataType.NumberSequence.class),
        new TypeVariants(EfxDataType.Boolean.class,
                         EfxDataType.BooleanScalar.class,
                         EfxDataType.BooleanSequence.class),
        new TypeVariants(EfxDataType.Date.class,
                         EfxDataType.DateScalar.class,
                         EfxDataType.DateSequence.class),
        new TypeVariants(EfxDataType.Time.class,
                         EfxDataType.TimeScalar.class,
                         EfxDataType.TimeSequence.class),
        new TypeVariants(EfxDataType.Duration.class,
                         EfxDataType.DurationScalar.class,
                         EfxDataType.DurationSequence.class),
        new TypeVariants(EfxDataType.Node.class,
                         EfxDataType.NodeScalar.class,
                         EfxDataType.NodeSequence.class)
    );

    /**
     * Returns the primitive type for the given EFX data type.
     *
     * @param type any EFX data type (primitive, scalar, or sequence)
     * @return the corresponding primitive type
     * @throws IllegalArgumentException if type is null
     * @throws TranslatorConfigurationException if type is not registered in TYPE_VARIANTS
     */
    public static Class<? extends EfxDataType.Primitive> toPrimitive(Class<? extends EfxDataType> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        for (TypeVariants variants : TYPE_VARIANTS) {
            if (variants.primitive.isAssignableFrom(type)) {
                return variants.primitive;
            }
        }
        throw TranslatorConfigurationException.typeNotRegistered(type);
    }

    /**
     * Returns the scalar variant for the given EFX data type.
     *
     * @param type any EFX data type (primitive, scalar, or sequence)
     * @return the corresponding scalar type
     * @throws IllegalArgumentException if type is null
     * @throws TranslatorConfigurationException if type is not registered in TYPE_VARIANTS
     */
    public static Class<? extends EfxDataType.ConcreteScalar> toScalar(Class<? extends EfxDataType> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        for (TypeVariants variants : TYPE_VARIANTS) {
            if (variants.primitive.isAssignableFrom(type)) {
                return variants.scalar;
            }
        }
        throw TranslatorConfigurationException.typeNotRegistered(type);
    }

    /**
     * Returns the sequence variant for the given EFX data type.
     *
     * @param type any EFX data type (primitive, scalar, or sequence)
     * @return the corresponding sequence type
     * @throws IllegalArgumentException if type is null
     * @throws TranslatorConfigurationException if type is not registered in TYPE_VARIANTS
     */
    public static Class<? extends EfxDataType.ConcreteSequence> toSequence(Class<? extends EfxDataType> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        for (TypeVariants variants : TYPE_VARIANTS) {
            if (variants.primitive.isAssignableFrom(type)) {
                return variants.sequence;
            }
        }
        throw TranslatorConfigurationException.typeNotRegistered(type);
    }

    /**
     * Returns true if the given type has scalar cardinality.
     *
     * @param type the EFX data type to check
     * @return true if the type implements {@link EfxDataType.Cardinality.Scalar}
     */
    public static boolean isScalar(Class<? extends EfxDataType> type) {
        return type != null && EfxDataType.Cardinality.Scalar.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the given type has sequence cardinality.
     *
     * @param type the EFX data type to check
     * @return true if the type implements {@link EfxDataType.Cardinality.Sequence}
     */
    public static boolean isSequence(Class<? extends EfxDataType> type) {
        return type != null && EfxDataType.Cardinality.Sequence.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the given type is registered in TYPE_VARIANTS.
     * Used by tests to verify all concrete types are registered.
     *
     * @param type the type to check
     * @return true if the type is registered
     */
    public static boolean isRegistered(Class<?> type) {
        for (TypeVariants variants : TYPE_VARIANTS) {
            if (variants.scalar.equals(type) || variants.sequence.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
