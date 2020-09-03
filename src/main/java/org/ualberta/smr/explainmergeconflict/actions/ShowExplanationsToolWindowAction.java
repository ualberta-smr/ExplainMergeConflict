package org.ualberta.smr.explainmergeconflict.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.vcs.log.VcsLogDataKeys;
import com.intellij.vcs.log.VcsLogUi;
import com.sun.istack.NotNull;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.UIController;
import org.ualberta.smr.explainmergeconflict.ui.TestWindow;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.awt.GridLayout;

public class ShowExplanationsToolWindowAction extends AnAction {
    // TODO - temporary code for testing log UI functionality
//    public static TestWindow window;

    @Override
    public void update(AnActionEvent e) {
        GitRepository repo = Utils.getCurrentRepository(e.getProject());
        e.getPresentation().setVisible(repo != null && Utils.isInConflictState(repo));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        GitRepository repo = Utils.getCurrentRepository(e.getProject());

        if (Utils.isConflictFile(e.getProject(), e.getData(CommonDataKeys.VIRTUAL_FILE))) {
            UIController.updateToolWindowAfterAction(repo);
            VcsLogUi logUi = e.getData(VcsLogDataKeys.VCS_LOG_UI);

//            if (window == null) {
//                window = new TestWindow(e.getProject(), logUi, e.getData(CommonDataKeys.VIRTUAL_FILE));
//            }
        } else {
            showPopup(e.getDataContext());
        }
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
