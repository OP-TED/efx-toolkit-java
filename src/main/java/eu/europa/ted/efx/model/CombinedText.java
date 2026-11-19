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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.misc.Interval;

/**
 * The combined text of a file, as the lexer reads it: the file's text with each {@code #include}
 * directive replaced by the combined text of the file it names. It keeps the {@link LineMap} of the
 * file, so that an error in it can give the file and the line it was written on.
 */
public final class CombinedText implements CharStream {

  private final CharStream text;
  private final LineMap lineMap;

  /**
   * @param text the combined text
   * @param lineMap the line map of the file whose combined text it is
   */
  public CombinedText(final String text, final LineMap lineMap) {
    this.text = CharStreams.fromString(text);
    this.lineMap = lineMap;
  }

  /**
   * @return the line map of the file whose combined text this is
   */
  public LineMap getLineMap() {
    return this.lineMap;
  }

  @Override
  public String getText(final Interval interval) {
    return this.text.getText(interval);
  }

  @Override
  public void consume() {
    this.text.consume();
  }

  @Override
  public int LA(final int i) {
    return this.text.LA(i);
  }

  @Override
  public int mark() {
    return this.text.mark();
  }

  @Override
  public void release(final int marker) {
    this.text.release(marker);
  }

  @Override
  public int index() {
    return this.text.index();
  }

  @Override
  public void seek(final int index) {
    this.text.seek(index);
  }

  @Override
  public int size() {
    return this.text.size();
  }

  @Override
  public String getSourceName() {
    return this.text.getSourceName();
  }

  /**
   * @return the combined text
   */
  @Override
  public String toString() {
    return this.text.toString();
  }
}
