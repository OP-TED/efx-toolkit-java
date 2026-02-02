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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

class EfxTypeLatticeTest {

    /**
     * Verifies that all concrete EfxDataType types (those implementing Cardinality.Scalar or Cardinality.Sequence)
     * are registered in EfxTypeLattice.TYPE_VARIANTS.
     *
     * This test catches "forgot to register" errors at build time when adding new types.
     */
    @Test
    void allConcreteTypesShouldBeRegistered() {
        for (Class<?> nested : EfxDataType.class.getDeclaredClasses()) {
            // Skip marker interfaces (Cardinality, Primitive, ConcreteScalar, ConcreteSequence)
            if (nested == EfxDataType.Cardinality.class
                || nested == EfxDataType.Primitive.class
                || nested == EfxDataType.ConcreteScalar.class
                || nested == EfxDataType.ConcreteSequence.class) {
                continue;
            }

            boolean isScalar = EfxDataType.Cardinality.Scalar.class.isAssignableFrom(nested);
            boolean isSequence = EfxDataType.Cardinality.Sequence.class.isAssignableFrom(nested);

            if (isScalar || isSequence) {
                assertTrue(
                    EfxTypeLattice.isRegistered(nested),
                    "Concrete type " + nested.getSimpleName() + " not registered in TYPE_VARIANTS"
                );
            }
        }
    }

    /**
     * Verifies that TYPE_VARIANTS is ordered correctly: subtypes must come before their supertypes.
     * This is critical because the lookup methods use isAssignableFrom() which would match the wrong type
     * if supertypes came first.
     *
     * For example, MultilingualString extends String, so MultilingualString must appear before String
     * in TYPE_VARIANTS, otherwise a MultilingualString type would incorrectly match String.
     */
    @Test
    @SuppressWarnings("unchecked")
    void typeVariantsShouldBeOrderedWithSubtypesBeforeSupertypes() throws Exception {
        // Use reflection to access the private TYPE_VARIANTS field
        Field typeVariantsField = EfxTypeLattice.class.getDeclaredField("TYPE_VARIANTS");
        typeVariantsField.setAccessible(true);
        List<?> typeVariants = (List<?>) typeVariantsField.get(null);

        // Get the TypeVariants inner class
        Class<?> typeVariantsClass = Class.forName("eu.europa.ted.efx.model.types.EfxTypeLattice$TypeVariants");
        Field primitiveField = typeVariantsClass.getDeclaredField("primitive");
        primitiveField.setAccessible(true);

        // Check each pair: earlier types should not be supertypes of later types
        for (int i = 0; i < typeVariants.size(); i++) {
            Object currentVariant = typeVariants.get(i);
            Class<?> currentPrimitive = (Class<?>) primitiveField.get(currentVariant);

            for (int j = i + 1; j < typeVariants.size(); j++) {
                Object laterVariant = typeVariants.get(j);
                Class<?> laterPrimitive = (Class<?>) primitiveField.get(laterVariant);

                // Check if the current type is a supertype of the later type
                // This would be a violation: supertypes must come AFTER subtypes
                if (currentPrimitive.isAssignableFrom(laterPrimitive)
                    && !currentPrimitive.equals(laterPrimitive)) {
                    fail(String.format(
                        "TYPE_VARIANTS ordering violation: %s (at index %d) is a supertype of %s (at index %d). " +
                        "Supertypes must come after their subtypes to ensure correct matching with isAssignableFrom(). " +
                        "Please move %s after %s in the TYPE_VARIANTS list.",
                        currentPrimitive.getSimpleName(), i,
                        laterPrimitive.getSimpleName(), j,
                        laterPrimitive.getSimpleName(),
                        currentPrimitive.getSimpleName()));
                }
            }
        }
    }
}
