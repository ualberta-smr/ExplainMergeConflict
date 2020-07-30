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

    public static void updateToolWindow(GitRepository repo) {
        Project project = repo.getProject();
        manager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = manager.getToolWindow(
                ExplainMergeConflictBundle.message("toolwindow" +
                        ".id"));

        if (!Utils.isInConflictState(repo) && toolWindow != null) {
            unregisterToolWindow(project);
        } else if (Utils.isInConflictState(repo) && toolWindow != null) {
            toolWindow.show();
        } else {
            // If we reach conflict state without tool window initialized
            // register and show it
            registerAndDisplayToolWindow(repo);
        }
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

    private static void unregisterToolWindow(Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                manager.unregisterToolWindow(
                                ExplainMergeConflictBundle.message("toolwindow.id"));
            }
        });
    }
}
