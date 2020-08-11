package org.ualberta.smr.explainmergeconflict.utils;

import com.intellij.openapi.vfs.VirtualFile;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.ConflictRegion;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNode;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNodeType;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class ConflictsTreeUtils {
    public static DefaultMutableTreeNode createRootAndChildren(ConflictNode rootNode, VirtualFile file) {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootNode);
        ConflictFile conflictFile = MergeConflictService.getConflictFiles().get(file.getPath());
        List<ConflictRegion> conflictRegions = conflictFile.getConflictRegions();

        // If this assertion fails, this means we have a race condition!
        assert !conflictRegions.isEmpty();

        for (int i = 0; i < conflictRegions.size(); i++) {
            String label = ExplainMergeConflictBundle.message("toolwindow.label.conflict") + " " + (i + 1);
            ConflictNode childNode = new ConflictNode(ConflictNodeType.CONFLICTREGION, label);
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(childNode);
            root.add(child);
        }

        return root;
    }
}
