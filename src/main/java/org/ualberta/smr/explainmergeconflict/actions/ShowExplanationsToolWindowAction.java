package org.ualberta.smr.explainmergeconflict.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.sun.istack.NotNull;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.services.UIController;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;

public class ShowExplanationsToolWindowAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        GitRepository repo = Utils.getCurrentRepository(e.getProject());
        e.getPresentation().setVisible(repo != null && Utils.isInConflictState(repo));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        GitRepository repo = Utils.getCurrentRepository(e.getProject());

        if (shouldShowToolWindow(e.getDataContext())) {
            UIController.updateToolWindowAfterAction(repo);
        } else {
            showPopup(e.getDataContext());
        }
    }

    private boolean shouldShowToolWindow(DataContext dataContext) {
        List<ConflictFile> conflicts = MergeConflictService.getConflictFiles();

        for (ConflictFile conflict: conflicts) {
            if (conflict.getFilePath()
                    .getPath()
                    .equals(Objects.requireNonNull(
                            dataContext.getData(CommonDataKeys.VIRTUAL_FILE)).getPath()
                    )) {
                return true;
            }
        }

        return false;
    }


    // Reference: RefactoringHistoryToolbar.java
    private void showPopup(DataContext dataContext) {
        JBPanel panel = new JBPanel(new GridLayout(0, 1));
        panel.add(new JBLabel(ExplainMergeConflictBundle.message("no.conflict.show.explanations")));
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null).createPopup();
        popup.showInBestPositionFor(dataContext);
    }
}
