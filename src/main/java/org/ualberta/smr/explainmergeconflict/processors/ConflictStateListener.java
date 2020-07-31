package org.ualberta.smr.explainmergeconflict.processors;

import git4idea.repo.GitRepository;
import git4idea.status.GitStagingAreaHolder;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.UIController;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

/**
 * Used by ExplainMergeConflict to listen for merge conflicts. Once a
 * conflict is detected, register the Explain Merge Conflict tool window and
 * set the Explain Merge Conflict action to visible in context menus.
 */
public class ConflictStateListener implements GitStagingAreaHolder.StagingAreaListener {
    @Override
    public void stagingAreaChanged(@NotNull GitRepository repository) {
        System.out.println("Conflict change listener triggered from " +
                "ConflictStateListener service");

        if (!Utils.isInConflictState(repository)) {
            System.out.println("No conflicts detected");
            UIController.updateToolWindowAfterNonConflictState(repository);
        } else {
            System.out.println("Conflicts detected");
        }
    }
}
