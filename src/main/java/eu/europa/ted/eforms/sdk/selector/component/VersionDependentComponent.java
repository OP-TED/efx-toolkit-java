package eu.europa.ted.eforms.sdk.selector.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.atteo.classindex.IndexAnnotated;

@IndexAnnotated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VersionDependentComponent {
  public static final String ANY_VERSION = "any";

  public String[] versions() default {ANY_VERSION};

  public VersionDependentComponentType componentType();
}
