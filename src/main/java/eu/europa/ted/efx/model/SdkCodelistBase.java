package eu.europa.ted.efx.model;

import java.util.List;
import java.util.Objects;
import eu.europa.ted.efx.interfaces.SdkCodelist;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
public abstract class SdkCodelistBase implements Comparable<SdkCodelistBase>, SdkCodelist {
  private final String codelistId;

  /**
   * Could avoid issues if the versions differ but the identifier is the same.
   */
  private final String codelistVersion;

  private final List<String> codes;

  @SuppressWarnings("unused")
  private SdkCodelistBase() {
    throw new UnsupportedOperationException();
  }

  /**
   * @param codelistId The identifier, not really unique as the version also matters, see .gc
   *        LongName tag. Inside the same SDK we should not have different versions of the same
   *        file.
   * @param codelistVersion The codelist version string, see Version tag in .gc files. This is NOT
   *        the SDK version. It can be useful for debug purposes and to avoid conflicts.
   */
  public SdkCodelistBase(final String codelistId, final String codelistVersion,
      final List<String> codes) {
    this.codelistId = codelistId;
    this.codelistVersion = codelistVersion;
    this.codes = codes;
  }

  @Override
  public String getCodelistId() {
    return codelistId;
  }

  @Override
  public String getVersion() {
    return codelistVersion;
  }

  @Override
  public List<String> getCodes() {
    return codes;
  }

  @Override
  public String toString() {
    return codelistId + "-" + codelistVersion;
  }

  @Override
  public int compareTo(final SdkCodelistBase cl) {
    return Objects.compare(this.getCodelistId() + this.getVersion(),
        cl.getCodelistId() + cl.getVersion(), String::compareTo);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SdkCodelistBase other = (SdkCodelistBase) obj;
    return Objects.equals(codelistId, other.codelistId)
        && Objects.equals(codelistVersion, other.codelistVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(codelistId, codelistVersion);
  }
}
