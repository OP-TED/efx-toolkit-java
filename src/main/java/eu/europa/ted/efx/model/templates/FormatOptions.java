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
package eu.europa.ted.efx.model.templates;

import java.util.Optional;
import java.util.OptionalInt;

import eu.europa.ted.efx.model.ParsedEntity;

/**
 * The format options of an expression block, as described by the formatOptions rule of the grammar. A
 * number of decimals gives options for numbers, a style gives options for dates and times, and an
 * expression block that gives neither uses the default options of the type of its value. No-unit,
 * after a number of decimals or alone, leaves the unit out; a style is never followed by it, since a
 * date or a time has no unit. No-formatting leaves the value as it is, without its unit.
 */
public abstract class FormatOptions implements ParsedEntity {

  /**
   * The style of a date or a time, as the formatStyle rule of the grammar gives it.
   */
  public enum FormatStyle {
    SHORT("short"), MEDIUM("medium"), LONG("long");

    private final String name;

    FormatStyle(final String name) {
      this.name = name;
    }

    /**
     * Returns the keyword that a template writes for this style.
     */
    public String getName() {
      return this.name;
    }
  }

  private final boolean noUnit;

  protected FormatOptions(final boolean noUnit) {
    this.noUnit = noUnit;
  }

  public boolean hideUnit() {
    return this.noUnit;
  }

  /**
   * Returns whether these options format the value. Only no-formatting does not: the value is then
   * displayed as it is, without its unit.
   */
  public boolean formatsValue() {
    return true;
  }

  /**
   * Returns whether these options give a number of decimals or a style. A value that has nothing to
   * format, such as a text, can only be displayed with options that give neither.
   */
  public abstract boolean hasSpecifier();

  /**
   * Returns the options to format a number with, or nothing if these options are for dates and times.
   */
  public abstract Optional<NumberFormatOptions> forNumbers();

  /**
   * Returns the options to format a date or a time with, or nothing if these options are for numbers.
   */
  public abstract Optional<DateTimeFormatOptions> forDatesAndTimes();

  /**
   * Options for numbers: a number of decimals, from 0 to 9, or up to 9 decimals by default.
   */
  public static class NumberFormatOptions extends FormatOptions {

    private final OptionalInt decimals;

    public NumberFormatOptions(final int decimals, final boolean noUnit) {
      super(noUnit);
      this.decimals = OptionalInt.of(decimals);
    }

    private NumberFormatOptions(final boolean noUnit) {
      super(noUnit);
      this.decimals = OptionalInt.empty();
    }

    /**
     * Returns the number of decimals given, or nothing if none is given and up to 9 are displayed.
     */
    public OptionalInt getDecimals() {
      return this.decimals;
    }

    /**
     * Returns the format-number pattern for the number of decimals, or for up to 9 decimals by
     * default.
     */
    public String getNumberPattern() {
      if (this.decimals.isEmpty()) {
        return "#,##0.#########";
      }
      int count = this.decimals.getAsInt();
      return count == 0 ? "#,##0" : "#,##0." + "0".repeat(count);
    }

    @Override
    public boolean hasSpecifier() {
      return this.decimals.isPresent();
    }

    @Override
    public Optional<NumberFormatOptions> forNumbers() {
      return Optional.of(this);
    }

    @Override
    public Optional<DateTimeFormatOptions> forDatesAndTimes() {
      return Optional.empty();
    }
  }

  /**
   * Options for dates and times: a style. A date or a time has no unit to leave out.
   */
  public static class DateTimeFormatOptions extends FormatOptions {

    private final FormatStyle style;

    public DateTimeFormatOptions(final FormatStyle style) {
      super(false);
      this.style = style;
    }

    public FormatStyle getStyle() {
      return this.style;
    }

    @Override
    public boolean hasSpecifier() {
      return true;
    }

    @Override
    public Optional<NumberFormatOptions> forNumbers() {
      return Optional.empty();
    }

    @Override
    public Optional<DateTimeFormatOptions> forDatesAndTimes() {
      return Optional.of(this);
    }
  }

  /**
   * Options that give neither a number of decimals nor a style: a number is displayed with up to 9
   * decimals, and a date or a time in the short style.
   */
  public static class DefaultFormatOptions extends FormatOptions {

    public DefaultFormatOptions(final boolean noUnit) {
      super(noUnit);
    }

    @Override
    public boolean hasSpecifier() {
      return false;
    }

    @Override
    public Optional<NumberFormatOptions> forNumbers() {
      return Optional.of(new NumberFormatOptions(this.hideUnit()));
    }

    @Override
    public Optional<DateTimeFormatOptions> forDatesAndTimes() {
      return Optional.of(new DateTimeFormatOptions(FormatStyle.SHORT));
    }
  }

  /**
   * Options that leave the value as it is, without its unit: no-formatting.
   */
  public static class NoFormattingOptions extends FormatOptions {

    public NoFormattingOptions() {
      super(true);
    }

    @Override
    public boolean formatsValue() {
      return false;
    }

    @Override
    public boolean hasSpecifier() {
      return false;
    }

    @Override
    public Optional<NumberFormatOptions> forNumbers() {
      return Optional.empty();
    }

    @Override
    public Optional<DateTimeFormatOptions> forDatesAndTimes() {
      return Optional.empty();
    }
  }
}
