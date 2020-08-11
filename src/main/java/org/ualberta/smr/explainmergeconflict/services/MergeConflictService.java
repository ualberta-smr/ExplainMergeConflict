package org.ualberta.smr.explainmergeconflict.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitConflict;
import git4idea.repo.GitRepository;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public final class MergeConflictService implements Disposable {
    private static final List<ConflictFile> conflictFiles = new ArrayList<>();
    private final Project project;

    public MergeConflictService(Project project) {
        this.project = project;
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
            conflictFiles.add(conflictFile);
        }

        conflictFiles.forEach(conflictFile -> System.out.println(conflictFile.getFilePath()));
    }

    public static List<ConflictFile> getConflictFiles() {
        return conflictFiles;
    }

    /**
     * Runs housekeeping tasks to cleanup unused components like the tool window. Also disposes the service itself.
     */
    @Override
    public void dispose() {
        UIController.updateToolWindowAfterNonConflictState(Utils.getCurrentRepository(project));
    }
}
