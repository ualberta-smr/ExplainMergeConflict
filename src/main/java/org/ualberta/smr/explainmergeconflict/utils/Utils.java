package org.ualberta.smr.explainmergeconflict.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

public class Utils {

    /**
     * Returns current repository instance.
     * @param project current project open in IntelliJ
     * @return current repository
     */
    public static GitRepository getCurrentRepository(Project project) {
        assert !GitRepositoryManager.getInstance(project)
                .getRepositories()
                .isEmpty();
        return GitRepositoryManager.getInstance(project)
                .getRepositories()
                .get(0);
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
