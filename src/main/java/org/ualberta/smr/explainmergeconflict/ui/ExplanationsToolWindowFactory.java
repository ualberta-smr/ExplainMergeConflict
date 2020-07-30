package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.sun.istack.NotNull;

public class ExplanationsToolWindowFactory implements ToolWindowFactory, DumbAware {
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow){
//        ExplanationsToolWindow explanationsToolWindow =
//                new ExplanationsToolWindow(project, toolWindow);
//        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//        Content content =
//                contentFactory.createContent(
//                        explanationsToolWindow.getContent(), null,
//                        false);
//        toolWindow.getContentManager().addContent(content);
    }
}
