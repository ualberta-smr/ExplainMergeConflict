package org.ualberta.smr.explainmergeconflict;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;

public class ConflictRegion {
    private VirtualFile file;
    private int startLine;
    private int length;
//    private ConflictSubRegion p1;
//    private ConflictSubRegion p2;

    public ConflictRegion(VirtualFile file, ArrayList<Integer> lineNumPair) {
        this.file = file;
        this.startLine = lineNumPair.get(0);
        this.length = lineNumPair.get(1);
//        this.p1 = p1;
//        this.p2 = p2;
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

//    public ConflictSubRegion getP1() {
//        return p1;
//    }
//
//    public ConflictSubRegion getP2() {
//        return p2;
//    }
}
