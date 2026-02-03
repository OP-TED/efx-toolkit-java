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
package eu.europa.ted.efx.model.expressions;

import eu.europa.ted.efx.exceptions.ConsistencyCheckException;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

/**
 * An {@link Expression} with an associated {@link EfxDataType}.
 *
 * Each concrete implementation is annotated with {@link EfxDataTypeAssociation} to declare its
 * type in the EFX type system. This enables compile-time type checking and type-safe conversions
 * during EFX translation.
 *
 * @see EfxDataType for the complete type hierarchy
 * @see EfxDataTypeAssociation for the annotation linking expressions to types
 */
public interface TypedExpression extends Expression {

  public Class<? extends EfxDataType> getDataType();

  public boolean is(Class<? extends EfxDataType> dataType);

  static Class<? extends EfxDataType> getEfxDataType(Class<? extends TypedExpression> clazz) {
    EfxDataTypeAssociation annotation = clazz.getAnnotation(EfxDataTypeAssociation.class);
    if (annotation == null) {
      throw ConsistencyCheckException.missingTypeAnnotation(clazz);
    }
    return annotation.dataType();
  }

  public static <T extends TypedExpression> T from(TypedExpression source, Class<T> targetType) {
    // When target is PathExpression, delegate to PathExpression.from
    if (PathExpression.class.isAssignableFrom(targetType)) {
      return targetType.cast(PathExpression.from(source, targetType.asSubclass(PathExpression.class)));
    }

    // When converting PathExpression to SequenceExpression, use the appropriate concrete sequence type
    if (source instanceof PathExpression && SequenceExpression.class.isAssignableFrom(targetType)) {
      Class<? extends EfxDataType> primitiveType = EfxTypeLattice.toPrimitive(source.getDataType());
      Class<? extends SequenceExpression> concreteType = SequenceExpression.fromEfxDataType.get(primitiveType);
      if (concreteType != null) {
        return targetType.cast(Expression.from(source, concreteType));
      }
    }

    // When converting PathExpression to ScalarExpression, use the appropriate concrete scalar type
    if (source instanceof PathExpression && ScalarExpression.class.isAssignableFrom(targetType)) {
      Class<? extends EfxDataType> primitiveType = EfxTypeLattice.toPrimitive(source.getDataType());
      Class<? extends ScalarExpression> concreteType = ScalarExpression.fromEfxDataType.get(primitiveType);
      if (concreteType != null) {
        return targetType.cast(Expression.from(source, concreteType));
      }
    }

    if (SequenceExpression.class.isAssignableFrom(targetType)) {
      return targetType.cast(SequenceExpression.from(source, targetType.asSubclass(SequenceExpression.class)));
    } else if (ScalarExpression.class.isAssignableFrom(targetType)) {
      return targetType.cast(ScalarExpression.from(source, targetType.asSubclass(ScalarExpression.class)));
    } else {
      throw ConsistencyCheckException.unknownExpressionType(targetType);
    }
  }

  public abstract class Impl<T extends EfxDataType> extends Expression.Impl implements TypedExpression {

    private Class<? extends T> dataType;

    protected Impl(final String script, Class<? extends T> dataType) {
      super(script);
      this.dataType = dataType;
    }

    @Override
    public Class<? extends T> getDataType() {
      return this.dataType;
    }

    @Override
    public boolean is(Class<? extends EfxDataType> dataType) {
      return dataType.isAssignableFrom(this.dataType);
    }
  }
}