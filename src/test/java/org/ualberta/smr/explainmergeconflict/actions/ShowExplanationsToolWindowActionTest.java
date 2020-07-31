package org.ualberta.smr.explainmergeconflict.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryCreator;
import git4idea.repo.GitRepositoryManager;
import git4idea.test.GitSingleRepoTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.ualberta.smr.explainmergeconflict.actions.ShowExplanationsToolWindowAction;

import java.io.File;
import java.io.IOException;

public class ShowExplanationsToolWindowActionTest extends BasePlatformTestCase {
    private String head;
//    GitSingleRepoTest
    private String projectPath;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

        @Override
        protected TempDirTestFixture createTempDirTestFixture() {
            return super.createTempDirTestFixture();
        }

        @Override
        protected Project getProject() {
            return super.getProject();
        }

        @Override
        public String getTestDataPath() {
            return "src/test/testData/exampleGitConflictProject";
    }

        public void testPresentationNotVisibleIfNoRepositoryFound() {
            // Note that there is currently no repository configured
            // Therefore, the assertion will pass
            myFixture.configureByFile("src/file1.txt");
            AnAction action = new ShowExplanationsToolWindowAction();
            Presentation presentation = myFixture.testAction(action);
            assertFalse(presentation.isVisible());
        }
}
