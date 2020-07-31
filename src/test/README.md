### Test Cases
All test cases are found in `test/java` in their appropriate packages. Most
 tests use JUnit and extend other test classes including
  `BasePlatformTestCase` from Intelilj Platform and `GitSingleRepoTest` from
   Git4Idea.

[Official documentation](https://jetbrains.org/intellij/sdk/docs/basics/testing_plugins/testing_plugins.html)

### Mock Data
All mocked projects are created in `test/testData`. This includes mocked
 git configurations, branches, and commits.
 

An example project is available and utilized as a template for other projects.
Examples from JetBrains using Git4Idea are [also available for reference](https://github.com/JetBrains/intellij-community/tree/master/plugins/git4idea/testData).