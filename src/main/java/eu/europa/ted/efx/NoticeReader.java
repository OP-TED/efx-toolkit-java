package eu.europa.ted.efx;

public interface NoticeReader {

    public void load(final String pathname);

    public String getRequiredSdkVersion();

    public String valueOf(final String xpath);
    public String valueOf(final String xpath, final String contextPath);
}
