package eu.europa.ted.efx.xpath;

import java.util.Objects;

/**
 * Representation of an SdkCodelist for usage in the symbols map.
 *
 * @author rouschr
 */
public class SdkCodelist implements Comparable<SdkCodelist> {

  private final String codelistId;

  /**
   * Could avoid issues if the versions differ but the identifier is the same.
   */
  private final String codelistVersion;

  private final String codelistAsEfxList;

  /**
   * @param codelistId The identifier, not really unique as the version also matters, see .gc
   *        LongName tag. Inside the same SDK we should not have different versions of the same
   *        file.
   * @param codelistVersion The codelist version string, see Version tag in .gc files. This is NOT
   *        the SDK version. It can be useful for debug purposes and to avoid conflicts.
   */
  public SdkCodelist(final String codelistId, final String codelistVersion,
      final String codelistAsEfxList) {
    this.codelistId = codelistId;
    this.codelistVersion = codelistVersion;
    this.codelistAsEfxList = codelistAsEfxList;
  }

  /**
   * Helps with hash maps collisions. Should be consistent with equals.
   */
  @Override
  public int compareTo(final SdkCodelist cl) {
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
    final SdkCodelist other = (SdkCodelist) obj;
    // Should we trust the version and not check the codes ?
    return Objects.equals(codelistId, other.codelistId)
        && Objects.equals(codelistVersion, other.codelistVersion);
  }

  public String getCodelistId() {
    return codelistId;
  }

  public String getCodelistAsEfxList() {
    return codelistAsEfxList;
  }

  public String getVersion() {
    return codelistVersion;
  }

  @Override
  public int hashCode() {
    // Should we trust the version and not check the codes ?
    return Objects.hash(codelistId, codelistVersion);
  }

  @Override
  public String toString() {
    return "TedefoCodelist [codelistId=" + codelistId + ", codelistVersion=" + codelistVersion
        + "]";
  }
}
