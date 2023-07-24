package eu.europa.ted.efx.model.types;

public interface EfxDataType {
    Class<? extends EfxDataType> ANY = EfxDataType.class;

    public interface Boolean extends EfxDataType {}
    public interface String extends EfxDataType {}
    public interface MultilingualString extends String {}
    public interface Number extends EfxDataType {}
    public interface Date extends EfxDataType {}
    public interface Duration extends EfxDataType {}
    public interface Time extends EfxDataType {}
    public interface Node extends EfxDataType {}
}
