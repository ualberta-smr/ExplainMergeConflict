package org.ualberta.smr.explainmergeconflict.utils;

import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNode;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNodeType;

import javax.swing.tree.DefaultMutableTreeNode;

public class ConflictsTreeUtils {
    public static DefaultMutableTreeNode createRootAndChildren(ConflictNode conflictNode) {
        // TODO - parse and build children here
        ConflictNode childNode = new ConflictNode(ConflictNodeType.COMMIT, "Commit 1");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(conflictNode);
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode(childNode);
        root.add(child1);
        return root;
    }
}
