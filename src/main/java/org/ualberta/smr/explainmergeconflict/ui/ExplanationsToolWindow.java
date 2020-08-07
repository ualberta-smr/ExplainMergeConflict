package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.services.ConflictRegionController;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExplanationsToolWindow implements DumbAware {
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
    private JPanel bodyPanel;

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

        showLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConflictRegionController.showConflictRegion(project, repo, file);
            }
        });
    }

    private void updateUI() {
        if (file == null) {
            headerTextPane.setText("No file currently opened.");
            bodyPanel.hide();
        } else if (file.getName().equals("")) {
            // Typically, non-source files have "" has file names for some reason.
            // We will just ignore them for now.
            headerTextPane.setText("Currently viewed file is not a source file.");
            bodyPanel.hide();
        } else {
            bodyPanel.show();
            headerTextPane.setText("<NUMBER> conflict regions were found in file " + file.getName());
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
