package org.ualberta.smr.explainmergeconflict;

public class ConflictSubRegion {
    private String ref;
    private int startLine;
    private int length;

    public ConflictSubRegion(String ref, int startLine, int length) {
        this.ref = ref;
        this.startLine = startLine;
        this.length = length;
    }

    public String getRef() {
        return ref;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getLength() {
        return length;
    }
}
