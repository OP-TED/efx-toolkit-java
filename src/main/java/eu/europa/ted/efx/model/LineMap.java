/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps each line of the combined text of a file back to the file and the line it was written on.
 *
 * <p>The combined text of a file is its text with each {@code #include} directive replaced by the
 * combined text of the file it names. The line map of a file holds an {@link Insert} for each of
 * its directives, and each insert holds the line map of the file it inserted. An inserted text
 * starts on a line of its own and ends with a line end, so each line keeps its columns, but the
 * rest of the line the directive ends on: in the combined text it starts the line after the
 * inserted text. The line map of a text that includes no file maps each line to itself.</p>
 */
public final class LineMap {

  /** The inserts, in the order of their lines. */
  private final List<Insert> inserts = new ArrayList<>();

  /**
   * Adds an insert after those added before it.
   *
   * @param line the line of the combined text where the inserted text starts, counted from 1
   * @param file the file inserted, as its {@code #include} directive names it
   * @param lineCount how many lines of the combined text the inserted text takes
   * @param lineMap the line map of the file inserted
   * @param endLine the line of the file this line map is of where the directive ends, counted
   *        from 1
   * @param endColumn the column where the directive ends on that line, counted from 0
   */
  public void addInsert(final int line, final String file, final int lineCount,
      final LineMap lineMap, final int endLine, final int endColumn) {
    this.inserts.add(new Insert(line, file, lineCount, lineMap, endLine, endColumn));
  }

  /**
   * Writes a position in the combined text as the position where it was written, as an error
   * message starts with it.
   *
   * @param line the line of the combined text, counted from 1
   * @param column the column, counted from 0
   * @return the position as "line X:Y", followed by " in 'file'" when it was written in an included
   *         file
   */
  public String positionOf(final int line, final int column) {
    return this.positionOf(line, column, null);
  }

  /**
   * @param line the line of the combined text, counted from 1
   * @param column the column, counted from 0
   * @param file the file this line map is of, as its {@code #include} directive names it; null for
   *        the file the translation reads
   * @return the position as {@link #positionOf(int, int)} writes it
   */
  private String positionOf(final int line, final int column, final String file) {
    Insert last = null;
    for (final Insert insert : this.inserts) {
      if (line < insert.line) {
        break;
      }
      last = insert;
    }
    if (last == null) {
      return write(line, column, file);
    }
    if (line < last.line + last.lineCount) {
      return last.lineMap.positionOf(line - last.line + 1, column, last.file);
    }
    // 0 on the rest of the line the directive ends on, which starts at its end
    final int linesAfter = line - (last.line + last.lineCount);
    return write(last.endLine + linesAfter, linesAfter == 0 ? last.endColumn + column : column,
        file);
  }

  /**
   * @param line the line of the file, counted from 1
   * @param column the column, counted from 0
   * @param file the file, as its {@code #include} directive names it; null for the file the
   *        translation reads
   * @return the position as {@link #positionOf(int, int)} writes it, in ASCII digits whatever the
   *         default locale
   */
  private static String write(final int line, final int column, final String file) {
    final String position = String.format(Locale.ROOT, "line %d:%d", line, column);
    return file == null ? position : String.format(Locale.ROOT, "%s in '%s'", position, file);
  }

  /**
   * The combined text of an included file, in the place of the {@code #include} directive that
   * names it: where it is in the combined text, and where the directive ends in the file.
   */
  private static final class Insert {

    private final int line;
    private final String file;
    private final int lineCount;
    private final LineMap lineMap;
    private final int endLine;
    private final int endColumn;

    private Insert(final int line, final String file, final int lineCount,
        final LineMap lineMap, final int endLine, final int endColumn) {
      this.line = line;
      this.file = file;
      this.lineCount = lineCount;
      this.lineMap = lineMap;
      this.endLine = endLine;
      this.endColumn = endColumn;
    }
  }
}
