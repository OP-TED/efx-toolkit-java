package eu.europa.ted.efx.model.expressions;

import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxDataTypeAssociation;
import eu.europa.ted.efx.model.types.EfxExpressionType;
import eu.europa.ted.efx.model.types.EfxExpressionTypeAssociation;

public interface TypedExpression extends Expression {

  public Class<? extends EfxExpressionType> getExpressionType();

  public Class<? extends EfxDataType> getDataType();

  public Boolean is(Class<? extends EfxDataType> dataType);

  public Boolean is(Class<? extends EfxExpressionType> referenceType, Class<? extends EfxDataType> dataType);

  static <T extends EfxDataType, E extends TypedExpression> Class<? extends T> getEfxDataType(
      Class<? extends E> clazz, Class<? extends T> dataType1) {
    EfxDataTypeAssociation annotation = clazz.getAnnotation(EfxDataTypeAssociation.class);
    if (annotation == null) {
      return EfxDataType.ANY.asSubclass(dataType1); // throw new IllegalArgumentException("Missing
                                                    // @EfxDataTypeAssociation annotation");
    }
    return annotation.dataType().asSubclass(dataType1);
  }

  static Class<? extends EfxDataType> getEfxDataType(Class<? extends TypedExpression> clazz) {
    EfxDataTypeAssociation annotation = clazz.getAnnotation(EfxDataTypeAssociation.class);
    if (annotation == null) {
      throw new IllegalArgumentException("Missing @EfxDataTypeAssociation annotation");
    }
    return annotation.dataType();
  }

  public static <T extends TypedExpression> T from(TypedExpression source, Class<T> targetType) {
    if (PathExpression.class.isAssignableFrom(targetType)) {
      return targetType.cast(PathExpression.from(source, targetType.asSubclass(PathExpression.class)));
    } else if (SequenceExpression.class.isAssignableFrom(targetType)) {
      return targetType.cast(SequenceExpression.from(source, targetType.asSubclass(SequenceExpression.class)));
    } else if (ScalarExpression.class.isAssignableFrom(targetType)) {
      return targetType.cast(ScalarExpression.from(source, targetType.asSubclass(ScalarExpression.class)));
    } else {
      throw new RuntimeException("Unknown expression type: " + targetType);
    }
  }

  public static <E extends EfxExpressionType, D extends EfxDataType> TypedExpression instantiate(String script,
      Class<E> expressionType, Class<D> dataType) {
    if (PathExpression.class.isAssignableFrom(expressionType)) {
      return PathExpression.instantiate(script, dataType);
    } else if (SequenceExpression.class.isAssignableFrom(expressionType)) {
      return SequenceExpression.instantiate(script, dataType);
    } else if (ScalarExpression.class.isAssignableFrom(expressionType)) {
      return ScalarExpression.instantiate(script, dataType);
    } else {
      throw new RuntimeException("Unknown expression type: " + expressionType);
    }
  }

  public static Boolean canConvert(Class<? extends TypedExpression> from, Class<? extends TypedExpression> to) {
    var fromExpressionType = from.getAnnotation(EfxExpressionTypeAssociation.class).expressionType();
    var fromDataType = from.getAnnotation(EfxDataTypeAssociation.class).dataType();
    var toExpressionType = to.getAnnotation(EfxExpressionTypeAssociation.class).expressionType();
    var toDataType = to.getAnnotation(EfxDataTypeAssociation.class).dataType();

    return toExpressionType.isAssignableFrom(fromExpressionType) && toDataType.isAssignableFrom(fromDataType);
  }

  public abstract class Impl<T extends EfxDataType> extends Expression.Impl implements TypedExpression {

    private Class<? extends EfxExpressionType> expressionType;
    private Class<? extends T> dataType;

    public Impl(final String script, Class<? extends EfxExpressionType> expressionType,
        Class<? extends T> dataType) {
      this(script, false, expressionType, dataType);
    }

    public Impl(final String script, final Boolean isLiteral,
        Class<? extends EfxExpressionType> expressionType, Class<? extends T> dataType) {
      super(script, isLiteral);
      this.expressionType = expressionType;
      this.dataType = dataType;
    }

    @Override
    public Class<? extends EfxExpressionType> getExpressionType() {
        return this.expressionType;
    }

    @Override
    public Class<? extends T> getDataType() {
      return this.dataType;
    }

    @Override
    public Boolean is(Class<? extends EfxDataType> dataType) {
      return dataType.isAssignableFrom(this.dataType);
    }

    @Override
    public Boolean is(Class<? extends EfxExpressionType> referenceType, Class<? extends EfxDataType> dataType) {
      return referenceType.isAssignableFrom(this.expressionType) && dataType.isAssignableFrom(this.dataType);
    }
  }
}