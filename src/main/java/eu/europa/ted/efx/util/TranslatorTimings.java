package eu.europa.ted.efx.util;

/**
 * Holds timing measurements for EFX template processing phases.
 * This data is used to generate comprehensive performance reports.
 */
public class TranslatorTimings {
    
    private final long preprocessingTimeMs;
    private final long translationTimeMs;
    private final long totalTimeMs;
    
    /**
     * Creates a new TimingData instance with the specified timing measurements.
     * 
     * @param preprocessingTimeMs Time spent in EFX preprocessing phase (milliseconds)
     * @param translationTimeMs Time spent in EFX translation phase (milliseconds)
     * @param totalTimeMs Total EFX processing time (milliseconds)
     */
    public TranslatorTimings(long preprocessingTimeMs, long translationTimeMs, long totalTimeMs) {
        this.preprocessingTimeMs = preprocessingTimeMs;
        this.translationTimeMs = translationTimeMs;
        this.totalTimeMs = totalTimeMs;
    }
    
    /**
     * @return Time spent in EFX preprocessing phase (milliseconds)
     */
    public long getPreprocessingTimeMs() {
        return preprocessingTimeMs;
    }
    
    /**
     * @return Time spent in EFX translation phase (milliseconds)
     */
    public long getTranslationTimeMs() {
        return translationTimeMs;
    }
    
    /**
     * @return Total EFX processing time (milliseconds)
     */
    public long getTotalTimeMs() {
        return totalTimeMs;
    }
    
    /**
     * @return Percentage of total time spent in preprocessing
     */
    public double getPreprocessingPercentage() {
        return totalTimeMs > 0 ? (double) preprocessingTimeMs / totalTimeMs * 100.0 : 0.0;
    }
    
    /**
     * @return Percentage of total time spent in translation
     */
    public double getTranslationPercentage() {
        return totalTimeMs > 0 ? (double) translationTimeMs / totalTimeMs * 100.0 : 0.0;
    }
}