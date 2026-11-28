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

import java.util.ArrayDeque;
import java.util.Deque;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.TranslatorContext;

/**
 * A content template displayed in place, made of fragments, as described by the displayTemplate rule
 * of the grammar.
 */
public class DisplayContentTemplate extends ContentTemplate {

  // The fragments in display order. The grammar nests the fragments of a template from the last to
  // the first, so the translator builds the template from its last fragment, putting each one in
  // front of those already there.
  private final Deque<ContentTemplateFragment> fragments = new ArrayDeque<>();

  // What the template displays before the colon that separates its caption, when it has one. The
  // fragments are then what it displays after that colon.
  private DisplayContentTemplate caption = null;

  public void setCaption(final DisplayContentTemplate caption) {
    this.caption = caption;
  }

  /**
   * Puts a fragment in front of those already in the template.
   */
  public void prepend(final ContentTemplateFragment fragment) {
    this.fragments.addFirst(fragment);
  }

  /**
   * Puts a text in front of those already in the template. When the template starts with a text,
   * the two are joined into one text, because the EFX-2 grammar reads a text one text block at a
   * time: a word, a run of whitespace, an escape sequence or a character reference.
   */
  public void prependText(final TextContentTemplateFragment text) {
    final ContentTemplateFragment first = this.fragments.peekFirst();
    if (first instanceof TextContentTemplateFragment) {
      this.fragments.removeFirst();
      this.fragments.addFirst(text.followedBy((TextContentTemplateFragment) first));
    } else {
      this.fragments.addFirst(text);
    }
  }

  /**
   * Puts a text after the fragments already in the template. When the template ends with a text,
   * the two are joined into one text, as {@link #prependText} joins them at the front.
   */
  public void appendText(final TextContentTemplateFragment text) {
    final ContentTemplateFragment last = this.fragments.peekLast();
    if (last instanceof TextContentTemplateFragment) {
      this.fragments.removeLast();
      this.fragments.addLast(((TextContentTemplateFragment) last).followedBy(text));
    } else {
      this.fragments.addLast(text);
    }
  }

  /**
   * Puts the fragments of another template after those already in this one. Two texts that meet
   * are joined into one text.
   */
  public void append(final DisplayContentTemplate next) {
    for (final ContentTemplateFragment fragment : next.fragments) {
      if (fragment instanceof TextContentTemplateFragment) {
        this.appendText((TextContentTemplateFragment) fragment);
      } else {
        this.fragments.addLast(fragment);
      }
    }
  }

  /**
   * @return whether the template displays a hyperlink block
   */
  public boolean displaysHyperlink() {
    return this.fragments.stream().anyMatch(HyperlinkContentTemplateFragment.class::isInstance)
        || (this.caption != null && this.caption.displaysHyperlink());
  }

  @Override
  public Markup render(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    StringBuilder markup = new StringBuilder();
    for (ContentTemplateFragment fragment : this.fragments) {
      markup.append(fragment.render(markupGenerator, translatorContext).script);
    }
    return new Markup(markup.toString());
  }

  @Override
  public Markup renderCaption(MarkupGenerator markupGenerator, TranslatorContext translatorContext) {
    return this.caption == null ? Markup.empty() : this.caption.render(markupGenerator, translatorContext);
  }
}
