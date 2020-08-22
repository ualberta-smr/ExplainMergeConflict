package org.ualberta.smr.explainmergeconflict;

import com.intellij.vcs.log.Hash;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConflictSubRegion {
    private final Ref ref;
    private final int startLine;
    private final int length;
    private final List<Hash> commitsHistoryIds;

    public ConflictSubRegion(@NotNull Ref ref, int startLine, int length, @NotNull List<Hash> commitsHistoryIds) {
        this.ref = ref;
        this.startLine = startLine;
        this.length = length;
        this.commitsHistoryIds = commitsHistoryIds;
    }

    public Ref getRef() {
        return ref;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getLength() {
        return length;
    }

    public List<Hash> getCommitsHistoryIds() {
        return commitsHistoryIds;
    }
}
