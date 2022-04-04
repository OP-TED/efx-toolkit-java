package eu.europa.ted.efx;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import eu.europa.ted.efx.interfaces.NoticeRenderer;

public class ContentBlock {
    final ContentBlock parent;
    final String id;
    final Integer indentationLevel;
    final String content;
    final Context context;
    final Queue<ContentBlock> children = new LinkedList<>();

    private ContentBlock() {
        this.parent = null;
        this.id = "block";
        this.indentationLevel = -1;
        this.content = "";
        this.context = null;
    }

    public ContentBlock(final ContentBlock parent, final String id, final String content,
            Context contextPath) {
        this.parent = parent;
        this.id = id;
        this.indentationLevel = parent.indentationLevel + 1;
        this.content = content;
        this.context = contextPath;
    }

    public static ContentBlock newRootBlock() {
        return new ContentBlock();
    }

    public ContentBlock addChild(final String content, final Context context) {
        String newBlockId = String.format("%s%02d", this.id, this.children.size() + 1);
        ContentBlock newBlock = new ContentBlock(this, newBlockId, content, context);
        this.children.add(newBlock);
        return newBlock;
    }

    public ContentBlock addSibling(final String content, final Context context) {
        if (this.parent == null) {
            throw new IllegalStateException("Cannot add sibling to root block");
        }
        return this.parent.addChild(content, context);
    }

    public ContentBlock findParentByLevel(final int parentIndentationLevel) {

        assert this.indentationLevel >= parentIndentationLevel : "Unexpected indentation tracker state.";
        
        ContentBlock targetBlock = this;
        while (targetBlock.indentationLevel > parentIndentationLevel) {
            targetBlock = targetBlock.parent;
        }
        return targetBlock;
    }

    public String renderContent(NoticeRenderer renderer) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.content);
        for (ContentBlock child : this.children) {
            sb.append("\n" + child.renderCallTemplate(renderer));
        }
        return sb.toString();
    }

    public void renderTemplate(NoticeRenderer renderer, List<String> templates) {
        templates.add(renderer.renderTemplate(this.id, this.renderContent(renderer)));
        for (ContentBlock child : this.children) {
            child.renderTemplate(renderer, templates);
        }
    }

    public String renderCallTemplate(NoticeRenderer renderer) {
        return renderer.renderCallTemplate(this.id, this.context.relativePath());
    }
}
