package org.ualberta.smr.explainmergeconflict.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.sun.istack.NotNull;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.awt.GridLayout;

public class ShowExplanationsToolWindowAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        if (e.getProject() != null) {
            boolean shouldActionBeVisible =
                    Utils.isInGitRepository(e.getProject());
            e.getPresentation().setVisible(shouldActionBeVisible);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        GitRepository repo = Utils.getCurrentRepository(e.getProject());

        if (Utils.isInConflictState(repo)) {
            showPopup(e.getDataContext());
            return;
        }

        ToolWindow toolWindow  = ToolWindowManager.getInstance(e.getProject())
                .getToolWindow(
                        ExplainMergeConflictBundle.message("toolwindow.id")
                );
        assert toolWindow != null;
        toolWindow.show();
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
