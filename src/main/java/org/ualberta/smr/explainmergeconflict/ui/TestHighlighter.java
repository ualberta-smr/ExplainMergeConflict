package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.ui.highlighters.VcsLogHighlighterFactory;
import com.intellij.vcs.log.util.VcsUserUtil;
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject;
import git4idea.repo.GitRepository;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.services.ConflictRegionHandler;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.*;

public class TestHighlighter implements VcsLogHighlighter {
    @NotNull private final VcsLogData myLogData;
    @NotNull private final VcsLogUi myLogUi;
    private boolean shouldHighlightCommits = false;

    public TestHighlighter(@NotNull VcsLogData logData, @NotNull VcsLogUi logUi) {
        myLogData = logData;
        myLogUi = logUi;
    }

    @NotNull
    @Override
    public VcsCommitStyle getStyle(int commitId, @NotNull VcsShortCommitDetails details, boolean isSelected) {
        // Highlight commits only if in merge conflict mode and enabled. Requires refresh though for main vcs log.
        if (myLogUi.isHighlighterEnabled(Factory.ID) && shouldHighlightCommits) {
            return VcsCommitStyleFactory.createStyle(JBColor.RED, JBColor.PanelBackground, TextStyle.BOLD);
        }

        return VcsCommitStyle.DEFAULT;

        // TODO - data.getProject() exists
//        Condition<Integer> condition = myLogData.getContainingBranchesGetter().getContainedInCurrentBranchCondition(details.getRoot());
//        Project project = Utils.getCurrentProject();
//        GitRepository repo = Utils.getCurrentRepository(project);
//        HashMap<String, ConflictFile> conflictsMap = MergeConflictService.getInstance(project).getConflictFiles();
//        VirtualFile file = Utils.getCurrentFileFromEditor(project);
//        Iterator iterator = conflictsMap.entrySet().iterator();
//
//        while (iterator.hasNext()) {
//            Map.Entry pair = (Map.Entry) iterator.next();
//            boolean isConflictFile = pair.getKey().equals(file.getPath());
//        }
//        if (!conflictsMap.isEmpty() && conflictsMap.containsKey(file.getPath())) {
////            System.out.println(conflictsMap);
//            ConflictFile conflictFile = conflictsMap.get(file.getPath());
//            ConflictRegionHandler.registerConflictsForFile(project, repo, file);
//            List<Hash> hashes = conflictFile.getConflictRegions().get(0).getP1().getCommitsHistoryIds();
//            int in = myLogData.getCommitIndex(hashes.get(0), repo.getRoot());
//            if (in == commitId) {
//                return VcsCommitStyleFactory.createStyle(JBColor.RED, JBColor.PanelBackground, TextStyle.BOLD);
////            return VcsCommitStyleFactory.background(CURRENT_BRANCH_BG);
//            }
//        }

//        return VcsCommitStyle.DEFAULT;
    }

    @Override
    public void update(@NotNull VcsLogDataPack dataPack, boolean refreshHappened) {
        System.out.println("update!");
        GitRepository repo = Utils.getCurrentRepository(myLogData.getProject());
        shouldHighlightCommits = Utils.isInConflictState(repo);
    }

    public static class Factory implements VcsLogHighlighterFactory {
        @NotNull public static final String ID = ExplainMergeConflictBundle.message("vcs.log.conflict.highlighter.id");

        @NotNull
        @Override
        public VcsLogHighlighter createHighlighter(@NotNull VcsLogData logData, @NotNull VcsLogUi logUi) {
            return new TestHighlighter(logData, logUi);
        }

        @NotNull
        @Override
        public String getId() {
            return ID;
        }

        @NotNull
        @Override
        public String getTitle() {
            return ExplainMergeConflictBundle.message("vcs.log.conflict.highlighter.label");
        }

        @Override
        public boolean showMenuItem() {
            return true;
        }
    }
}
