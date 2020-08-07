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
    public static void jumpToLine(Project project, GitRepository repo, VirtualFile file) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "explainmergeconflict: running git diff to detect conflict region lines" + project.getName(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // Get diff view that only shows the conflicting regions for the current file
                GitLineHandler h = new GitLineHandler(project, repo.getRoot(), GitCommand.DIFF);
                h.addParameters("-U0");
                h.addParameters(file.getPath());
                h.endOptions();
                GitCommandResult result = Git.getInstance().runCommand(h);
                List<String> resultList = result.getOutput();
                System.out.println(resultList);
                // Line index starts at 0
                test(resultList, file);
                updateDescriptor(project, file, resultList);
            }
        });
    }

    // TODO - read conflicts
    public static List<GitConflict> getConflictFiles(@NotNull GitRepository repo) {
        List<GitConflict> conflicts = new ArrayList<>();
        repo.getStagingAreaHolder().getAllConflicts().forEach(gitConflict -> {
            conflicts.add(gitConflict);
            System.out.println("THIS IS A CONFLICT FILE" + gitConflict.getFilePath().getName());
        });
        return conflicts;
    }

    public static void getConflictRegionDataForFile(@NotNull VirtualFile file, @NotNull List<String> resultOutputList) {
        if (resultOutputList.isEmpty()) return;

        ArrayList<String> fileNameList = new ArrayList<>(resultOutputList);
//        ArrayList<String> data = new ArrayList<>(resultOutputList);
        fileNameList.removeIf(e -> !e.startsWith("diff --cc"));
//        data.removeIf(e -> !e.startsWith("@@@") && !e.endsWith("@@@"));

        int indexOfCurrentFileName;
        // Extract the file name from "diff --cc <file name>" within the output
        // Replace "diff --cc <file name>" with the file name we extract
        for (int i = 0; i < fileNameList.size(); i++) {
            String fileName = fileNameList.get(i)
                    .replaceAll("diff --cc", "")
                    .trim();

            if (fileName.equals(file.getName())) {
                indexOfCurrentFileName = resultOutputList.indexOf(fileNameList.get(i));
                break;
            }
        }

        // TODO - error or ignore if filename is never found


        System.out.println(fileNameList);
    }

    public static void test(List<String> resultList, VirtualFile file) {
        if (resultList.isEmpty()) return;
        ArrayList<String> filteredList = new ArrayList<>(resultList);
        filteredList.removeIf(e -> !e.startsWith("diff --cc") && !e.startsWith("@@@"));
//        filteredList.removeIf(e -> !e.startsWith("@@@"));
        System.out.println(filteredList);
        if (filteredList.isEmpty()) return; // TODO condition where there is no diff
        String currentKey = null;
        HashMap<String, ArrayList<String>> map = new HashMap<>();
        ArrayList<String> values = new ArrayList<>();
        // TODO - arraylist for values
        boolean startOfResults = true;
        for (String result: filteredList) {
            if (result.startsWith("diff --cc")) {
                result = result.replaceAll("diff --cc", "");
                result = result.trim();
                currentKey = result;
                if (startOfResults) {
                    startOfResults = false;
                } else {
                    map.put(currentKey, values);
                }
            } else if (result.startsWith("@@@")) {
                // https://stackoverflow.com/questions/4662215/how-to-extract-a-substring-using-regex
                // Extract the start line and length of the third pair for a file diff
                // This will give us the information we need for an entire conflict region within a file
                int indexOfThirdPairComma = result.lastIndexOf(",");
                Pattern pattern = Pattern.compile("\\d+,\\d+\\s@");
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    String pair = matcher.group().replaceAll("\\s@", "").trim();
                    values.add(pair);
                }
            }
            System.out.println(result);
        }
        System.out.println(map);
    }


    private static void updateDescriptor(@NotNull Project project, @NotNull VirtualFile file, @NotNull List<String> resultList) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO TEMPORARY
                GitRepository repo = GitRepositoryManager.getInstance(project).getRepositories().get(0);
                getConflictFiles(repo);
                test(resultList, file);
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, 12, 0);
                descriptor.navigateInEditor(project, true);
            }
        });
    }
}
