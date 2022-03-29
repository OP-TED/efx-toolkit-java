package eu.europa.ted.efx.interfaces;
public interface NoticeRenderer {
    void beginFile();
    void endFile();

    void beginBlock(int level, String content);
    void endBlock();
}
