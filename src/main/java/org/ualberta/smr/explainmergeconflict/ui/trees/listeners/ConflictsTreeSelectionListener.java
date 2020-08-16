package org.ualberta.smr.explainmergeconflict.ui.trees.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.ualberta.smr.explainmergeconflict.utils.ConflictRegionUtils;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNode;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNodeType;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

public class ConflictsTreeSelectionListener implements TreeSelectionListener {
    private JTree tree;
    private Project project;

    public ConflictsTreeSelectionListener(JTree tree, Project project) {
        this.tree = tree;
        this.project = project;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        // If null, our listener was triggered when selecting a node and then moving to another conflict file.
        if (node == null) return;
        // If null, root node was selected. Ignore node.
        if (node.getParent() == null) return;

        VirtualFile file = Utils.getCurrentFileFromEditor(project);
        ConflictNode object = (ConflictNode) node.getUserObject();
        int indexOfNode = node.getParent().getIndex(node);

        if (object.getType() == ConflictNodeType.CONFLICTREGION) {
            ConflictRegionUtils.showConflictRegionInEditor(project, file, indexOfNode);
        } else if (object.getType() == ConflictNodeType.COMMIT) {
            System.out.println("Commit selected!");
        }
    }
}
