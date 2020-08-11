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
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.ConflictRegion;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConflictRegionController {

    public static void setConflictRegionsForFile(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file) {
        assert Utils.isConflictFile(file);
        runDiffForFileAndThenUpdate(project, repo, file);
    }

    public static void showConflictRegionInEditor(@NotNull Project project, @NotNull VirtualFile file) {
        assert isConflictRegionProperlyInitialized(file);
        updateDescriptor(project, file);
    }

    private static void runDiffForFileAndThenUpdate(Project project, GitRepository repo, VirtualFile file) {
        /*
         * Rather than running the git command asynchronously as a background task, we should instead run it synchronously
         * so that we don't face any race conditions - particularly, when we try to read conflict file data while it
         * hasn't been processed yet. We do this using Task.Modal rather than Task.Background.
         *
         * For example, when we launch the Explain Merge Conflict tool window after running the show action, let the
         * tool window wait for the git handler to resolve so that we have data to show. Otherwise, we will display an
         * empty tree despite having already had initialized the conflict file data.
         */
        ProgressManager.getInstance().run(new Task.Modal(project, "explainmergeconflict: running git diff to detect conflict regions" + project.getName(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                /*
                 * Get diff view that only shows the conflicting regions for the current file
                 * Reference: Are Refactorings To Blame? An Empirical Study of Refactorings in Merge Conflicts
                 */
                GitLineHandler h = new GitLineHandler(project, repo.getRoot(), GitCommand.DIFF);
                h.addParameters("-U0");
                h.endOptions();
                h.addParameters(file.getPath());

                GitCommandResult result = Git.getInstance().runCommand(h);
                List<String> resultList = result.getOutput();

                registerConflictRegions(resultList, file);
            }
        });
    }

    private static void registerConflictRegions(List<String> resultList, VirtualFile file) {
        ArrayList<String> filteredList = new ArrayList<>(resultList);
        filteredList.removeIf(e -> !e.startsWith("@@@") && !e.endsWith("@@@"));

        if (filteredList.isEmpty()) return; // TODO condition where there is no diff

        List<ConflictRegion> conflictRegionList = new ArrayList<>();
        HashMap<String, ConflictFile> conflictsMap = MergeConflictService.getConflictFiles();
        ConflictFile conflictFile = conflictsMap.get(file.getPath());
        Pattern pattern = Pattern.compile("\\d+,\\d+\\s@");
        Matcher matcher;

        /*
         * Extract only the third pair of numbers for each region
         * Format: (start line number of conflict region, length of conflict region)
         */
        for (String region: filteredList) {
            matcher = pattern.matcher(region);

            if (matcher.find()) {
                String pair = matcher.group()
                        .replaceAll("\\s@", "")
                        .trim();

                ArrayList<Integer> newPair = convertPair(pair);
                ConflictRegion conflictRegion = new ConflictRegion(file, newPair);
                conflictRegionList.add(conflictRegion);
            }
        }

        conflictFile.setConflictRegions(conflictRegionList);
        // TODO simplify setting conflict files and remove repetition
        MergeConflictService.getConflictFiles().replace(file.getPath(), conflictFile);
    }

    private static ArrayList<Integer> convertPair(String pair) {
        int startLine = 0;
        int length = 0;
        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile("\\d+,");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String startLineStr = matcher.group().replaceAll(",", "").trim();
            startLine = Integer.parseInt(startLineStr);
        }

        pattern = Pattern.compile(",\\d");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String lengthStr = matcher.group().replaceAll(",", "").trim();
            length = Integer.parseInt(lengthStr);
        }

        ArrayList<Integer> newPair = new ArrayList<>();
        newPair.add(0, startLine);
        newPair.add(1, length);

        return newPair;
    }

    private static boolean isConflictRegionProperlyInitialized(VirtualFile file) {
        HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getConflictFiles();
        List<ConflictRegion> conflictRegions = conflictFiles.get(file.getPath()).getConflictRegions();

        // TODO - make sure if has subregions

        return !conflictRegions.isEmpty();
    }

    private static void updateDescriptor(@NotNull Project project, @NotNull VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getConflictFiles();
                List<ConflictRegion> conflictRegions = conflictFiles.get(file.getPath()).getConflictRegions();
                int regionStartLine = conflictRegions.get(0).getStartLine(); // TODO - no fixed index value

                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, regionStartLine-1, 0);
                descriptor.navigateInEditor(project, true);
            }
        });
    }
}
