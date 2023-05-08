package eu.europa.ted.efx.model;

/**
 * Represents markup in the target template language.
 */
public class Markup extends CallStackObject {

  /**
   * Stores the markup script in the target language.
   */
  public final String script;

  public Markup(final String script) {
    this.script = script == null ? "" : script;
  }

  /**
   * Helps combine two subsequent markup elements into one.
   * 
   * @param next the next markup element.
   * @return the combined markup element.
   */
  public Markup join(final Markup next) {
    return new Markup(this.script + next.script);
  }

  public static Markup empty() {
    return new Markup("");
  }
}
