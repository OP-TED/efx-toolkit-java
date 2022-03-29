package eu.europa.ted.efx;
public interface NoticeRenderer {
    void beginFile();
    void endFile();

    void beginBlock(int level, String content);
    void endBlock();
}
