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
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.UIController;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.awt.GridLayout;

public class ShowExplanationsToolWindowAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (e.getProject() != null) {
            GitRepository repo = Utils.getCurrentRepository(project);
            boolean isInGit = Utils.isInGitRepository(project);
            boolean isInConflict = Utils.isInConflictState(repo);
            boolean shouldActionBeVisible = isInGit && isInConflict;
            e.getPresentation().setVisible(shouldActionBeVisible);
//            System.out.println(e.getDataContext());
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        GitRepository repo = Utils.getCurrentRepository(e.getProject());

        // TODO - show popup only for files without conflicts
        // Ideally, action should not be visible if no conflicts are found
        if (!Utils.isInConflictState(repo)) {
            showPopup(e.getDataContext());
            return;
        }

        UIController.updateToolWindowAfterAction(repo);
    }

    // Reference: RefactoringHistoryToolbar.java
    private void showPopup(DataContext datacontext) {
        JBPanel panel = new JBPanel(new GridLayout(0, 1));
        panel.add(new JBLabel(ExplainMergeConflictBundle.message("no.conflict.show.explanations")));
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null).createPopup();
        popup.showInBestPositionFor(datacontext);
    }
}
