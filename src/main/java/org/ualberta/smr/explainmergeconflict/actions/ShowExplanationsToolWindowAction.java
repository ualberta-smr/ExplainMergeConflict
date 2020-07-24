package org.ualberta.smr.explainmergeconflict.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.sun.istack.NotNull;
import git4idea.repo.GitRepositoryManager;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;

import java.awt.*;

public class ShowExplanationsToolWindowAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // TODO
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) return;
        // TODO utils for reading current repository
        if (GitRepositoryManager.getInstance(project).getRepositories().isEmpty()) return;

        System.out.println("Explain Merge Conflict action selected in " +
                "project" + e.getProject().getName());

        // TODO clean this up with helper functions
        if (GitRepositoryManager.getInstance(project).getRepositories().get(0).getConflictsHolder().getConflicts().isEmpty()) {
            showPopup(e.getDataContext());
        }
    }

    // Reference: RefactoringHistoryToolbar.java
    // TODO break this up in another file (perhaps the tool window code)
    private void showPopup(DataContext datacontext) {
        JBPanel panel = new JBPanel(new GridLayout(0, 1));
        panel.add(new JBLabel(ExplainMergeConflictBundle.message("no.conflict.show.explanations")));
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null).createPopup();
        popup.showInBestPositionFor(datacontext);
    }
}
