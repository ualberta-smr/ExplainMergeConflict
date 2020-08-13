package org.ualberta.smr.explainmergeconflict.ui.windows;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.utils.ConflictRegionUtils;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.ui.trees.listeners.ConflictsTreeSelectionListener;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNode;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictsTreeCellRenderer;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNodeType;
import org.ualberta.smr.explainmergeconflict.utils.ConflictsTreeUtils;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

public class ExplanationsToolWindow implements DumbAware {
    private Project project;
    private GitRepository repo;
    private VirtualFile file;

    private JPanel explanationsToolWindowContent;
    private JPanel headerPanel;
    private JTextPane textPaneHeader;
    private JTextPane textPaneOurs;
    private JLabel labelOurs;
    private JLabel labelTheirs;
    private JScrollPane headerPane;
    private JPanel bodyPanel;
    private JTree treeTheirs;
    private JTextPane textPaneTheirs;
    private JSplitPane splitPaneOurs;
    private JSplitPane splitPaneTheirs;
    private JScrollPane scrollPaneOursLeft;
    private JScrollPane scrollPaneOursRight;
    private JScrollPane scrollPaneTheirsLeft;
    private JScrollPane scrollPaneTheirsRight;
    private JTree treeOurs;

    public ExplanationsToolWindow(GitRepository repo,
                                  Project project) {
        this.project = project;
        this.repo = repo;
        init();
    }

    private void init() {
        // TODO - register as plugin listener in plugin.xml
        // reference https://intellij-support.jetbrains.com/hc/en-us/community/posts/206762535-Action-to-trigger-on-currently-selected-file-change
        // Borrowed from VcsLogTabsWatcher.java
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new PluginEditorManagerListener() {
            @Override
            public void selectionChanged(@org.jetbrains.annotations.NotNull final FileEditorManagerEvent event){
                if (file == null || !file.equals(event.getNewFile())) {
                    file = event.getNewFile();
                    updateUI();
                }
            }
        });
        updateUI();
    }

    private void updateUI() {
        // TODO - bundle text
        // If viewing an actual conflict file, render all panels including the trees. Otherwise, remove it,
        if (Utils.isConflictFile(file)) {
            // TODO - find a way to only set regions if conflict regions are not registered!
            ConflictRegionUtils.registerConflictsForFile(project, repo, file);
            setNewTreeModelForCurrentFile();
            bodyPanel.setVisible(true);
            textPaneHeader.setText("<NUMBER> conflict regions were found in file " + file.getName());
        } else {
            if (file == null) {
                textPaneHeader.setText("No file currently opened.");
            } else if (file.getName().equals("")) {
                // Typically, non-source files have "" has file names for some reason.
                // We will just ignore them for now.
                textPaneHeader.setText("Currently viewed file is not a source file.");
            } else {
                textPaneHeader.setText("No conflict regions were found in file " + file.getName());
            }

            bodyPanel.setVisible(false);
            treeOurs.setModel(null);
        }
    }

    private void setNewTreeModelForCurrentFile() {
        ConflictNode rootNode = new ConflictNode(ConflictNodeType.BRANCHROOT, ExplainMergeConflictBundle.message("toolwindow.label.ours"));
        DefaultMutableTreeNode rootOurs = ConflictsTreeUtils.createRootAndChildren(rootNode, file);
        TreeModel model = new DefaultTreeModel(rootOurs);
        treeOurs.setModel(model);
    }

//    private void updateBackgroundColors() {
//        // TODO update color when Editor theme changes
//        // We can do so by referring to EditorColorsManager.getInstance()
//        // .getGlobalScheme
//    }

    public JPanel getContent() {
        return explanationsToolWindowContent;
    }

    /**
     * Initializes components with Custom Create enabled. This function is called in the constructor after tool window
     * attributes are initialized.
     */
    private void createUIComponents() {
        /*
         * Trees is JetBrains's implementation of JTree. Due to UI issues using JetBrain's Tree with
         * ColoredTreeCellRenderer, we will simply use JTree for now.
         * For more information, see ConflictsTreeCellRenderer.
         * TODO - use Tree after resolving disppearing text with ColoredTreeCellRenderer.
         */
        // TODO - custom create scrollpaneoursright, scrollpanetheirsright

        // Upon initialization, the file editor manager listener will not trigger if registered within the tool window
        // Until the listener is registered in plugin.xml, we will need to call our utils method to read file for now
        file = Utils.getCurrentFileFromEditor(project);

        // Ours
        treeOurs = new JTree();
        scrollPaneOursLeft = new JBScrollPane(treeOurs);
        treeOurs.setCellRenderer(new ConflictsTreeCellRenderer());
        treeOurs.addTreeSelectionListener(new ConflictsTreeSelectionListener(treeOurs, project));

        // Theirs
//        rootNode = new ConflictNode(ConflictNodeType.BRANCHROOT, ExplainMergeConflictBundle.message("toolwindow.label.theirs"));
//        DefaultMutableTreeNode rootTheirs = ConflictsTreeUtils.createRootAndChildren(rootNode, file);
//        DefaultTreeModel modelTheirs = new DefaultTreeModel(rootTheirs);
//        treeTheirs = new JTree(modelTheirs);
       treeTheirs = new JTree();
        scrollPaneTheirsLeft = new JBScrollPane(treeTheirs);
//        treeTheirs.setCellRenderer(new ConflictsTreeCellRenderer());
    }

    /**
     * A FileEditorManagerListener instance just for this tool window.
     * Needed to subscribe to the FileEditorManagerListener message bus
     * whenever we read a new file in the code editor.
     */
    private abstract static class PluginEditorManagerListener implements FileEditorManagerListener {
        @Override
        public void selectionChanged(@org.jetbrains.annotations.NotNull final FileEditorManagerEvent event){
        }
    }
}
