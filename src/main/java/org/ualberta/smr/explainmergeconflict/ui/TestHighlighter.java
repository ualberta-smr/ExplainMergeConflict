package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.ui.highlighters.VcsLogHighlighterFactory;
import com.intellij.vcs.log.util.VcsLogUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.ConflictRegion;
import org.ualberta.smr.explainmergeconflict.services.ConflictRegionHandler;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.*;

public class TestHighlighter implements VcsLogHighlighter {
    private static final VcsCommitStyle CONFLICT_STYLE = VcsCommitStyleFactory.createStyle(JBColor.RED, null, TextStyle.BOLD);
    @NotNull private final VcsLogData myLogData;
    @NotNull private final VcsLogUi myLogUi;
    @NotNull private final GitRepository repo;
    private final HashMap<ConflictRegion, List<Hash>> conflictsMap = new HashMap<>();
    private boolean shouldHighlightCommits = false;

    public TestHighlighter(@NotNull VcsLogData logData, @NotNull VcsLogUi logUi) {
        myLogData = logData;
        myLogUi = logUi;
        repo = Objects.requireNonNull(Utils.getCurrentRepository(myLogData.getProject()));
    }

    /**
     * Get the vcs commit style we should apply depending on whether or not there is a merge conflict + the conflict
     * commits highlighter is enabled through the presentation settings. Is called whenever we enable/disable a
     * highlighter and load commits. Is constantly updated.
     */
    @NotNull
    @Override
    public VcsCommitStyle getStyle(int commitId, @NotNull VcsShortCommitDetails details, boolean isSelected) {
        // Highlight commits only if in merge conflict mode and enabled. Might require refresh though for main vcs log.
        shouldHighlightCommits = Utils.isInConflictState(repo) && myLogUi.isHighlighterEnabled(Factory.ID);

        if (shouldHighlightCommits && !conflictsMap.isEmpty()) {
            Iterator iterator = conflictsMap.entrySet().iterator();

            // For each conflict region, see which commit has to be highlighted
            while(iterator.hasNext()) {
                Map.Entry pair = (Map.Entry) iterator.next();
                List<Hash> commits = (List<Hash>) pair.getValue();

                for (Hash hashId: commits) {
                    int index = myLogData.getCommitIndex(hashId, repo.getRoot());
                    if (index == commitId) return CONFLICT_STYLE;
                    // TODO condition if not found?
                }
            }
        }
        return VcsCommitStyle.DEFAULT;
    }

    private void readConflicts() {
        Project project = myLogData.getProject();

        // Filters
        VcsLogFilterCollection filters = myLogUi.getFilterUi().getFilters();
        VcsLogBranchFilter branchFilter  = filters.get(VcsLogFilterCollection.BRANCH_FILTER);
        VcsLogStructureFilter structureFilter = filters.get(VcsLogFilterCollection.STRUCTURE_FILTER);

        // If file path filter is not ALL
        if (structureFilter != null) {
            Collection<FilePath> filterFiles = structureFilter.getFiles();

            for (FilePath filterFile: filterFiles) {
                VirtualFile file = filterFile.getVirtualFile();

                if (Utils.isConflictFile(project, file)) {
                    updateCommitsToHighlight(project, file, branchFilter);
                }
            }
        } else { // Otherwise just clear highlighting for now if viewing for all files
            conflictsMap.clear();
        }
    }

    private void updateCommitsToHighlight(Project project, VirtualFile file, VcsLogBranchFilter branchFilter) {
        // FIXME - running this in toolwindow
        ConflictRegionHandler.registerConflictsForFile(project, repo, file);

        HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getInstance(project).getConflictFiles();
        ConflictFile conflictFile = conflictFiles.get(file.getPath());
        List<ConflictRegion> conflictRegions = conflictFile.getConflictRegions();

        // If branch is not ALL
        if (branchFilter != null) {
            if (branchFilter.matches(VcsLogUtil.HEAD) || branchFilter.matches(MergeConflictService.getHeadBranchName())) {
                for (ConflictRegion conflictRegion: conflictRegions) {
                    conflictsMap.put(conflictRegion, conflictRegion.getP1().getCommitsHistoryIds());
                }
            } else if (branchFilter.matches(MergeConflictService.getMergeBranchName())) {
                for (ConflictRegion conflictRegion: conflictRegions) {
                    conflictsMap.put(conflictRegion, conflictRegion.getP2().getCommitsHistoryIds());
                }
            } else {
                // Clear highlighting for any other branch that is not head or merge_head
                conflictsMap.clear();
            }
        } else {
            // Otherwise just highlight all conflict commits for the time being
            for (ConflictRegion conflictRegion: conflictRegions) {
                conflictsMap.put(conflictRegion, conflictRegion.getP1().getCommitsHistoryIds());
                conflictsMap.put(conflictRegion, conflictRegion.getP2().getCommitsHistoryIds());
            }
        }
    }

    /**
     * Runs whenever the log is filtered. This includes using the search bar, intellisort, and using the provided
     * filter options such as branch and date.
     * @param dataPack data such as refs and filters for the current log provider
     * @param refreshHappened true if the refresh action was enabled
     */
    @Override
    public void update(@NotNull VcsLogDataPack dataPack, boolean refreshHappened) {
        System.out.println("update!");
        // FIXME - using isEmpty as a condition to read conflicts won't work if we're changing filter to a new file
//        if (conflictsMap.isEmpty()) {
//
//        }
        readConflicts();
        // TODO figure out when to clear conflictsMap
    }

    public static class Factory implements VcsLogHighlighterFactory {
        @NotNull public static final String ID = "CONFLICT_COMMITS";

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
