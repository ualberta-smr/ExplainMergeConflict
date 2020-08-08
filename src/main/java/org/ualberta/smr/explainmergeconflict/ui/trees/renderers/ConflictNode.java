package org.ualberta.smr.explainmergeconflict.ui.trees.renderers;

/**
 * Represents node metadata for the conflicts tree. While implementation is similiar to ConflictNode in RefactorInsight, this
 * class is specific to trees in Explain Merge Conflict.
 */
public class ConflictNode {
    private final ConflictNodeType type;
    private final String content;

    public ConflictNode(ConflictNodeType type, String content) {
        this.type = type;
        this.content = content;
    }

    public ConflictNodeType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    /*
     * This is a hack fix to display proper name for conflict tree nodes. Otherwise, the Node instance's address will
     * be displayed.
     *
     * TODO - display proper node name without modifying toString()
     */
    @Override
    public String toString() {
        return getContent();
    }
}
