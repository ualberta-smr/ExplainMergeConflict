package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.TimedVcsCommit;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitConflict;
import git4idea.repo.GitRepository;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.*;
import org.ualberta.smr.explainmergeconflict.utils.ConflictRegionUtils;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.*;

@Service
public final class MergeConflictService implements Disposable {
    private static final HashMap<String, ConflictFile> conflictFilesMap = new HashMap<>();
    private static String headBranchName;
    private static String mergeBranchName;
    private static String baseRevId;
    private final Project project;

    public MergeConflictService(Project project) {
        this.project = project;
        init();
    }

    public static MergeConflictService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, MergeConflictService.class);
    }

    /**
     * Initializes values for all attributes: the common ancestor commit id, the branch name of HEAD, the branch name of
     * MERGE_HEAD, and a map of {@link ConflictFile}s. The map of conflict files contains all files affected by the
     * merge conflict and is formatted as such:
     *
     * {
     *     <path of conflicted file>: <instance of ConflictFile>
     * }
     */
    private void init() {
        GitRepository repo = Objects.requireNonNull(Utils.getCurrentRepository(project));

        // Common ancestor
        setBaseRevId(repo);

        // Branches
        setBranches(repo);

        // Conflict files
        setConflictFiles(repo);
    }

    private void setBaseRevId(GitRepository repo) {
        try {
            baseRevId = Objects.requireNonNull(GitHistoryUtils.getMergeBase(
                    project,
                    repo.getRoot(),
                    GitUtil.HEAD,
                    GitUtil.MERGE_HEAD)).getRev();
        } catch (VcsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the branch names for the HEAD and MERGE_HEAD references by using {@link GitLineHandler} to run a the git
     * command `git log <ref> -1 --pretty=%D`.
     * @param repo current repository
     */
    private void setBranches(GitRepository repo) {
        headBranchName = GitBranchUtil.getBranchNameOrRev(repo);

        /*
         * %D returns the branch name of a ref without the "()" brackets surrounding it.
         * Reference: https://git-scm.com/docs/git-log#_pretty_formats
         */
        GitLineHandler h = new GitLineHandler(project, repo.getRoot(), GitCommand.LOG);
        h.addParameters(GitUtil.MERGE_HEAD);
        h.addParameters("-1");
        h.addParameters("--pretty=%D");
        GitCommandResult result = Git.getInstance().runCommand(h);
        List<String> output = result.getOutput();

        assert output.size() == 1;

        mergeBranchName = output.get(0);
    }

    private void setConflictFiles(GitRepository repo) {
        List<GitConflict> conflicts = new ArrayList<>(repo.getStagingAreaHolder()
                .getAllConflicts()
        );

        for (GitConflict conflict: conflicts) {
            ConflictFile conflictFile = new ConflictFile(
                    conflict.getRoot(),
                    conflict.getFilePath(),
                    conflict.getStatus(GitConflict.ConflictSide.OURS),
                    conflict.getStatus(GitConflict.ConflictSide.THEIRS)
            );
            FilePath keyPath = conflict.getFilePath();
            conflictFilesMap.put(keyPath.toString(), conflictFile);

            // Set conflict regions for the conflict files
            VirtualFile file = keyPath.getVirtualFile();
            runDiffForFileAndThenUpdate(project, repo, file);
        }
    }

    public static String getBaseRevId() {
        return baseRevId;
    }

    public static String getHeadBranchName() {
        return headBranchName;
    }

    public static String getMergeBranchName() {
        return mergeBranchName;
    }

    public HashMap<String, ConflictFile> getConflictFiles() {
        return conflictFilesMap;
    }

    /**
     * Runs housekeeping tasks to cleanup unused components like the tool window. Also disposes the service itself.
     */
    @Override
    public void dispose() {
        conflictFilesMap.clear();
        UIController.updateToolWindowAfterNonConflictState(Utils.getCurrentRepository(project));
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
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "explainmergeconflict: running git diff to detect conflict regions" + project.getName(), false) {
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
        ConflictFile conflictFile = conflictFilesMap.get(file.getPath());
        conflictFile.setConflictRegions(conflictRegionList);

        conflictFilesMap.replace(file.getPath(), conflictFile);
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
//            System.out.println("Running: git log -L" + linesTest + ":" + file.getPath() + " " + getBaseRevId() + ".." + ref);
            if (pair.getSecond() == 0) {
                throw new ConflictRegionIsEmptyException();
            }

            lines = pair.getFirst() + ",+" + pair.getSecond();
            List<? extends TimedVcsCommit> commits = GitHistoryUtils.collectTimedCommits(
                    project,
                    repo.getRoot(),
                    "-L" + lines + ":" + file.getPath(),
                    getBaseRevId() + ".." + ref);

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
