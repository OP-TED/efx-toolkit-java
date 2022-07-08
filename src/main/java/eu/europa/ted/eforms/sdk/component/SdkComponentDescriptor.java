package eu.europa.ted.eforms.sdk.component;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Objects;

public class SdkComponentDescriptor<T> implements Serializable {
  private static final long serialVersionUID = -6237218459963821365L;

  private String sdkVersion;

  private SdkComponentTypeEnum componentType;

  private Class<T> implType;

  public SdkComponentDescriptor(String sdkVersion, SdkComponentTypeEnum componentType,
      Class<T> implType) {
    this.sdkVersion = sdkVersion;
    this.componentType = componentType;
    this.implType = implType;
  }

  public T createInstance() throws InstantiationException {
    try {
      return implType.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
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
