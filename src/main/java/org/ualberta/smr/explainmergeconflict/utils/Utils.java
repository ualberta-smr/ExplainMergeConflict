package org.ualberta.smr.explainmergeconflict.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

public class Utils {

    /**
     * Returns true if a conflict is raised and a repository is in a conflict
     * state.
     * @param repo the current repository
     * @return boolean true if conflicts exist; otherwise false
     */
    public static boolean isInConflictState(GitRepository repo) {
        return !repo.getConflictsHolder().getConflicts().isEmpty();
    }

    /**
     * Returns true if Git is running and a repository exists.
     * @param project the current project
     * @return boolean true if git repository; otherwise false
     */
    public static boolean isInGitRepository(Project project) {
        return !GitRepositoryManager.getInstance(project).getRepositories().isEmpty();
    }

    /**
     * Returns current repository instance.
     * @param project current project open in IntelliJ
     * @return current repository
     */
    public static GitRepository getCurrentRepository(Project project) {
        if (!isInGitRepository(project)) {
            return null;
        }
        return GitRepositoryManager.getInstance(project)
                .getRepositories()
                .get(0);
    }

    public static String getRevisionShortAsString(Project project,
                                                    GitRepository repo,
                                                    String headOrMergeHead) throws VcsException {
        GitLineHandler h = new GitLineHandler(project, repo.getRoot(),
                GitCommand.REV_PARSE);
        h.addParameters("--short");
        h.addParameters(headOrMergeHead);
        h.endOptions();

        return Git.getInstance().runCommand(h).getOutputOrThrow();
    }

    /**
     * Returns an instance of the currently viewed file in the code editor.
     * @param project current project open in IntelliJ
     * @return current file as {@link VirtualFile}
     */
    public static VirtualFile getCurrentFileFromEditor(Project project) {
        // reference: // reference: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206795775-Get-current-Project-current-file-in-editor
        Editor editor =
                FileEditorManager.getInstance(project).getSelectedTextEditor();
        assert editor != null;
        Document doc = editor.getDocument();
        return FileDocumentManager.getInstance().getFile(doc);
    }
}
