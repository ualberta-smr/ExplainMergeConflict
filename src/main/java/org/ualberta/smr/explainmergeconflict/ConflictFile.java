package org.ualberta.smr.explainmergeconflict;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitConflict;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConflictFile extends GitConflict {
    private List<ConflictRegion> conflictRegionList;

    public ConflictFile(@NotNull VirtualFile root, @NotNull FilePath filePath, @NotNull Status ourStatus, @NotNull Status theirStatus) {
        super(root, filePath, ourStatus, theirStatus);
    }
}
