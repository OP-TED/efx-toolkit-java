package eu.europa.ted.efx.model.types;

public interface EfxExpressionType {
    public interface Scalar extends EfxExpressionType {}
    public interface Sequence extends EfxExpressionType {}
    public interface Path extends EfxExpressionType.Scalar, EfxExpressionType.Sequence {}
}