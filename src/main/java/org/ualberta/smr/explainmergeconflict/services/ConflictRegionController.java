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
import git4idea.repo.GitConflict;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ConflictRegionController {

    public static void showConflictRegion(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file) {
        runDiffForFileAndThenUpdate(project, repo, file);
    }

    private static void runDiffForFileAndThenUpdate(Project project, GitRepository repo, VirtualFile file) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "explainmergeconflict: running git diff to detect conflict region lines" + project.getName(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // Get diff view that only shows the conflicting regions for the current file
                // Reference: Are Refactorings To Blame? An Empirical Study of Refactorings in Merge Conflicts
                GitLineHandler h = new GitLineHandler(project, repo.getRoot(), GitCommand.DIFF);
                h.addParameters("-U0");
                h.endOptions();
                h.addParameters(file.getPath());

                GitCommandResult result = Git.getInstance().runCommand(h);
                List<String> resultList = result.getOutput();

                test(resultList, file);
                updateDescriptor(project, file, resultList);
            }
        });
    }

    // TODO - read conflicts
    private static List<GitConflict> getConflictFiles(@NotNull GitRepository repo) {
        List<GitConflict> conflicts = new ArrayList<>();
        repo.getStagingAreaHolder().getAllConflicts().forEach(gitConflict -> {
            conflicts.add(gitConflict);
        });
        return conflicts;
    }

    private static void test(List<String> resultList, VirtualFile file) {
        ArrayList<String> filteredList = new ArrayList<>(resultList);
        filteredList.removeIf(e -> !e.startsWith("@@@") && !e.endsWith("@@@"));

        if (filteredList.isEmpty()) return; // TODO condition where there is no diff

        Pattern pattern = Pattern.compile("\\d+,\\d+\\s@");
        Matcher matcher;

        // Extract only the third pair of numbers for each region
        // Format: (start line number of conflict region, length of conflict region)
        for (String conflictRegion: filteredList) {
            matcher = pattern.matcher(conflictRegion);

            if (matcher.find()) {
                String pair = matcher.group()
                        .replaceAll("\\s@", "")
                        .trim();
            }
        }
    }


    private static void updateDescriptor(@NotNull Project project, @NotNull VirtualFile file, @NotNull List<String> resultList) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO TEMPORARY
                GitRepository repo = GitRepositoryManager.getInstance(project).getRepositories().get(0);
                getConflictFiles(repo);
                if (!resultList.isEmpty()) {
                    test(resultList, file);
                }
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, 12, 0);
                descriptor.navigateInEditor(project, true);
            }
        });
    }
}
