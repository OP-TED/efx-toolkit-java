package eu.europa.ted.eforms.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.atteo.classindex.IndexAnnotated;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;

@IndexAnnotated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SdkComponent {
  public static final String ANY = "any";

  public String[] versions() default {ANY};

  public SdkComponentTypeEnum componentType();
}
