package eu.europa.ted.efx.model;

import java.util.Stack;

public class ContentBlockStack extends Stack<ContentBlock>{
    
    /**
     * Adds a new child block to the top of the stack.
     * When the child is later removed, its parent will return to the top of the stack again.
     */
    public void pushChild(final Markup content, final Context context) {
        this.push(this.peek().addChild(content, context));
    }

    /**
     * Removes the block at the top of the stack and replaces it by a new sibling block.
     * When the last sibling is later removed, their parent block will return to the top of the stack again.
     */
    public void pushSibling(final Markup content, Context context) {
        this.push(this.pop().addSibling(content, context));
    }

    /**
     * Finds the block in the stack that has the given indentation level.
     * Works from the top of the stack to the bottom.
     */
    public ContentBlock blockAtLevel(final int indentationLevel) {
        if (this.isEmpty()) {
            return null;
        }
        return this.peek().findParentByLevel(indentationLevel);
    }

    /**
     * Returns the indentation level of the block at the top of the stack or zero if the stack is empty.
     * Works from the bottom of the stack to the top.
     */
    public int currentIndentationLevel() {
        if (this.isEmpty()) {
            return 0;
        }
        return this.peek().getIndentationLevel();
    }
}
