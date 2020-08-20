package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import git4idea.GitUtil;
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
    private static String baseRevId;
    private final Project project;

    public MergeConflictService(Project project) {
        this.project = project;

        try {
            GitRepository repo = Objects.requireNonNull(Utils.getCurrentRepository(project));
            baseRevId = Objects.requireNonNull(GitHistoryUtils.getMergeBase(
                    project,
                    repo.getRoot(),
                    GitUtil.HEAD,
                    GitUtil.MERGE_HEAD)).getRev();
        } catch (VcsException e) {
            e.printStackTrace();
        }
    }

    public static MergeConflictService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, MergeConflictService.class);
    }

    /**
     * Creates a list of {@link ConflictFile}s that each contain data from {@link GitConflict}, as well as additional data
     * such as conflict regions.
     */
    public void initConflictFiles() {
        GitRepository repo = Utils.getCurrentRepository(project);
        assert repo != null;
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
            conflictFilesMap.put(conflict.getFilePath().toString(), conflictFile);
        }
    }

    public static String getBaseRevId() {
        return baseRevId;
    }

    public HashMap<String, ConflictFile> getConflictFiles() {
        return conflictFilesMap;
    }

    /**
     * Runs housekeeping tasks to cleanup unused components like the tool window. Also disposes the service itself.
     */
    @Override
    public void dispose() {
        UIController.updateToolWindowAfterNonConflictState(Utils.getCurrentRepository(project));
    }
}
