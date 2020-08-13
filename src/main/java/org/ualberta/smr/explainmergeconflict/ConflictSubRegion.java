package org.ualberta.smr.explainmergeconflict;

public class ConflictSubRegion {
    private final Ref ref;
    private final int startLine;
    private final int length;

    public ConflictSubRegion(Ref ref, int startLine, int length) {
        this.ref = ref;
        this.startLine = startLine;
        this.length = length;
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
}
