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
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.*;

public class TestHighlighter implements VcsLogHighlighter {
    @NotNull private final VcsLogData myLogData;
    @NotNull private final VcsLogUi myLogUi;
    private boolean myShouldHighlightUser = false;

    public TestHighlighter(@NotNull VcsLogData logData, @NotNull VcsLogUi logUi) {
        myLogData = logData;
        myLogUi = logUi;
    }

    @NotNull
    @Override
    public VcsCommitStyle getStyle(int commitId, @NotNull VcsShortCommitDetails details, boolean isSelected) {
        // TODO currentbranchhighter, mycommitshighlighter
        if (!myLogUi.isHighlighterEnabled(Factory.ID)) return VcsCommitStyle.DEFAULT;
        if (myLogUi.isHighlighterEnabled(Factory.ID) && isSelected) return VcsCommitStyleFactory.createStyle(JBColor.RED, JBColor.RED, TextStyle.NORMAL);
        return VcsCommitStyleFactory.createStyle(JBColor.RED, JBColor.PanelBackground, TextStyle.BOLD);

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
//        myShouldHighlightUser = !isSingleUser() && !isFilteredByCurrentUser(dataPack.getFilters());
//        System.out.println(myShouldHighlightUser);
        System.out.println("update!");
    }

    // returns true if only one user commits to this repository
    private boolean isSingleUser() {
        THashSet<VcsUser> users = new THashSet<>(myLogData.getCurrentUser().values(), new VcsUserUtil.VcsUserHashingStrategy());
        return myLogData.getUserRegistry().all(user -> users.contains(user));
    }

    // returns true if filtered by "me"
    private static boolean isFilteredByCurrentUser(@NotNull VcsLogFilterCollection filters) {
        VcsLogUserFilter userFilter = filters.get(VcsLogFilterCollection.USER_FILTER);
        if (userFilter == null) return false;
        if (Collections.singleton(VcsLogFilterObject.ME).containsAll(userFilter.getValuesAsText())) return true;
        return false;
    }

    public static class Factory implements VcsLogHighlighterFactory {
        @NotNull public static final String ID = "CONFLICT_COMMITS"; // NON-NLS

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
            return "Conflict Commits";
        }

        @Override
        public boolean showMenuItem() {
            return true;
        }
    }
}
