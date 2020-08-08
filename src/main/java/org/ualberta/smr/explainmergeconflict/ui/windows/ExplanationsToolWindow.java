package org.ualberta.smr.explainmergeconflict.ui.windows;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.services.ConflictRegionController;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNode;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictsTreeCellRenderer;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNodeType;
import org.ualberta.smr.explainmergeconflict.utils.ConflictsTreeUtils;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

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
        // Upon initialization, the file editor manager listener will not trigger if registered within the tool window
        // Until the listener is registered in plugin.xml, we will need to call our utils method to read file for now
        file = Utils.getCurrentFileFromEditor(project);

        init();
        updateUI();
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
    }

    private void updateUI() {
        if (file == null) {
            textPaneHeader.setText("No file currently opened.");
            bodyPanel.hide();
        } else if (file.getName().equals("")) {
            // Typically, non-source files have "" has file names for some reason.
            // We will just ignore them for now.
            textPaneHeader.setText("Currently viewed file is not a source file.");
            bodyPanel.hide();
        } else {
            bodyPanel.show();
            textPaneHeader.setText("<NUMBER> conflict regions were found in file " + file.getName());
        }
    }

//    private void updateBackgroundColors() {
//        // TODO update color when Editor theme changes
//        // We can do so by referring to EditorColorsManager.getInstance()
//        // .getGlobalScheme
//    }

    public JPanel getContent() {
        return explanationsToolWindowContent;
    }

    // Needed for Custom Create components declared through GUI Designer
    private void createUIComponents() {
        /*
         * Trees is JetBrains's implementation of JTree. Due to UI issues using JetBrain's Tree with
         * ColoredTreeCellRenderer, we will simply use JTree for now.
         * For more information, see ConflictsTreeCellRenderer.
         * TODO - use Tree after resolving disppearing text with ColoredTreeCellRenderer.
         */

        // TODO - custom create scrollpaneoursright, scrollpanetheirsright

        // Ours
        // TODO - use bundle for static node text
        // TODO - separate function for this
        DefaultMutableTreeNode rootOurs = ConflictsTreeUtils.createRootAndChildren(new ConflictNode(ConflictNodeType.BRANCHROOT, "Ours"));
        DefaultTreeModel modelOurs = new DefaultTreeModel(rootOurs);
        treeOurs = new JTree(modelOurs);
        scrollPaneOursLeft = new JBScrollPane(treeOurs);
        treeOurs.setCellRenderer(new ConflictsTreeCellRenderer());
        treeOurs.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeOurs.getLastSelectedPathComponent();
                ConflictNode object = (ConflictNode) node.getUserObject();

                if (object.getType() == ConflictNodeType.CONFLICTREGION) {
                    ConflictRegionController.showConflictRegion(project, repo, file);

                }
            }
        });

        // Theirs
        DefaultMutableTreeNode rootTheirs = ConflictsTreeUtils.createRootAndChildren(new ConflictNode(ConflictNodeType.BRANCHROOT, "Theirs"));
        DefaultTreeModel modelTheirs = new DefaultTreeModel(rootTheirs);
        treeTheirs = new JTree(modelTheirs);
        scrollPaneTheirsLeft = new JBScrollPane(treeTheirs);
        treeTheirs.setCellRenderer(new ConflictsTreeCellRenderer());
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
