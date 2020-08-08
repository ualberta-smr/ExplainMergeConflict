package org.ualberta.smr.explainmergeconflict.ui.trees.renderers;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class ConflictsTreeCellRenderer extends DefaultTreeCellRenderer {
    /*
     * TODO - figure out how to use ColoredTreeCellRenderer instead
     * IntelliJ uses ColoredTreeCellRenderer for JBComponents, and it would be better to use it instead of
     * DefaultCellRenderer. However, for some reason while attempting to use ColoredCellRenderer with Tree or JTree,
     * the node text would not appear. For now, we will use DefaultTreeCellRenderer.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

        if (!(node.getUserObject() instanceof ConflictNode)) return null;

        Icon icon = null;

        if (((ConflictNode) node.getUserObject()).getType() == ConflictNodeType.BRANCHROOT) {
            icon = AllIcons.Vcs.BranchNode;
        } else if (((ConflictNode) node.getUserObject()).getType() == ConflictNodeType.COMMIT) {
            setForeground(JBColor.RED);
        }

        setIcon(icon);
        return this;
    }

}
