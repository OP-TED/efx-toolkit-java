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
package eu.europa.ted.efx.sdk1;

import eu.europa.ted.efx.interfaces.TypeChecker;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxTypeLattice;

/**
 * V1-compatible type checker that handles abstract base types (Sequence, Scalar).
 * Replicates V1 behavior where EfxExpressionType.Path extended both Scalar and Sequence,
 * allowing PathExpressions to convert to either target type.
 */
public class TypeCheckerV1 implements TypeChecker {

  public static final TypeCheckerV1 INSTANCE = new TypeCheckerV1();

  private TypeCheckerV1() {}

  @Override
  public boolean canConvert(Class<? extends TypedExpression> from,
                            Class<? extends TypedExpression> to) {
    // Direct class compatibility
    if (to.isAssignableFrom(from)) {
      return true;
    }

    // Handle abstract base types FIRST (before annotation check, since these have no annotations)
    // PathExpression can convert to ANY Scalar or Sequence target
    if (PathExpression.class.isAssignableFrom(from)) {
      if (to == SequenceExpression.class || to == ScalarExpression.class) {
        return true;
      }
    }

    // Get source annotation (required for remaining checks)
    var fromAnnotation = from.getAnnotation(EfxDataTypeAssociation.class);
    if (fromAnnotation == null) {
      return false;
    }
    var fromType = fromAnnotation.dataType();

    // Handle abstract target types (ScalarExpression/SequenceExpression have no annotations)
    if (to == SequenceExpression.class) {
      return EfxTypeLattice.isSequence(fromType) || EfxTypeLattice.isScalar(fromType);
    }
    if (to == ScalarExpression.class) {
      return EfxTypeLattice.isScalar(fromType);
    }

    // For concrete target types, get the annotation
    var toAnnotation = to.getAnnotation(EfxDataTypeAssociation.class);
    if (toAnnotation == null) {
      return false;
    }
    var toType = toAnnotation.dataType();

    // PathExpression to concrete type - check primitive compatibility
    if (PathExpression.class.isAssignableFrom(from)) {
      var fromPrimitive = EfxTypeLattice.toPrimitive(fromType);
      var toPrimitive = EfxTypeLattice.toPrimitive(toType);
      return toPrimitive.isAssignableFrom(fromPrimitive);
    }

    // Direct type compatibility
    if (toType.isAssignableFrom(fromType)) {
      return true;
    }

    // Scalar can promote to Sequence
    if (EfxTypeLattice.isScalar(fromType) && EfxTypeLattice.isSequence(toType)) {
      var fromPrimitive = EfxTypeLattice.toPrimitive(fromType);
      var toPrimitive = EfxTypeLattice.toPrimitive(toType);
      return toPrimitive.isAssignableFrom(fromPrimitive);
    }

    return false;
  }
}
