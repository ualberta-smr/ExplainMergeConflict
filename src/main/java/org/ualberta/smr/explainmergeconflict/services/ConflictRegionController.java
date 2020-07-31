package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConflictRegionController {
    public static void jumpToLine(Project project, GitRepository repo, VirtualFile file) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Detecting all conflict files for " + project.getName(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitLineHandler h = new GitLineHandler(project, repo.getRoot(), GitCommand.DIFF);
                h.addParameters("-U0");
                h.endOptions();
                GitCommandResult result = Git.getInstance().runCommand(h);
                List<String> resultList = result.getOutput();
                System.out.println(resultList);
                // Line index starts at 0
                updateDescriptor(project, file);
            }
        });
    }

    public static void updateDescriptor(Project project, VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, 12, 0);
                descriptor.navigateInEditor(project, true);
            }
        });
    }
}
