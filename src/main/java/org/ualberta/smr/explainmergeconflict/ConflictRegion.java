package org.ualberta.smr.explainmergeconflict;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ConflictRegion {
    private final VirtualFile file;
    private final int startLine;
    private final int length;
    private final ConflictSubRegion p1;
    private final ConflictSubRegion p2;

    public ConflictRegion(@NotNull VirtualFile file,int startLine, int length, @NotNull ConflictSubRegion p1, @NotNull ConflictSubRegion p2) {
        this.file = file;
        this.startLine = startLine;
        this.length = length;
        this.p1 = p1;
        this.p2 = p2;
    }

    public VirtualFile getFile() {
        return file;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getLength() {
        return length;
    }

    public ConflictSubRegion getP1() {
        return p1;
    }

    public ConflictSubRegion getP2() {
        return p2;
    }
}
