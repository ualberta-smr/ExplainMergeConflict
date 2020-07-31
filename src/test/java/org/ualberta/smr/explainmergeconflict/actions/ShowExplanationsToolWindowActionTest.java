package org.ualberta.smr.explainmergeconflict.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;

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
