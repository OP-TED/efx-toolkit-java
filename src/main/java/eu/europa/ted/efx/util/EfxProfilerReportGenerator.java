package eu.europa.ted.efx.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.antlr.v4.runtime.atn.DecisionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.efx.sdk2.EfxParser;

/**
 * Utility class for generating EFX parser profiling reports.
 * This class provides methods to generate HTML reports from ANTLR profiling
 * data.
 */
public class EfxProfilerReportGenerator {

  private static final Logger logger = LoggerFactory.getLogger(EfxProfilerReportGenerator.class);

  private EfxProfilerReportGenerator() {
    // Utility class - prevent instantiation
  }

  /**
   * Generates and saves an HTML profiling report to the specified file path.
   * 
   * @param parser     The EFX parser that was profiled
   * @param decisions  Array of decision information from profiling
   * @param totalTime  Total parsing time in nanoseconds
   * @param timingData Timing measurements for different processing phases
   * @param outputPath Path where the HTML report should be saved
   */
  public static void generateAndSaveProfilerReport(final EfxParser parser, final DecisionInfo[] decisions,
      final long totalTime, final TranslatorTimings timingData, final Path outputPath) {
    if (outputPath == null) {
      logger.debug("No output path provided for profiling report, skipping file generation");
      return;
    }

    try {
      Files.createDirectories(outputPath.getParent());
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
        writer.print(generateProfilerReport(parser, decisions, totalTime, timingData));
      }
      logger.info("EFX profiling results saved to: {}", outputPath);
    } catch (IOException e) {
      logger.error("Failed to write EFX profiling results to file: {}", outputPath, e);
    }
  }

  /**
   * Generates an HTML profiling report as a string.
   * 
   * @param parser     The EFX parser that was profiled
   * @param decisions  Array of decision information from profiling
   * @param totalTime  Total parsing time in nanoseconds
   * @param timingData Timing measurements for different processing phases
   * @return HTML report as a string
   */
  public static String generateProfilerReport(final EfxParser parser, final DecisionInfo[] decisions,
      final long totalTime, final TranslatorTimings timingData) {
    StringBuilder html = new StringBuilder();

    // HTML structure with CSS similar to XSLT profiling
    html.append("<!DOCTYPE HTML>\n");
    html.append("<html>\n");
    html.append("<head>\n");
    html.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
    html.append("  <title>EFX Parser Profiling Results</title>\n");
    html.append("  <style>\n");
    html.append("    body { background: #e4eef0; font-family: Verdana, Arial, Helvetica, sans-serif; margin: 20px; }\n");
    html.append("    h1 { font-size: 14pt; color: #3D5B96; font-weight: bold; margin-bottom: 10px; }\n");
    html.append("    h2 { font-size: 12pt; color: #96433D; font-weight: bold; margin-top: 20px; margin-bottom: 10px; }\n");
    html.append("    p { font-size: 9pt; color: #3D5B96; line-height: 1.3em; margin: 5px 0; }\n");
    html.append("    table { border-collapse: collapse; border: 1px solid black; margin: 10px 0; width: 100%; }\n");
    html.append("    th, td { border: 1px solid black; font-size: 9pt; color: #3D5B96; padding: 5px; text-align: left; }\n");
    html.append("    th { background-color: #d0d8e0; font-weight: bold; }\n");
    html.append("    .number { text-align: right; }\n");
    html.append("    .high-time { background-color: #ffeeee; }\n");
    html.append("    .medium-time { background-color: #fff8ee; }\n");
    html.append("    .summary { background-color: #f0f8ff; padding: 10px; border: 1px solid #3D5B96; margin: 10px 0; }\n");
    html.append("  </style>\n");
    html.append("</head>\n");
    html.append("<body>\n");

    // Header
    html.append("  <h1>EFX Parser Profiling Results</h1>\n");

    // Summary section
    html.append("  <div class=\"summary\">\n");
    html.append("    <h2>Summary</h2>\n");
    html.append("    <p><strong>Total Grammar Rules Analyzed:</strong> ").append(decisions.length).append("</p>\n");
    html.append("    <p><strong>Total Parser Time:</strong> ").append(String.format("%.2f ms (%,d ns)", totalTime / 1_000_000.0, totalTime)).append("</p>\n");
    html.append("    <p><strong>Analysis Date:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
    html.append("  </div>\n");

    // Timing breakdown section
    if (timingData != null) {
      html.append("  <div class=\"summary\">\n");
      html.append("    <h2>Processing Time Breakdown</h2>\n");
      html.append("    <p><strong>EFX Preprocessing:</strong> ").append(timingData.getPreprocessingTimeMs()).append(" ms (").append(String.format("%.1f%%", timingData.getPreprocessingPercentage())).append(")</p>\n");
      html.append("    <p><strong>EFX Translation:</strong> ").append(timingData.getTranslationTimeMs()).append(" ms (").append(String.format("%.1f%%", timingData.getTranslationPercentage())).append(")</p>\n");
      html.append("    <p><strong>Total Processing Time:</strong> ").append(timingData.getTotalTimeMs()).append(" ms</p>\n");
      html.append("  </div>\n");
    }

    // Top performing grammar rules table
    html.append("  <h2>Top 15 Most Time-Consuming Grammar Rules</h2>\n");
    html.append("  <table>\n");
    html.append("    <thead>\n");
    html.append("      <tr>\n");
    html.append("        <th>Rank</th>\n");
    html.append("        <th>Grammar Rule</th>\n");
    html.append("        <th>Time (ms)</th>\n");
    html.append("        <th>Time (ns)</th>\n");
    html.append("        <th>% of Total</th>\n");
    html.append("        <th>Invocations</th>\n");
    html.append("        <th>Ambiguities</th>\n");
    html.append("        <th>Errors</th>\n");
    html.append("        <th>Avg Time/Invocation (μs)</th>\n");
    html.append("      </tr>\n");
    html.append("    </thead>\n");
    html.append("    <tbody>\n");

    int displayCount = Math.min(15, decisions.length);
    for (int i = 0; i < displayCount; i++) {
      DecisionInfo decision = decisions[i];
      if (decision.timeInPrediction <= 0)
        continue;

      String ruleName = (parser != null && parser.getRuleNames().length > decision.decision)
          ? parser.getRuleNames()[decision.decision]
          : "Unknown Rule #" + decision.decision;

      double timeMs = decision.timeInPrediction / 1_000_000.0;
      double percentage = (double) decision.timeInPrediction / totalTime * 100;
      double avgTimePerInvocation = decision.invocations > 0
          ? decision.timeInPrediction / (1000.0 * decision.invocations)
          : 0;

      // Apply CSS class based on time percentage
      String rowClass = "";
      if (percentage > 10) {
        rowClass = "high-time";
      } else if (percentage > 5) {
        rowClass = "medium-time";
      }

      html.append("      <tr").append(rowClass.isEmpty() ? "" : " class=\"" + rowClass + "\"").append(">\n");
      html.append("        <td class=\"number\">").append(i + 1).append("</td>\n");
      html.append("        <td>").append(escapeHtml(ruleName)).append("</td>\n");
      html.append("        <td class=\"number\">").append(String.format("%.2f", timeMs)).append("</td>\n");
      html.append("        <td class=\"number\">").append(String.format("%,d", decision.timeInPrediction)).append("</td>\n");
      html.append("        <td class=\"number\">").append(String.format("%.1f%%", percentage)).append("</td>\n");
      html.append("        <td class=\"number\">").append(String.format("%,d", decision.invocations)).append("</td>\n");
      html.append("        <td class=\"number\">").append(decision.ambiguities.size()).append("</td>\n");
      html.append("        <td class=\"number\">").append(decision.errors.size()).append("</td>\n");
      html.append("        <td class=\"number\">").append(String.format("%.2f", avgTimePerInvocation)).append("</td>\n");
      html.append("      </tr>\n");
    }

    html.append("    </tbody>\n");
    html.append("  </table>\n");

    // Footer
    html.append("  <p style=\"margin-top: 20px; font-size: 8pt; color: #666;\">Generated by EFX Toolkit Profiler</p>\n");
    html.append("</body>\n");
    html.append("</html>\n");

    return html.toString();
  }

  /**
   * Escapes HTML special characters in the given text.
   * 
   * @param text The text to escape
   * @return HTML-escaped text
   */
  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}