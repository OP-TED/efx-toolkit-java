package eu.europa.ted.efx.model.types;

import java.util.HashMap;
import java.util.Map;

public enum FieldTypes {
    ID("id"), //
    ID_REF("id-ref"), //
    TEXT("text"), //
    TEXT_MULTILINGUAL("text-multilingual"), //
    INDICATOR("indicator"), //
    AMOUNT("amount"), //
    NUMBER("number"), //
    MEASURE("measure"), //
    CODE("code"), //
    INTERNAL_CODE("internal-code"), //
    INTEGER("integer"), //
    DATE("date"), //
    ZONED_DATE("zoned-date"), //
    TIME("time"), //
    ZONED_TIME("zoned-time"), //
    URL("url"), //
    PHONE("phone"), //
    EMAIL("email");

    private static final Map<String, FieldTypes> lookup = new HashMap<>();

    static {
        for (FieldTypes fieldType : FieldTypes.values()) {
            lookup.put(fieldType.getName(), fieldType);
        }
    }

    private String name;

    FieldTypes(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FieldTypes fromString(String typeName) {
        return lookup.get(typeName);
    }
}