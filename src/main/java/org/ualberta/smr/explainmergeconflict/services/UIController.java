package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.ui.ExplanationsToolWindow;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

public class UIController {
    static ToolWindowManager manager;

    public static void updateToolWindowAfterAction(GitRepository repo) {
        assert Utils.isInConflictState(repo);

        Project project = repo.getProject();
        manager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = manager.getToolWindow(
                ExplainMergeConflictBundle.message("toolwindow" +
                        ".id"));

        if (toolWindow != null) {
            displayToolWindow();
        } else {
            registerAndDisplayToolWindow(repo);
        }
    }

    public static void updateToolWindowAfterNonConflictState(GitRepository repo) {
        assert !Utils.isInConflictState(repo);
        if (manager.getToolWindow(
                ExplainMergeConflictBundle.message("toolwindow.id")) != null) {
            unregisterToolWindow();
        }
    }

    private static void displayToolWindow() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindow toolWindow =
                        manager.getToolWindow(
                                ExplainMergeConflictBundle.message(
                                        "toolwindow.id"));
                toolWindow.show();
            }
        });
    }

    private static void registerAndDisplayToolWindow(GitRepository repo) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindow toolWindow =
                        manager.registerToolWindow(
                                ExplainMergeConflictBundle.message(
                                        "toolwindow.id"),
                                true,
                                ToolWindowAnchor.RIGHT);
                getToolWindowContent(repo);
                toolWindow.show();
            }
        });
    }

    private static void getToolWindowContent(GitRepository repo) {
        ToolWindow toolWindow =
                manager.getToolWindow(ExplainMergeConflictBundle.message(
                                "toolwindow.id"));

        if (toolWindow == null) return;

        ExplanationsToolWindow explanationsToolWindow =
        new ExplanationsToolWindow(repo, repo.getProject());
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content =
                contentFactory.createContent(
                        explanationsToolWindow.getContent(), null,
                        false);
        toolWindow.getContentManager().addContent(content);
    }

    private static void unregisterToolWindow() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                manager.unregisterToolWindow(
                                ExplainMergeConflictBundle.message("toolwindow.id"));
            }
        });
    }
}
