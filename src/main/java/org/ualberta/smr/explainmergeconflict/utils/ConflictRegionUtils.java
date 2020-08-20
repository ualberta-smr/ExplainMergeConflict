package org.ualberta.smr.explainmergeconflict.utils;

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
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConflictRegionUtils {

    public static void registerConflictsForFile(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file) {
        assert Utils.isConflictFile(project, file);
        runDiffForFileAndThenUpdate(project, repo, file);
    }

    public static void showConflictRegionInEditor(@NotNull Project project, @NotNull VirtualFile file, int nodeIndex) {
        assert isConflictRegionProperlyInitialized(project, file);
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
        ArrayList<String> filteredList = new ArrayList<>(output);
        filteredList.removeIf(e -> !e.startsWith("@@@") && !e.endsWith("@@@"));

        assert !filteredList.isEmpty();

        List<ConflictRegion> conflictRegionList = new ArrayList<>();

        // Triple represents the three pair of hunks data
        for (String region: filteredList) {
            Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> pairs = parseAndFindPairsForConflictRegion(region);
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
     * Parses through the git diff output from the git handler and gets the appropriate start line numbers and length
     * data for p1, p2, and the entire conflict region
     *
     * Example:
     *
     * <<<<<<< HEAD           <-- line 1 of conflict region
     * hello                  <-- p1
     * =======
     * hello world            <-- p2
     * >>>>>>> conflictBranch <-- line 5 of conflict region
     *
     * Output = [@@@ -1,1 -1,1 +1,5 @@@]
     * conflict region pair = [1,5]
     * p1 pair = [1,1]
     * p2 pair = [1,1]
     * Return = [[1,5], [1,1], [1,1]]
     *
     * @param region conflict region line data in output
     * @return {@link Triple} instance containing conflict region pair, p1 pair, and p2 pair
     * (each as {@link Pair}<Integer, Integer> respectively)
     */
    private static Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> parseAndFindPairsForConflictRegion(@NotNull String region) {
        Pair<Integer, Integer> regionPair = null;
        Pair<Integer, Integer> p1Pair = null;
        Pair<Integer, Integer> p2Pair = null;
        Pattern pattern;
        Matcher matcher;

        // Get region pair
        pattern = Pattern.compile("\\d+,\\d+\\s@");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            String pair = matcher.group()
                    .replaceAll("\\s@", "")
                    .trim();
            regionPair = convertPair(pair);
        }
        assert regionPair != null;

        // Get p1 pair
        pattern = Pattern.compile("@\\s[-+]\\d+,\\d+");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            String pair = matcher.group()
                    .replaceAll("@\\s[-+]", "")
                    .trim();
            p1Pair = convertPair(pair);
        }
        assert p1Pair != null;

        // Get p2 pair
        pattern = Pattern.compile("\\d\\s[-+]\\d+,\\d+\\s[-+]");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            // Extract "x,"
            String pairLeft = matcher.group()
                    .replaceAll("\\d\\s[-+]", "")
                    .trim();

            // Extract "y"
            String pairRight = matcher.group()
                    .replaceAll("\\d\\s[-+]\\d+,", "")
                    .trim();
            pairRight = pairRight.replaceAll("\\s[-+]", "");

            p2Pair = convertPair(pairLeft + pairRight);
        }
        assert p2Pair != null;

        return new Triple<>(regionPair, p1Pair, p2Pair);
    }

    /**
     * Converts a String pair "x,y" into an ArrayList<Integer> [x,y] to be used by the descriptor and other functions
     * for determining the position of the (sub)conflict region in the code editor.
     *
     * x = start line number of the (sub)conflict region
     * y = of the (sub)conflict region
     *
     * @param pair String formatted as "x,y" where x is the startLine of the conflict region and y is its length
     * @return an {@link Pair}<Integer, Integer> containing the conflict region startLine as an integer on index 0,
     * and the conflict region length as an integer on index 1
     */
    private static Pair<Integer, Integer> convertPair(@NotNull String pair) {
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

        pattern = Pattern.compile(",\\d+");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String lengthStr = matcher.group().replaceAll(",", "").trim();
            length = Integer.parseInt(lengthStr);
        }

        return new Pair<>(startLine, length);
    }

    /**
     * Verifies if a file (that is also a valid key for the conflict file hashmap for {@link MergeConflictService}) has
     * conflict regions. The function will fail if the file is not registered as a valid key for MergeConflictService's
     * conflict file hashmap.
     * @param file conflict file
     * @return true if conflict regions are initialized for the current file under MergeConflictService; otherwise false
     */
    private static boolean isConflictRegionProperlyInitialized(@NotNull Project project, @NotNull VirtualFile file) {
        HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getInstance(project).getConflictFiles();
        String key = file.getPath();
        assert conflictFiles.containsKey(key);
        List<ConflictRegion> conflictRegions = conflictFiles.get(key).getConflictRegions();

        // TODO - make sure if has subregions

        return !conflictRegions.isEmpty();
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
                List<ConflictRegion> conflictRegions = conflictFiles.get(file.getPath()).getConflictRegions();
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
            if (pair.getSecond() == 0) {
                throw new ConflictRegionIsEmptyException();
            }

            lines = pair.getFirst() + ",+" + pair.getSecond();
            List<Hash> commitsIds = new ArrayList<>();
            List<? extends TimedVcsCommit> commits = GitHistoryUtils.collectTimedCommits(
                    project,
                    repo.getRoot(),
                    "-L" + lines + ":" + file.getPath(),
                    MergeConflictService.getBaseRevId() + ".." + ref);

            assert !commits.isEmpty();

            for (TimedVcsCommit commit: commits) {
                commitsIds.add(commit.getId());
            }

            return commitsIds;
        } catch (ConflictRegionIsEmptyException e) {
            // TODO - display this in GUI
            System.out.println("Conflict region for ref " + ref + " in file " + file.getPath() + " is empty. Unable to retrieve commit history for this file.");
        } catch (VcsException e) {
            e.printStackTrace();
        }
        return null;
    }
}
