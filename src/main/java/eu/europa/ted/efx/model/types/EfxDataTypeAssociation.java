package eu.europa.ted.efx.model.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime annotation linking an expression class to its {@link EfxDataType}.
 *
 * Applied to concrete expression classes to declare their type in the EFX type system,
 * enabling type introspection and type-safe operations during translation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EfxDataTypeAssociation {
    Class<? extends EfxDataType> dataType();
}