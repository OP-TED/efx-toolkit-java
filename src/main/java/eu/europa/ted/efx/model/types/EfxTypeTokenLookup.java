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

import static java.util.Map.entry;

import java.util.Map;
import java.util.stream.Collectors;

import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.sdk2.EfxLexer;

/**
 * Maps between different type name representations used in the EFX type system.
 * Provides safe lookup methods that throw {@link TranslatorConfigurationException}
 * when a mapping is missing, instead of silently returning null.
 */
public final class EfxTypeTokenLookup {

  public static final String TEXT = getLexerSymbol(EfxLexer.Text);
  public static final String INDICATOR = getLexerSymbol(EfxLexer.Indicator);
  public static final String NUMERIC = getLexerSymbol(EfxLexer.Number);
  public static final String DATE = getLexerSymbol(EfxLexer.Date);
  public static final String TIME = getLexerSymbol(EfxLexer.Time);
  public static final String DURATION = getLexerSymbol(EfxLexer.Duration);

  /** The name of a node in error messages: no type keyword names one. */
  private static final String NODE = "node";

  /** The mark of a sequence type in a type declaration. */
  private static final String SEQUENCE_MARK = getLexerSymbol(EfxLexer.Star);

  private static final Map<String, String> FROM_EFORMS_TYPE = Map.ofEntries(
      entry(FieldTypes.ID.getName(), TEXT),
      entry(FieldTypes.ID_REF.getName(), TEXT),
      entry(FieldTypes.TEXT.getName(), TEXT),
      entry(FieldTypes.TEXT_MULTILINGUAL.getName(), TEXT),
      entry(FieldTypes.INDICATOR.getName(), INDICATOR),
      entry(FieldTypes.AMOUNT.getName(), NUMERIC),
      entry(FieldTypes.NUMBER.getName(), NUMERIC),
      entry(FieldTypes.MEASURE.getName(), NUMERIC),
      entry(FieldTypes.DURATION.getName(), DURATION),
      entry(FieldTypes.CODE.getName(), TEXT),
      entry(FieldTypes.INTERNAL_CODE.getName(), TEXT),
      entry(FieldTypes.INTEGER.getName(), NUMERIC),
      entry(FieldTypes.DATE.getName(), DATE),
      entry(FieldTypes.ZONED_DATE.getName(), DATE),
      entry(FieldTypes.TIME.getName(), TIME),
      entry(FieldTypes.ZONED_TIME.getName(), TIME),
      entry(FieldTypes.URL.getName(), TEXT),
      entry(FieldTypes.PHONE.getName(), TEXT),
      entry(FieldTypes.EMAIL.getName(), TEXT));

  private static final Map<Class<? extends EfxDataType>, String> FROM_JAVA_TYPE = Map.ofEntries(
      entry(EfxDataType.String.class, TEXT),
      entry(EfxDataType.Boolean.class, INDICATOR),
      entry(EfxDataType.Number.class, NUMERIC),
      entry(EfxDataType.Duration.class, DURATION),
      entry(EfxDataType.Date.class, DATE),
      entry(EfxDataType.Time.class, TIME));

  /**
   * The inverse of {@link #FROM_JAVA_TYPE}, whose types are all primitives.
   */
  private static final Map<String, Class<? extends EfxDataType.Primitive>> TO_JAVA_TYPE =
      FROM_JAVA_TYPE.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getValue,
          javaType -> javaType.getKey().asSubclass(EfxDataType.Primitive.class)));

  /**
   * Resolves an eForms SDK field type name to the corresponding EFX type name.
   *
   * @throws TranslatorConfigurationException if the type is not mapped
   */
  public static String fromEformsType(String eformsType) {
    String result = FROM_EFORMS_TYPE.get(eformsType);
    if (result == null) {
      throw TranslatorConfigurationException.missingTypeMapping(eformsType, "EfxTypeTokenLookup.FROM_EFORMS_TYPE");
    }
    return result;
  }

  /**
   * Resolves a Java EfxDataType class to the corresponding EFX type name.
   *
   * @throws TranslatorConfigurationException if the type is not mapped
   */
  public static String fromJavaType(Class<? extends EfxDataType> javaType) {
    String result = FROM_JAVA_TYPE.get(javaType);
    if (result == null) {
      throw TranslatorConfigurationException.missingTypeMapping(javaType, "EfxTypeTokenLookup.FROM_JAVA_TYPE");
    }
    return result;
  }

  /**
   * Names the type of values of an EFX data type as a type declaration writes it, with a star for
   * a sequence: {@code text}, {@code number*}. A node has no type keyword, since no declaration
   * holds one, and is named {@code node}.
   *
   * @param dataType an EFX data type, scalar or sequence
   * @return its name in error messages
   * @throws TranslatorConfigurationException if its primitive is not mapped
   */
  public static String nameOf(Class<? extends EfxDataType> dataType) {
    final Class<? extends EfxDataType.Primitive> primitive = EfxTypeLattice.toPrimitive(dataType);
    final String name = EfxDataType.Node.class.equals(primitive) ? NODE
        : FROM_JAVA_TYPE.entrySet().stream()
            .filter(entry -> entry.getKey().isAssignableFrom(primitive))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> TranslatorConfigurationException.missingTypeMapping(primitive,
                "EfxTypeTokenLookup.FROM_JAVA_TYPE"));
    return EfxTypeLattice.isSequence(dataType) ? name + SEQUENCE_MARK : name;
  }

  /**
   * Resolves an EFX type name, as a type declaration writes it, to the corresponding primitive
   * EfxDataType class.
   *
   * @throws TranslatorConfigurationException if the type name is not mapped
   */
  public static Class<? extends EfxDataType.Primitive> toJavaType(String typeName) {
    Class<? extends EfxDataType.Primitive> result = TO_JAVA_TYPE.get(typeName);
    if (result == null) {
      throw TranslatorConfigurationException.missingTypeMapping(typeName, "EfxTypeTokenLookup.TO_JAVA_TYPE");
    }
    return result;
  }

  private static String getLexerSymbol(int tokenType) {
    return EfxLexer.VOCABULARY.getLiteralName(tokenType).replaceAll("^'|'$", "");
  }

  private EfxTypeTokenLookup() {}
}
