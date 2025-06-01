package eu.europa.ted.efx.model.types;

public interface EfxDataType {
    Class<? extends EfxDataType> ANY = EfxDataType.class;
    Class<? extends EfxDataType> UNDEFINED = Undefined.class;

    interface Boolean extends EfxDataType {}
    interface String extends EfxDataType {}
    interface MultilingualString extends String {}
    interface Number extends EfxDataType {}
    interface Date extends EfxDataType {}
    interface Duration extends EfxDataType {}
    interface Time extends EfxDataType {}
    interface Node extends EfxDataType {}
    interface Undefined extends EfxDataType {}
}
