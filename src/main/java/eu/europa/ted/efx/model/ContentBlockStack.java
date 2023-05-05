package eu.europa.ted.efx.model;

import java.util.Stack;

public class ContentBlockStack extends Stack<ContentBlock> {

  /**
   * Adds a new child block to the top of the stack. When the child is later removed, its parent
   * will return to the top of the stack again.
   * 
   * @param number the outline number of the child block.
   * @param content the content of the child block.
   * @param context the context of the child block.
   * @param variables the variables of the child block.
   */
  public void pushChild(final int number, final Markup content, final Context context,
      final VariableList variables) {
    this.push(this.peek().addChild(number, content, context, variables));
  }

  /**
   * Removes the block at the top of the stack and replaces it by a new sibling block. When the last
   * sibling is later removed, their parent block will return to the top of the stack again.
   * 
   * @param number the outline number of the sibling block.
   * @param content the content of the sibling block.
   * @param context the context of the sibling block.
   * @param variables the variables of the sibling block.
   */
  public void pushSibling(final int number, final Markup content, Context context,
      final VariableList variables) {
    this.push(this.pop().addSibling(number, content, context, variables));
  }

  /**
   * Finds the block in the stack that has the given indentation level. Works from the top of the
   * stack to the bottom.
   * 
   * @param indentationLevel the indentation level to look for.
   * @return the block with the given indentation level or null if no such block exists.
   */
  public ContentBlock blockAtLevel(final int indentationLevel) {
    if (this.isEmpty()) {
      return null;
    }
    return this.peek().findParentByLevel(indentationLevel);
  }

  /**
   * Returns the indentation level of the block at the top of the stack or zero if the stack is
   * empty. Works from the bottom of the stack to the top.
   * 
   * @return the indentation level of the block at the top of the stack or zero if the stack is
   */
  public int currentIndentationLevel() {
    if (this.isEmpty()) {
      return 0;
    }
    return this.peek().getIndentationLevel();
  }

  public Context currentContext() {
    if (this.isEmpty()) {
      return null;
    }
    return this.peek().getContext();
  }

  public Context parentContext() {
    if (this.isEmpty()) {
      return null;
    }
    return this.peek().getParentContext();
  }
}
