package eu.europa.ted.eforms.sdk.selector.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdkComponentDescriptor<T> implements Serializable {
  private static final long serialVersionUID = -6237218459963821365L;

  private static final Logger log = LoggerFactory.getLogger(SdkComponentDescriptor.class);

  private String sdkVersion;

  private SdkComponentType componentType;

  private Class<T> implType;

  public SdkComponentDescriptor(String sdkVersion, SdkComponentType componentType,
      Class<T> implType) {
    this.sdkVersion = sdkVersion;
    this.componentType = componentType;
    this.implType = implType;
  }

  @SuppressWarnings("unchecked")
  public T createInstance(Object... initArgs) throws InstantiationException {
    try {
      Class<?>[] paramTypes = Arrays.asList(Optional.ofNullable(initArgs).orElse(new Object[0]))
          .stream().map(Object::getClass).collect(Collectors.toList()).toArray(new Class[0]);

      log.trace("Creating an instance of [{}] using constructor with parameter types: {}", implType,
          paramTypes);

      return (T) Arrays.asList(implType.getDeclaredConstructors()).stream()
          .filter((Constructor<?> c) -> {
            Class<?>[] declaredParamTypes = c.getParameterTypes();

            if (declaredParamTypes.length != paramTypes.length) {
              return false;
            }

            for (int i = 0; i < declaredParamTypes.length; i++) {
              if (!declaredParamTypes[i].isAssignableFrom(paramTypes[i])) {
                return false;
              }
            }

            return true;
          }).collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() != 1) {
              throw new IllegalStateException("Ambiguous constructor parameter types");
            }

            return list.get(0);
          })).newInstance(initArgs);
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      throw new InstantiationException(MessageFormat.format(
          "Failed to instantiate [{0}] as SDK component type [{1}] for SDK [{2}]. Error was: {3}",
          implType, componentType, sdkVersion, e.getMessage()));
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentType, sdkVersion);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SdkComponentDescriptor<?> other = (SdkComponentDescriptor<?>) obj;
    return componentType == other.componentType && Objects.equals(sdkVersion, other.sdkVersion);
  }

  public Class<T> getImplType() {
    return implType;
  }
}
