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
package eu.europa.ted.efx.sdk2;

import eu.europa.ted.efx.interfaces.TypeChecker;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

/**
 * V2 strict type checker that requires concrete types.
 * Rejects abstract Sequence.class and Scalar.class targets.
 */
public class TypeCheckerV2 implements TypeChecker {

  public static final TypeCheckerV2 INSTANCE = new TypeCheckerV2();

  private TypeCheckerV2() {}

  @Override
  public boolean canConvert(Class<? extends TypedExpression> from,
                            Class<? extends TypedExpression> to) {
    // Direct class compatibility
    if (to.isAssignableFrom(from)) {
      return true;
    }

    var fromAnnotation = from.getAnnotation(EfxDataTypeAssociation.class);
    var toAnnotation = to.getAnnotation(EfxDataTypeAssociation.class);
    assert fromAnnotation != null : "Missing @EfxDataTypeAssociation on " + from.getName();
    assert toAnnotation != null : "Missing @EfxDataTypeAssociation on " + to.getName();

    var fromType = fromAnnotation.dataType();
    var toType = toAnnotation.dataType();

    // V2 requires concrete types - reject abstract Sequence/Scalar
    assert to != SequenceExpression.class : "Cannot convert to untyped Sequence - use a typed sequence";
    assert to != ScalarExpression.class : "Cannot convert to untyped Scalar - use a typed scalar";

    // Direct type compatibility
    if (toType.isAssignableFrom(fromType)) {
      return true;
    }

    // Primitive types must be compatible (from can be a subtype of to)
    var fromPrimitive = EfxTypeLattice.toPrimitive(fromType);
    var toPrimitive = EfxTypeLattice.toPrimitive(toType);
    if (!toPrimitive.isAssignableFrom(fromPrimitive)) {
      return false;
    }

    // Scalar can promote to Sequence
    if (EfxTypeLattice.isScalar(fromType) && EfxTypeLattice.isSequence(toType)) {
      return true;
    }

    return false;
  }
}
