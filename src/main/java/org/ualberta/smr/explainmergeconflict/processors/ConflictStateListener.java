package org.ualberta.smr.explainmergeconflict.processors;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Disposer;
import git4idea.repo.GitRepository;
import git4idea.status.GitStagingAreaHolder;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.services.UIController;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

/**
 * Used by ExplainMergeConflict to listen for merge conflicts. Once a
 * conflict is detected, register the Explain Merge Conflict tool window and
 * set the Explain Merge Conflict action to visible in context menus.
 */
public class ConflictStateListener implements GitStagingAreaHolder.StagingAreaListener {
    private MergeConflictService mergeConflictService = null;

    @Override
    public void stagingAreaChanged(@NotNull GitRepository repository) {
        if (!Utils.isInConflictState(repository)) {
            System.out.println("No conflicts detected");

            /*
             * In the case where the merge conflict service is active, but we are not in a conflict state, we should
             * unset the variable pointing to the service and dispose it. This occurs because we aborted an existing
             * merge conflict or resolved it. Now that we no longer need the service, we can just remove it.
             */
            if (mergeConflictService != null) {
                Disposer.dispose(mergeConflictService);
                mergeConflictService = null;
            }

        } else {
            System.out.println("Conflicts detected");

            // Once a conflict is detected, initialize the service and determine which files + commits are affected.
            if (mergeConflictService == null) {
                System.out.println("Triggering Merge Conflict Service!");
                mergeConflictService = MergeConflictService.getInstance(repository.getProject());
                assert mergeConflictService != null;
            }
        }
    }
}
