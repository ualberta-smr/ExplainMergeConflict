package org.ualberta.smr.explainmergeconflict;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitConflict;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConflictFile extends GitConflict {
    private List<ConflictRegion> conflictRegions = new ArrayList<>();

    public ConflictFile(@NotNull VirtualFile root, @NotNull FilePath filePath, @NotNull Status ourStatus, @NotNull Status theirStatus) {
        super(root, filePath, ourStatus, theirStatus);
    }

    public List<ConflictRegion> getConflictRegions() {
        return conflictRegions;
    }

    public void setConflictRegions(List<ConflictRegion> regions) {
        conflictRegions = regions;
    }
}
