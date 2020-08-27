package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitConflict;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
        try {
            baseRevId = Objects.requireNonNull(GitHistoryUtils.getMergeBase(
                    project,
                    repo.getRoot(),
                    GitUtil.HEAD,
                    GitUtil.MERGE_HEAD)).getRev();
        } catch (VcsException e) {
            e.printStackTrace();
        }

        // Branches
        setBranches(repo);

        // Conflict Files
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
            String key = conflict.getFilePath().toString();
            conflictFilesMap.put(key, conflictFile);
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
}
