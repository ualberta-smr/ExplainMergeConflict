package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.TimedVcsCommit;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.*;
import org.ualberta.smr.explainmergeconflict.utils.ConflictRegionUtils;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConflictRegionHandler {
    public static void registerConflictsForFile(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file) {
        assert Utils.isConflictFile(project, file);
        runDiffForFileAndThenUpdate(project, repo, file);
    }

    public static void showConflictRegionInEditor(@NotNull Project project, @NotNull VirtualFile file, int nodeIndex) {
        assert ConflictRegionUtils.isConflictFileValid(project, file);
        updateDescriptor(project, file, nodeIndex);
    }

    /**
     * Runs the git diff command using {@link GitLineHandler} and reads the stdout output for additional parsing to find
     * conflict region data. Once the output is parsed, register the conflict regions to {@link MergeConflictService}.
     * This command is run synchronously to ensure we initialize all conflict regions for the currently opened file
     * before trying to access them in the UI (i.e. Explain Merge Conflict tool window).
     * @param project current project
     * @param repo current Git repository
     * @param file currently opened file
     */
    private static void runDiffForFileAndThenUpdate(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file) {
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

                registerConflictRegions(project, repo, file, resultList);
            }
        });
    }

    /**
     * Initialize and register {@link ConflictRegion}s for the currently opened file under {@link ConflictFile}.
     * @param output git diff command output
     * @param file current file
     */
    private static void registerConflictRegions(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file, @NotNull List<String> output) {
        // TODO - add to utils
        ArrayList<String> filteredList = new ArrayList<>(output);
        // Filter output to only show hunk data.
        filteredList.removeIf(e -> !e.startsWith("@@@") && !e.endsWith("@@@"));

        assert !filteredList.isEmpty();

        List<ConflictRegion> conflictRegionList = new ArrayList<>();

        // Triple represents the three pairs of hunks data - conflict region, p1, and p2 respectively
        for (String region: filteredList) {
            Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> pairs = ConflictRegionUtils.parseAndFindPairsForConflictRegion(region);
            Pair<Integer,Integer> regionPair = pairs.getFirst();
            Pair<Integer,Integer> p1Pair = pairs.getSecond();
            Pair<Integer,Integer> p2Pair = pairs.getThird();

            // Get list of commits that modified p1 and p2
            List<Hash> p1Commits = getAllCommitIdsForRegionInFile(project, repo, file, GitUtil.HEAD, p1Pair);
            List<Hash> p2Commits = getAllCommitIdsForRegionInFile(project, repo, file, GitUtil.MERGE_HEAD, p2Pair);
            assert p1Commits != null;
            assert p2Commits != null;

            ConflictSubRegion p1 = new ConflictSubRegion(
                    Ref.HEAD,
                    p1Pair.getFirst(),
                    p1Pair.getSecond(),
                    p1Commits
            );
            ConflictSubRegion p2 = new ConflictSubRegion(
                    Ref.MERGE_HEAD,
                    p2Pair.getFirst(),
                    p2Pair.getSecond(),
                    p2Commits
            );
            ConflictRegion conflictRegion = new ConflictRegion(
                    file,
                    regionPair.getFirst(),
                    regionPair.getSecond(),
                    p1,
                    p2
            );
            conflictRegionList.add(conflictRegion);
        }

        assert !conflictRegionList.isEmpty();

        // Access conflict files hashmap in MergeConflictService and update their values with the new conflict classes
        HashMap<String, ConflictFile> conflictsMap = MergeConflictService.getInstance(project).getConflictFiles();
        ConflictFile conflictFile = conflictsMap.get(file.getPath());
        conflictFile.setConflictRegions(conflictRegionList);

        // TODO simplify setting conflict files and remove repetition
        MergeConflictService.getInstance(project).getConflictFiles().replace(file.getPath(), conflictFile);
    }

    /**
     * Updates the cursor and current line in the code editor to point to the starting line of a conflict region.
     * @param project current project
     * @param file currently viewed file
     * @param nodeIndex index of the currently selected conflict node in ours/theirs tree. Order of conflict regions in
     *                  the tree is the same as the order of conflict regions under {@link MergeConflictService}
     */
    private static void updateDescriptor(@NotNull Project project, @NotNull VirtualFile file, int nodeIndex) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getInstance(project).getConflictFiles();
                ConflictFile conflictFile = conflictFiles.get(file.getPath());
                List<ConflictRegion> conflictRegions = conflictFile.getConflictRegions();
                int regionStartLine = conflictRegions.get(nodeIndex).getStartLine();

                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, regionStartLine-1, 0);
                descriptor.navigateInEditor(project, true);
            }
        });
    }

    /**
     * Gets the git commit history for the p1 and p2 regions of a conflict region by running
     * {@code git log <ref> -L<startLine>,<endLine>:<filePath> <commonAncestor>..<ref>}.
     * @param project current project
     * @param repo current git repository
     * @param file currently opened file
     * @param ref HEAD or MERGE_HEAD reference
     * @param pair p1 or p2 pair hunk data
     * @return list of commit {@link Hash}s
     */
    private static List<Hash> getAllCommitIdsForRegionInFile(@NotNull Project project, @NotNull GitRepository repo,
                                                             @NotNull VirtualFile file, @NotNull String ref,
                                                             @NotNull Pair<Integer, Integer> pair) {
        List<Hash> commitsIds = new ArrayList<>();
        try {
            /*
             * No logs would be recorded with these specific parameters when running GitHistoryUtils#history or
             * GitHistoryUtils#loadDetails. GitHistoryUtils#collectVcsMetadata does not permit option parameters
             * since it runs in stdin mode. Thus, the only method in GitHistoryUtils that actually works with
             * these params is collectTimedCommits.
             *
             * To avoid the need to manually parse through git log output using regex, we will use
             * collectTimedCommits to quickly find the commit ids of relevant commits. We can then use these ids
             * as parameters for other methods if needed.
             */
            String lines;
            /*
             * Length = 0 means that the p1 or p2 sub conflict region is empty. This is still valid however, for it
             * is still important information within a conflict region to consider when resolving merge conflicts.
             * Empty conflict regions also vary from a file to file basis depending on the types of changes made.
             */
            String linesTest = pair.getFirst() + ",+" + pair.getSecond();
            System.out.println("Running: git log -L" + linesTest + ":" + file.getPath() + " " + MergeConflictService.getBaseRevId() + ".." + ref);
            if (pair.getSecond() == 0) {
                throw new ConflictRegionIsEmptyException();
            }

            lines = pair.getFirst() + ",+" + pair.getSecond();
            List<? extends TimedVcsCommit> commits = GitHistoryUtils.collectTimedCommits(
                    project,
                    repo.getRoot(),
                    "-L" + lines + ":" + file.getPath(),
                    MergeConflictService.getBaseRevId() + ".." + ref);

            assert !commits.isEmpty();

            for (TimedVcsCommit commit: commits) {
                commitsIds.add(commit.getId());
            }
        } catch (ConflictRegionIsEmptyException e) {
            // TODO - display this in GUI
            System.out.println("Conflict region for ref " + ref + " in file " + file.getPath() + " is empty. Unable to retrieve commit history for this file.");
        } catch (VcsException e) {
            e.printStackTrace();
        }
        return commitsIds;
    }
}
