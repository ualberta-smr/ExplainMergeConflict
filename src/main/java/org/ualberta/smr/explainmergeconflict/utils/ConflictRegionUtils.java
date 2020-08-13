package org.ualberta.smr.explainmergeconflict.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.ConflictRegion;
import org.ualberta.smr.explainmergeconflict.ConflictSubRegion;
import org.ualberta.smr.explainmergeconflict.Ref;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConflictRegionUtils {

    public static void registerConflictsForFile(@NotNull Project project, @NotNull GitRepository repo, @NotNull VirtualFile file) {
        assert Utils.isConflictFile(file);
        runDiffForFileAndThenUpdate(project, repo, file);
    }

    public static void showConflictRegionInEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull int nodeIndex) {
        assert isConflictRegionProperlyInitialized(file);
        updateDescriptor(project, file, nodeIndex);
    }

    /**
     * Runs the git diff command using {@link GitLineHandler} and reads the stdout output for additional parsing to find
     * conflict region data.
     * @param project current project
     * @param repo current Git repository
     * @param file currently opened file
     */
    private static void runDiffForFileAndThenUpdate(Project project, GitRepository repo, VirtualFile file) {
        /*
         * Rather than running the git command asynchronously as a background task, we should instead run it synchronously
         * so that we don't face any race conditions - particularly, when we try to read conflict file data while it
         * hasn't been processed yet. We do this using Task.Modal rather than Task.Backgroundable.
         *
         * For example, when we launch the Explain Merge Conflict tool window after running the show action, let the
         * tool window wait for the git handler to resolve so that we have data to show. Otherwise, we will display an
         * empty tree despite having already had initialized the conflict file data.
         */
        ProgressManager.getInstance().run(new Task.Modal(project, "explainmergeconflict: running git diff to detect conflict regions" + project.getName(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                /*
                 * Get diff view that only shows the conflicting regions for the current file
                 * Reference: Are Refactorings To Blame? An Empirical Study of Refactorings in Merge Conflicts
                 */
                GitLineHandler h = new GitLineHandler(project, repo.getRoot(), GitCommand.DIFF);
                h.addParameters("-U0");
                h.endOptions();
                h.addParameters(file.getPath());

                GitCommandResult result = Git.getInstance().runCommand(h);
                List<String> resultList = result.getOutput();

                registerConflictRegions(resultList, file);
            }
        });
    }

    /**
     * Initialize and register {@link ConflictRegion}s for the currently opened file under {@link ConflictFile}.
     * @param output git diff command output.
     * @param file current file
     */
    private static void registerConflictRegions(List<String> output, VirtualFile file) {
        ArrayList<String> filteredList = new ArrayList<>(output);
        filteredList.removeIf(e -> !e.startsWith("@@@") && !e.endsWith("@@@"));

        assert !filteredList.isEmpty();

        List<ConflictRegion> conflictRegionList = new ArrayList<>();

        // Create new ConflictRegion instances and initialize the appropriate data for each one
        final int regionIndex = 0;
        final int p1Index = 1;
        final int p2Index = 2;

        for (String region: filteredList) {
            List<List<Integer>> pairs = parseAndFindPairsForConflictRegion(region, file);
            ConflictSubRegion p1 = new ConflictSubRegion(
                    Ref.HEAD,
                    pairs.get(p1Index).get(0),
                    pairs.get(p1Index).get(1)
            );
            ConflictSubRegion p2 = new ConflictSubRegion(
                    Ref.MERGE_HEAD,
                    pairs.get(p2Index).get(0),
                    pairs.get(p2Index).get(1)
            );
            ConflictRegion conflictRegion = new ConflictRegion(
                    file,
                    pairs.get(regionIndex).get(0),
                    pairs.get(regionIndex).get(1),
                    p1,
                    p2
            );
            conflictRegionList.add(conflictRegion);
        }

        HashMap<String, ConflictFile> conflictsMap = MergeConflictService.getConflictFiles();
        ConflictFile conflictFile = conflictsMap.get(file.getPath());
        assert !conflictRegionList.isEmpty();
        conflictFile.setConflictRegions(conflictRegionList);
        // TODO simplify setting conflict files and remove repetition
        MergeConflictService.getConflictFiles().replace(file.getPath(), conflictFile);
    }

    /**
     * Parses through the git diff output from the git handler and gets the appropriate start line numbers and length
     * data for p1, p2, and the entire conflict region.
     *
     * Example:
     *
     * <<<<<<< HEAD           <-- line 1 of conflict region
     * hello                  <-- p1
     * =======
     * hello world            <-- p2
     * >>>>>>> conflictBranch <-- line 5 of conflict region
     *
     * Output = [@@@ -1,1 -1,1 +1,5 @@@]
     * conflict region pair = [1,5]
     * p1 pair = [1,1]
     * p2 pair = [1,1]
     * Return = [[1,5], [1,1], [1,1]]
     *
     * @param region conflict region line data in output
     * @param file current file
     * @return list of integer ArrayLists containing the conflict region pair, p1 pair, and p2 pair respectively
     */
    private static List<List<Integer>> parseAndFindPairsForConflictRegion(String region, VirtualFile file) {
        List<List<Integer>> pairs = new ArrayList<>();
        List<Integer> regionPair = new ArrayList<>();
        List<Integer> p1Pair = new ArrayList<>();
        List<Integer> p2Pair = new ArrayList<>();
        Pattern pattern;
        Matcher matcher;

        // Get region pair
        pattern = Pattern.compile("\\d+,\\d+\\s@");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            String pair = matcher.group()
                    .replaceAll("\\s@", "")
                    .trim();
            regionPair = convertPair(pair);
        }

        // Get p1 pair
        pattern = Pattern.compile("@\\s[-+]\\d+,\\d");
        matcher = pattern.matcher(region);
        if (matcher.find()) {
            String pair = matcher.group()
                    .replaceAll("@\\s[-+]", "")
                    .trim();
            p1Pair = convertPair(pair);
        }

        // Get p2 pair
        pattern = Pattern.compile("\\d\\s[-+]\\d+,\\d+\\s[-+]");
        matcher = pattern.matcher(region);
        if (matcher.find()) {
            // Extract "x,"
            String pairLeft = matcher.group()
                    .replaceAll("\\d\\s[-+]", "")
                    .trim();

            // Extract "y"
            String pairRight = matcher.group()
                    .replaceAll("\\d\\s[-+]\\d+,", "")
                    .trim();
            pairRight = pairRight.replaceAll("\\s[-+]", "");

            p2Pair = convertPair(pairLeft + pairRight);
        }

        pairs.add(regionPair);
        pairs.add(p1Pair);
        pairs.add(p2Pair);

        return pairs;
    }

    /**
     * Converts a String pair "x,y" into an ArrayList<Integer> [x,y] to be used by the descriptor and other functions
     * for determining the position of the (sub)conflict region in the code editor.
     *
     * x = start line number of the (sub)conflict region
     * y = of the (sub)conflict region
     *
     * @param pair String formatted as "x,y" where x is the startLine of the conflict region and y is its length.
     * @return an integer ArrayList containing the conflict region startLine as an integer on index 0, and the conflict
     * region length as an integer on index 1.
     */
    private static ArrayList<Integer> convertPair(String pair) {
        int startLine = 0;
        int length = 0;
        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile("\\d+,");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String startLineStr = matcher.group().replaceAll(",", "").trim();
            startLine = Integer.parseInt(startLineStr);
        }

        pattern = Pattern.compile(",\\d");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String lengthStr = matcher.group().replaceAll(",", "").trim();
            length = Integer.parseInt(lengthStr);
        }

        ArrayList<Integer> newPair = new ArrayList<>();
        newPair.add(0, startLine);
        newPair.add(1, length);

        return newPair;
    }

    /**
     * Verifies if a file (that is also a valid key for the conflict file hashmap for {@link MergeConflictService}) has
     * conflict regions. The function will fail if the file is not registered as a valid key for MergeConflictService's
     * conflict file hashmap.
     * @param file conflict file
     * @return true if conflict regions are initialized for the current file under MergeConflictService; otherwise false.
     */
    private static boolean isConflictRegionProperlyInitialized(VirtualFile file) {
        HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getConflictFiles();
        String key = file.getPath();
        assert conflictFiles.containsKey(key);
        List<ConflictRegion> conflictRegions = conflictFiles.get(key).getConflictRegions();

        // TODO - make sure if has subregions

        return !conflictRegions.isEmpty();
    }

    /**
     * Updates the cursor and current line in the code editor to point to the starting line of a conflict region.
     * @param project current project
     * @param file currently viewed file
     * @param nodeIndex index of the currently selected conflict node in ours/theirs tree. Order of conflict regions in
     *                  the tree is the same as the order of conflict regions under {@link MergeConflictService}.
     */
    private static void updateDescriptor(@NotNull Project project, @NotNull VirtualFile file, @NotNull int nodeIndex) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getConflictFiles();
                List<ConflictRegion> conflictRegions = conflictFiles.get(file.getPath()).getConflictRegions();
                int regionStartLine = conflictRegions.get(nodeIndex).getStartLine();

                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, regionStartLine-1, 0);
                descriptor.navigateInEditor(project, true);
            }
        });
    }
}
