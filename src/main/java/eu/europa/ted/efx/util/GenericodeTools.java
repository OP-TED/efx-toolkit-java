package eu.europa.ted.efx.util;

import java.nio.charset.StandardCharsets;
import com.helger.genericode.Genericode10CodeListMarshaller;
import com.helger.genericode.v10.Column;
import com.helger.genericode.v10.Value;

/**
 * @author rouschr
 */
public class GenericodeTools {

  public static final String KEY_CODE = "code";
  public static final String EXTENSION_DOT_GC = ".gc";

  public static final Genericode10CodeListMarshaller getMarshaller() {
    // https://stackoverflow.com/questions/7400422/jaxb-creating-context-and-marshallers-cost

    // JAXBContext is thread safe and should only be created once and reused to avoid the cost of
    // initializing the metadata multiple times. Marshaller and Unmarshaller are not thread safe,
    // but are lightweight to create and could be created per operation.

    final Genericode10CodeListMarshaller marshaller = new Genericode10CodeListMarshaller();
    marshaller.setCharset(StandardCharsets.UTF_8);
    marshaller.setFormattedOutput(true);
    marshaller.setIndentString(" "); // Official EU .gc files seem to be using one tab.

    return marshaller;
  }

  public static String extractColRefId(final Value value) {
    return ((Column) value.getColumnRef()).getId();
  }

}
