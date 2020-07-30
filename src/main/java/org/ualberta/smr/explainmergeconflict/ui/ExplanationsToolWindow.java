package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.sun.istack.NotNull;
import git4idea.repo.GitConflictsHolder;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import javax.swing.*;

public class ExplanationsToolWindow {
    private Project project;
    private ToolWindow toolWindow;
    private GitRepository repo;
    private VirtualFile file;
    private JPanel explanationsToolWindowContent;
    private JPanel headerPanel;
    private JTextPane headerTextPane;
    private JTextPane textPaneOurs;
    private JTextPane textPaneTheirs;
    private JButton showLogButton;
    private JButton showLogButton1;
    private JLabel labelOurs;
    private JLabel labelTheirs;
    private JScrollPane headerPane;

    public ExplanationsToolWindow(GitRepository repo,
                                  Project project) {
        this.project = project;
        this.repo = repo;
        file = Utils.getCurrentFileFromEditor(project);

        initializeToolWindow();
    }

    private void initializeToolWindow() {
        // FIXME - do not initialize plugin if no repo or no file upon
        //  initialization
        updateUIIfMergeConflictState();
//        updateBackgroundColors();

        // reference https://intellij-support.jetbrains.com/hc/en-us/community/posts/206762535-Action-to-trigger-on-currently-selected-file-change
        // Borrowed from VcsLogTabsWatcher.java
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new PluginEditorManagerListener() {
            @Override
            public void selectionChanged(@org.jetbrains.annotations.NotNull final FileEditorManagerEvent event){
                if (file == null || !file.equals(event.getNewFile())) {
                    file = event.getNewFile();
                    updateUIIfMergeConflictState();
                }
            }
        });

//        // Borrowed from GitConflictsToolWindowManager.java
//        project.getMessageBus().connect().subscribe(GitConflictsHolder.CONFLICTS_CHANGE, repository -> {
//            System.out.println("Conflict change listener triggered");
//            updateUIIfMergeConflictState();
//        });
    }

    private void updateUIIfMergeConflictState() {
        boolean conflictsExist =
                Utils.isInConflictState(repo);

        if (conflictsExist) {
            updateUIInConflictState();
        } else {
            resetUIToNonConflictState();
        }
    }

    private void updateUIInConflictState() {
        if (file == null) {
            headerTextPane.setText("No file currently opened");
        } else {
            // Typically name is empty if we are viewing a diff of a file
            // For now, let's ignore diff views
            if (file.getName().equals("")) {
                headerTextPane.setText("Currently viewed file must be source");
            } else {
                headerTextPane.setText("<NUMBER> conflict regions were found in file " + file.getName());
            }
        }
    }

    private void resetUIToNonConflictState() {
        headerTextPane.setText(null);
    }

    private void updateBackgroundColors() {
        // TODO update color when Editor theme changes
        // We can do so by referring to EditorColorsManager.getInstance()
        // .getGlobalScheme
    }

    public JPanel getContent() {
        return explanationsToolWindowContent;
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
